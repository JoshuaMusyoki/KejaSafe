package com.rotd.havenpad.listing

import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.RoommateMatch
import com.rotd.havenpad.RoommateMatchGuest
import com.rotd.havenpad.RoommateMatchStatus
import com.rotd.havenpad.RoommatePreferences
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.RoommateMatchesTable
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Automatic roommate pairing for pending [seekingRoommate] bookings on the same listing.
 *
 * Score = preference Jaccard (50%) + date overlap (30%) + budget fit (20%).
 * Emits a [RoommateMatchStatus.SUGGESTED] match when score >= [SCORE_THRESHOLD].
 */
object RoommateMatchingService {

    const val SCORE_THRESHOLD = 60

    private data class Seeker(
        val bookingId: String,
        val userId: String?,
        val name: String,
        val startMs: Long,
        val endMs: Long,
        val budgetMax: Double?,
        val prefs: Set<String>,
        val major: String?,
        val yearOfStudy: Int?
    )

    fun tryMatchListing(listingId: String) {
        transaction {
            matchListingInternal(listingId)
        }
    }

    /** Release any SUGGESTED match linked to this booking so peers can rematch. */
    fun releaseBooking(bookingId: String) {
        transaction {
            val row = ListingBookingsTable.selectAll()
                .where { ListingBookingsTable.id eq bookingId }
                .singleOrNull()
                ?: return@transaction
            val matchId = row[ListingBookingsTable.roommateMatchId] ?: return@transaction
            val listingId = row[ListingBookingsTable.listingId]
            declineMatchInternal(matchId, clearOnlyIfSuggested = true)
            matchListingInternal(listingId)
        }
    }

    fun getMatch(matchId: String, landlordId: String): RoommateMatch? = transaction {
        loadMatch(matchId, landlordId)
    }

    fun approveMatch(matchId: String, landlordId: String): RoommateMatch = transaction {
        val matchRow = RoommateMatchesTable.selectAll()
            .where { RoommateMatchesTable.id eq matchId }
            .singleOrNull()
            ?: error("Match not found")
        if (matchRow[RoommateMatchesTable.landlordId] != landlordId) {
            error("Not your listing match")
        }
        if (matchRow[RoommateMatchesTable.status] != RoommateMatchStatus.SUGGESTED) {
            error("Only suggested matches can be approved")
        }

        val listingId = matchRow[RoommateMatchesTable.listingId]
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")
        val units = listing[ListingsTable.unitsCount].coerceAtLeast(1)
        val active = ListingService.recountActiveBookings(listingId)
        if (active + 1 > units) {
            error("Cannot approve — no open units left for a shared room")
        }

        val bookingAId = matchRow[RoommateMatchesTable.bookingAId]
        val bookingBId = matchRow[RoommateMatchesTable.bookingBId]
        listOf(bookingAId, bookingBId).forEach { bid ->
            val booking = ListingBookingsTable.selectAll()
                .where { ListingBookingsTable.id eq bid }
                .singleOrNull()
                ?: error("Booking $bid not found")
            if (booking[ListingBookingsTable.status] != BookingStatus.PENDING) {
                error("Both bookings must still be pending")
            }
        }

        // Primary keeps 1 slot; secondary shares the unit (0 capacity).
        ListingBookingsTable.update({ ListingBookingsTable.id eq bookingAId }) {
            it[status] = BookingStatus.CONFIRMED
            it[slotCount] = 1
            it[roommateMatchId] = matchId
        }
        ListingBookingsTable.update({ ListingBookingsTable.id eq bookingBId }) {
            it[status] = BookingStatus.CONFIRMED
            it[slotCount] = 0
            it[roommateMatchId] = matchId
        }
        RoommateMatchesTable.update({ RoommateMatchesTable.id eq matchId }) {
            it[status] = RoommateMatchStatus.ACCEPTED
        }
        ListingService.recountActiveBookings(listingId)
        loadMatch(matchId, landlordId) ?: error("Match disappeared")
    }

    fun declineMatch(matchId: String, landlordId: String): RoommateMatch = transaction {
        val matchRow = RoommateMatchesTable.selectAll()
            .where { RoommateMatchesTable.id eq matchId }
            .singleOrNull()
            ?: error("Match not found")
        if (matchRow[RoommateMatchesTable.landlordId] != landlordId) {
            error("Not your listing match")
        }
        if (matchRow[RoommateMatchesTable.status] != RoommateMatchStatus.SUGGESTED) {
            error("Only suggested matches can be declined")
        }
        val listingId = matchRow[RoommateMatchesTable.listingId]
        declineMatchInternal(matchId, clearOnlyIfSuggested = false)
        matchListingInternal(listingId)
        loadMatch(matchId, landlordId) ?: error("Match disappeared")
    }

    fun suggestedMatchesForLandlord(landlordId: String): List<RoommateMatch> = transaction {
        RoommateMatchesTable.selectAll()
            .where {
                (RoommateMatchesTable.landlordId eq landlordId) and
                    (RoommateMatchesTable.status eq RoommateMatchStatus.SUGGESTED)
            }
            .mapNotNull { row -> loadMatch(row[RoommateMatchesTable.id], landlordId) }
    }

