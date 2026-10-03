package com.rotd.havenpad.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Sealed interface representing all navigation destinations in the HavenPad app.
 * Implements [NavKey] to be used with Navigation 3's back stack management.
 */
sealed interface HavenPadDestination : NavKey {

    @Serializable
    data object Home : HavenPadDestination

    @Serializable
    data object LandlordHome : HavenPadDestination

    @Serializable
    data object TenantHome : HavenPadDestination

    @Serializable
    data class PropertyDetails(val id: String) : HavenPadDestination

    @Serializable
    data object Profile : HavenPadDestination

    @Serializable
    data object EditProfile : HavenPadDestination

    /** Landlord ID and residential-address documents. [allowSkip] is used right after signup. */
    @Serializable
    data class LandlordKyc(val allowSkip: Boolean = false) : HavenPadDestination

    /** Student university ID. [allowSkip] is used right after signup. */
    @Serializable
    data class StudentKyc(val allowSkip: Boolean = false) : HavenPadDestination

    /** HavenPad team: people, verification, and account actions. */
    @Serializable
    data object Admin : HavenPadDestination

    /** Web marketing entry — not used as mobile start destination. */
    @Serializable
    data object Landing : HavenPadDestination

    @Serializable
    data object Features : HavenPadDestination

    @Serializable
    data object Policy : HavenPadDestination

    @Serializable
    data object Login : HavenPadDestination

    @Serializable
    data object Register : HavenPadDestination

    @Serializable
    data object Rent : HavenPadDestination

    @Serializable
    data object RequestProperty : HavenPadDestination

    /** Browse / filter all houses. Optional query and coordinates come from tenant home. */
    @Serializable
    data class ListingSearch(
        val query: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null,
    ) : HavenPadDestination

    /** New listing when [listingId] is null; edit when set. */
    @Serializable
    data class CreateListing(val listingId: String? = null) : HavenPadDestination

    @Serializable
    data class ListingAnalytics(val listingId: String) : HavenPadDestination

    @Serializable
    data class ListingCalendar(val listingId: String) : HavenPadDestination

    /** Landlord booking requests and approved stays. */
    @Serializable
    data object LandlordBookings : HavenPadDestination

    /** LiveKit live house tour session. */
    @Serializable
    data class LiveTourCall(val sessionId: String) : HavenPadDestination

    /** In-app list of message notifications. */
    @Serializable
    data object Notifications : HavenPadDestination

    /** Apartment-scoped landlord ↔ tenant messaging. */
    @Serializable
    data object Chat : HavenPadDestination

    @Serializable
    data class ChatThread(
        val peerUserId: String,
        val listingId: String,
        val listingTitle: String,
        val bookingId: String,
        val displayName: String,
        val peerIsLandlord: Boolean,
    ) : HavenPadDestination
}
