package com.rotd.havenpad.domain.chat

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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
    val profilePictureUrl: String? = null,
)

@Serializable
data class ProfilePhotoLookupRequest(
    val userIds: List<String> = emptyList(),
)

@Serializable
data class ProfilePhotoRef(
    val userId: String,
    val profilePictureUrl: String? = null,
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
    val type: String = TYPE_TEXT,
    val fileName: String? = null,
    val mimeType: String? = null,
    val attachmentId: String? = null,
    /** SENT = 1 tick, DELIVERED = 2 ticks, READ = blue 2 ticks. */
    val status: MessageStatus = MessageStatus.DELIVERED,
    val replyToId: String? = null,
    val replyToPreview: String = "",
    /** One emoji per user id. Tapping the same emoji again removes it. */
    val reactions: Map<String, String> = emptyMap(),
    /** Non-zero when this message is pinned to the top of the thread. */
    val pinnedAt: Long = 0L,
    val forwarded: Boolean = false,
    /** Cleared for both people. The bubble stays so the other person can see it was removed. */
    val deleted: Boolean = false,
) {
    fun isImage(): Boolean = !deleted && type.equals(TYPE_IMAGE, ignoreCase = true)
    fun isPdf(): Boolean = !deleted && type.equals(TYPE_PDF, ignoreCase = true)

    fun previewText(): String = when {
        deleted -> "Message deleted"
        isImage() -> imagePreviewLabel(fileName)
        isPdf() -> fileName?.takeIf { it.isNotBlank() } ?: "PDF"
        else -> content
    }

    fun deletedLabel(isMine: Boolean): String =
        if (isMine) "You deleted this message" else "This message was deleted"

    companion object {
        const val TYPE_TEXT = "TEXT"
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_PDF = "PDF"
    }
}

/** True when [name] looks like a real file name, not a MediaStore id such as `57` or `image:12`. */
fun looksLikeUserFacingFileName(name: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.all { it.isDigit() }) return false
    if (':' in trimmed) {
        val after = trimmed.substringAfter(':')
        if (after.isNotEmpty() && after.all { it.isDigit() }) return false
    }
    val ext = trimmed.substringAfterLast('.', missingDelimiterValue = "")
    if (ext.length in 2..5 && ext.any { it.isLetter() }) return true
    return ':' !in trimmed && trimmed.any { it.isLetter() } && trimmed.length >= 4
}

fun imagePreviewLabel(fileName: String?): String {
    val name = fileName?.trim().orEmpty()
    return if (looksLikeUserFacingFileName(name)) name else "Image"
}

private val reactionJson = Json { ignoreUnknownKeys = true }
private val reactionSerializer = MapSerializer(String.serializer(), String.serializer())

fun encodeMessageReactions(reactions: Map<String, String>): String =
    if (reactions.isEmpty()) "" else reactionJson.encodeToString(reactionSerializer, reactions)

fun decodeMessageReactions(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        reactionJson.decodeFromString(reactionSerializer, raw)
    }.getOrDefault(emptyMap())
}

fun ApartmentChatMessage.replySnippet(): String = previewText().take(140)

fun apartmentConversationKey(listingId: String, userA: String, userB: String): String {
    val pair = listOf(userA, userB).sorted().joinToString("_")
    return "${listingId}_$pair"
}

/** Canonical connection id shared by both users. */
fun connectionIdFor(userA: String, userB: String): String =
    listOf(userA, userB).sorted().joinToString("_")
