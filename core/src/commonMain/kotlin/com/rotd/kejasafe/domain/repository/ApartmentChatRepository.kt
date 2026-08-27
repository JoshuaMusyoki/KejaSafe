package com.rotd.kejasafe.domain.repository

import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.Connection
import kotlinx.coroutines.flow.Flow

/**
 * Landlord ↔ tenant messaging scoped to shared apartments (confirmed bookings).
 * Messaging requires an ACCEPTED local connection between the two users.
 */
interface ApartmentChatRepository {
    fun observeEligiblePeers(): Flow<List<ApartmentChatPeer>>

    fun observeMessageEpoch(): Flow<Long>

    fun observeConnections(): Flow<List<Connection>>

    fun observeMessages(listingId: String, peerUserId: String): Flow<List<ApartmentChatMessage>>

    suspend fun refreshEligiblePeers(): Result<List<ApartmentChatPeer>>

    fun clearPeers()

    suspend fun sendConnectionRequest(toUserId: String): Result<Connection>

    suspend fun acceptConnectionRequest(fromUserId: String): Result<Connection>

    suspend fun rejectConnectionRequest(fromUserId: String): Result<Unit>

    fun connectionBetween(userA: String, userB: String): Connection?

    suspend fun sendMessage(
        listingId: String,
        bookingId: String,
        peerUserId: String,
        content: String,
    ): Result<ApartmentChatMessage>

    suspend fun deleteMessage(
        listingId: String,
        peerUserId: String,
        messageId: String,
    ): Result<Unit>

    suspend fun latestMessage(listingId: String, peerUserId: String): ApartmentChatMessage?

    /** Booking-eligible and ACCEPTED connection. */
    suspend fun canMessage(listingId: String, peerUserId: String): Boolean
}
