package com.rotd.havenpad.db

import com.rotd.havenpad.BillStatus
import com.rotd.havenpad.CampusProximity
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.ElectricityBilling
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.ListingStatus
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.LiveTourStatus
import com.rotd.havenpad.PropertyType
import com.rotd.havenpad.RequestType
import com.rotd.havenpad.RoommateMatchStatus
import com.rotd.havenpad.UserRole
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object UsersTable : Table("users") {
    val id = varchar("id", 50)
    val email = varchar("email", 100).uniqueIndex()
    val username = varchar("username", 50).uniqueIndex()
    val fullName = varchar("full_name", 100)
    val passwordHash = varchar("password_hash", 255).nullable()
    val role = enumerationByName("role", 20, UserRole::class)
    val kycStatus = enumerationByName("kyc_status", 20, KycStatus::class).default(KycStatus.NOT_SUBMITTED)
    val profilePictureUrl = varchar("profile_picture_url", 255).nullable()
    val googleId = varchar("google_id", 100).nullable().uniqueIndex()
    val major = varchar("major", 100).nullable()
    val yearOfStudy = integer("year_of_study").nullable()
    val roommatePreferences = varchar("roommate_preferences", 500).nullable()
    val phone = varchar("phone", 40).nullable()
    val businessName = varchar("business_name", 150).nullable()
    val active = bool("is_active").default(true)
    val createdAt = long("created_at").default(0L)

    override val primaryKey = PrimaryKey(id)
}

object KycTable : Table("kyc_submissions") {
    val id = integer("id").autoIncrement()
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val documentType = varchar("document_type", 50)
    val documentNumber = varchar("document_number", 50)
    val documentFrontImageUrl = varchar("document_front_image_url", 255)
    val documentBackImageUrl = varchar("document_back_image_url", 255).nullable()
    val selfieImageUrl = varchar("selfie_image_url", 255)
    val residentialAddress = varchar("residential_address", 300).default("")
    val addressProofImageUrl = varchar("address_proof_image_url", 255).default("")
    val reviewNote = varchar("review_note", 400).nullable()
    val reviewedAt = long("reviewed_at").nullable()
    val submittedAt = long("submitted_at")

    override val primaryKey = PrimaryKey(id)
}

object DeviceTokensTable : Table("device_tokens") {
    val token = varchar("token", 512)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val platform = varchar("platform", 20).default("android")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(token)
}

