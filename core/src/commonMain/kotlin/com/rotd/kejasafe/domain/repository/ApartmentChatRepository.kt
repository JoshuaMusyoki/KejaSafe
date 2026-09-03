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
        type: String = ApartmentChatMessage.TYPE_TEXT,
        fileName: String? = null,
        mimeType: String? = null,
        attachmentBytes: ByteArray? = null,
    ): Result<ApartmentChatMessage>

    suspend fun deleteMessage(
        listingId: String,
        peerUserId: String,
        messageId: String,
    ): Result<Unit>

    suspend fun latestMessage(listingId: String, peerUserId: String): ApartmentChatMessage?

    fun loadAttachment(attachmentId: String): ByteArray?

    /**
     * Recipient has the conversation payload (inbox/thread opened as receiver).
     * SENT → DELIVERED for incoming messages.
     */
    suspend fun markMessagesDelivered(listingId: String, peerUserId: String)

    /**
     * Recipient is viewing the thread. Incoming messages become READ.
     */
    suspend fun markMessagesRead(listingId: String, peerUserId: String)

    /** Booking-eligible and ACCEPTED connection. */
    suspend fun canMessage(listingId: String, peerUserId: String): Boolean
}
