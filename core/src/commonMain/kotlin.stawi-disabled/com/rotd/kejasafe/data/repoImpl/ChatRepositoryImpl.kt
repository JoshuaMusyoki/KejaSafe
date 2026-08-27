package com.rotd.stawi.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import com.rotd.stawi.domain.model.*
import com.rotd.stawi.data.mapper.*
import com.rotd.stawi.domain.repository.ChatRepository
import com.rotd.stawi.domain.repository.SmsRepository
import com.rotd.stawi.domain.repository.AuthRepository
import com.rotd.stawi.domain.repository.NotificationRepository
import com.rotd.stawi.data.local.dao.OrbitDao
import com.rotd.stawi.data.local.entities.ChatMessageEntity
import com.rotd.stawi.domain.util.ConnectivityObserver
import com.rotd.stawi.domain.util.ChatSessionState
import com.rotd.stawi.domain.util.MessageNotificationTracker
import com.rotd.stawi.domain.util.Notifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import androidx.core.graphics.scale


class ChatRepositoryImpl(
    private val context: android.content.Context,
    private val dao: OrbitDao,
    private val firestore: FirebaseFirestore,
    private val connectivityObserver: ConnectivityObserver,
    private val smsRepository: SmsRepository,
    private val authRepository: AuthRepository,
    private val notifier: Notifier,
    private val notificationRepository: NotificationRepository,
    private val storage: FirebaseStorage,
    private val chatSessionState: ChatSessionState,
    private val messageNotificationTracker: MessageNotificationTracker,
) : ChatRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var remoteMessageListener: ListenerRegistration? = null
    /** Prevents double-notify when the Firestore listener re-attaches (legacy ids; FCM uses tracker). */
    private val notifiedMessageIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val notifiedConnectionIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var connectionsListener: ListenerRegistration? = null

    init {
        // Observe connectivity changes for auto-retry
        connectivityObserver.status.onEach { status ->
            if (status == ConnectivityObserver.Status.Available) {
                retryFailedMessages()
            }
        }.launchIn(repositoryScope)

        // Observe Auth State to listen for messages
        authRepository.currentUser.onEach { user ->
            remoteMessageListener?.remove()
            connectionsListener?.remove()
            chatSessionState.setCurrentUser(user?.uid)
            chatSessionState.setActivePeer(null)
            // Do not clear notifiedMessageIds — listener re-attach previously re-fired tray alerts.
            // Bound the set so memory does not grow without limit.
            if (notifiedMessageIds.size > 2_000) {
                val overflow = notifiedMessageIds.size - 1_000
                val iterator = notifiedMessageIds.iterator()
                var removed = 0
                while (iterator.hasNext() && removed < overflow) {
                    iterator.next()
                    iterator.remove()
                    removed++
                }
            }
            if (user != null) {
                listenForRemoteMessages(user.uid)
                listenForConnections(user.uid)
            } else {
                notifier.cancelAllChatNotifications()
                notifiedMessageIds.clear()
                notifiedConnectionIds.clear()
            }
        }.launchIn(repositoryScope)
    }

    private fun listenForConnections(userId: String) {
        connectionsListener = firestore.collection("connections")
            .whereArrayContains("users", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                snapshots?.documentChanges?.forEach { dc ->
                    val data = dc.document.data
                    val users = data["users"] as? List<*> ?: return@forEach
                    val status = data["status"] as? String ?: return@forEach
                    val requestBy = data["requestBy"] as? String ?: return@forEach
                    
                    val senderId = requestBy
                    val receiverId = users.find { it != senderId } as? String ?: return@forEach
                    
                    val connection = Connection(
                        id = dc.document.id,
                        senderId = senderId,
                        receiverId = receiverId,
                        status = ConnectionStatus.valueOf(status),
                        timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
                    )
                    
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED || 
                        dc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                        
                        val now = System.currentTimeMillis()
                        val ageMs = now - connection.timestamp
                        val isFresh = ageMs in 0..(5 * 60 * 1000L)

                        if (isFresh && notifiedConnectionIds.add(connection.id + "_" + connection.status.name)) {
                            if (connection.status == ConnectionStatus.PENDING && connection.receiverId == userId) {
                                repositoryScope.launch {
                                    val senderName = resolveUsername(connection.senderId)
                                    val content = "$senderName wants to connect with you."
                                    notifier.showChatMessageNotification(
                                        sender = "Connection Request",
                                        message = content,
                                        peerId = connection.senderId,
                                    )
                                    
                                    notificationRepository.addNotification(
                                        Notification(
                                            type = NotificationType.CONNECTION,
                                            title = "Connection Request",
                                            content = content,
                                            timestamp = connection.timestamp,
                                            targetId = connection.senderId,
                                            ownerUid = userId,
                                        )
                                    )
                                }
                            } else if (connection.status == ConnectionStatus.ACCEPTED && connection.senderId == userId) {
                                repositoryScope.launch {
                                    val receiverName = resolveUsername(connection.receiverId)
                                    val content = "$receiverName accepted your connection request."
                                    notifier.showChatMessageNotification(
                                        sender = "Request Accepted",
                                        message = content,
                                        peerId = connection.receiverId,
                                    )
                                    
                                    notificationRepository.addNotification(
                                        Notification(
                                            type = NotificationType.CONNECTION,
                                            title = "Request Accepted",
                                            content = content,
                                            timestamp = connection.timestamp,
                                            targetId = connection.receiverId,
                                            ownerUid = userId,
                                        )
                                    )
                                }
                            }
                        }
                    }

                    repositoryScope.launch {
                        dao.insertConnection(connection.toEntity())
                    }
                }
            }
    }

    private fun listenForRemoteMessages(userId: String) {
        // Use an OR filter so both parties securely sync their entire unified thread history
        // Removed server-side .orderBy() to bypass demanding complex composite indexes
        remoteMessageListener = firestore.collection("chats")
            .where(
                Filter.or(
                    Filter.equalTo("receiverId", userId),
                    Filter.equalTo("senderId", userId)
                )
            )
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type != com.google.firebase.firestore.DocumentChange.Type.ADDED &&
                        dc.type != com.google.firebase.firestore.DocumentChange.Type.MODIFIED
                    ) {
                        return@forEach
                    }

                    val raw = parseChatMessageDocument(dc.document) ?: return@forEach

                    repositoryScope.launch {
                            val now = System.currentTimeMillis()
                            val reactions = parseReactionsFromFirestore(dc.document.get("reactions"))

                            // 1. Force the Firestore document ID to be the message UUID to prevent duplicates
                            // 2. If the sender's clock is ahead of yours, clip the timestamp right here upon receipt!
                            val uniqueMessage = raw.copy(
                                uuid = dc.document.id,
                                timestamp = if (raw.timestamp > now) now else raw.timestamp,
                                reactions = reactions
                            )

                            val entity = uniqueMessage.copy(transportType = TransportType.INTERNET)
                                .toEntity()
                                .copy(synced = true)

                            dao.insertMessage(entity) // Writes to database with a clean, static timestamp

                            // If we are the receiver and the message is not yet DELIVERED/READ, mark it as DELIVERED
                            if (uniqueMessage.receiverId == userId && uniqueMessage.status == MessageStatus.SENT) {
                                updateMessageStatus(uniqueMessage.uuid, MessageStatus.DELIVERED)
                            }

                            // Alert only for newly arrived messages (not historical listener replay)
                            if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED &&
                                uniqueMessage.senderId != userId &&
                                uniqueMessage.receiverId == userId &&
                                uniqueMessage.status != MessageStatus.READ
                            ) {
                                val messagePackage = dc.document.getString("appPackage").orEmpty()
                                val isSameAppPackage = messagePackage.isBlank() ||
                                    messagePackage == context.packageName
                                val ageMs = System.currentTimeMillis() - uniqueMessage.timestamp
                                val isFresh = ageMs in 0..(5 * 60 * 1000L)
                                if (isSameAppPackage && isFresh && notifiedMessageIds.add(uniqueMessage.uuid)) {
                                    val senderName = resolveUsername(uniqueMessage.senderId)
                                    val preview = uniqueMessage.content.trim().let { body ->
                                        if (body.isNotEmpty()) {
                                            if (body.length <= 120) body else body.take(117).trimEnd() + "…"
                                        } else {
                                            when (uniqueMessage.attachmentType) {
                                                AttachmentType.IMAGE -> "Sent a photo"
                                                AttachmentType.VIDEO -> "Sent a video"
                                                AttachmentType.STICKER -> "Sent a sticker"
                                                AttachmentType.PDF -> "Sent a file"
                                                null -> "New message"
                                            }
                                        }
                                    }

                                    notificationRepository.addNotification(
                                        Notification(
                                            type = NotificationType.CHAT,
                                            title = senderName,
                                            content = preview,
                                            timestamp = uniqueMessage.timestamp,
                                            targetId = uniqueMessage.senderId,
                                            ownerUid = userId,
                                        )
                                    )

                                    // Skip tray while conversation is open, or if FCM already claimed this message.
                                    val alreadyNotified =
                                        !messageNotificationTracker.tryClaim(uniqueMessage.uuid)
                                    if (!alreadyNotified &&
                                        !chatSessionState.shouldSuppressTrayForPeer(uniqueMessage.senderId)
                                    ) {
                                        notifier.showChatMessageNotification(
                                            sender = senderName,
                                            message = preview,
                                            peerId = uniqueMessage.senderId,
                                        )
                                    }
                                }
                            }

                            // Remote READ can race tray posts — drop system notif when peer's messages are read.
                            if (uniqueMessage.receiverId == userId &&
                                uniqueMessage.status == MessageStatus.READ
                            ) {
                                notifier.cancelChatNotificationsForPeer(uniqueMessage.senderId)
                            }
                        }
                }
            }
    }

    override fun getMessages(): Flow<List<ChatMessage>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                // Only messages that belong to the signed-in account (avoids leftover Room rows
                // from a previous login on the same phone).
                dao.getMessagesForUser(user.uid).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override suspend fun sendMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        // 1. Initial save. If it's a new message or forward, it should start as SENDING.
        // If the ViewModel passes SENT, we still save it as SENDING locally first 
        // to avoid the single-tick flicker while checking connectivity.
        val initialStatus = if (message.status == MessageStatus.SENT) MessageStatus.SENDING else message.status
        dao.insertMessage(message.copy(status = initialStatus).toEntity())

        // 2. If it's just a local save request (e.g. while uploading attachments), stop here.
        if (message.status == MessageStatus.SENDING) return@withContext

        // Wait up to 3 seconds for connection to become available
        val finalStatus = withTimeoutOrNull(kotlin.time.Duration.parse("3s")) {
            connectivityObserver.status.first { it == ConnectivityObserver.Status.Available }
        }

        if (finalStatus == ConnectivityObserver.Status.Available) {
            sendOverInternet(message)
        } else {
            // Mark as FAILED locally if no internet after grace period
            updateMessageStatus(message.uuid, MessageStatus.FAILED)
        }
    }

    private suspend fun retryFailedMessages() {
        val failedMessages = dao.getFailedMessages()
        failedMessages.forEach { entity ->
            val uuid = entity.uuid ?: return@forEach
            
            var currentMessage = entity.toDomain()
            
            // If attachment failed but we have a local path, try uploading again
            if (currentMessage.attachmentUrl == null && entity.localAttachmentPath != null) {
                updateMessageStatus(uuid, MessageStatus.SENDING)
                val type = currentMessage.attachmentType ?: AttachmentType.IMAGE
                val uploadResult = uploadFile(Uri.parse(entity.localAttachmentPath), type, entity.fileName)
                if (uploadResult.isSuccess) {
                    val url = uploadResult.getOrNull()!!
                    updateMessageAttachment(uuid, url, type, entity.fileName)
                    currentMessage = currentMessage.copy(attachmentUrl = url)
                } else {
                    updateMessageStatus(uuid, MessageStatus.FAILED)
                    return@forEach
                }
            }
            
            updateMessageStatus(uuid, MessageStatus.SENDING)
            sendOverInternet(currentMessage)
        }
    }

    override suspend fun sendBulkSms(contacts: List<String>, message: String, scheduledTime: Long?) {
        smsRepository.sendBulkSms(contacts, message, scheduledTime)
    }

    private suspend fun sendOverInternet(message: ChatMessage) {
        try {
            val messageToSend = message.copy(
                transportType = TransportType.INTERNET,
                status = MessageStatus.SENT // Ensure it lands on server as SENT
            )
            
            firestore.collection("chats")
                .document(messageToSend.uuid) // Use UUID as document ID to prevent duplicates on Firestore too
                .set(messageToSend)
                .await()
            // Tag originating applicationId so peer apps + Cloud Functions can filter notifies.
            firestore.collection("chats")
                .document(messageToSend.uuid)
                .set(mapOf("appPackage" to context.packageName), com.google.firebase.firestore.SetOptions.merge())
                .await()

            dao.markMessageSynced(message.uuid)
            // Update status to SENT locally now that it's confirmed on server
            updateMessageStatus(message.uuid, MessageStatus.SENT)
        } catch (e: Exception) {
            // Keep as FAILED or mark as FAILED if internet failed during send
            updateMessageStatus(message.uuid, MessageStatus.FAILED)
        }
    }

    override suspend fun updateMessageStatus(uuid: String, status: MessageStatus) {
        withContext(Dispatchers.IO) {
            dao.updateMessageStatus(uuid, status.name)
            try {
                firestore.collection("chats").document(uuid)
                    .set(mapOf("status" to status.name), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                // Offline or failed to update remote status
            }
        }
    }

    override suspend fun updateMessageAttachment(uuid: String, url: String, type: AttachmentType, fileName: String?) {
        withContext(Dispatchers.IO) {
            dao.updateMessageAttachment(uuid, url, type.name, fileName)
            try {
                firestore.collection("chats").document(uuid)
                    .set(
                        mapOf(
                            "attachmentUrl" to url,
                            "attachmentType" to type.name,
                            "fileName" to fileName
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            } catch (e: Exception) {
                // Offline
            }
        }
    }

    override suspend fun updateMessageLocalPath(uuid: String, path: String?) {
        withContext(Dispatchers.IO) {
            dao.updateMessageLocalPath(uuid, path)
        }
    }

    override suspend fun markMessagesAsRead(peerId: String) {
        val currentUserId = authRepository.currentUser.first()?.uid ?: return
        withContext(Dispatchers.IO) {
            dao.markMessagesAsRead(peerId)
            notifier.cancelChatNotificationsForPeer(peerId)
            runCatching { notificationRepository.markTargetAsRead(peerId) }
            
            // Update Firestore for all messages from this peer to current user
            try {
                val batch = firestore.batch()
                val unreadMessages = firestore.collection("chats")
                    .whereEqualTo("senderId", peerId)
                    .whereEqualTo("receiverId", currentUserId)
                    .whereIn("status", listOf(MessageStatus.SENT.name, MessageStatus.DELIVERED.name))
                    .get()
                    .await()
                
                unreadMessages.documents.forEach { doc ->
                    batch.update(doc.reference, "status", MessageStatus.READ.name)
                }
                batch.commit().await()
            } catch (e: Exception) {
                // Handle or ignore
            }
        }
    }

    override suspend fun deleteMessage(uuid: String) {
        withContext(Dispatchers.IO) {
            dao.deleteMessage(uuid)
            try {
                firestore.collection("chats").document(uuid).delete().await()
            } catch (e: Exception) {
                // Offline or failed to delete remote
            }
        }
    }

    override suspend fun togglePinMessage(uuid: String, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            val pinnedAt = if (isPinned) System.currentTimeMillis() else 0L
            dao.updateMessagePinStatus(uuid, isPinned, pinnedAt)
            try {
                firestore.collection("chats").document(uuid)
                    .set(
                        mapOf("isPinned" to isPinned, "pinnedAt" to pinnedAt),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            } catch (e: Exception) {
                // Offline
            }
        }
    }

    override suspend fun reactToMessage(
        uuid: String,
        userId: String,
        emoji: String,
        isSuper: Boolean
    ) = withContext(Dispatchers.IO) {
        val entity = dao.getMessageByUuid(uuid) ?: return@withContext
        val current = decodeReactions(entity.reactionsJson).toMutableMap()
        val existing = current[userId]

        if (existing != null && existing.emoji == emoji && existing.isSuper == isSuper) {
            // Tap same reaction again → remove (Instagram toggle)
            current.remove(userId)
        } else {
            current[userId] = MessageReaction(
                userId = userId,
                emoji = emoji,
                isSuper = isSuper,
                timestamp = System.currentTimeMillis()
            )
        }

        val json = encodeReactions(current)
        dao.updateMessageReactions(uuid, json)

        try {
            val firestoreReactions = current.mapValues { (_, reaction) ->
                mapOf(
                    "userId" to reaction.userId,
                    "emoji" to reaction.emoji,
                    "isSuper" to reaction.isSuper,
                    "timestamp" to reaction.timestamp
                )
            }
            firestore.collection("chats").document(uuid)
                .set(mapOf("reactions" to firestoreReactions), com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            // Offline — local reaction still applied
        }
    }

    private fun parseChatMessageDocument(doc: DocumentSnapshot): ChatMessage? {
        return try {
            val senderId = doc.getString("senderId").orEmpty()
            val receiverId = doc.getString("receiverId").orEmpty()
            if (senderId.isBlank() || receiverId.isBlank()) return null

            ChatMessage(
                uuid = doc.id,
                senderId = senderId,
                receiverId = receiverId,
                content = doc.getString("content").orEmpty(),
                timestamp = doc.getLong("timestamp") ?: 0L,
                transportType = parseEnum(doc.getString("transportType"), TransportType.INTERNET),
                status = parseEnum(doc.getString("status"), MessageStatus.SENT),
                replyToUuid = doc.getString("replyToUuid"),
                replyToContent = doc.getString("replyToContent"),
                synced = doc.getBoolean("synced") ?: true,
                attachmentUrl = doc.getString("attachmentUrl"),
                attachmentType = parseAttachmentType(doc.get("attachmentType")),
                fileName = doc.getString("fileName"),
                isPinned = doc.getBoolean("isPinned") ?: false,
                pinnedAt = doc.getLong("pinnedAt") ?: 0L,
                reactions = emptyMap() // filled by parseReactionsFromFirestore
            )
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "Failed to parse chat doc ${doc.id}", e)
            null
        }
    }

    private fun parseAttachmentType(raw: Any?): AttachmentType? {
        val name = when (raw) {
            is String -> raw
            is AttachmentType -> return raw
            else -> return null
        }
        return runCatching { AttachmentType.valueOf(name.trim().uppercase()) }.getOrElse {
            // Older clients / typos should not crash the whole chat listener.
            val clean = name.lowercase()
            when {
                clean.contains("stick") -> AttachmentType.STICKER
                clean.contains("pdf") || clean.contains("doc") -> AttachmentType.PDF
                clean.contains("video") || clean.contains("mp4") -> AttachmentType.VIDEO
                clean.contains("image") || clean.contains("photo") -> AttachmentType.IMAGE
                else -> AttachmentType.IMAGE
            }
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw.trim().uppercase()) }.getOrDefault(default)
    }

    private fun parseReactionsFromFirestore(raw: Any?): Map<String, MessageReaction> {
        if (raw == null) return emptyMap()
        val result = mutableMapOf<String, MessageReaction>()
        try {
            when (raw) {
                is Map<*, *> -> {
                    raw.forEach { (key, value) ->
                        val userId = key?.toString() ?: return@forEach
                        val map = value as? Map<*, *> ?: return@forEach
                        result[userId] = MessageReaction(
                            userId = map["userId"]?.toString() ?: userId,
                            emoji = map["emoji"]?.toString() ?: return@forEach,
                            isSuper = map["isSuper"] as? Boolean ?: false,
                            timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }
                }
                is List<*> -> {
                    raw.forEach { item ->
                        val map = item as? Map<*, *> ?: return@forEach
                        val userId = map["userId"]?.toString() ?: return@forEach
                        result[userId] = MessageReaction(
                            userId = userId,
                            emoji = map["emoji"]?.toString() ?: return@forEach,
                            isSuper = map["isSuper"] as? Boolean ?: false,
                            timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    }
                }
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return result
    }

    private fun encodeReactions(reactions: Map<String, MessageReaction>): String {
        return try {
            val root = org.json.JSONObject()
            reactions.forEach { (userId, reaction) ->
                root.put(
                    userId,
                    org.json.JSONObject().apply {
                        put("userId", reaction.userId)
                        put("emoji", reaction.emoji)
                        put("isSuper", reaction.isSuper)
                        put("timestamp", reaction.timestamp)
                    }
                )
            }
            root.toString()
        } catch (_: Exception) {
            "{}"
        }
    }

    private fun decodeReactions(json: String?): Map<String, MessageReaction> {
        if (json.isNullOrBlank() || json == "{}") return emptyMap()
        return try {
            val root = org.json.JSONObject(json)
            val result = mutableMapOf<String, MessageReaction>()
            root.keys().forEach { userId ->
                val obj = root.optJSONObject(userId) ?: return@forEach
                result[userId] = MessageReaction(
                    userId = obj.optString("userId", userId),
                    emoji = obj.optString("emoji"),
                    isSuper = obj.optBoolean("isSuper", false),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }


    override suspend fun uploadFile(uri: Uri, type: AttachmentType, fileName: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uid = authRepository.currentUser.first()?.uid
                ?: return@withContext Result.failure(Exception("Not logged in"))

            // Pre-check file size for non-image types (images are auto-compressed)
            if (type != AttachmentType.IMAGE && type != AttachmentType.STICKER) {
                val fileSize = getFileSize(uri)
                val limit = when(type) {
                    AttachmentType.VIDEO -> 15 * 1024 * 1024L // 15MB
                    else -> 10 * 1024 * 1024L // 10MB (PDF/Documents)
                }
                if (fileSize > limit) {
                    val label = if (type == AttachmentType.VIDEO) "Video" else "Document"
                    val limitMb = limit / (1024 * 1024)
                    return@withContext Result.failure(Exception("$label is too large (max ${limitMb}MB). Please send a smaller file."))
                }
            }

            val bytes = if (type == AttachmentType.IMAGE) {
                compressImage(uri)
            } else {
                context.contentResolver.openInputStream(uri)?.readBytes()
            } ?: return@withContext Result.failure(Exception("Failed to read file"))

            val extension = when (type) {
                AttachmentType.IMAGE -> "jpg"
                AttachmentType.VIDEO -> "mp4"
                AttachmentType.STICKER -> {
                    val ext = fileName?.substringAfterLast('.', "")
                        ?: uri.path?.substringAfterLast('.', "")
                    if (!ext.isNullOrEmpty() && ext.length <= 4) ext.lowercase() else "gif"
                }
                AttachmentType.PDF -> {
                    val ext = fileName?.substringAfterLast('.', "")
                    if (!ext.isNullOrEmpty() && ext.length <= 5) {
                        ext.lowercase()
                    } else {
                        val pathExt = uri.path?.substringAfterLast('.', "")
                        if (!pathExt.isNullOrEmpty() && pathExt.length <= 5) pathExt.lowercase() else "pdf"
                    }
                }
            }

            val mimeType = when (type) {
                AttachmentType.IMAGE -> "image/jpeg"
                AttachmentType.VIDEO -> "video/mp4"
                AttachmentType.STICKER -> when (extension) {
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
                AttachmentType.PDF -> when (extension) {
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "doc" -> "application/msword"
                    else -> "application/pdf"
                }
            }

            // Local cache for sender
            val localFile = java.io.File(context.filesDir, "attachment_${System.currentTimeMillis()}.$extension")
            localFile.writeBytes(bytes)
            
            // Note: We don't have the message UUID here to update the DB immediately, 
            // but the result URI can be used by the caller to update it.
            val localUri = Uri.fromFile(localFile).toString()

            if (type == AttachmentType.IMAGE) {
                val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (decoded == null) {
                    return@withContext Result.failure(Exception("Generated invalid image bytes"))
                }
            }

            // 1) Prefer Firebase Storage (HTTPS URL works for receivers)
            val storageResult = uploadToFirebaseStorage(uid, localFile, mimeType)
            if (storageResult.isSuccess) {
                return@withContext Result.success(storageResult.getOrNull()!!)
            }

            val storageError = storageResult.exceptionOrNull()?.message.orEmpty()
            android.util.Log.w("ChatRepo", "Storage upload failed, trying inline fallback: $storageError")

            // 2) Fallback: embed small files as data URIs in Firestore so receivers can open them
            // Firestore doc limit is 1MB — keep payload comfortably under that.
            val maxInlineBytes = 800_000
            if (bytes.size <= maxInlineBytes) {
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val dataUri = "data:$mimeType;base64,$base64"
                android.util.Log.d("ChatRepo", "Using inline data URI (${bytes.size} bytes)")
                return@withContext Result.success(dataUri)
            }

            Result.failure(
                Exception(
                    "Upload failed. The file is too large to send without cloud storage. " +
                        "Check your internet or try a smaller file."
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "uploadFile failed", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadToFirebaseStorage(
        uid: String,
        localFile: java.io.File,
        mimeType: String
    ): Result<String> {
        val remotePath = "chat_media/$uid/${localFile.name}"
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        val ref = storage.reference.child(remotePath)
        return try {
            ref.putFile(Uri.fromFile(localFile), metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            android.util.Log.d("ChatRepo", "Uploaded to Storage: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            android.util.Log.e(
                "ChatRepo",
                "Storage upload failed",
                e
            )
            Result.failure(e)
        }
    }

    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // 1. Decode bounds to check size
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 2. Calculate sample size
            val maxDimension = 600 // Reduced from 800
            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode with inSampleSize
            val secondStream = context.contentResolver.openInputStream(uri) ?: return null
            val scaledOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decodedBitmap = android.graphics.BitmapFactory.decodeStream(secondStream, null, scaledOptions) ?: return null
            secondStream.close()
            
            // 4. Final precise scaling if needed
            val width = decodedBitmap.width
            val height = decodedBitmap.height
            val scale = if (width > height) {
                if (width > maxDimension) maxDimension.toFloat() / width else 1f
            } else {
                if (height > maxDimension) maxDimension.toFloat() / height else 1f
            }
            
            val finalBitmap = if (scale < 1f) {
                decodedBitmap.scale((width * scale).toInt(), (height * scale).toInt())
            } else {
                decodedBitmap
            }
            
            val outputStream = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream) // Reduced quality to 60
            val result = outputStream.toByteArray()
            
            android.util.Log.d("ChatRepo", "Compressed image size: ${result.size / 1024} KB")
            result
        } catch (e: Exception) {
            android.util.Log.e("ChatRepo", "Compression failed", e)
            null
        }
    }

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        uuid = uuid ?: "",
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        timestamp = timestamp,
        transportType = TransportType.valueOf(transportType),
        status = MessageStatus.valueOf(status),
        replyToUuid = replyToUuid,
        replyToContent = replyToContent,
        synced = synced,
        attachmentUrl = attachmentUrl,
        attachmentType = attachmentType?.let { raw ->
            runCatching { AttachmentType.valueOf(raw) }.getOrElse {
                parseAttachmentType(raw)
            }
        },
        fileName = fileName,
        localAttachmentPath = localAttachmentPath,
        isPinned = isPinned,
        pinnedAt = pinnedAt,
        reactions = decodeReactions(reactionsJson)
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        uuid = uuid,
        senderId = senderId,
        receiverId = receiverId,
        content = content,
        timestamp = timestamp,
        transportType = transportType.name,
        status = status.name,
        replyToUuid = replyToUuid,
        replyToContent = replyToContent,
        synced = synced,
        attachmentUrl = attachmentUrl,
        attachmentType = attachmentType?.name,
        fileName = fileName,
        localAttachmentPath = localAttachmentPath,
        isPinned = isPinned,
        pinnedAt = pinnedAt,
        reactionsJson = encodeReactions(reactions)
    )

    override fun getConnections(): Flow<List<Connection>> =
        dao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun sendConnectionRequest(toUserId: String): Result<Unit> {
        val myUid = authRepository.currentUser.first()?.uid ?: return Result.failure(Exception("Not logged in"))
        val docId = if (myUid < toUserId) "${myUid}_$toUserId" else "${toUserId}_$myUid"
        
        val data = mapOf(
            "users" to listOf(myUid, toUserId),
            "status" to ConnectionStatus.PENDING.name,
            "requestBy" to myUid,
            "timestamp" to System.currentTimeMillis()
        )
        
        return try {
            firestore.collection("connections").document(docId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptConnectionRequest(fromUserId: String): Result<Unit> {
        val myUid = authRepository.currentUser.first()?.uid ?: return Result.failure(Exception("Not logged in"))
        val docId = if (myUid < fromUserId) "${myUid}_$fromUserId" else "${fromUserId}_$myUid"
        
        return try {
            firestore.collection("connections").document(docId)
                .update("status", ConnectionStatus.ACCEPTED.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveUsername(uid: String): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getString("username")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "New message"
        } catch (_: Exception) {
            "New message"
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}