/** Account facts an admin may see. Chat content is never written here. */
object AccountEventsTable : Table("account_events") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val title = varchar("title", 80)
    val detail = varchar("detail", 200)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object UserDevicesTable : Table("user_devices") {
    val id = varchar("id", 64)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val name = varchar("name", 120)
    val platform = varchar("platform", 20).default("")
    val trusted = bool("trusted").default(true)
    val lastSeenAt = long("last_seen_at")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object SignInChallengesTable : Table("sign_in_challenges") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val deviceId = varchar("device_id", 64)
    val deviceName = varchar("device_name", 120)
    val platform = varchar("platform", 20).default("")
    val status = varchar("status", 20)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object BillsTable : Table("rent_bills") {
    val id = varchar("id", 50)
    val userId = varchar("user_id", 50).references(UsersTable.id)
    val propertyId = varchar("property_id", 50)
    val propertyTitle = varchar("property_title", 200).default("")
    val tenantName = varchar("tenant_name", 120).default("")
    val bookingId = varchar("booking_id", 50).nullable()
    val month = integer("month")
    val year = integer("year")
    val totalAmount = double("total_amount")
    val amountPaid = double("amount_paid").default(0.0)
    /** Amount requested in the current open STK Push (applied on success callback). */
    val pendingPaymentAmount = double("pending_payment_amount").nullable()
    val dueDate = long("due_date")
    val status = enumerationByName("status", 24, BillStatus::class).default(BillStatus.UNPAID)
    val depositIncluded = bool("deposit_included").default(false)
    val electricityNote = varchar("electricity_note", 200).nullable()
    val paymentRequested = bool("payment_requested").default(false)
    val mpesaCheckoutRequestId = varchar("mpesa_checkout_request_id", 100).nullable()
    val mpesaReceiptCode = varchar("mpesa_receipt_code", 40).nullable()
    val mpesaPayerName = varchar("mpesa_payer_name", 120).nullable()
    val paymentFailureReason = varchar("payment_failure_reason", 240).nullable()

    override val primaryKey = PrimaryKey(id)
}

object BillItemsTable : Table("bill_items") {
    val id = integer("id").autoIncrement()
    val billId = varchar("bill_id", 50).references(BillsTable.id)
    val name = varchar("name", 100)
    val amount = double("amount")

    override val primaryKey = PrimaryKey(id)
}

object ListingsTable : Table("listings") {
    val id = varchar("id", 50)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val title = varchar("title", 200)
    val address = varchar("address", 300)
    val pricePerMonth = double("price_per_month").nullable()
    val currency = varchar("currency", 10).default("KES")
    val status = enumerationByName("status", 20, ListingStatus::class).default(ListingStatus.PENDING)
    val proximity = enumerationByName("proximity", 30, CampusProximity::class).nullable()
    val university = varchar("university", 150).nullable()
    val propertyType = enumerationByName("property_type", 30, PropertyType::class)
        .default(PropertyType.BEDSITTER)
    /** Number of units of this type available (replaces occupancy "capacity"). */
    val unitsCount = integer("units_count").default(1)
    val garbageCollection = double("garbage_collection").default(0.0)
    val water = double("water").default(0.0)
    val electricityBilling = enumerationByName("electricity_billing", 20, ElectricityBilling::class)
        .default(ElectricityBilling.TOKEN)
    val electricityMonthly = double("electricity_monthly").default(0.0)
    val extraChargesJson = text("extra_charges_json").default("[]")
    val deposit = double("deposit").default(0.0)
    val activeBookings = integer("active_bookings").default(0)
    val totalViews = integer("total_views").default(0)
    val inquiries = integer("inquiries").default(0)
    val latitude = double("latitude").default(-1.2921)
    val longitude = double("longitude").default(36.8219)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ListingPhotosTable : Table("listing_photos") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val url = varchar("url", 500)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ListingRequestsTable : Table("listing_requests") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val requesterName = varchar("requester_name", 120)
    val type = enumerationByName("type", 30, RequestType::class)
    val propertyTitle = varchar("property_title", 200)
    val detail = varchar("detail", 500).default("")
    val timeLabel = varchar("time_label", 80)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ListingBookingsTable : Table("listing_bookings") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val guestUserId = varchar("guest_user_id", 50).nullable()
    val guestName = varchar("guest_name", 120)
    val startDateMillis = long("start_date_millis")
    val endDateMillis = long("end_date_millis")
    val status = enumerationByName("status", 20, com.rotd.havenpad.BookingStatus::class)
    val notes = varchar("notes", 500).default("")
    val slotCount = integer("slot_count").default(1)
    val seekingRoommate = bool("seeking_roommate").default(false)
    val budgetMax = double("budget_max").nullable()
    val roommateMatchId = varchar("roommate_match_id", 50).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object RoommateMatchesTable : Table("roommate_matches") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val bookingAId = varchar("booking_a_id", 50)
    val bookingBId = varchar("booking_b_id", 50)
    val score = integer("score").default(0)
    val status = enumerationByName("status", 20, RoommateMatchStatus::class)
        .default(RoommateMatchStatus.SUGGESTED)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ListingEventsTable : Table("listing_events") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val eventType = varchar("event_type", 20) // VIEW | INQUIRY
    val userId = varchar("user_id", 50).nullable()
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object LiveTourSessionsTable : Table("live_tour_sessions") {
    val id = varchar("id", 50)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val tenantId = varchar("tenant_id", 50).references(UsersTable.id)
    val requestId = varchar("request_id", 50)
    val roomName = varchar("room_name", 120).default("")
    val mediaType = enumerationByName("media_type", 20, LiveTourMediaType::class)
        .default(LiveTourMediaType.VIDEO)
    val status = enumerationByName("status", 20, LiveTourStatus::class)
        .default(LiveTourStatus.SCHEDULED)
    val preferredTimeNote = varchar("preferred_time_note", 500).default("")
    val createdAt = long("created_at")
    val startedAt = long("started_at").nullable()
    val endedAt = long("ended_at").nullable()
    val callerId = varchar("caller_id", 50).default("")
    val answeredAt = long("answered_at").nullable()
    val missed = bool("missed").default(false)

    override val primaryKey = PrimaryKey(id)
}

object LiveTourRtcTable : Table("live_tour_rtc") {
    val sessionId = varchar("session_id", 50)
    val offerSdp = text("offer_sdp").nullable()
    val answerSdp = text("answer_sdp").nullable()

    override val primaryKey = PrimaryKey(sessionId)
}

object LiveTourIceTable : Table("live_tour_ice") {
    val id = varchar("id", 50)
    val sessionId = varchar("session_id", 50)
    val fromCaller = bool("from_caller")
    val candidate = text("candidate")
    val sdpMid = varchar("sdp_mid", 64).nullable()
    val sdpMLineIndex = integer("sdp_m_line_index").nullable()

    override val primaryKey = PrimaryKey(id)
}

object CommunityStoriesTable : Table("community_stories") {
    val id = varchar("id", 50)
    val authorId = varchar("author_id", 50).references(UsersTable.id)
    val content = text("content")
    val rating = integer("rating").default(5)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ChatConnectionsTable : Table("chat_connections") {
    val id = varchar("id", 80)
    val senderId = varchar("sender_id", 50).references(UsersTable.id)
    val receiverId = varchar("receiver_id", 50).references(UsersTable.id)
    val status = varchar("status", 20)
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object ChatMessagesTable : Table("chat_messages") {
    val id = varchar("id", 80)
    val listingId = varchar("listing_id", 50).references(ListingsTable.id)
    val bookingId = varchar("booking_id", 50).default("")
    val senderId = varchar("sender_id", 50).references(UsersTable.id)
    val receiverId = varchar("receiver_id", 50).references(UsersTable.id)
    val content = text("content")
    val timestamp = long("timestamp")
    val type = varchar("type", 20).default("TEXT")
    val fileName = varchar("file_name", 255).nullable()
    val mimeType = varchar("mime_type", 120).nullable()
    val attachmentId = varchar("attachment_id", 80).nullable()
    val status = varchar("status", 20).default("SENT")
    val replyToId = varchar("reply_to_id", 80).nullable()
    val replyToPreview = text("reply_to_preview").default("")
    val reactions = text("reactions").default("")
    val pinnedAt = long("pinned_at").default(0L)
    val forwarded = bool("forwarded").default(false)
    val updatedAt = long("updated_at").default(0L)
    val deleted = bool("deleted").default(false)

    override val primaryKey = PrimaryKey(id)
}

object StayReviewsTable : Table("stay_reviews") {
    val id = varchar("id", 50)
    val subject = varchar("subject", 20)
    val listingId = varchar("listing_id", 50)
    val landlordId = varchar("landlord_id", 50)
    val studentId = varchar("student_id", 50)
    val authorId = varchar("author_id", 50)
    val rating = integer("rating")
    val body = text("body").default("")
    val createdAt = long("created_at")
    val reply = text("reply").default("")
    val replyAt = long("reply_at").default(0L)
    val reportStatus = varchar("report_status", 20).default("NONE")
    val reportReason = text("report_reason").default("")
    val followUpNote = text("follow_up_note").default("")
    val reportedAt = long("reported_at").default(0L)

    override val primaryKey = PrimaryKey(id)
}

object HiddenCallLogsTable : Table("hidden_call_logs") {
    val userId = varchar("user_id", 50)
    val sessionId = varchar("session_id", 50)
    val hiddenAt = long("hidden_at")

    override val primaryKey = PrimaryKey(userId, sessionId)
}

fun initDatabase() {
    Database.connect(createDataSource())
    transaction {
        SchemaUtils.create(
            UsersTable,
            DeviceTokensTable,
            AccountEventsTable,
            UserDevicesTable,
            SignInChallengesTable,
            KycTable,
            BillsTable,
            BillItemsTable,
            ListingsTable,
            ListingPhotosTable,
            ListingRequestsTable,
            ListingBookingsTable,
            ListingEventsTable,
            RoommateMatchesTable,
            LiveTourSessionsTable,
            LiveTourRtcTable,
            LiveTourIceTable,
            CommunityStoriesTable,
            ChatConnectionsTable,
            ChatMessagesTable,
            StayReviewsTable,
            HiddenCallLogsTable,
            EscrowTransactionsTable,
            EscrowAuditLogTable,
            PropertyRequestsTable
        )
        SchemaUtils.createMissingTablesAndColumns(
            UsersTable,
            DeviceTokensTable,
            AccountEventsTable,
            UserDevicesTable,
            SignInChallengesTable,
            KycTable,
            BillsTable,
            BillItemsTable,
            ListingsTable,
            ListingPhotosTable,
            ListingRequestsTable,
            ListingBookingsTable,
            ListingEventsTable,
            RoommateMatchesTable,
            LiveTourSessionsTable,
            LiveTourRtcTable,
            LiveTourIceTable,
            CommunityStoriesTable,
            ChatConnectionsTable,
            ChatMessagesTable,
            StayReviewsTable,
            HiddenCallLogsTable,
            EscrowTransactionsTable,
            EscrowAuditLogTable,
            PropertyRequestsTable
        )
    }
}

/**
 * Pooled connections, so a serverless instance reuses sockets instead of dialing the
 * database on every transaction. Works for both the local H2 file and managed Postgres.
 */
private fun createDataSource(): HikariDataSource = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = ServerConfig.databaseUrl
        driverClassName = ServerConfig.databaseDriver
        ServerConfig.databaseUser?.let { username = it }
        ServerConfig.databasePassword?.let { password = it }
        maximumPoolSize = ServerConfig.databasePoolSize
        isAutoCommit = false
        poolName = "havenpad-db"
    }
)
