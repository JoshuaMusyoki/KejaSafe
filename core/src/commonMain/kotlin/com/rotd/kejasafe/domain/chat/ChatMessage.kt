package com.rotd.kejasafe.domain.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: Long = 0,
    val uuid: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val transportType: TransportType = TransportType.INTERNET,
    val status: MessageStatus = MessageStatus.SENDING,
    val replyToUuid: String? = null,
    val replyToContent: String? = null,
    val synced: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType? = null,
    val fileName: String? = null,
    val localAttachmentPath: String? = null,
    val isPinned: Boolean = false,
    val pinnedAt: Long = 0L,
    /** Keyed by userId — one reaction per user (Instagram-style). */
    val reactions: Map<String, MessageReaction> = emptyMap()
) {
    /** Aggregated emoji/sticker counts for display chips. */
    fun reactionGroups(): List<Pair<String, List<MessageReaction>>> =
        reactions.values
            .groupBy { it.emoji }
            .entries
            .sortedByDescending { (_, group) ->
                group.maxOfOrNull { it.timestamp } ?: 0L
            }
            .map { (emoji, group) -> emoji to group.sortedByDescending { it.timestamp } }
}

@Serializable
enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

@Serializable
enum class TransportType {
    INTERNET, SMS
}

@Serializable
enum class AttachmentType {
    IMAGE, PDF, STICKER, VIDEO
}


