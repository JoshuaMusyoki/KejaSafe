package com.rotd.kejasafe.domain.chat

import kotlinx.serialization.Serializable

/**
 * Someone the current user may message because they share a confirmed booking
 * on the same listing (apartment).
 */
@Serializable
data class ApartmentChatPeer(
    val userId: String,
    val displayName: String,
    val listingId: String,
    val listingTitle: String,
    val bookingId: String,
    /** True when the peer is the landlord of [listingId]. */
    val peerIsLandlord: Boolean,
    /** Booking start (tenant joined / stay began), epoch millis. */
    val joinedAtMillis: Long = 0L,
)

@Serializable
data class ApartmentChatMessage(
    val id: String,
    val listingId: String,
    val bookingId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val type: String = "TEXT",
)

fun apartmentConversationKey(listingId: String, userA: String, userB: String): String {
    val pair = listOf(userA, userB).sorted().joinToString("_")
    return "${listingId}_$pair"
}

/** Canonical connection id shared by both users. */
fun connectionIdFor(userA: String, userB: String): String =
    listOf(userA, userB).sorted().joinToString("_")
