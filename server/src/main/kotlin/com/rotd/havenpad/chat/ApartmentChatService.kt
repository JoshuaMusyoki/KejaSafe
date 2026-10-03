package com.rotd.havenpad.chat

import com.rotd.havenpad.BookingStatus
import com.rotd.havenpad.PinChatMessageRequest
import com.rotd.havenpad.ReactToChatMessageRequest
import com.rotd.havenpad.SendApartmentChatMessageRequest
import com.rotd.havenpad.UpsertChatConnectionRequest
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.db.ChatConnectionsTable
import com.rotd.havenpad.db.ChatMessagesTable
import com.rotd.havenpad.db.ListingBookingsTable
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.UsersTable
import com.rotd.havenpad.domain.chat.ApartmentChatMessage
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.domain.chat.Connection
import com.rotd.havenpad.domain.chat.decodeMessageReactions
import com.rotd.havenpad.domain.chat.encodeMessageReactions
import com.rotd.havenpad.domain.chat.ConnectionStatus
import com.rotd.havenpad.domain.chat.MessageStatus
import com.rotd.havenpad.domain.chat.connectionIdFor
import com.rotd.havenpad.media.MAX_CHAT_ATTACHMENT_BYTES
import com.rotd.havenpad.media.looksLikePdf
import java.io.File
import java.util.Base64
import java.util.UUID
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * Server-backed apartment chat: messages and connect requests sync across devices.
 */
object ApartmentChatService {
    private val chatUploadRoot: File =
        File(ServerConfig.uploadDir, "chat").apply { mkdirs() }

    fun listConnections(userId: String): List<Connection> = transaction {
        ChatConnectionsTable.selectAll()
            .where {
                (ChatConnectionsTable.senderId eq userId) or
                    (ChatConnectionsTable.receiverId eq userId)
            }
            .map { row ->
                Connection(
                    id = row[ChatConnectionsTable.id],
                    senderId = row[ChatConnectionsTable.senderId],
                    receiverId = row[ChatConnectionsTable.receiverId],
                    status = parseConnectionStatus(row[ChatConnectionsTable.status]),
                    timestamp = row[ChatConnectionsTable.updatedAt],
                )
            }
            .sortedByDescending { it.timestamp }
    }

    fun upsertConnection(userId: String, request: UpsertChatConnectionRequest): Connection {
        require(userId != request.peerUserId) { "Cannot connect to yourself" }
        require(request.peerUserId.isNotBlank()) { "Missing peer" }
        val id = connectionIdFor(userId, request.peerUserId)
        val now = System.currentTimeMillis()
        return transaction {
            val existing = ChatConnectionsTable.selectAll()
                .where { ChatConnectionsTable.id eq id }
                .singleOrNull()

            when (request.status) {
                ConnectionStatus.PENDING -> {
                    if (existing != null) {
                        val status = parseConnectionStatus(existing[ChatConnectionsTable.status])
                        if (status == ConnectionStatus.ACCEPTED) {
                            return@transaction existing.toConnection()
                        }
                        // Peer already requested us — accept instead.
                        if (status == ConnectionStatus.PENDING &&
                            existing[ChatConnectionsTable.receiverId] == userId
                        ) {
                            ChatConnectionsTable.update({ ChatConnectionsTable.id eq id }) {
                                it[ChatConnectionsTable.status] = ConnectionStatus.ACCEPTED.name
                                it[updatedAt] = now
                            }
                        } else if (status == ConnectionStatus.REJECTED ||
                            existing[ChatConnectionsTable.senderId] == userId
                        ) {
                            ChatConnectionsTable.update({ ChatConnectionsTable.id eq id }) {
                                it[senderId] = userId
                                it[receiverId] = request.peerUserId
                                it[ChatConnectionsTable.status] = ConnectionStatus.PENDING.name
                                it[updatedAt] = now
                            }
                        }
                    } else {
                        ChatConnectionsTable.insert {
                            it[ChatConnectionsTable.id] = id
                            it[senderId] = userId
                            it[receiverId] = request.peerUserId
                            it[status] = ConnectionStatus.PENDING.name
                            it[updatedAt] = now
                        }
                    }
                }
                ConnectionStatus.ACCEPTED -> {
                    if (existing == null) {
                        ChatConnectionsTable.insert {
                            it[ChatConnectionsTable.id] = id
                            it[senderId] = userId
                            it[receiverId] = request.peerUserId
                            it[status] = ConnectionStatus.ACCEPTED.name
                            it[updatedAt] = now
                        }
                    } else {
                        val receiver = existing[ChatConnectionsTable.receiverId]
                        require(
                            receiver == userId ||
                                existing[ChatConnectionsTable.senderId] == userId
                        ) { "Not a party to this connection" }
                        ChatConnectionsTable.update({ ChatConnectionsTable.id eq id }) {
                            it[ChatConnectionsTable.status] = ConnectionStatus.ACCEPTED.name
                            it[updatedAt] = now
                        }
                    }
                }
                ConnectionStatus.REJECTED -> {
                    val row = existing ?: error("No connection request found")
                    require(row[ChatConnectionsTable.receiverId] == userId) {
                        "Only the recipient can decline"
                    }
                    ChatConnectionsTable.update({ ChatConnectionsTable.id eq id }) {
                        it[ChatConnectionsTable.status] = ConnectionStatus.REJECTED.name
                        it[updatedAt] = now
                    }
                }
            }

            ChatConnectionsTable.selectAll()
                .where { ChatConnectionsTable.id eq id }
                .single()
                .toConnection()
        }
    }

