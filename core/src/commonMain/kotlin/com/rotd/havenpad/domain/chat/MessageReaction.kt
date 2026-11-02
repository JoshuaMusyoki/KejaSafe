package com.rotd.havenpad.domain.chat

import kotlinx.serialization.Serializable

/**
 * One reaction per user per message (Instagram-style).
 * [emoji] is either a unicode emoji or a sticker/image URI.
 */
@Serializable
data class MessageReaction(
    val userId: String = "",
    val emoji: String = "",
    val isSuper: Boolean = false,
    val timestamp: Long = 0L
) {
    val isSticker: Boolean
        get() = emoji.startsWith("http://") ||
            emoji.startsWith("https://") ||
            emoji.startsWith("file:") ||
            emoji.startsWith("content:") ||
            emoji.startsWith("data:") ||
            emoji.startsWith("/")
}