    private fun matchListingInternal(listingId: String) {
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return
        val price = listing[ListingsTable.pricePerMonth]
        val landlordId = listing[ListingsTable.landlordId]

        val seekers = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId eq listingId) and
                    (ListingBookingsTable.status eq BookingStatus.PENDING) and
                    (ListingBookingsTable.seekingRoommate eq true) and
                    ListingBookingsTable.roommateMatchId.isNull()
            }
            .map { row -> row.toSeeker() }
            .filter { it.userId != null }

        if (seekers.size < 2) return

        val paired = mutableSetOf<String>()
        // Greedy: highest-scoring unpaired pairs first.
        val candidates = mutableListOf<Triple<Seeker, Seeker, Int>>()
        for (i in seekers.indices) {
            for (j in i + 1 until seekers.size) {
                val a = seekers[i]
                val b = seekers[j]
                if (a.userId == b.userId) continue
                if (!datesOverlap(a.startMs, a.endMs, b.startMs, b.endMs)) continue
                val score = scorePair(a, b, price)
                if (score >= SCORE_THRESHOLD) {
                    candidates += Triple(a, b, score)
                }
            }
        }
        candidates.sortByDescending { it.third }

        for ((a, b, score) in candidates) {
            if (a.bookingId in paired || b.bookingId in paired) continue
            if (wasDeclined(a.bookingId, b.bookingId)) continue
            val matchId = UUID.randomUUID().toString()
            RoommateMatchesTable.insert {
                it[id] = matchId
                it[RoommateMatchesTable.listingId] = listingId
                it[RoommateMatchesTable.landlordId] = landlordId
                it[bookingAId] = a.bookingId
                it[bookingBId] = b.bookingId
                it[RoommateMatchesTable.score] = score
                it[status] = RoommateMatchStatus.SUGGESTED
                it[createdAt] = System.currentTimeMillis()
            }
            ListingBookingsTable.update({ ListingBookingsTable.id eq a.bookingId }) {
                it[roommateMatchId] = matchId
            }
            ListingBookingsTable.update({ ListingBookingsTable.id eq b.bookingId }) {
                it[roommateMatchId] = matchId
            }
            paired += a.bookingId
            paired += b.bookingId
        }
    }

    private fun wasDeclined(bookingAId: String, bookingBId: String): Boolean {
        return RoommateMatchesTable.selectAll()
            .where { RoommateMatchesTable.status eq RoommateMatchStatus.DECLINED }
            .any { row ->
                val a = row[RoommateMatchesTable.bookingAId]
                val b = row[RoommateMatchesTable.bookingBId]
                (a == bookingAId && b == bookingBId) || (a == bookingBId && b == bookingAId)
            }
    }

    private fun declineMatchInternal(matchId: String, clearOnlyIfSuggested: Boolean) {
        val matchRow = RoommateMatchesTable.selectAll()
            .where { RoommateMatchesTable.id eq matchId }
            .singleOrNull()
            ?: return
        if (clearOnlyIfSuggested && matchRow[RoommateMatchesTable.status] != RoommateMatchStatus.SUGGESTED) {
            return
        }
        val a = matchRow[RoommateMatchesTable.bookingAId]
        val b = matchRow[RoommateMatchesTable.bookingBId]
        listOf(a, b).forEach { bid ->
            ListingBookingsTable.update({ ListingBookingsTable.id eq bid }) {
                it[roommateMatchId] = null
            }
        }
        if (matchRow[RoommateMatchesTable.status] == RoommateMatchStatus.SUGGESTED) {
            RoommateMatchesTable.update({ RoommateMatchesTable.id eq matchId }) {
                it[status] = RoommateMatchStatus.DECLINED
            }
        }
    }

    private fun loadMatch(matchId: String, landlordId: String): RoommateMatch? {
        val matchRow = RoommateMatchesTable.selectAll()
            .where { RoommateMatchesTable.id eq matchId }
            .singleOrNull()
            ?: return null
        if (matchRow[RoommateMatchesTable.landlordId] != landlordId) return null

        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq matchRow[RoommateMatchesTable.listingId] }
            .singleOrNull()

        fun guest(bookingId: String): RoommateMatchGuest? {
            val row = ListingBookingsTable.selectAll()
                .where { ListingBookingsTable.id eq bookingId }
                .singleOrNull()
                ?: return null
            val userId = row[ListingBookingsTable.guestUserId]
            val user = userId?.let { uid ->
                UsersTable.selectAll().where { UsersTable.id eq uid }.singleOrNull()
            }
            return RoommateMatchGuest(
                bookingId = bookingId,
                userId = userId,
                name = row[ListingBookingsTable.guestName],
                preferences = RoommatePreferences.normalize(user?.get(UsersTable.roommatePreferences)),
                startDateMillis = row[ListingBookingsTable.startDateMillis],
                endDateMillis = row[ListingBookingsTable.endDateMillis],
                budgetMax = row[ListingBookingsTable.budgetMax],
                major = user?.get(UsersTable.major),
                yearOfStudy = user?.get(UsersTable.yearOfStudy)
            )
        }

        val guests = listOfNotNull(
            guest(matchRow[RoommateMatchesTable.bookingAId]),
            guest(matchRow[RoommateMatchesTable.bookingBId])
        )
        return RoommateMatch(
            id = matchId,
            listingId = matchRow[RoommateMatchesTable.listingId],
            listingTitle = listing?.get(ListingsTable.title).orEmpty(),
            landlordId = landlordId,
            guests = guests,
            score = matchRow[RoommateMatchesTable.score],
            status = matchRow[RoommateMatchesTable.status],
            pricePerMonth = listing?.get(ListingsTable.pricePerMonth),
            currency = listing?.get(ListingsTable.currency) ?: "KES"
        )
    }

    private fun ResultRow.toSeeker(): Seeker {
        val userId = this[ListingBookingsTable.guestUserId]
        val user = userId?.let { uid ->
            UsersTable.selectAll().where { UsersTable.id eq uid }.singleOrNull()
        }
        return Seeker(
            bookingId = this[ListingBookingsTable.id],
            userId = userId,
            name = this[ListingBookingsTable.guestName],
            startMs = this[ListingBookingsTable.startDateMillis],
            endMs = this[ListingBookingsTable.endDateMillis],
            budgetMax = this[ListingBookingsTable.budgetMax],
            prefs = RoommatePreferences.normalizeSet(
                RoommatePreferences.normalize(user?.get(UsersTable.roommatePreferences))
            ),
            major = user?.get(UsersTable.major),
            yearOfStudy = user?.get(UsersTable.yearOfStudy)
        )
    }

    private fun scorePair(a: Seeker, b: Seeker, listingPrice: Double?): Int {
        val prefScore = RoommatePreferences.jaccard(a.prefs, b.prefs)
        val dateScore = dateOverlapRatio(a.startMs, a.endMs, b.startMs, b.endMs)
        val hasBudget = a.budgetMax != null || b.budgetMax != null
        val budgetScore = if (hasBudget) budgetFit(a.budgetMax, b.budgetMax, listingPrice) else 0.0

        val total = if (hasBudget) {
            prefScore * 0.50 + dateScore * 0.30 + budgetScore * 0.20
        } else {
            prefScore * (0.50 / 0.80) + dateScore * (0.30 / 0.80)
        }
        return (total * 100).roundToInt().coerceIn(0, 100)
    }

    // Exposed for unit-style calls with raw fields from ListingService tests if needed.
    fun scoreForTest(
        prefsA: List<String>,
        prefsB: List<String>,
        startA: Long,
        endA: Long,
        startB: Long,
        endB: Long,
        budgetA: Double?,
        budgetB: Double?,
        listingPrice: Double?
    ): Int = scorePair(
        Seeker("a", "u1", "A", startA, endA, budgetA, RoommatePreferences.normalizeSet(prefsA), null, null),
        Seeker("b", "u2", "B", startB, endB, budgetB, RoommatePreferences.normalizeSet(prefsB), null, null),
        listingPrice
    )

    private fun datesOverlap(startA: Long, endA: Long, startB: Long, endB: Long): Boolean =
        startA <= endB && startB <= endA

    private fun dateOverlapRatio(startA: Long, endA: Long, startB: Long, endB: Long): Double {
        val overlapStart = max(startA, startB)
        val overlapEnd = min(endA, endB)
        if (overlapEnd < overlapStart) return 0.0
        val overlap = (overlapEnd - overlapStart + 1).toDouble()
        val spanA = (endA - startA + 1).toDouble().coerceAtLeast(1.0)
        val spanB = (endB - startB + 1).toDouble().coerceAtLeast(1.0)
        return overlap / min(spanA, spanB)
    }

    private fun budgetFit(budgetA: Double?, budgetB: Double?, listingPrice: Double?): Double {
        val price = listingPrice ?: return peerBudgetProximity(budgetA, budgetB)
        fun fit(budget: Double?): Double {
            if (budget == null || budget <= 0) return 0.5
            val ratio = abs(budget - price) / price.coerceAtLeast(1.0)
            return (1.0 - min(ratio, 1.0)).coerceIn(0.0, 1.0)
        }
        return (fit(budgetA) + fit(budgetB) + peerBudgetProximity(budgetA, budgetB)) / 3.0
    }

    private fun peerBudgetProximity(budgetA: Double?, budgetB: Double?): Double {
        if (budgetA == null || budgetB == null || budgetA <= 0 || budgetB <= 0) return 0.5
        val mid = (budgetA + budgetB) / 2.0
        val ratio = abs(budgetA - budgetB) / mid.coerceAtLeast(1.0)
        return (1.0 - min(ratio, 1.0)).coerceIn(0.0, 1.0)
    }
}