    fun listMessages(
        userId: String,
        listingId: String,
        peerUserId: String,
        afterTimestamp: Long = 0L,
    ): List<ApartmentChatMessage> = transaction {
        require(canShareApartment(userId, listingId, peerUserId)) {
            "Not allowed to view this apartment chat"
        }
        ChatMessagesTable.selectAll()
            .where {
                (ChatMessagesTable.listingId eq listingId) and
                    (
                        (
                            (ChatMessagesTable.senderId eq userId) and
                                (ChatMessagesTable.receiverId eq peerUserId)
                            ) or (
                            (ChatMessagesTable.senderId eq peerUserId) and
                                (ChatMessagesTable.receiverId eq userId)
                            )
                        ) and
                    (
                        (ChatMessagesTable.timestamp greater afterTimestamp) or
                            (ChatMessagesTable.updatedAt greater afterTimestamp)
                        )
            }
            .orderBy(ChatMessagesTable.timestamp to SortOrder.ASC)
            .map { it.toMessage() }
    }

    fun sendMessage(
        userId: String,
        request: SendApartmentChatMessageRequest,
    ): ApartmentChatMessage {
        require(request.listingId.isNotBlank()) { "Missing listing" }
        require(request.peerUserId.isNotBlank()) { "Missing peer" }
        require(userId != request.peerUserId) { "Cannot message yourself" }
        require(canShareApartment(userId, request.listingId, request.peerUserId)) {
            "You can message the landlord of this house, or people from a confirmed stay."
        }

        val typeNorm = request.type.uppercase().ifBlank { ApartmentChatMessage.TYPE_TEXT }
        val isAttachment = typeNorm == ApartmentChatMessage.TYPE_IMAGE ||
            typeNorm == ApartmentChatMessage.TYPE_PDF
        val trimmed = request.content.trim()
        require(isAttachment || trimmed.isNotEmpty()) { "Message cannot be empty" }

        var attachmentId: String? = null
        if (isAttachment) {
            val raw = request.attachmentBase64?.trim().orEmpty()
            require(raw.isNotEmpty()) { "Attachment is missing" }
            val bytes = try {
                Base64.getDecoder().decode(raw)
            } catch (_: Exception) {
                error("Invalid attachment encoding")
            }
            require(bytes.isNotEmpty()) { "Attachment is empty" }
            require(bytes.size <= MAX_CHAT_ATTACHMENT_BYTES) {
                "File is too large (max ${MAX_CHAT_ATTACHMENT_BYTES / (1024 * 1024)} MB)"
            }
            if (typeNorm == ApartmentChatMessage.TYPE_PDF) {
                require(bytes.looksLikePdf()) { "That file is not a readable PDF" }
            }
            attachmentId = "att_${UUID.randomUUID().toString().replace("-", "")}"
            File(chatUploadRoot, attachmentId).writeBytes(bytes)
        }

        // Landlord ↔ tenant from a confirmed stay: auto-accept connection.
        if (isLandlordTenantPair(userId, request.listingId, request.peerUserId)) {
            upsertConnection(
                userId,
                UpsertChatConnectionRequest(
                    peerUserId = request.peerUserId,
                    status = ConnectionStatus.ACCEPTED,
                )
            )
        } else {
            val conn = connectionBetween(userId, request.peerUserId)
            require(conn?.status == ConnectionStatus.ACCEPTED) {
                "Connect with this person first, then you can message them."
            }
        }

        val displayContent = when {
            trimmed.isNotEmpty() -> trimmed
            typeNorm == ApartmentChatMessage.TYPE_IMAGE -> "Image"
            typeNorm == ApartmentChatMessage.TYPE_PDF ->
                request.fileName?.ifBlank { null } ?: "PDF"
            else -> trimmed
        }
        val id = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val timestamp = System.currentTimeMillis()

        return transaction {
            ChatMessagesTable.insert {
                it[ChatMessagesTable.id] = id
                it[listingId] = request.listingId
                it[bookingId] = request.bookingId
                it[senderId] = userId
                it[receiverId] = request.peerUserId
                it[content] = displayContent
                it[ChatMessagesTable.timestamp] = timestamp
                it[type] = if (isAttachment) typeNorm else ApartmentChatMessage.TYPE_TEXT
                it[fileName] = request.fileName
                it[mimeType] = request.mimeType
                it[ChatMessagesTable.attachmentId] = attachmentId
                it[status] = MessageStatus.SENT.name
                it[replyToId] = request.replyToId?.takeIf { it.isNotBlank() }
                it[replyToPreview] = request.replyToPreview.take(140)
                it[reactions] = ""
                it[pinnedAt] = 0L
                it[forwarded] = request.forwarded
                it[updatedAt] = timestamp
            }
            ApartmentChatMessage(
                id = id,
                listingId = request.listingId,
                bookingId = request.bookingId,
                senderId = userId,
                receiverId = request.peerUserId,
                content = displayContent,
                timestamp = timestamp,
                type = if (isAttachment) typeNorm else ApartmentChatMessage.TYPE_TEXT,
                fileName = request.fileName,
                mimeType = request.mimeType,
                attachmentId = attachmentId,
                status = MessageStatus.SENT,
                replyToId = request.replyToId?.takeIf { it.isNotBlank() },
                replyToPreview = request.replyToPreview.take(140),
                forwarded = request.forwarded,
            )
        }
    }

