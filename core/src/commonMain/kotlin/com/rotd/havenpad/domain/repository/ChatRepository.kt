package com.rotd.keja_safe.domain.repository

import com.rotd.keja_safe.domain.model.AttachmentType
import com.rotd.keja_safe.domain.model.ChatMessage
import com.rotd.keja_safe.domain.model.Connection
import com.rotd.keja_safe.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage)
    suspend fun sendBulkSms(contacts: List<String>, message: String, scheduledTime: Long? = null)
    suspend fun updateMessageStatus(uuid: String, status: MessageStatus)
    suspend fun updateMessageAttachment(uuid: String, url: String, type: AttachmentType, fileName: String?)
    suspend fun updateMessageLocalPath(uuid: String, path: String?)
    suspend fun markMessagesAsRead(peerId: String)
    suspend fun uploadFile(uri: String, type: AttachmentType, fileName: String? = null): Result<String>
    suspend fun deleteMessage(uuid: String)
    suspend fun togglePinMessage(uuid: String, isPinned: Boolean)
    /**
     * Instagram-style reaction: one reaction per user.
     * - Same emoji again → remove
     * - Different emoji → replace
     * - [isSuper] from long-press
     */
    suspend fun reactToMessage(uuid: String, userId: String, emoji: String, isSuper: Boolean = false)

    fun getConnections(): Flow<List<Connection>>
    suspend fun sendConnectionRequest(toUserId: String): Result<Unit>
    suspend fun acceptConnectionRequest(fromUserId: String): Result<Unit>
}


