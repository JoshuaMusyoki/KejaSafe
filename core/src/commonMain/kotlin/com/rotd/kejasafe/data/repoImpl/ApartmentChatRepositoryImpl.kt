package com.rotd.kejasafe.data.repoImpl

import com.rotd.kejasafe.BookingStatus
import com.rotd.kejasafe.ListingBooking
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.domain.chat.ApartmentChatMessage
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.domain.chat.Connection
import com.rotd.kejasafe.domain.chat.ConnectionStatus
import com.rotd.kejasafe.domain.chat.apartmentConversationKey
import com.rotd.kejasafe.domain.chat.connectionIdFor
import com.rotd.kejasafe.domain.repository.ApartmentChatRepository
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.storage.LocalSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * Apartment-scoped chat + local connect requests.
 * Eligibility: confirmed booking. Messaging: ACCEPTED connection.
 */
class ApartmentChatRepositoryImpl(
    private val listingRepository: ListingRepository,
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
        clearPeers()
        val bookings = listingRepository.getMyBookings().getOrElse { return Result.failure(it) }
        val confirmed = bookings.value.filter { it.status == BookingStatus.CONFIRMED }
        val bookingPeers = confirmed
            .mapNotNull { booking -> booking.toPeer(me.id, me.role) }
            .distinctBy { "${it.listingId}_${it.userId}" }

        val matePeers = listingRepository.getApartmentMates()
            .getOrElse { emptyList() }
            .mapNotNull { booking -> booking.toCoTenantPeer(me.id) }
            .distinctBy { "${it.listingId}_${it.userId}" }

        val eligible = (bookingPeers + matePeers)
            .distinctBy { "${it.listingId}_${it.userId}" }

        // Landlord ↔ tenant from a confirmed stay: connected by default.
        // Co-tenants keep requiring an explicit Connect request.
        ensureLandlordTenantConnections(me.id, me.role, bookingPeers)

        peers.value = eligible
        return Result.success(eligible)
    }

    /**
     * Upsert ACCEPTED local connections for landlord–tenant booking pairs.
     */
    private fun ensureLandlordTenantConnections(
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
            upsertConnection(
                Connection(
                    id = connectionIdFor(myId, peer.userId),
                    senderId = myId,
                    receiverId = peer.userId,
                    status = ConnectionStatus.ACCEPTED,
                    timestamp = System.currentTimeMillis(),
                )
            )
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
                // They already requested us — accept instead.
                if (existing.receiverId == me) {
                    return acceptConnectionRequest(existing.senderId)
                }
                return Result.success(existing)
            }
            ConnectionStatus.REJECTED, null -> Unit
        }
        val connection = Connection(
            id = connectionIdFor(me, toUserId),
            senderId = me,
            receiverId = toUserId,
            status = ConnectionStatus.PENDING,
            timestamp = System.currentTimeMillis(),
        )
        upsertConnection(connection)
        return Result.success(connection)
    }

    override suspend fun acceptConnectionRequest(fromUserId: String): Result<Connection> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = connectionBetween(me, fromUserId)
            ?: return Result.failure(IllegalStateException("No connection request found"))
        if (existing.receiverId != me) {
            return Result.failure(IllegalStateException("Only the recipient can accept"))
        }
        if (existing.status == ConnectionStatus.ACCEPTED) {
            return Result.success(existing)
        }
        val accepted = existing.copy(
            status = ConnectionStatus.ACCEPTED,
            timestamp = System.currentTimeMillis(),
        )
        upsertConnection(accepted)
        return Result.success(accepted)
    }

    override suspend fun rejectConnectionRequest(fromUserId: String): Result<Unit> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = connectionBetween(me, fromUserId)
            ?: return Result.failure(IllegalStateException("No connection request found"))
        if (existing.receiverId != me) {
            return Result.failure(IllegalStateException("Only the recipient can decline"))
        }
        upsertConnection(
            existing.copy(
                status = ConnectionStatus.REJECTED,
                timestamp = System.currentTimeMillis(),
            )
        )
        return Result.success(Unit)
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

    override suspend fun sendMessage(
        listingId: String,
        bookingId: String,
        peerUserId: String,
        content: String,
    ): Result<ApartmentChatMessage> {
        val me = SessionManager.currentUser.value?.id
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }
        if (!canMessage(listingId, peerUserId)) {
            return Result.failure(
                IllegalStateException(
                    "Connect with this person first, then you can message them."
                )
            )
        }
        val message = ApartmentChatMessage(
            id = "${System.currentTimeMillis()}_${Random.nextLong()}",
            listingId = listingId,
            bookingId = bookingId,
            senderId = me,
            receiverId = peerUserId,
            content = trimmed,
            timestamp = System.currentTimeMillis(),
        )
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        messagesByKey.update { current ->
            val next = (current[key].orEmpty() + message).sortedBy { it.timestamp }
            persistMessages(key, next)
            current + (key to next)
        }
        bumpMessageEpoch()
        return Result.success(message)
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
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        val existing = messagesByKey.value[key].orEmpty()
        val target = existing.firstOrNull { it.id == messageId }
            ?: return Result.failure(IllegalStateException("Message not found"))
        if (target.senderId != me) {
            return Result.failure(IllegalStateException("You can only delete your own messages"))
        }
        val next = existing.filterNot { it.id == messageId }
        persistMessages(key, next)
        messagesByKey.update { it + (key to next) }
        bumpMessageEpoch()
        return Result.success(Unit)
    }

    override suspend fun latestMessage(listingId: String, peerUserId: String): ApartmentChatMessage? {
        val me = SessionManager.currentUser.value?.id ?: return null
        val key = apartmentConversationKey(listingId, me, peerUserId)
        ensureMessagesLoaded(key)
        return messagesByKey.value[key].orEmpty().maxByOrNull { it.timestamp }
    }

    private fun bumpMessageEpoch() {
        messageEpoch.update { it + 1L }
    }

    private fun upsertConnection(connection: Connection) {
        connections.update { current ->
            val next = current.filterNot { it.id == connection.id } + connection
            persistConnections(next)
            next
        }
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

    /** Another confirmed guest on a shared listing (tenant↔tenant; connect required). */
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