    fun markDelivered(userId: String, listingId: String, peerUserId: String) {
        upgradeIncoming(userId, listingId, peerUserId, MessageStatus.DELIVERED)
    }

    fun markRead(userId: String, listingId: String, peerUserId: String) {
        upgradeIncoming(userId, listingId, peerUserId, MessageStatus.READ)
    }

    fun deleteMessage(userId: String, messageId: String): ApartmentChatMessage? = transaction {
        val row = ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .singleOrNull()
            ?: return@transaction null
        if (row[ChatMessagesTable.senderId] != userId) return@transaction null
        if (row[ChatMessagesTable.deleted]) return@transaction row.toMessage()
        val attachmentId = row[ChatMessagesTable.attachmentId]
        val now = System.currentTimeMillis()
        ChatMessagesTable.update({ ChatMessagesTable.id eq messageId }) {
            it[content] = ""
            it[type] = ApartmentChatMessage.TYPE_TEXT
            it[fileName] = null
            it[mimeType] = null
            it[ChatMessagesTable.attachmentId] = null
            it[reactions] = ""
            it[pinnedAt] = 0L
            it[replyToPreview] = ""
            it[deleted] = true
            it[updatedAt] = now
        }
        ChatMessagesTable.update({ ChatMessagesTable.replyToId eq messageId }) {
            it[replyToPreview] = "This message was deleted"
            it[updatedAt] = now
        }
        attachmentId?.let { File(chatUploadRoot, it).delete() }
        ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .single()
            .toMessage()
    }

    fun reactToMessage(
        userId: String,
        messageId: String,
        request: ReactToChatMessageRequest,
    ): ApartmentChatMessage = transaction {
        val emoji = request.emoji.trim()
        require(emoji.isNotEmpty() && emoji.length <= 16) { "Pick an emoji" }
        messageRowForParticipant(userId, messageId)
        val row = ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .single()
        val current = decodeMessageReactions(row[ChatMessagesTable.reactions]).toMutableMap()
        if (current[userId] == emoji) current.remove(userId) else current[userId] = emoji
        val now = System.currentTimeMillis()
        ChatMessagesTable.update({ ChatMessagesTable.id eq messageId }) {
            it[reactions] = encodeMessageReactions(current)
            it[updatedAt] = now
        }
        ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .single()
            .toMessage()
    }

    fun setMessagePinned(
        userId: String,
        messageId: String,
        request: PinChatMessageRequest,
    ): ApartmentChatMessage = transaction {
        val row = messageRowForParticipant(userId, messageId)
        val listingId = row[ChatMessagesTable.listingId]
        val sender = row[ChatMessagesTable.senderId]
        val receiver = row[ChatMessagesTable.receiverId]
        val now = System.currentTimeMillis()
        ChatMessagesTable.update({
            (ChatMessagesTable.listingId eq listingId) and (
                (
                    (ChatMessagesTable.senderId eq sender) and
                        (ChatMessagesTable.receiverId eq receiver)
                    ) or (
                    (ChatMessagesTable.senderId eq receiver) and
                        (ChatMessagesTable.receiverId eq sender)
                    )
                )
        }) {
            it[pinnedAt] = 0L
            it[updatedAt] = now
        }
        if (request.pinned) {
            ChatMessagesTable.update({ ChatMessagesTable.id eq messageId }) {
                it[pinnedAt] = now
                it[updatedAt] = now
            }
        }
        ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .single()
            .toMessage()
    }

