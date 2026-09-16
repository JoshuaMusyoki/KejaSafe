package com.rotd.kejasafe.db

import com.rotd.kejasafe.BillStatus
import com.rotd.kejasafe.CampusProximity
import com.rotd.kejasafe.config.ServerConfig
import com.rotd.kejasafe.ElectricityBilling
import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.ListingStatus
import com.rotd.kejasafe.LiveTourMediaType
import com.rotd.kejasafe.LiveTourStatus
import com.rotd.kejasafe.PropertyType
import com.rotd.kejasafe.RequestType
import com.rotd.kejasafe.RoommateMatchStatus
import com.rotd.kejasafe.UserRole
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
    val submittedAt = long("submitted_at")

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
    val dueDate = long("due_date")
    val status = enumerationByName("status", 20, BillStatus::class).default(BillStatus.UNPAID)
    val depositIncluded = bool("deposit_included").default(false)
    val electricityNote = varchar("electricity_note", 200).nullable()
    val paymentRequested = bool("payment_requested").default(false)

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
    val status = enumerationByName("status", 20, com.rotd.kejasafe.BookingStatus::class)
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

fun initDatabase() {
    Database.connect(createDataSource())
    transaction {
        SchemaUtils.create(
            UsersTable,
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
            CommunityStoriesTable
        )
        SchemaUtils.createMissingTablesAndColumns(
            UsersTable,
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
            CommunityStoriesTable
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
        poolName = "kejasafe-db"
    }
)
