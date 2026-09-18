package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.BookingStatus
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.SendApartmentChatMessageRequest
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.data.platformEpochMillis
import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.Connection
import com.rotd.kejasafe.domain.chat.ConnectionStatus
import com.rotd.kejasafe.domain.chat.MessageStatus
import com.rotd.kejasafe.domain.chat.apartmentConversationKey
import com.rotd.kejasafe.domain.chat.connectionIdFor
import com.rotd.kejasafe.domain.chat.looksLikeUserFacingFileName
import com.rotd.kejasafe.domain.repository.ApartmentChatRepository
import com.rotd.kejasafe.media.ChatAttachmentStore
import com.rotd.kejasafe.media.MAX_CHAT_ATTACHMENT_BYTES
import com.rotd.kejasafe.media.MAX_CHAT_ATTACHMENT_MB
import com.rotd.kejasafe.media.looksLikePdf
import com.rotd.kejasafe.network.ChatClient
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.storage.LocalSettings
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Apartment-scoped chat synced through the Ktor API so messages reach the other device.
 * Local cache keeps the UI snappy; server is the source of truth.
 */
class ApartmentChatRepositoryImpl(
    private val listingRepository: ListingRepository,
    private val attachments: ChatAttachmentStore,
) : ApartmentChatRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val peers = MutableStateFlow<List<ApartmentChatPeer>>(emptyList())
    private val messagesByKey = MutableStateFlow<Map<String, List<ApartmentChatMessage>>>(emptyMap())
    private val messageEpoch = MutableStateFlow(0L)
    private val connections = MutableStateFlow<List<Connection>>(emptyList())

    init {
        loadConnections()
    }

    override fun observeEligiblePeers(): Flow<List<ApartmentChatPeer>> = peers.asStateFlow()

    override fun observeMessageEpoch(): Flow<Long> = messageEpoch.asStateFlow()

    override fun observeConnections(): Flow<List<Connection>> = connections.asStateFlow()

    override fun observeMessages(listingId: String, peerUserId: String): Flow<List<ApartmentChatMessage>> {
        val me = SessionManager.currentUser.value?.id.orEmpty()
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        return messagesByKey.map { map -> map[key].orEmpty().sortedBy { it.timestamp } }
    }

    override fun clearPeers() {
        peers.value = emptyList()
    }

    override suspend fun refreshEligiblePeers(): Result<List<ApartmentChatPeer>> {
        val me = SessionManager.currentUser.value
            ?: run {
                clearPeers()
                return Result.failure(IllegalStateException("Not signed in"))
            }
        syncConnectionsFromServer()

        val bookings = listingRepository.getMyBookings().getOrElse { return Result.failure(it) }
        val confirmed = bookings.value.filter { it.status == BookingStatus.CONFIRMED }
        val bookingPeers = confirmed
            .mapNotNull { booking -> booking.toPeer(me.id, me.role) }
            .distinctBy { "${it.listingId}_${it.userId}" }

        ensureLandlordTenantConnections(me.id, me.role, bookingPeers)
        peers.value = bookingPeers

        val matePeers = if (me.role == UserRole.STUDENT) {
            listingRepository.getApartmentMates()
                .getOrElse { emptyList() }
                .mapNotNull { booking -> booking.toCoTenantPeer(me.id) }
                .distinctBy { "${it.listingId}_${it.userId}" }
        } else {
            emptyList()
        }

        val eligible = (bookingPeers + matePeers)
            .distinctBy { "${it.listingId}_${it.userId}" }

        peers.value = eligible
        return Result.success(eligible)
    }

    private suspend fun ensureLandlordTenantConnections(
        myId: String,
        myRole: UserRole,
        bookingPeers: List<ApartmentChatPeer>,
    ) {
        val iAmLandlord = myRole == UserRole.LANDLORD || myRole == UserRole.ADMIN
        bookingPeers.forEach { peer ->
            val isLandlordTenantPair = peer.peerIsLandlord || iAmLandlord
            if (!isLandlordTenantPair) return@forEach
            val existing = connectionBetween(myId, peer.userId)
            if (existing?.status == ConnectionStatus.ACCEPTED) return@forEach
            ChatClient.upsertConnection(peer.userId, ConnectionStatus.ACCEPTED)
                .onSuccess { applyRemoteConnection(it) }
                .onFailure {
                    applyRemoteConnection(
                        Connection(
                            id = connectionIdFor(myId, peer.userId),
                            senderId = myId,
                            receiverId = peer.userId,
                            status = ConnectionStatus.ACCEPTED,
                            timestamp = platformEpochMillis(),
                        )
                    )
                }
        }
    }

    override fun connectionBetween(userA: String, userB: String): Connection? {
        val id = connectionIdFor(userA, userB)
        return connections.value.firstOrNull { it.id == id }
    }

    override suspend fun sendConnectionRequest(toUserId: String): Result<Connection> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (me == toUserId) {
            return Result.failure(IllegalArgumentException("Cannot connect to yourself"))
        }
        if (peers.value.none { it.userId == toUserId }) {
            return Result.failure(
                IllegalStateException("You can only connect with people from a confirmed stay.")
            )
        }
        val existing = connectionBetween(me, toUserId)
        when (existing?.status) {
            ConnectionStatus.ACCEPTED -> return Result.success(existing)
            ConnectionStatus.PENDING -> {
                if (existing.receiverId == me) {
                    return acceptConnectionRequest(existing.senderId)
                }
                return Result.success(existing)
            }
            ConnectionStatus.REJECTED, null -> Unit
        }
        return ChatClient.upsertConnection(toUserId, ConnectionStatus.PENDING)
            .onSuccess { applyRemoteConnection(it) }
    }

    override suspend fun acceptConnectionRequest(fromUserId: String): Result<Connection> {
        SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return ChatClient.upsertConnection(fromUserId, ConnectionStatus.ACCEPTED)
            .onSuccess { applyRemoteConnection(it) }
    }

    override suspend fun rejectConnectionRequest(fromUserId: String): Result<Unit> {
        SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return ChatClient.upsertConnection(fromUserId, ConnectionStatus.REJECTED)
            .map { applyRemoteConnection(it); Unit }
    }

    override suspend fun canMessage(listingId: String, peerUserId: String): Boolean {
        val me = SessionManager.currentUser.value?.id ?: return false
        if (me == peerUserId) return false
        val list = peers.value.ifEmpty {
            refreshEligiblePeers().getOrNull().orEmpty()
        }
        val eligible = list.any { it.listingId == listingId && it.userId == peerUserId }
        if (!eligible) return false
        return connectionBetween(me, peerUserId)?.status == ConnectionStatus.ACCEPTED
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun sendMessage(
        listingId: String,
        bookingId: String,
        peerUserId: String,
        content: String,
        type: String,
        fileName: String?,
        mimeType: String?,
        attachmentBytes: ByteArray?,
    ): Result<ApartmentChatMessage> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val trimmed = content.trim()
        val typeNorm = type.uppercase()
        val isAttachment = typeNorm == ApartmentChatMessage.TYPE_IMAGE ||
            typeNorm == ApartmentChatMessage.TYPE_PDF
        if (!isAttachment && trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        if (isAttachment) {
            val bytes = attachmentBytes
                ?: return Result.failure(IllegalArgumentException("Attachment is missing"))
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Attachment is empty"))
            }
            if (bytes.size > MAX_CHAT_ATTACHMENT_BYTES) {
                return Result.failure(
                    IllegalArgumentException(
                        "File is too large. Choose a file under $MAX_CHAT_ATTACHMENT_MB MB.",
                    )
                )
            }
            if (typeNorm == ApartmentChatMessage.TYPE_PDF && !bytes.looksLikePdf()) {
                return Result.failure(
                    IllegalArgumentException("That file is not a readable PDF. Try another file.")
                )
            }
        }
        if (!canMessage(listingId, peerUserId)) {
            return Result.failure(
                IllegalStateException(
                    "Connect with this person first, then you can message them."
                )
            )
        }

        val storedFileName = when {
            typeNorm == ApartmentChatMessage.TYPE_IMAGE ->
                fileName?.takeIf { looksLikeUserFacingFileName(it) }
            else -> fileName
        }
        val request = SendApartmentChatMessageRequest(
            listingId = listingId,
            bookingId = bookingId,
            peerUserId = peerUserId,
            content = trimmed,
            type = if (isAttachment) typeNorm else ApartmentChatMessage.TYPE_TEXT,
            fileName = storedFileName,
            mimeType = mimeType,
            attachmentBase64 = attachmentBytes?.let { Base64.encode(it) },
        )
        return ChatClient.sendMessage(request).onSuccess { message ->
            message.attachmentId?.let { id ->
                attachmentBytes?.let { attachments.save(id, it) }
            }
            mergeRemoteMessages(listingId, peerUserId, listOf(message))
        }
    }

    override suspend fun deleteMessage(
        listingId: String,
        peerUserId: String,
        messageId: String,
    ): Result<Unit> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (!canMessage(listingId, peerUserId)) {
            return Result.failure(IllegalStateException("Not allowed for this apartment chat"))
        }
        val remote = ChatClient.deleteMessage(messageId)
        if (remote.isFailure) return remote

        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        val existing = messagesByKey.value[key].orEmpty()
        val target = existing.firstOrNull { it.id == messageId }
        target?.attachmentId?.let { attachments.delete(it) }
        val next = existing.filterNot { it.id == messageId }
        persistMessages(key, next)
        messagesByKey.update { it + (key to next) }
        bumpMessageEpoch()
        return Result.success(Unit)
    }

    override suspend fun latestMessage(listingId: String, peerUserId: String): ApartmentChatMessage? {
        syncConversation(listingId, peerUserId)
        val me = SessionManager.currentUser.value?.id ?: return null
        val key = apartmentConversationKey(listingId, me, peerUserId)
        return messagesByKey.value[key].orEmpty().maxByOrNull { it.timestamp }
    }

    override fun loadAttachment(attachmentId: String): ByteArray? =
        attachments.load(attachmentId)

    override suspend fun markMessagesDelivered(listingId: String, peerUserId: String) {
        ChatClient.markDelivered(listingId, peerUserId)
        syncConversation(listingId, peerUserId)
    }

    override suspend fun markMessagesRead(listingId: String, peerUserId: String) {
        ChatClient.markRead(listingId, peerUserId)
        syncConversation(listingId, peerUserId)
    }

    /**
     * Pull remote messages into the local cache (and download any missing attachments).
     * Called when opening a thread / refreshing inbox previews.
     */
    override suspend fun syncConversation(listingId: String, peerUserId: String) {
        val me = SessionManager.currentUser.value?.id ?: return
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        val localMax = messagesByKey.value[key].orEmpty().maxOfOrNull { it.timestamp } ?: 0L
        // Re-fetch a small overlap so status upgrades (delivered/read) land.
        val after = (localMax - 60_000L).coerceAtLeast(0L)
        val remote = ChatClient.listMessages(listingId, peerUserId, after).getOrNull() ?: return
        mergeRemoteMessages(listingId, peerUserId, remote)
        ChatClient.markDelivered(listingId, peerUserId)
        hydrateMissingAttachments(remote)
    }

    private suspend fun hydrateMissingAttachments(messages: List<ApartmentChatMessage>) {
        messages.forEach { message ->
            val id = message.attachmentId ?: return@forEach
            if (attachments.load(id) != null) return@forEach
            ChatClient.downloadAttachment(id).onSuccess { bytes ->
                attachments.save(id, bytes)
            }
        }
    }

    private fun mergeRemoteMessages(
        listingId: String,
        peerUserId: String,
        remote: List<ApartmentChatMessage>,
    ) {
        if (remote.isEmpty()) return
        val me = SessionManager.currentUser.value?.id ?: return
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        messagesByKey.update { current ->
            val merged = (current[key].orEmpty() + remote)
                .associateBy { it.id }
                .values
                .sortedBy { it.timestamp }
            persistMessages(key, merged)
            current + (key to merged)
        }
        bumpMessageEpoch()
    }

    private suspend fun syncConnectionsFromServer() {
        ChatClient.listConnections().onSuccess { remote ->
            connections.value = remote
            persistConnections(remote)
        }
    }

    private fun applyRemoteConnection(connection: Connection) {
        connections.update { current ->
            val next = current.filterNot { it.id == connection.id } + connection
            persistConnections(next)
            next
        }
    }

    private fun bumpMessageEpoch() {
        messageEpoch.update { it + 1L }
    }

    private fun loadConnections() {
        val raw = LocalSettings.getString(CONNECTIONS_KEY) ?: return
        val loaded = runCatching {
            json.decodeFromString(ListSerializer(Connection.serializer()), raw)
        }.getOrDefault(emptyList())
        connections.value = loaded
    }

    private fun persistConnections(list: List<Connection>) {
        LocalSettings.putString(
            CONNECTIONS_KEY,
            json.encodeToString(ListSerializer(Connection.serializer()), list)
        )
    }

    private fun ListingBooking.toPeer(myId: String, myRole: UserRole): ApartmentChatPeer? {
        val guestId = guestUserId?.takeIf { it.isNotBlank() } ?: return null
        val landlord = landlordId.takeIf { it.isNotBlank() } ?: return null
        return when {
            myId == landlord && guestId != myId -> ApartmentChatPeer(
                userId = guestId,
                displayName = guestName.ifBlank { "Tenant" },
                listingId = listingId,
                listingTitle = listingTitle.ifBlank { "Apartment" },
                bookingId = id,
                peerIsLandlord = false,
                joinedAtMillis = startDateMillis,
            )
            myId == guestId && landlord != myId -> ApartmentChatPeer(
                userId = landlord,
                displayName = "Landlord · ${listingTitle.ifBlank { "Apartment" }}",
                listingId = listingId,
                listingTitle = listingTitle.ifBlank { "Apartment" },
                bookingId = id,
                peerIsLandlord = true,
                joinedAtMillis = startDateMillis,
            )
            myRole == UserRole.LANDLORD || myRole == UserRole.ADMIN -> {
                if (guestId == myId) null
                else ApartmentChatPeer(
                    userId = guestId,
                    displayName = guestName.ifBlank { "Tenant" },
                    listingId = listingId,
                    listingTitle = listingTitle.ifBlank { "Apartment" },
                    bookingId = id,
                    peerIsLandlord = false,
                    joinedAtMillis = startDateMillis,
                ).takeIf { landlord == myId || landlord.isBlank() }
            }
            else -> null
        }
    }

    private fun ListingBooking.toCoTenantPeer(myId: String): ApartmentChatPeer? {
        val guestId = guestUserId?.takeIf { it.isNotBlank() } ?: return null
        if (guestId == myId) return null
        return ApartmentChatPeer(
            userId = guestId,
            displayName = guestName.ifBlank { "Tenant" },
            listingId = listingId,
            listingTitle = listingTitle.ifBlank { "Apartment" },
            bookingId = id,
            peerIsLandlord = false,
            joinedAtMillis = startDateMillis,
        )
    }

    private fun ensureMessagesLoaded(key: String) {
        if (messagesByKey.value.containsKey(key)) return
        val raw = LocalSettings.getString(storageKey(key)) ?: return
        val loaded = runCatching {
            json.decodeFromString(ListSerializer(ApartmentChatMessage.serializer()), raw)
        }.getOrDefault(emptyList())
        messagesByKey.update { it + (key to loaded) }
    }

    private fun persistMessages(key: String, messages: List<ApartmentChatMessage>) {
        LocalSettings.putString(
            storageKey(key),
            json.encodeToString(ListSerializer(ApartmentChatMessage.serializer()), messages)
        )
    }

    private fun storageKey(conversationKey: String) = "apartment_chat_$conversationKey"

    private companion object {
        const val CONNECTIONS_KEY = "apartment_chat_connections"
    }
}