    private fun messageRowForParticipant(
        userId: String,
        messageId: String,
    ): org.jetbrains.exposed.v1.core.ResultRow {
        val row = ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.id eq messageId }
            .singleOrNull()
            ?: error("Message not found")
        val sender = row[ChatMessagesTable.senderId]
        val receiver = row[ChatMessagesTable.receiverId]
        require(userId == sender || userId == receiver) { "Not allowed" }
        return row
    }

    fun loadAttachment(userId: String, attachmentId: String): ByteArray? = transaction {
        val row = ChatMessagesTable.selectAll()
            .where { ChatMessagesTable.attachmentId eq attachmentId }
            .firstOrNull()
            ?: return@transaction null
        val sender = row[ChatMessagesTable.senderId]
        val receiver = row[ChatMessagesTable.receiverId]
        if (userId != sender && userId != receiver) return@transaction null
        val file = File(chatUploadRoot, attachmentId)
        if (!file.exists()) return@transaction null
        file.readBytes()
    }

    private fun upgradeIncoming(
        userId: String,
        listingId: String,
        peerUserId: String,
        target: MessageStatus,
    ) = transaction {
        val rows = ChatMessagesTable.selectAll()
            .where {
                (ChatMessagesTable.listingId eq listingId) and
                    (ChatMessagesTable.receiverId eq userId) and
                    (ChatMessagesTable.senderId eq peerUserId)
            }
            .toList()
        rows.forEach { row ->
            val current = parseMessageStatus(row[ChatMessagesTable.status])
            if (canUpgrade(current, target)) {
                ChatMessagesTable.update({ ChatMessagesTable.id eq row[ChatMessagesTable.id] }) {
                    it[status] = target.name
                }
            }
        }
    }

    private fun connectionBetween(userA: String, userB: String): Connection? = transaction {
        val id = connectionIdFor(userA, userB)
        ChatConnectionsTable.selectAll()
            .where { ChatConnectionsTable.id eq id }
            .singleOrNull()
            ?.toConnection()
    }

    fun inquiryPeers(userId: String): List<ApartmentChatPeer> = transaction {
        val rows = ChatMessagesTable.selectAll()
            .where {
                (ChatMessagesTable.senderId eq userId) or (ChatMessagesTable.receiverId eq userId)
            }
            .toList()
        val grouped = rows.groupBy { row ->
            val other = if (row[ChatMessagesTable.senderId] == userId) {
                row[ChatMessagesTable.receiverId]
            } else {
                row[ChatMessagesTable.senderId]
            }
            row[ChatMessagesTable.listingId] to other
        }
        grouped.mapNotNull { (key, messages) ->
            val (listingId, otherId) = key
            if (otherId.isBlank() || otherId == userId) return@mapNotNull null
            val listing = ListingsTable.selectAll()
                .where { ListingsTable.id eq listingId }
                .singleOrNull()
                ?: return@mapNotNull null
            val landlordId = listing[ListingsTable.landlordId]
            val aboutLandlord = otherId == landlordId || userId == landlordId
            if (!aboutLandlord) return@mapNotNull null
            val name = UsersTable.selectAll()
                .where { UsersTable.id eq otherId }
                .singleOrNull()
                ?.get(UsersTable.fullName)
                ?.ifBlank { null }
                ?: if (otherId == landlordId) "Landlord" else "HavenPad"
            val firstAt = messages.minOf { it[ChatMessagesTable.timestamp] }
            ApartmentChatPeer(
                userId = otherId,
                displayName = name,
                listingId = listingId,
                listingTitle = listing[ListingsTable.title],
                bookingId = messages.first()[ChatMessagesTable.bookingId],
                peerIsLandlord = otherId == landlordId,
                joinedAtMillis = firstAt,
            )
        }.distinctBy { "${it.listingId}_${it.userId}" }
    }

    private fun canShareApartment(userId: String, listingId: String, peerId: String): Boolean =
        transaction {
            if (isLandlordInquiry(userId, listingId, peerId)) return@transaction true
            val confirmed = ListingBookingsTable.selectAll()
                .where {
                    (ListingBookingsTable.listingId eq listingId) and
                        (ListingBookingsTable.status eq BookingStatus.CONFIRMED)
                }
                .toList()
            if (confirmed.isEmpty()) return@transaction false

            val listing = ListingsTable.selectAll()
                .where { ListingsTable.id eq listingId }
                .singleOrNull()
                ?: return@transaction false
            val landlordId = listing[ListingsTable.landlordId]

            fun isGuest(uid: String) = confirmed.any { it[ListingBookingsTable.guestUserId] == uid }
            fun isLandlord(uid: String) = uid == landlordId

            when {
                isLandlord(userId) && isGuest(peerId) -> true
                isGuest(userId) && isLandlord(peerId) -> true
                isGuest(userId) && isGuest(peerId) -> true
                else -> false
            }
        }

    private fun isLandlordTenantPair(
        userId: String,
        listingId: String,
        peerId: String,
    ): Boolean = transaction {
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: return@transaction false
        val landlordId = listing[ListingsTable.landlordId]
        if (isLandlordInquiry(userId, listingId, peerId)) return@transaction true
        val confirmedGuests = ListingBookingsTable.selectAll()
            .where {
                (ListingBookingsTable.listingId eq listingId) and
                    (ListingBookingsTable.status eq BookingStatus.CONFIRMED)
            }
            .mapNotNull { it[ListingBookingsTable.guestUserId] }
            .toSet()
        (userId == landlordId && peerId in confirmedGuests) ||
            (peerId == landlordId && userId in confirmedGuests)
    }

    private fun isLandlordInquiry(userId: String, listingId: String, peerId: String): Boolean {
        if (userId.isBlank() || peerId.isBlank() || userId == peerId || listingId.isBlank()) return false
        val landlordId = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?.get(ListingsTable.landlordId)
            ?: return false
        return when {
            peerId == landlordId -> true
            userId == landlordId -> ChatMessagesTable.selectAll()
                .where {
                    (ChatMessagesTable.listingId eq listingId) and (
                        (
                            (ChatMessagesTable.senderId eq userId) and
                                (ChatMessagesTable.receiverId eq peerId)
                            ) or (
                            (ChatMessagesTable.senderId eq peerId) and
                                (ChatMessagesTable.receiverId eq userId)
                            )
                        )
                }
                .limit(1)
                .singleOrNull() != null
            else -> false
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toConnection() = Connection(
        id = this[ChatConnectionsTable.id],
        senderId = this[ChatConnectionsTable.senderId],
        receiverId = this[ChatConnectionsTable.receiverId],
        status = parseConnectionStatus(this[ChatConnectionsTable.status]),
        timestamp = this[ChatConnectionsTable.updatedAt],
    )

    private fun org.jetbrains.exposed.v1.core.ResultRow.toMessage() = ApartmentChatMessage(
        id = this[ChatMessagesTable.id],
        listingId = this[ChatMessagesTable.listingId],
        bookingId = this[ChatMessagesTable.bookingId],
        senderId = this[ChatMessagesTable.senderId],
        receiverId = this[ChatMessagesTable.receiverId],
        content = this[ChatMessagesTable.content],
        timestamp = this[ChatMessagesTable.timestamp],
        type = this[ChatMessagesTable.type],
        fileName = this[ChatMessagesTable.fileName],
        mimeType = this[ChatMessagesTable.mimeType],
        attachmentId = this[ChatMessagesTable.attachmentId],
        status = parseMessageStatus(this[ChatMessagesTable.status]),
        replyToId = this[ChatMessagesTable.replyToId],
        replyToPreview = this[ChatMessagesTable.replyToPreview],
        reactions = decodeMessageReactions(this[ChatMessagesTable.reactions]),
        pinnedAt = this[ChatMessagesTable.pinnedAt],
        forwarded = this[ChatMessagesTable.forwarded],
        deleted = this[ChatMessagesTable.deleted],
    )

    private fun parseConnectionStatus(raw: String): ConnectionStatus =
        runCatching { ConnectionStatus.valueOf(raw) }.getOrDefault(ConnectionStatus.PENDING)

    private fun parseMessageStatus(raw: String): MessageStatus =
        runCatching { MessageStatus.valueOf(raw) }.getOrDefault(MessageStatus.SENT)

    private fun canUpgrade(current: MessageStatus, target: MessageStatus): Boolean {
        val order = listOf(
            MessageStatus.SENDING,
            MessageStatus.SENT,
            MessageStatus.DELIVERED,
            MessageStatus.READ,
        )
        val ci = order.indexOf(current)
        val ti = order.indexOf(target)
        return ci >= 0 && ti > ci
    }
}
