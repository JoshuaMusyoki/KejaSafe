package com.rotd.havenpad.listing

import com.rotd.havenpad.BookListingRequest
import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.CalendarDaySlot
import com.rotd.havenpad.CreateListingRequest
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.ElectricityBilling
import com.rotd.havenpad.ExtraCharge
import com.rotd.havenpad.IncomingRequest
import com.rotd.havenpad.ListingAnalytics
import com.rotd.havenpad.ListingBooking
import com.rotd.havenpad.ListingCalendar
import com.rotd.havenpad.ListingCharges
import com.rotd.havenpad.ListingDayStat
import com.rotd.havenpad.ListingPerformance
import com.rotd.havenpad.ListingPhotoResponse
import com.rotd.havenpad.ListingStatus
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.RequestType
import com.rotd.havenpad.RoommateMatch
import com.rotd.havenpad.db.EscrowTransactionsTable
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingEventsTable
import com.rotd.havenpad.db.ListingPhotosTable
import com.rotd.havenpad.db.ListingRequestsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.UsersTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

object ListingService {
    val uploadRoot: File = File(ServerConfig.uploadDir).apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun chargesToJson(charges: ListingCharges): String =
        json.encodeToString(charges.extraCharges)

    private fun chargesFromRow(row: ResultRow): ListingCharges {
        val extras = runCatching {
            json.decodeFromString<List<ExtraCharge>>(row[ListingsTable.extraChargesJson])
        }.getOrElse { emptyList() }
        return ListingCharges(
            garbageCollection = row[ListingsTable.garbageCollection],
            water = row[ListingsTable.water],
            electricityBilling = row[ListingsTable.electricityBilling],
            electricityMonthly = row[ListingsTable.electricityMonthly],
            extraCharges = extras,
            deposit = row[ListingsTable.deposit]
        )
    }

    /**
     * Removes demo/autofilled bookings (no linked tenant) and resyncs unit occupancy.
     * Call once on server start so calendars only show real tenant requests.
     */
    fun purgeSyntheticBookings() = transaction {
        ListingBookingsTable.deleteWhere { ListingBookingsTable.guestUserId.isNull() }
        // Listings were historically created as PENDING ("In review") — open them for tenants.
        ListingsTable.update({ ListingsTable.status eq ListingStatus.PENDING }) {
            it[status] = ListingStatus.VERIFIED
        }
        ListingsTable.selectAll().forEach { row ->
            recountActiveBookings(row[ListingsTable.id])
        }
    }

    fun createListing(landlordId: String, request: CreateListingRequest): PropertyListing = transaction {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val units = (request.unitsCount ?: 1).coerceAtLeast(1)
        val type = request.propertyType
        val charges = request.charges

        ListingsTable.insert {
            it[ListingsTable.id] = id
            it[ListingsTable.landlordId] = landlordId
            it[ListingsTable.title] = request.title.trim()
            it[ListingsTable.address] = request.address.trim()
            it[ListingsTable.pricePerMonth] = request.pricePerMonth
            it[ListingsTable.status] = ListingStatus.VERIFIED
            it[ListingsTable.proximity] = request.proximity
            it[ListingsTable.university] = request.university?.trim()?.ifBlank { null }
            it[ListingsTable.propertyType] = type
            it[ListingsTable.unitsCount] = units
            it[ListingsTable.garbageCollection] = charges.garbageCollection.coerceAtLeast(0.0)
            it[ListingsTable.water] = charges.water.coerceAtLeast(0.0)
            it[ListingsTable.electricityBilling] = charges.electricityBilling
            it[ListingsTable.electricityMonthly] =
                if (charges.electricityBilling == ElectricityBilling.MONTHLY) {
                    charges.electricityMonthly.coerceAtLeast(0.0)
                } else 0.0
            it[ListingsTable.extraChargesJson] = chargesToJson(charges)
            it[ListingsTable.deposit] = charges.deposit.coerceAtLeast(0.0)
            it[ListingsTable.activeBookings] = 0
            it[ListingsTable.totalViews] = 0
            it[ListingsTable.inquiries] = 0
            request.latitude?.let { lat -> it[ListingsTable.latitude] = lat }
            request.longitude?.let { lng -> it[ListingsTable.longitude] = lng }
            it[ListingsTable.createdAt] = now
        }
        PropertyListing(
            id = id,
            title = request.title.trim(),
            address = request.address.trim(),
            landlordId = landlordId,
            pricePerMonth = request.pricePerMonth,
            status = ListingStatus.VERIFIED,
            proximity = request.proximity,
            university = request.university?.trim()?.ifBlank { null },
            propertyType = type,
            unitsCount = units,
            charges = charges,
            activeBookings = 0,
            totalViews = 0,
            inquiries = 0,
            createdAt = now
        )
    }

