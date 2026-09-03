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
    val type: String = TYPE_TEXT,
    val fileName: String? = null,
    val mimeType: String? = null,
    val attachmentId: String? = null,
    /** SENT = 1 tick, DELIVERED = 2 ticks, READ = blue 2 ticks. */
    val status: MessageStatus = MessageStatus.DELIVERED,
) {
    fun isImage(): Boolean = type.equals(TYPE_IMAGE, ignoreCase = true)
    fun isPdf(): Boolean = type.equals(TYPE_PDF, ignoreCase = true)

    fun previewText(): String = when {
        isImage() -> imagePreviewLabel(fileName)
        isPdf() -> fileName?.takeIf { it.isNotBlank() } ?: "PDF"
        else -> content
    }

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

fun apartmentConversationKey(listingId: String, userA: String, userB: String): String {
    val pair = listOf(userA, userB).sorted().joinToString("_")
    return "${listingId}_$pair"
}

/** Canonical connection id shared by both users. */
fun connectionIdFor(userA: String, userB: String): String =
    listOf(userA, userB).sorted().joinToString("_")
