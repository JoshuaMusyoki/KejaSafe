package com.rotd.kejasafe.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing all navigation destinations in the KejaSafe app.
 * Implements [NavKey] to be used with Navigation 3's back stack management.
 */
sealed interface KejaSafeDestination : NavKey {

    @Serializable
    data object Home : KejaSafeDestination

    @Serializable
    data object LandlordHome : KejaSafeDestination

    @Serializable
    data object TenantHome : KejaSafeDestination

    @Serializable
    data class PropertyDetails(val id: String) : KejaSafeDestination

    @Serializable
    data object Profile : KejaSafeDestination

    @Serializable
    data object EditProfile : KejaSafeDestination

    /** Web marketing entry — not used as mobile start destination. */
    @Serializable
    data object Landing : KejaSafeDestination

    @Serializable
    data object Features : KejaSafeDestination

    @Serializable
    data object Policy : KejaSafeDestination

    @Serializable
    data object Login : KejaSafeDestination

    @Serializable
    data object Register : KejaSafeDestination

    @Serializable
    data object Rent : KejaSafeDestination

    /** New listing when [listingId] is null; edit when set. */
    @Serializable
    data class CreateListing(val listingId: String? = null) : KejaSafeDestination

    @Serializable
    data class ListingAnalytics(val listingId: String) : KejaSafeDestination

    @Serializable
    data class ListingCalendar(val listingId: String) : KejaSafeDestination

    /** Landlord booking requests and approved stays. */
    @Serializable
    data object LandlordBookings : KejaSafeDestination

    /** LiveKit live house tour session. */
    @Serializable
    data class LiveTourCall(val sessionId: String) : KejaSafeDestination

    /** Apartment-scoped landlord ↔ tenant messaging. */
    @Serializable
    data object Chat : KejaSafeDestination

    @Serializable
    data class ChatThread(
        val peerUserId: String,
        val listingId: String,
        val listingTitle: String,
        val bookingId: String,
        val displayName: String,
        val peerIsLandlord: Boolean,
    ) : KejaSafeDestination
}