    fun getListingsForLandlord(landlordId: String): List<PropertyListing> = transaction {
        ListingsTable.selectAll()
            .where { ListingsTable.landlordId eq landlordId }
            .map { row -> row.toListing(loadPhotos(row[ListingsTable.id])) }
            .sortedByDescending { it.createdAt }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun getBrowseListings(userLat: Double? = null, userLng: Double? = null): List<PropertyListing> = transaction {
        val allListings = ListingsTable.selectAll()
            .where { ListingsTable.status inList listOf(ListingStatus.VERIFIED, ListingStatus.PENDING) }
            .map { row -> row.toListing(loadPhotos(row[ListingsTable.id])) }
        
        if (userLat != null && userLng != null) {
            // Filter by 30km and sort by distance
            allListings.filter { haversineDistance(userLat, userLng, it.latitude, it.longitude) <= 30.0 }
                .sortedBy { haversineDistance(userLat, userLng, it.latitude, it.longitude) }
                .ifEmpty {
                    // If none within 30km, return all sorted by distance (suggestions)
                    allListings.sortedBy { haversineDistance(userLat, userLng, it.latitude, it.longitude) }
                }
        } else {
            allListings.sortedByDescending { it.createdAt }
        }
    }

    fun getListing(id: String): PropertyListing? = transaction {
        ListingsTable.selectAll()
            .where { ListingsTable.id eq id }
            .singleOrNull()
            ?.let { it.toListing(loadPhotos(it[ListingsTable.id])) }
    }

    fun getListingAnalytics(listingId: String, landlordId: String): ListingAnalytics? = transaction {
        val row = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return@transaction null
        if (row[ListingsTable.landlordId] != landlordId) return@transaction null

        val totalViews = countUniqueEventUsers(listOf(listingId), EVENT_VIEW)
        val inquiries = countUniqueEventUsers(listOf(listingId), EVENT_INQUIRY)
        syncListingCounters(listingId, totalViews, inquiries)
        val units = row[ListingsTable.unitsCount].coerceAtLeast(1)
        val activeBookings = recountActiveBookings(listingId)
        val viewsDelta = uniqueViewsDeltaPercent(listOf(listingId))
        val conversion = if (totalViews > 0) {
            ((inquiries.toDouble() / totalViews) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val occupancy = ((activeBookings.toDouble() / units) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
        val weeklyInquiries = countUniqueEventUsersSince(
            listOf(listingId),
            EVENT_INQUIRY,
            daysAgoMillis(7)
        )
        val hasNewInquiries = weeklyInquiries > 0

        ListingAnalytics(
            listingId = listingId,
            title = row[ListingsTable.title],
            address = row[ListingsTable.address],
            status = row[ListingsTable.status],
            university = row[ListingsTable.university],
            pricePerMonth = row[ListingsTable.pricePerMonth],
            currency = row[ListingsTable.currency],
            propertyType = row[ListingsTable.propertyType],
            unitsCount = units,
            totalViews = totalViews,
            viewsDeltaPercent = viewsDelta,
            inquiries = inquiries,
            hasNewInquiries = hasNewInquiries,
            activeBookings = activeBookings,
            conversionRatePercent = conversion,
            occupancyPercent = occupancy,
            weekly = buildWeeklySeriesFromEvents(listingId),
            createdAt = row[ListingsTable.createdAt]
        )
    }

    /**
     * Records a tenant view of a listing (calendar / details).
     * Counts unique logged-in individuals only; landlord self-views and anonymous hits are ignored.
     */
    fun recordListingView(listingId: String, viewerUserId: String?): Boolean = transaction {
        val row = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return@transaction false
        // Only authenticated tenants count as individuals who viewed.
        if (viewerUserId.isNullOrBlank()) return@transaction false
        if (row[ListingsTable.landlordId] == viewerUserId) {
            return@transaction false
        }
        // Dedupe same viewer within a rolling day to avoid spam on calendar redraws.
        val dayAgo = System.currentTimeMillis() - 86_400_000L
        val already = ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId eq listingId) and
                    (ListingEventsTable.eventType eq EVENT_VIEW) and
                    (ListingEventsTable.userId eq viewerUserId) and
                    (ListingEventsTable.createdAt greaterEq dayAgo)
            }
            .count()
        if (already > 0) return@transaction false

        insertEvent(listingId, EVENT_VIEW, viewerUserId)
        val uniqueViews = countUniqueEventUsers(listOf(listingId), EVENT_VIEW)
        ListingsTable.update({ ListingsTable.id eq listingId }) {
            it[totalViews] = uniqueViews
        }
        true
    }

    fun updateListing(
        listingId: String,
        landlordId: String,
        request: CreateListingRequest
    ): PropertyListing? = transaction {
        val existing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return@transaction null
        if (existing[ListingsTable.landlordId] != landlordId) return@transaction null

        val units = (request.unitsCount ?: 1).coerceAtLeast(1)
        val charges = request.charges
        val updated = ListingsTable.update({ ListingsTable.id eq listingId }) {
            it[title] = request.title.trim()
            it[address] = request.address.trim()
            it[proximity] = request.proximity
            it[university] = request.university?.trim()?.ifBlank { null }
            it[pricePerMonth] = request.pricePerMonth
            it[propertyType] = request.propertyType
            it[unitsCount] = units
            it[garbageCollection] = charges.garbageCollection.coerceAtLeast(0.0)
            it[water] = charges.water.coerceAtLeast(0.0)
            it[electricityBilling] = charges.electricityBilling
            it[electricityMonthly] =
                if (charges.electricityBilling == ElectricityBilling.MONTHLY) {
                    charges.electricityMonthly.coerceAtLeast(0.0)
                } else 0.0
            it[extraChargesJson] = chargesToJson(charges)
            it[deposit] = charges.deposit.coerceAtLeast(0.0)
            request.latitude?.let { lat -> it[latitude] = lat }
            request.longitude?.let { lng -> it[longitude] = lng }
        }
        if (updated == 0) return@transaction null
        ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?.let { it.toListing(loadPhotos(it[ListingsTable.id])) }
    }

    fun getPerformance(landlordId: String): ListingPerformance = transaction {
        val rows = ListingsTable.selectAll()
            .where { ListingsTable.landlordId eq landlordId }
            .toList()
        val listingIds = rows.map { it[ListingsTable.id] }
        // Unique people across the landlord's portfolio (not raw page hits).
        val totalViews = countUniqueEventUsers(listingIds, EVENT_VIEW)
        val inquiries = countUniqueEventUsers(listingIds, EVENT_INQUIRY)
        listingIds.forEach { id ->
            syncListingCounters(
                id,
                countUniqueEventUsers(listOf(id), EVENT_VIEW),
                countUniqueEventUsers(listOf(id), EVENT_INQUIRY)
            )
        }
        val activeBookings = listingIds.sumOf { recountActiveBookings(it) }
        val units = rows.sumOf { it[ListingsTable.unitsCount].coerceAtLeast(0) }
            .coerceAtLeast(activeBookings)
        val recentInquiries = countUniqueEventUsersSince(
            listingIds,
            EVENT_INQUIRY,
            daysAgoMillis(7)
        )
        ListingPerformance(
            totalViews = totalViews,
            viewsDeltaPercent = uniqueViewsDeltaPercent(listingIds),
            inquiries = inquiries,
            hasNewInquiries = recentInquiries > 0,
            activeBookings = activeBookings,
            unitsCount = units.coerceAtLeast(1)
        )
    }

    fun getIncomingRequests(landlordId: String): List<IncomingRequest> {
        val roommateSplits = RoommateMatchingService.suggestedMatchesForLandlord(landlordId)
            .map { match ->
                val names = match.guests.joinToString(" & ") { it.name }
                IncomingRequest(
                    id = match.id,
                    requesterName = names.ifBlank { "Roommate pair" },
                    type = RequestType.ROOMMATE_SPLIT,
                    propertyTitle = match.listingTitle,
                    detail = "${match.score}% match · overlapping stay",
                    timeLabel = "Suggested pair"
                )
            }

        val liveTours = LiveTourService.incomingForLandlord(landlordId)

        val pendingAndInbox = transaction {
            val fromInbox = ListingRequestsTable.selectAll()
                .where { ListingRequestsTable.landlordId eq landlordId }
                .mapNotNull { row ->
                    // Live tours are surfaced from LiveTourSessions (session id), not raw inbox rows.
                    if (row[ListingRequestsTable.type] == RequestType.LIVE_INSPECTION) return@mapNotNull null
                    IncomingRequest(
                        id = row[ListingRequestsTable.id],
                        requesterName = row[ListingRequestsTable.requesterName],
                        type = row[ListingRequestsTable.type],
                        propertyTitle = row[ListingRequestsTable.propertyTitle],
                        detail = row[ListingRequestsTable.detail],
                        timeLabel = row[ListingRequestsTable.timeLabel]
                    )
                }

            // Pending bookings awaiting landlord review.
            // Unmatched roommate-seekers are visible; matched ones surface as ROOMMATE_SPLIT instead.
            val pendingBookings = ListingBookingsTable.selectAll()
                .where {
                    (ListingBookingsTable.landlordId eq landlordId) and
                        (ListingBookingsTable.status eq BookingStatus.PENDING) and
                        ListingBookingsTable.roommateMatchId.isNull()
                }
                .map { row ->
                    val title = ListingsTable.selectAll()
                        .where { ListingsTable.id eq row[ListingBookingsTable.listingId] }
                        .singleOrNull()
                        ?.get(ListingsTable.title)
                        .orEmpty()
                    val start = Instant.ofEpochMilli(row[ListingBookingsTable.startDateMillis])
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    val seeking = row[ListingBookingsTable.seekingRoommate]
                    val baseDetail = row[ListingBookingsTable.notes].ifBlank {
                        "Wants to book from $start (${row[ListingBookingsTable.slotCount]} slot)"
                    }
                    IncomingRequest(
                        id = row[ListingBookingsTable.id],
                        requesterName = row[ListingBookingsTable.guestName],
                        type = RequestType.BOOKING,
                        propertyTitle = title,
                        detail = if (seeking) {
                            "Looking for a roommate · $baseDetail"
                        } else {
                            baseDetail
                        },
                        timeLabel = if (seeking) "Awaiting roommate match" else "Pending approval"
                    )
                }

            pendingBookings + fromInbox
        }

        return (pendingAndInbox + roommateSplits + liveTours).sortedByDescending { it.id }
    }

    fun isOwnedBy(listingId: String, landlordId: String): Boolean = transaction {
        ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?.get(ListingsTable.landlordId) == landlordId
    }

    fun getListingCalendar(
        listingId: String,
        year: Int? = null,
        month: Int? = null
    ): ListingCalendar? = transaction {
        val row = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return@transaction null

        val units = row[ListingsTable.unitsCount].coerceAtLeast(1)
        val activeBookings = recountActiveBookings(listingId)
        val price = row[ListingsTable.pricePerMonth]
        val currency = row[ListingsTable.currency]
        val propertyType = row[ListingsTable.propertyType]

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val ym = if (year != null && month != null && month in 1..12) {
            YearMonth.of(year, month)
        } else {
            YearMonth.from(today)
        }
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()
        val rangeStartMs = monthStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val rangeEndMs = monthEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

        val bookingsInMonth = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId eq listingId) and
                    (ListingBookingsTable.startDateMillis lessEq rangeEndMs) and
                    (ListingBookingsTable.endDateMillis greaterEq rangeStartMs) and
                    (ListingBookingsTable.status inList listOf(BookingStatus.CONFIRMED, BookingStatus.PENDING))
            }
            .map { it.toBooking(row[ListingsTable.title], price, currency) }
            .sortedBy { it.startDateMillis }

        // All active requests for this listing (not only the visible month) so tenants
        // always see their pending booking under "Your requests".
        val allActiveBookings = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId eq listingId) and
                    (ListingBookingsTable.status inList listOf(BookingStatus.CONFIRMED, BookingStatus.PENDING))
            }
            .map { it.toBooking(row[ListingsTable.title], price, currency) }
            .sortedByDescending { it.startDateMillis }

        val dayCounts = mutableMapOf<LocalDate, Int>()
        bookingsInMonth.forEach { booking ->
            // Only confirmed bookings occupy units
            if (booking.status != BookingStatus.CONFIRMED) return@forEach
            var d = Instant.ofEpochMilli(booking.startDateMillis).atZone(zone).toLocalDate()
            val end = Instant.ofEpochMilli(booking.endDateMillis).atZone(zone).toLocalDate()
            while (!d.isAfter(end)) {
                if (!d.isBefore(monthStart) && !d.isAfter(monthEnd)) {
                    dayCounts[d] = (dayCounts[d] ?: 0) + booking.slotCount
                }
                d = d.plusDays(1)
            }
        }

        // Pad calendar to full weeks (Mon–Sun)
        val startPad = monthStart.dayOfWeek.value // 1=Mon ... 7=Sun
        val gridStart = monthStart.minusDays((startPad - 1).toLong())
        val days = (0 until 42).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            val booked = (dayCounts[date] ?: 0).coerceAtMost(units)
            val inMonth = date.monthValue == ym.monthValue && date.year == ym.year
            CalendarDaySlot(
                date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                dayOfMonth = date.dayOfMonth,
                bookedCount = if (inMonth) booked else 0,
                unitsCount = units,
                fullyBooked = inMonth && booked >= units,
                isToday = date == today,
                inCurrentMonth = inMonth
            )
        }

        val openSlots = (units - activeBookings).coerceAtLeast(0)
        ListingCalendar(
            listingId = listingId,
            title = row[ListingsTable.title],
            address = row[ListingsTable.address],
            propertyType = propertyType,
            unitsCount = units,
            activeBookings = activeBookings,
            openSlots = openSlots,
            fullyBooked = openSlots == 0 || activeBookings >= units,
            pricePerMonth = price,
            currency = currency,
            month = ym.monthValue,
            year = ym.year,
            monthLabel = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}",
            days = days,
            bookings = allActiveBookings
        )
    }

    /**
     * Tenant requests a booking. Status starts as [BookingStatus.PENDING] until landlord approves.
     */
    fun bookListing(
        listingId: String,
        guestUserId: String,
        request: BookListingRequest
    ): ListingBooking = transaction {
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")

        if (listing[ListingsTable.landlordId] == guestUserId) {
            error("You cannot book your own listing")
        }

        val units = listing[ListingsTable.unitsCount].coerceAtLeast(1)
        val active = recountActiveBookings(listingId)
        val slots = request.slotCount.coerceAtLeast(1)
        if (active + slots > units) {
            error("Not enough open units (need $slots, have ${units - active})")
        }

        val zone = ZoneId.systemDefault()
        val start = try {
            LocalDate.parse(request.startDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            error("Invalid start date (use yyyy-MM-dd)")
        }
        val endDateRaw = request.endDate
        val end = if (!endDateRaw.isNullOrBlank()) {
            try {
                LocalDate.parse(endDateRaw.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Exception) {
                error("Invalid end date (use yyyy-MM-dd)")
            }
        } else {
            start.plusDays(29)
        }
        if (end.isBefore(start)) error("End date must be on or after start date")

        val guestName = UsersTable.selectAll()
            .where { UsersTable.id eq guestUserId }
            .singleOrNull()
            ?.get(UsersTable.fullName)
            ?: "Tenant"

        val id = UUID.randomUUID().toString()
        val seeking = request.seekingRoommate
        ListingBookingsTable.insert {
            it[ListingBookingsTable.id] = id
            it[ListingBookingsTable.listingId] = listingId
            it[ListingBookingsTable.landlordId] = listing[ListingsTable.landlordId]
            it[ListingBookingsTable.guestUserId] = guestUserId
            it[ListingBookingsTable.guestName] = guestName
            it[ListingBookingsTable.startDateMillis] = start.atStartOfDay(zone).toInstant().toEpochMilli()
            it[ListingBookingsTable.endDateMillis] = end.atStartOfDay(zone).toInstant().toEpochMilli()
            it[ListingBookingsTable.status] = BookingStatus.PENDING
            it[ListingBookingsTable.notes] = request.notes.trim()
            it[ListingBookingsTable.slotCount] = slots
            it[ListingBookingsTable.seekingRoommate] = seeking
            it[ListingBookingsTable.budgetMax] = request.budgetMax?.takeIf { it > 0 }
            it[ListingBookingsTable.roommateMatchId] = null
            it[ListingBookingsTable.createdAt] = System.currentTimeMillis()
        }
        // First booking request from this guest counts as one inquiry (unique person).
        val priorInquiry = ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId eq listingId) and
                    (ListingEventsTable.eventType eq EVENT_INQUIRY) and
                    (ListingEventsTable.userId eq guestUserId)
            }
            .count()
        if (priorInquiry == 0L) {
            insertEvent(listingId, EVENT_INQUIRY, guestUserId)
        }
        val uniqueInquiries = countUniqueEventUsers(listOf(listingId), EVENT_INQUIRY)
        ListingsTable.update({ ListingsTable.id eq listingId }) {
            it[inquiries] = uniqueInquiries
        }
        // Pending does not bump activeBookings until approve.
        val booking = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq id }
            .single()
            .toBooking(
                listingTitle = listing[ListingsTable.title],
                pricePerMonth = listing[ListingsTable.pricePerMonth],
                currency = listing[ListingsTable.currency]
            )
        booking
    }.also { booking ->
        if (booking.seekingRoommate) {
            RoommateMatchingService.tryMatchListing(listingId)
        }
    }

    fun approveBooking(bookingId: String, landlordId: String): ListingBooking = transaction {
        val row = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq bookingId }
            .singleOrNull()
            ?: error("Booking not found")
        if (row[ListingBookingsTable.landlordId] != landlordId) {
            error("Not your listing booking")
        }
        if (row[ListingBookingsTable.status] != BookingStatus.PENDING) {
            error("Only pending bookings can be approved")
        }
        val listingId = row[ListingBookingsTable.listingId]
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")
        val units = listing[ListingsTable.unitsCount].coerceAtLeast(1)
        val active = recountActiveBookings(listingId)
        val slots = row[ListingBookingsTable.slotCount]
        if (active + slots > units) {
            error("Cannot approve — no open units left")
        }

        ListingBookingsTable.update({ ListingBookingsTable.id eq bookingId }) {
            it[status] = BookingStatus.CONFIRMED
        }
        recountActiveBookings(listingId)

        ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq bookingId }
            .single()
            .toBooking(
                listingTitle = listing[ListingsTable.title],
                pricePerMonth = listing[ListingsTable.pricePerMonth],
                currency = listing[ListingsTable.currency]
            )
    }

    fun declineBooking(bookingId: String, landlordId: String): ListingBooking = transaction {
        val row = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq bookingId }
            .singleOrNull()
            ?: error("Booking not found")
        if (row[ListingBookingsTable.landlordId] != landlordId) {
            error("Not your listing booking")
        }
        if (row[ListingBookingsTable.status] !in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED)) {
            error("Booking cannot be declined")
        }
        val listingId = row[ListingBookingsTable.listingId]
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")

        ListingBookingsTable.update({ ListingBookingsTable.id eq bookingId }) {
            it[status] = BookingStatus.CANCELLED
        }
        recountActiveBookings(listingId)

        ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq bookingId }
            .single()
            .toBooking(
                listingTitle = listing[ListingsTable.title],
                pricePerMonth = listing[ListingsTable.pricePerMonth],
                currency = listing[ListingsTable.currency]
            )
    }.also {
        RoommateMatchingService.releaseBooking(bookingId)
    }

    fun getRoommateMatch(matchId: String, landlordId: String): RoommateMatch? =
        RoommateMatchingService.getMatch(matchId, landlordId)

    fun approveRoommateMatch(matchId: String, landlordId: String): RoommateMatch =
        RoommateMatchingService.approveMatch(matchId, landlordId)

    fun declineRoommateMatch(matchId: String, landlordId: String): RoommateMatch =
        RoommateMatchingService.declineMatch(matchId, landlordId)

    fun getBookingForLandlord(bookingId: String, landlordId: String): ListingBooking? = transaction {
        val row = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.id eq bookingId }
            .singleOrNull()
            ?: return@transaction null
        if (row[ListingBookingsTable.landlordId] != landlordId) return@transaction null
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq row[ListingBookingsTable.listingId] }
            .singleOrNull()
            ?: return@transaction null
        row.toBooking(
            listingTitle = listing[ListingsTable.title],
            pricePerMonth = listing[ListingsTable.pricePerMonth],
            currency = listing[ListingsTable.currency]
        )
    }

    /** Returns confirmed active slot count and persists [ListingsTable.activeBookings]. */
    fun recountActiveBookings(listingId: String): Int {
        val sum = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId eq listingId) and
                    (ListingBookingsTable.status eq BookingStatus.CONFIRMED)
            }
            .sumOf { it[ListingBookingsTable.slotCount] }
        ListingsTable.update({ ListingsTable.id eq listingId }) {
            it[activeBookings] = sum
        }
        return sum
    }

    fun getMyBookings(userId: String): List<ListingBooking> = transaction {
        fun attach(row: ResultRow): ListingBooking {
            val listing = ListingsTable.selectAll()
                .where { ListingsTable.id eq row[ListingBookingsTable.listingId] }
                .singleOrNull()
            return row.toBooking(
                listingTitle = listing?.get(ListingsTable.title).orEmpty(),
                pricePerMonth = listing?.get(ListingsTable.pricePerMonth),
                currency = listing?.get(ListingsTable.currency) ?: "KES",
                requesterUserId = userId
            )
        }

        val asGuest = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.guestUserId eq userId }
            .map(::attach)

        val asLandlord = ListingBookingsTable.selectAll()
            .where { ListingBookingsTable.landlordId eq userId }
            .map(::attach)

        (asGuest + asLandlord)
            .distinctBy { it.id }
            .sortedByDescending { it.startDateMillis }
    }

    /**
     * Other confirmed guests on listings where [userId] also has a confirmed booking.
     * Used for tenant↔tenant chat discovery (connect required).
     */
    fun getConfirmedApartmentMates(userId: String): List<ListingBooking> = transaction {
        val myListingIds = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.guestUserId eq userId) and
                    (ListingBookingsTable.status eq BookingStatus.CONFIRMED)
            }
            .map { it[ListingBookingsTable.listingId] }
            .distinct()
        if (myListingIds.isEmpty()) return@transaction emptyList()

        ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId inList myListingIds) and
                    (ListingBookingsTable.status eq BookingStatus.CONFIRMED) and
                    (ListingBookingsTable.guestUserId neq userId) and
                    ListingBookingsTable.guestUserId.isNotNull()
            }
            .map { row ->
                val listing = ListingsTable.selectAll()
                    .where { ListingsTable.id eq row[ListingBookingsTable.listingId] }
                    .singleOrNull()
                row.toBooking(
                    listingTitle = listing?.get(ListingsTable.title).orEmpty(),
                    pricePerMonth = listing?.get(ListingsTable.pricePerMonth),
                    currency = listing?.get(ListingsTable.currency) ?: "KES"
                )
            }
            .distinctBy { it.guestUserId to it.listingId }
            .sortedBy { it.guestName.lowercase() }
    }

    fun addPhoto(
        listingId: String,
        fileName: String,
        bytes: ByteArray
    ): ListingPhotoResponse {
        require(isValidImageName(fileName)) { "Unsupported image type" }
        require(bytes.isNotEmpty()) { "Empty image" }
        require(bytes.size <= 8 * 1024 * 1024) { "Image too large (max 8MB)" }

        val safeName = sanitizeFileName(fileName)
        val photoId = UUID.randomUUID().toString()
        val relativeDir = "listings/$listingId"
        val dir = File(uploadRoot, relativeDir).apply { mkdirs() }
        val storedName = "${photoId}_$safeName"
        File(dir, storedName).writeBytes(bytes)

        val publicUrl = "/uploads/$relativeDir/$storedName"
        val sortOrder = transaction {
            ListingPhotosTable.selectAll()
                .where { ListingPhotosTable.listingId eq listingId }
                .count()
                .toInt()
        }

        transaction {
            ListingPhotosTable.insert {
                it[ListingPhotosTable.id] = photoId
                it[ListingPhotosTable.listingId] = listingId
                it[ListingPhotosTable.url] = publicUrl
                it[ListingPhotosTable.sortOrder] = sortOrder
                it[ListingPhotosTable.createdAt] = System.currentTimeMillis()
            }
        }

        return ListingPhotoResponse(
            id = photoId,
            listingId = listingId,
            url = publicUrl,
            sortOrder = sortOrder
        )
    }

    private fun loadPhotos(listingId: String): List<ListingPhotoResponse> {
        return ListingPhotosTable.selectAll()
            .where { ListingPhotosTable.listingId eq listingId }
            .map { row -> row }
            .sortedBy { it[ListingPhotosTable.sortOrder] }
            .map { row ->
                ListingPhotoResponse(
                    id = row[ListingPhotosTable.id],
                    listingId = listingId,
                    url = row[ListingPhotosTable.url],
                    sortOrder = row[ListingPhotosTable.sortOrder]
                )
            }
    }

    fun deletePhoto(listingId: String, photoId: String, landlordId: String): Boolean = transaction {
        if (!isOwnedBy(listingId, landlordId)) return@transaction false
        val row = ListingPhotosTable.selectAll()
            .where {
                (ListingPhotosTable.id eq photoId) and
                    (ListingPhotosTable.listingId eq listingId)
            }
            .singleOrNull()
            ?: return@transaction false
        val url = row[ListingPhotosTable.url]
        val deleted = ListingPhotosTable.deleteWhere {
            (ListingPhotosTable.id eq photoId) and
                (ListingPhotosTable.listingId eq listingId)
        }
        if (deleted > 0) {
            // Best-effort file cleanup for /uploads/listings/{id}/...
            val relative = url.removePrefix("/uploads/")
            val file = File(uploadRoot, relative)
            if (file.exists()) file.delete()
        }
        deleted > 0
    }

    private fun ResultRow.toListing(photos: List<ListingPhotoResponse>): PropertyListing {
        val photoUrls = photos.map { it.url }
        return PropertyListing(
            id = this[ListingsTable.id],
            title = this[ListingsTable.title],
            address = this[ListingsTable.address],
            landlordId = this[ListingsTable.landlordId],
            pricePerMonth = this[ListingsTable.pricePerMonth],
            currency = this[ListingsTable.currency],
            imageUrl = photoUrls.firstOrNull(),
            imageUrls = photoUrls,
            photos = photos,
            status = this[ListingsTable.status],
            proximity = this[ListingsTable.proximity],
            university = this[ListingsTable.university],
            propertyType = this[ListingsTable.propertyType],
            unitsCount = this[ListingsTable.unitsCount],
            charges = chargesFromRow(this),
            activeBookings = this[ListingsTable.activeBookings],
            totalViews = this[ListingsTable.totalViews],
            inquiries = this[ListingsTable.inquiries],
            latitude = this[ListingsTable.latitude],
            longitude = this[ListingsTable.longitude],
            createdAt = this[ListingsTable.createdAt]
        )
    }

    private fun isValidImageName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".webp") ||
            !name.contains('.') // mime may omit extension — accept and fix
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return when {
            cleaned.contains('.') -> cleaned
            else -> "$cleaned.jpg"
        }
    }

    private val EVENT_VIEW = "VIEW"
    private val EVENT_INQUIRY = "INQUIRY"

    private fun daysAgoMillis(days: Int): Long =
        System.currentTimeMillis() - days * 86_400_000L

    private fun insertEvent(listingId: String, type: String, userId: String?) {
        ListingEventsTable.insert {
            it[ListingEventsTable.id] = UUID.randomUUID().toString()
            it[ListingEventsTable.listingId] = listingId
            it[ListingEventsTable.eventType] = type
            it[ListingEventsTable.userId] = userId
            it[ListingEventsTable.createdAt] = System.currentTimeMillis()
        }
    }

    /** Distinct logged-in users who produced [type] events for these listings. */
    private fun countUniqueEventUsers(listingIds: List<String>, type: String): Int {
        if (listingIds.isEmpty()) return 0
        return ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId inList listingIds) and
                    (ListingEventsTable.eventType eq type)
            }
            .mapNotNull { it[ListingEventsTable.userId]?.takeIf { id -> id.isNotBlank() } }
            .toSet()
            .size
    }

    private fun countUniqueEventUsersSince(
        listingIds: List<String>,
        type: String,
        sinceMillis: Long
    ): Int {
        if (listingIds.isEmpty()) return 0
        return ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId inList listingIds) and
                    (ListingEventsTable.eventType eq type) and
                    (ListingEventsTable.createdAt greaterEq sinceMillis)
            }
            .mapNotNull { it[ListingEventsTable.userId]?.takeIf { id -> id.isNotBlank() } }
            .toSet()
            .size
    }

    private fun countUniqueEventUsersInRange(
        listingIds: List<String>,
        type: String,
        fromInclusive: Long,
        toExclusive: Long
    ): Int {
        if (listingIds.isEmpty()) return 0
        return ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId inList listingIds) and
                    (ListingEventsTable.eventType eq type) and
                    (ListingEventsTable.createdAt greaterEq fromInclusive) and
                    (ListingEventsTable.createdAt lessEq (toExclusive - 1))
            }
            .mapNotNull { it[ListingEventsTable.userId]?.takeIf { id -> id.isNotBlank() } }
            .toSet()
            .size
    }

    private fun syncListingCounters(listingId: String, views: Int, inquiries: Int) {
        ListingsTable.update({ ListingsTable.id eq listingId }) {
            it[totalViews] = views
            it[ListingsTable.inquiries] = inquiries
        }
    }

    /** Week-over-week % change for unique viewers across [listingIds]. */
    private fun uniqueViewsDeltaPercent(listingIds: List<String>): Int {
        if (listingIds.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val week = 7 * 86_400_000L
        val recent = countUniqueEventUsersSince(listingIds, EVENT_VIEW, now - week)
        val previous = countUniqueEventUsersInRange(
            listingIds,
            EVENT_VIEW,
            now - 2 * week,
            now - week
        )
        if (previous == 0) return if (recent > 0) 100 else 0
        return (((recent - previous).toDouble() / previous) * 100.0).roundToInt()
    }

    /** Real last-7-days series: unique individuals per day (not raw hits). */
    private fun buildWeeklySeriesFromEvents(listingId: String): List<ListingDayStat> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.minusDays(6)
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val events = ListingEventsTable.selectAll()
            .where {
                (ListingEventsTable.listingId eq listingId) and
                    (ListingEventsTable.createdAt greaterEq startMillis)
            }
            .toList()

        return (0..6).map { offset ->
            val day = start.plusDays(offset.toLong())
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEvents = events.filter {
                val t = it[ListingEventsTable.createdAt]
                t in dayStart until dayEnd
            }
            val uniqueViews = dayEvents
                .filter { it[ListingEventsTable.eventType] == EVENT_VIEW }
                .mapNotNull { it[ListingEventsTable.userId]?.takeIf { id -> id.isNotBlank() } }
                .toSet()
                .size
            val uniqueInquiries = dayEvents
                .filter { it[ListingEventsTable.eventType] == EVENT_INQUIRY }
                .mapNotNull { it[ListingEventsTable.userId]?.takeIf { id -> id.isNotBlank() } }
                .toSet()
                .size
            ListingDayStat(
                label = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                views = uniqueViews,
                inquiries = uniqueInquiries
            )
        }
    }

    private fun ResultRow.toBooking(
        listingTitle: String,
        pricePerMonth: Double? = null,
        currency: String = "KES",
        requesterUserId: String? = null
    ): ListingBooking {
        val bid = this[ListingBookingsTable.id]
        val landlordId = this[ListingBookingsTable.landlordId]
        val landlordName = UsersTable.selectAll()
            .where { UsersTable.id eq landlordId }
            .singleOrNull()
            ?.get(UsersTable.fullName)
            .orEmpty()
        val escrow = EscrowTransactionsTable.selectAll()
            .where { EscrowTransactionsTable.bookingId eq bid }
            .singleOrNull()

        return ListingBooking(
            id = bid,
            listingId = this[ListingBookingsTable.listingId],
            listingTitle = listingTitle,
            landlordId = landlordId,
            landlordName = landlordName,
            guestName = this[ListingBookingsTable.guestName],
            guestUserId = this[ListingBookingsTable.guestUserId],
            startDateMillis = this[ListingBookingsTable.startDateMillis],
            endDateMillis = this[ListingBookingsTable.endDateMillis],
            status = this[ListingBookingsTable.status],
            notes = this[ListingBookingsTable.notes],
            slotCount = this[ListingBookingsTable.slotCount],
            pricePerMonth = pricePerMonth,
            currency = currency,
            seekingRoommate = this[ListingBookingsTable.seekingRoommate],
            budgetMax = this[ListingBookingsTable.budgetMax],
            roommateMatchId = this[ListingBookingsTable.roommateMatchId],
            escrowState = escrow?.get(EscrowTransactionsTable.state),
            escrowTransactionId = escrow?.get(EscrowTransactionsTable.id),
            escrowVerificationOtp = if (requesterUserId == landlordId) {
                escrow?.get(EscrowTransactionsTable.verificationOtp)
            } else null
        )
    }
}
