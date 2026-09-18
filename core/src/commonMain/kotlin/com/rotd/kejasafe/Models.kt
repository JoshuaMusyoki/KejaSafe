package com.rotd.kejasafe

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    STUDENT, LANDLORD, ADMIN
}

@Serializable
enum class KycStatus {
    NOT_SUBMITTED, PENDING, VERIFIED, REJECTED
}

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val kycStatus: KycStatus = KycStatus.NOT_SUBMITTED,
    val profilePictureUrl: String? = null,
    val major: String? = null,
    val yearOfStudy: Int? = null,
    val roommatePreferences: List<String> = emptyList(),
    val phone: String? = null,
    val businessName: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String,
    val username: String,
    val major: String? = null,
    val yearOfStudy: Int? = null,
    val roommatePreferences: List<String> = emptyList(),
    val phone: String? = null,
    val businessName: String? = null
)

@Serializable
data class KycSubmission(
    val userId: String,
    val documentType: String,
    val documentNumber: String,
    val documentFrontImageUrl: String,
    val documentBackImageUrl: String? = null,
    val selfieImageUrl: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val role: UserRole
)

@Serializable
data class RegistrationRequest(
    val email: String,
    val username: String,
    val fullName: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class LoginRequest(
    val identifier: String, // email or username
    val password: String
)

@Serializable
enum class BillStatus {
    UNPAID, PAID, OVERDUE
}

@Serializable
data class BillItem(
    val name: String,
    val amount: Double
)

/** How electricity is charged for a listing unit. */
@Serializable
enum class ElectricityBilling {
    /** Prepaid token — tenant tops up; no fixed monthly line. */
    TOKEN,
    /** Fixed monthly amount billed with rent. */
    MONTHLY
}

@Serializable
data class ExtraCharge(
    val name: String,
    val amount: Double
)

/**
 * Recurring and one-time charges the landlord sets on a listing.
 * Used when generating tenant bills and shown on the listing form with a running total.
 */
@Serializable
data class ListingCharges(
    val garbageCollection: Double = 0.0,
    val water: Double = 0.0,
    val electricityBilling: ElectricityBilling = ElectricityBilling.TOKEN,
    /** Used only when [electricityBilling] is [ElectricityBilling.MONTHLY]. */
    val electricityMonthly: Double = 0.0,
    val extraCharges: List<ExtraCharge> = emptyList(),
    /** Security / refundable deposit (billed once with first invoice for a booking). */
    val deposit: Double = 0.0
) {
    /** Monthly utilities only (no base rent, no deposit). */
    fun monthlyUtilityTotal(): Double {
        var total = garbageCollection.coerceAtLeast(0.0) + water.coerceAtLeast(0.0)
        if (electricityBilling == ElectricityBilling.MONTHLY) {
            total += electricityMonthly.coerceAtLeast(0.0)
        }
        total += extraCharges.sumOf { it.amount.coerceAtLeast(0.0) }
        return total
    }

    fun monthlyGrandTotal(baseRent: Double): Double =
        baseRent.coerceAtLeast(0.0) + monthlyUtilityTotal()

    fun utilityLineItems(): List<BillItem> = buildList {
        if (garbageCollection > 0) add(BillItem("Garbage collection", garbageCollection))
        if (water > 0) add(BillItem("Water", water))
        when (electricityBilling) {
            ElectricityBilling.TOKEN -> add(BillItem("Electricity (prepaid token)", 0.0))
            ElectricityBilling.MONTHLY -> {
                if (electricityMonthly > 0) add(BillItem("Electricity", electricityMonthly))
            }
        }
        extraCharges.forEach { c ->
            if (c.name.isNotBlank() && c.amount > 0) {
                add(BillItem(c.name.trim(), c.amount))
            }
        }
    }
}

@Serializable
data class RentBill(
    val id: String,
    val userId: String,
    val propertyId: String,
    val propertyTitle: String = "",
    /** Tenant / guest the bill is issued to. */
    val tenantName: String = "",
    val bookingId: String? = null,
    val month: Int,
    val year: Int,
    val items: List<BillItem>,
    val totalAmount: Double,
    val dueDate: Long,
    val status: BillStatus = BillStatus.UNPAID,
    val eTimsInvoiceUrl: String? = null,
    val kraTaxAmount: Double? = null,
    val depositIncluded: Boolean = false,
    val electricityNote: String? = null,
    /** Landlord asked the tenant to settle this bill. */
    val paymentRequested: Boolean = false
)

@Serializable
data class PaymentRequest(
    val billId: String,
    val amount: Double = 0.0,
    val paymentMethod: String = "M-PESA"
)

@Serializable
data class RequestPaymentRequest(
    val billId: String
)

@Serializable
data class GenerateBillRequest(
    val bookingId: String,
    val month: Int? = null,
    val year: Int? = null,
    /** Optional overrides; empty means use charges defined on the listing. */
    val utilities: List<BillItem> = emptyList(),
    val includeDeposit: Boolean? = null
)

/**
 * Landlord adjusts an unpaid bill — water/electricity and other lines change month to month.
 * [items] replaces the full line-item list; total is recomputed server-side.
 */
@Serializable
data class UpdateBillRequest(
    val items: List<BillItem>,
    val electricityNote: String? = null
)

@Serializable
enum class ListingStatus {
    VERIFIED, PENDING, DRAFT, REJECTED
}

@Serializable
enum class CampusProximity {
    INSIDE_CAMPUS, WALKING_DISTANCE, SHORT_COMMUTE, FAR
}

/** Physical unit type for a listing (not occupancy capacity). */
@Serializable
enum class PropertyType {
    SINGLE_ROOM,
    BEDSITTER,
    ONE_BEDROOM,
    TWO_BEDROOM,
    THREE_BEDROOM,
    MAISONETTE
}

@Serializable
data class PropertyListing(
    val id: String,
    val title: String,
    val address: String,
    val landlordId: String = "",
    val pricePerMonth: Double? = null,
    val currency: String = "KES",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    /** Full photo records (ids needed to delete while editing). */
    val photos: List<ListingPhotoResponse> = emptyList(),
    val status: ListingStatus = ListingStatus.PENDING,
    val proximity: CampusProximity? = null,
    val university: String? = null,
    /** SingleRoom, Bedsitter, 1BR, 2BR, 3BR, or maisonette. */
    val propertyType: PropertyType? = null,
    /** How many units of this type are available. */
    val unitsCount: Int? = null,
    val charges: ListingCharges = ListingCharges(),
    val activeBookings: Int = 0,
    val totalViews: Int = 0,
    val inquiries: Int = 0,
    val createdAt: Long = 0L
)

@Serializable
data class CreateListingRequest(
    val title: String,
    val address: String,
    val proximity: CampusProximity = CampusProximity.INSIDE_CAMPUS,
    val university: String? = null,
    val pricePerMonth: Double? = null,
    val propertyType: PropertyType = PropertyType.BEDSITTER,
    val unitsCount: Int? = 1,
    val charges: ListingCharges = ListingCharges()
)

@Serializable
data class ListingPhotoResponse(
    val id: String,
    val listingId: String,
    val url: String,
    val sortOrder: Int = 0
)

@Serializable
enum class RequestType {
    LIVE_INSPECTION, ROOMMATE_SPLIT, BOOKING, GENERAL
}

@Serializable
data class IncomingRequest(
    val id: String,
    val requesterName: String,
    val type: RequestType,
    val propertyTitle: String,
    val detail: String,
    val timeLabel: String
)

@Serializable
enum class RoommateMatchStatus {
    SUGGESTED, ACCEPTED, DECLINED
}

@Serializable
data class RoommateMatchGuest(
    val bookingId: String,
    val userId: String?,
    val name: String,
    val preferences: List<String> = emptyList(),
    val startDateMillis: Long,
    val endDateMillis: Long,
    val budgetMax: Double? = null,
    val major: String? = null,
    val yearOfStudy: Int? = null
)

@Serializable
data class RoommateMatch(
    val id: String,
    val listingId: String,
    val listingTitle: String = "",
    val landlordId: String = "",
    val guests: List<RoommateMatchGuest> = emptyList(),
    val score: Int = 0,
    val status: RoommateMatchStatus = RoommateMatchStatus.SUGGESTED,
    val pricePerMonth: Double? = null,
    val currency: String = "KES"
)

@Serializable
data class ListingPerformance(
    val totalViews: Int,
    val viewsDeltaPercent: Int,
    val inquiries: Int,
    val hasNewInquiries: Boolean,
    val activeBookings: Int,
    val unitsCount: Int
)

@Serializable
data class ListingDayStat(
    val label: String,
    val views: Int,
    val inquiries: Int = 0
)

/** Per-listing analytics for landlord “View Analytics”. */
@Serializable
data class ListingAnalytics(
    val listingId: String,
    val title: String,
    val address: String,
    val status: ListingStatus = ListingStatus.PENDING,
    val university: String? = null,
    val pricePerMonth: Double? = null,
    val currency: String = "KES",
    val propertyType: PropertyType? = null,
    val unitsCount: Int = 1,
    val totalViews: Int = 0,
    val viewsDeltaPercent: Int = 0,
    val inquiries: Int = 0,
    val hasNewInquiries: Boolean = false,
    val activeBookings: Int = 0,
    val conversionRatePercent: Int = 0,
    val occupancyPercent: Int = 0,
    val weekly: List<ListingDayStat> = emptyList(),
    val createdAt: Long = 0L
)

@Serializable
enum class BookingStatus {
    CONFIRMED, PENDING, CANCELLED, COMPLETED
}

@Serializable
data class ListingBooking(
    val id: String,
    val listingId: String,
    val listingTitle: String = "",
    val landlordId: String = "",
    val guestName: String,
    val guestUserId: String? = null,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val notes: String = "",
    val slotCount: Int = 1,
    val pricePerMonth: Double? = null,
    val currency: String = "KES",
    val seekingRoommate: Boolean = false,
    val budgetMax: Double? = null,
    val roommateMatchId: String? = null
)

@Serializable
data class BookListingRequest(
    /** ISO date yyyy-MM-dd */
    val startDate: String,
    /** ISO date yyyy-MM-dd — defaults to start + 30 days if blank on server */
    val endDate: String? = null,
    val notes: String = "",
    val slotCount: Int = 1,
    /** When true, booking waits for an auto-matched roommate before landlord review. */
    val seekingRoommate: Boolean = false,
    /** Optional monthly budget ceiling (KES) used for match scoring. */
    val budgetMax: Double? = null
)

@Serializable
enum class LiveTourStatus {
    SCHEDULED, READY, ACTIVE, ENDED
}

@Serializable
enum class LiveTourMediaType {
    VIDEO, AUDIO
}

@Serializable
data class CreateLiveTourRequest(
    /** Preferred time / note for the landlord (free text). */
    val preferredTimeNote: String = "",
    val mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO
)

/** Start or resume an immediate LiveKit call with a chat peer on a listing. */
@Serializable
data class DirectLiveTourRequest(
    val peerUserId: String,
    val mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO
)

@Serializable
data class RescheduleLiveTourRequest(
    val preferredTimeNote: String
)

@Serializable
data class LiveTourSession(
    val id: String,
    val listingId: String,
    val listingTitle: String = "",
    val landlordId: String,
    val landlordName: String = "",
    val tenantId: String,
    val tenantName: String = "",
    val requestId: String = "",
    val roomName: String = "",
    val mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO,
    val status: LiveTourStatus = LiveTourStatus.SCHEDULED,
    val preferredTimeNote: String = "",
    val createdAt: Long = 0L,
    val startedAt: Long? = null,
    val endedAt: Long? = null
)

@Serializable
data class LiveKitConnectionDetails(
    val serverUrl: String,
    val roomName: String,
    val participantName: String,
    val participantToken: String,
    val sessionId: String = "",
    val listingTitle: String = "",
    val mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO
)

@Serializable
data class CalendarDaySlot(
    /** ISO date yyyy-MM-dd */
    val date: String,
    val dayOfMonth: Int,
    val bookedCount: Int,
    val unitsCount: Int,
    val fullyBooked: Boolean,
    val isToday: Boolean = false,
    val inCurrentMonth: Boolean = true
)

@Serializable
data class ListingCalendar(
    val listingId: String,
    val title: String,
    val address: String,
    val propertyType: PropertyType? = null,
    val unitsCount: Int,
    val activeBookings: Int,
    val openSlots: Int,
    val fullyBooked: Boolean,
    val pricePerMonth: Double? = null,
    val currency: String = "KES",
    val month: Int,
    val year: Int,
    val monthLabel: String,
    val days: List<CalendarDaySlot> = emptyList(),
    val bookings: List<ListingBooking> = emptyList()
)

@Serializable
data class CreateListingDraft(
    val title: String = "",
    val address: String = "",
    val proximity: CampusProximity = CampusProximity.INSIDE_CAMPUS,
    val university: String = "",
    val pricePerMonth: String = "",
    val propertyType: PropertyType = PropertyType.BEDSITTER,
    val unitsCount: String = "1",
    val garbageCollection: String = "",
    val water: String = "",
    val electricityIsToken: Boolean = true,
    val electricityMonthly: String = "",
    val deposit: String = "",
    val extraCharges: List<DraftExtraCharge> = emptyList()
)

@Serializable
data class DraftExtraCharge(
    val id: String,
    val name: String = "",
    val amount: String = ""
)

/**
 * Local (pre-upload) photo selected by the landlord on device.
 * Not persisted until [ListingClient.uploadListingPhotos] succeeds.
 */
data class PendingListingPhoto(
    val id: String,
    val fileName: String,
    val mimeType: String = "image/jpeg",
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PendingListingPhoto
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class PickedImage(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PickedImage
        return fileName == other.fileName && bytes.contentEquals(other.bytes) && mimeType == other.mimeType
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

@Serializable
data class CommunityStory(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: UserRole,
    val authorProfilePictureUrl: String? = null,
    val content: String,
    val rating: Int = 5,
    val createdAt: Long = 0L
)

@Serializable
data class PostStoryRequest(
    val content: String,
    val rating: Int = 5
)

/** Live counters shown on the marketing site, computed from real server data. */
@Serializable
data class PlatformStats(
    val studentsHoused: Int = 0,
    val verifiedListings: Int = 0,
    val universitiesCovered: Int = 0,
    val landlords: Int = 0,
    val protectedInEscrow: Double = 0.0,
    val stories: Int = 0,
    val averageRating: Double = 0.0
)

/** Client → server apartment chat message (text or attachment metadata + optional bytes). */
@Serializable
data class SendApartmentChatMessageRequest(
    val listingId: String,
    val bookingId: String,
    val peerUserId: String,
    val content: String = "",
    val type: String = "TEXT",
    val fileName: String? = null,
    val mimeType: String? = null,
    /** Base64 payload for IMAGE/PDF; omitted for text. */
    val attachmentBase64: String? = null,
)

/** Sync a Connect / Accept / Reject between two users. */
@Serializable
data class UpsertChatConnectionRequest(
    val peerUserId: String,
    val status: com.rotd.kejasafe.domain.chat.ConnectionStatus,
)
