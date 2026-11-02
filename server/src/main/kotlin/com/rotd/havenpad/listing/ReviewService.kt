package com.rotd.havenpad.listing

import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.FollowUpReviewRequest
import com.rotd.havenpad.ListingReviewPage
import com.rotd.havenpad.MyReviews
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.ReplyReviewRequest
import com.rotd.havenpad.ReportReviewRequest
import com.rotd.havenpad.Reputation
import com.rotd.havenpad.ReviewComposer
import com.rotd.havenpad.ReviewReportStatus
import com.rotd.havenpad.ReviewStudentOption
import com.rotd.havenpad.ReviewSubject
import com.rotd.havenpad.StayReview
import com.rotd.havenpad.SubmitReviewRequest
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.trust.HouseScore
import com.rotd.havenpad.trust.rankByAverageRating
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.StayReviewsTable
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object ReviewService {
    private const val SUBJECT_LANDLORD = "LANDLORD"
    private const val SUBJECT_STUDENT = "STUDENT"

    fun applyLandlordRank(listings: List<PropertyListing>): List<PropertyListing> {
        if (listings.isEmpty()) return listings
        val scores = houseScores()
        return listings.map { listing ->
            val score = scores[listing.id]
            listing.copy(
                reviewPercent = score?.percent ?: 0,
                reviewCount = score?.count ?: 0,
                landlordRank = score?.rank,
            )
        }.sortedWith(
            compareByDescending<PropertyListing> { it.reviewCount > 0 }
                .thenByDescending { it.reviewPercent }
                .thenByDescending { it.reviewCount }
                .thenBy { it.landlordRank ?: Int.MAX_VALUE }
                .thenByDescending { it.createdAt }
        )
    }

    fun listingPage(listingId: String): ListingReviewPage = transaction {
        val listing = listingRow(listingId)
        val landlordId = listing[ListingsTable.landlordId]
        val score = houseScores()[listingId]
        val reviews = StayReviewsTable.selectAll()
            .where {
                (StayReviewsTable.subject eq SUBJECT_LANDLORD) and
                    (StayReviewsTable.listingId eq listingId)
            }
            .map { it.toReview(includeFollowUp = false) }
            .sortedByDescending { it.createdAt }
        ListingReviewPage(
            listingId = listingId,
            landlordId = landlordId,
            landlordName = nameOf(landlordId),
            percent = score?.percent ?: 0,
            reviewCount = score?.count ?: 0,
            rank = score?.rank,
            reviews = reviews,
        )
    }

    fun composer(userId: String, listingId: String): ReviewComposer = transaction {
        val listing = listingRow(listingId)
        val landlordId = listing[ListingsTable.landlordId]
        val role = roleOf(userId)
        val mine = StayReviewsTable.selectAll()
            .where {
                (StayReviewsTable.subject eq SUBJECT_LANDLORD) and
                    (StayReviewsTable.listingId eq listingId) and
                    (StayReviewsTable.authorId eq userId)
            }
            .singleOrNull()
            ?.toReview(includeFollowUp = true)
        val stayed = role == UserRole.STUDENT && hasStay(listingId, userId)
        val students = if (landlordId == userId && (role == UserRole.LANDLORD || role == UserRole.ADMIN)) {
            stayedStudents(listingId, userId)
        } else {
            emptyList()
        }
        ReviewComposer(
            canReviewHouse = stayed && mine == null,
            isHouseLandlord = landlordId == userId,
            students = students,
            myHouseReview = mine,
        )
    }

    fun mine(userId: String): MyReviews = transaction {
        val role = roleOf(userId)
        val subject = if (role == UserRole.STUDENT) ReviewSubject.STUDENT else ReviewSubject.LANDLORD
        val about = reputation(userId, subject)
        val followUps = StayReviewsTable.selectAll()
            .where { StayReviewsTable.authorId eq userId }
            .map { it.toReview(includeFollowUp = true) }
            .filter { it.followUpNote.isNotBlank() }
            .sortedByDescending { it.createdAt }
        MyReviews(aboutMe = about, followUps = followUps)
    }

    fun submit(userId: String, request: SubmitReviewRequest): StayReview = transaction {
        val listing = listingRow(request.listingId)
        val landlordId = listing[ListingsTable.landlordId]
        val rating = request.rating
        require(rating in 1..5) { "Choose a rating from 1 to 5." }
        val body = request.body.trim().take(1000)
        val role = roleOf(userId)
        val now = System.currentTimeMillis()
        when (request.subject) {
            ReviewSubject.LANDLORD -> {
                require(role == UserRole.STUDENT) { "Students review the landlord after a stay." }
                require(hasStay(request.listingId, userId)) {
                    "You can review this landlord after your stay is confirmed."
                }
                require(existingHouseReview(request.listingId, userId) == null) {
                    "You already reviewed this house."
                }
                val id = UUID.randomUUID().toString()
                StayReviewsTable.insert {
                    it[StayReviewsTable.id] = id
                    it[StayReviewsTable.subject] = SUBJECT_LANDLORD
                    it[StayReviewsTable.listingId] = request.listingId
                    it[StayReviewsTable.landlordId] = landlordId
                    it[StayReviewsTable.studentId] = userId
                    it[StayReviewsTable.authorId] = userId
                    it[StayReviewsTable.rating] = rating
                    it[StayReviewsTable.body] = body
                    it[StayReviewsTable.createdAt] = now
                }
                loadReview(id, includeFollowUp = true)
            }
            ReviewSubject.STUDENT -> {
                require(role == UserRole.LANDLORD || role == UserRole.ADMIN) {
                    "Landlords review students who stayed in the house."
                }
                require(landlordId == userId) { "You can review students on your own houses." }
                val studentId = request.studentId.trim()
                require(studentId.isNotBlank()) { "Choose the student to review." }
                require(hasStay(request.listingId, studentId)) {
                    "That student does not have a confirmed stay in this house."
                }
                val already = StayReviewsTable.selectAll().where {
                    (StayReviewsTable.subject eq SUBJECT_STUDENT) and
                        (StayReviewsTable.listingId eq request.listingId) and
                        (StayReviewsTable.authorId eq userId) and
                        (StayReviewsTable.studentId eq studentId)
                }.count() > 0
                require(!already) { "You already reviewed this student for this house." }
                val id = UUID.randomUUID().toString()
                StayReviewsTable.insert {
                    it[StayReviewsTable.id] = id
                    it[StayReviewsTable.subject] = SUBJECT_STUDENT
                    it[StayReviewsTable.listingId] = request.listingId
                    it[StayReviewsTable.landlordId] = userId
                    it[StayReviewsTable.studentId] = studentId
                    it[StayReviewsTable.authorId] = userId
                    it[StayReviewsTable.rating] = rating
                    it[StayReviewsTable.body] = body
                    it[StayReviewsTable.createdAt] = now
                }
                loadReview(id, includeFollowUp = true)
            }
        }
    }

    fun reply(userId: String, reviewId: String, request: ReplyReviewRequest): StayReview = transaction {
        val row = reviewRow(reviewId)
        val text = request.reply.trim().take(1000)
        require(text.isNotBlank()) { "Write a reply first." }
        val allowed = when (row[StayReviewsTable.subject]) {
            SUBJECT_LANDLORD -> row[StayReviewsTable.landlordId] == userId
            SUBJECT_STUDENT -> row[StayReviewsTable.studentId] == userId
            else -> false
        }
        require(allowed) { "Only the person who was reviewed can reply." }
        StayReviewsTable.update({ StayReviewsTable.id eq reviewId }) {
            it[StayReviewsTable.reply] = text
            it[StayReviewsTable.replyAt] = System.currentTimeMillis()
        }
        loadReview(reviewId, includeFollowUp = true)
    }

    fun report(userId: String, reviewId: String, request: ReportReviewRequest): StayReview = transaction {
        val row = reviewRow(reviewId)
        require(row[StayReviewsTable.authorId] != userId) { "You cannot report your own review." }
        val reason = request.reason.trim().take(500)
        require(reason.isNotBlank()) { "Say what is wrong with this review." }
        StayReviewsTable.update({ StayReviewsTable.id eq reviewId }) {
            it[StayReviewsTable.reportStatus] = ReviewReportStatus.OPEN.name
            it[StayReviewsTable.reportReason] = reason
            it[StayReviewsTable.reportedAt] = System.currentTimeMillis()
        }
        loadReview(reviewId, includeFollowUp = false)
    }

    fun openReports(): List<StayReview> = transaction {
        StayReviewsTable.selectAll()
            .map { it.toReview(includeFollowUp = true) }
            .filter { isActionableReport(it.reportStatus, it.reportReason) }
            .sortedByDescending { it.createdAt }
    }

    fun followUp(reviewId: String, request: FollowUpReviewRequest): StayReview = transaction {
        reviewRow(reviewId)
        val note = request.note.trim().take(1000)
        require(note.length >= 8) { "Write what you told the student." }
        StayReviewsTable.update({ StayReviewsTable.id eq reviewId }) {
            it[StayReviewsTable.reportStatus] = ReviewReportStatus.FOLLOWED_UP.name
            it[StayReviewsTable.followUpNote] = note
        }
        loadReview(reviewId, includeFollowUp = true)
    }

    fun dismiss(reviewId: String): StayReview = transaction {
        reviewRow(reviewId)
        StayReviewsTable.update({ StayReviewsTable.id eq reviewId }) {
            it[StayReviewsTable.reportStatus] = ReviewReportStatus.DISMISSED.name
        }
        loadReview(reviewId, includeFollowUp = true)
    }

    fun delete(reviewId: String) {
        transaction {
            val removed = StayReviewsTable.deleteWhere { StayReviewsTable.id eq reviewId }
            if (removed == 0) error("Review was not found.")
        }
    }

    private fun reputation(userId: String, subject: ReviewSubject): Reputation {
        val columnMatch = if (subject == ReviewSubject.LANDLORD) {
            (StayReviewsTable.subject eq SUBJECT_LANDLORD) and (StayReviewsTable.landlordId eq userId)
        } else {
            (StayReviewsTable.subject eq SUBJECT_STUDENT) and (StayReviewsTable.studentId eq userId)
        }
        val reviews = StayReviewsTable.selectAll()
            .where { columnMatch }
            .map { it.toReview(includeFollowUp = false) }
            .sortedByDescending { it.createdAt }
        val scores = if (subject == ReviewSubject.LANDLORD) landlordScores() else studentScores()
        val score = scores[userId]
        return Reputation(
            userId = userId,
            subject = subject,
            percent = score?.percent ?: 0,
            reviewCount = score?.count ?: reviews.size,
            rank = score?.rank,
            reviews = reviews,
        )
    }

    private fun landlordScores(): Map<String, HouseScore> =
        scoresFor(SUBJECT_LANDLORD) { it[StayReviewsTable.landlordId] }

    private fun houseScores(): Map<String, HouseScore> =
        scoresFor(SUBJECT_LANDLORD) { it[StayReviewsTable.listingId] }

    private fun studentScores(): Map<String, HouseScore> =
        scoresFor(SUBJECT_STUDENT) { it[StayReviewsTable.studentId] }

    private fun scoresFor(
        subject: String,
        owner: (ResultRow) -> String,
    ): Map<String, HouseScore> {
        val grouped = StayReviewsTable.selectAll()
            .where { StayReviewsTable.subject eq subject }
            .groupBy(owner)
            .mapValues { (_, rows) -> rows.map { it[StayReviewsTable.rating] } }
        return rankByAverageRating(grouped)
    }

    private fun listingRow(listingId: String): ResultRow =
        ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("House was not found.")

    private fun reviewRow(reviewId: String): ResultRow =
        StayReviewsTable.selectAll()
            .where { StayReviewsTable.id eq reviewId }
            .singleOrNull()
            ?: error("Review was not found.")

    private fun loadReview(reviewId: String, includeFollowUp: Boolean): StayReview =
        reviewRow(reviewId).toReview(includeFollowUp)

    private fun existingHouseReview(listingId: String, studentId: String): ResultRow? =
        StayReviewsTable.selectAll().where {
            (StayReviewsTable.subject eq SUBJECT_LANDLORD) and
                (StayReviewsTable.listingId eq listingId) and
                (StayReviewsTable.authorId eq studentId)
        }.singleOrNull()

    private fun hasStay(listingId: String, studentId: String): Boolean =
        ListingBookingsTable.selectAll().where {
            (ListingBookingsTable.listingId eq listingId) and
                (ListingBookingsTable.guestUserId eq studentId) and
                (ListingBookingsTable.status inList listOf(BookingStatus.CONFIRMED, BookingStatus.COMPLETED))
        }.count() > 0

    private fun stayedStudents(listingId: String, landlordId: String): List<ReviewStudentOption> {
        val reviewed = StayReviewsTable.selectAll().where {
            (StayReviewsTable.subject eq SUBJECT_STUDENT) and
                (StayReviewsTable.listingId eq listingId) and
                (StayReviewsTable.authorId eq landlordId)
        }.map { it[StayReviewsTable.studentId] }.toSet()
        return ListingBookingsTable.selectAll().where {
            (ListingBookingsTable.listingId eq listingId) and
                (ListingBookingsTable.status inList listOf(BookingStatus.CONFIRMED, BookingStatus.COMPLETED))
        }.mapNotNull { row ->
            val studentId = row[ListingBookingsTable.guestUserId] ?: return@mapNotNull null
            if (studentId in reviewed || studentId.isBlank()) return@mapNotNull null
            ReviewStudentOption(studentId = studentId, studentName = nameOf(studentId))
        }.distinctBy { it.studentId }
    }

    private fun roleOf(userId: String): UserRole =
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            ?.get(UsersTable.role)
            ?: error("Sign in again.")

    private fun nameOf(userId: String): String =
        UsersTable.selectAll().where { UsersTable.id eq userId }.singleOrNull()
            ?.get(UsersTable.fullName)
            .orEmpty()

    private fun ResultRow.toReview(includeFollowUp: Boolean): StayReview {
        val subjectName = this[StayReviewsTable.subject]
        val listingId = this[StayReviewsTable.listingId]
        val title = ListingsTable.selectAll().where { ListingsTable.id eq listingId }
            .singleOrNull()?.get(ListingsTable.title).orEmpty()
        val status = runCatching { ReviewReportStatus.valueOf(this[StayReviewsTable.reportStatus]) }
            .getOrDefault(ReviewReportStatus.NONE)
        return StayReview(
            id = this[StayReviewsTable.id],
            subject = if (subjectName == SUBJECT_STUDENT) ReviewSubject.STUDENT else ReviewSubject.LANDLORD,
            listingId = listingId,
            listingTitle = title,
            landlordId = this[StayReviewsTable.landlordId],
            landlordName = nameOf(this[StayReviewsTable.landlordId]),
            studentId = this[StayReviewsTable.studentId],
            studentName = nameOf(this[StayReviewsTable.studentId]),
            authorId = this[StayReviewsTable.authorId],
            authorName = nameOf(this[StayReviewsTable.authorId]),
            rating = this[StayReviewsTable.rating],
            body = this[StayReviewsTable.body],
            createdAt = this[StayReviewsTable.createdAt],
            reply = this[StayReviewsTable.reply],
            replyAt = this[StayReviewsTable.replyAt],
            reportStatus = status,
            reportReason = this[StayReviewsTable.reportReason],
            followUpNote = if (includeFollowUp) this[StayReviewsTable.followUpNote] else "",
        )
    }
}

/** A blank "Report review" must not show up in the admin queue. */
internal fun isActionableReport(status: ReviewReportStatus, reason: String): Boolean =
    status == ReviewReportStatus.OPEN ||
        status == ReviewReportStatus.FOLLOWED_UP ||
        (reason.isNotBlank() && status != ReviewReportStatus.DISMISSED)
