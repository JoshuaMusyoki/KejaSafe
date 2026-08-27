package com.rotd.kejasafe.domain.chat

data class Notification(
    val id: Long = 0,
    val type: NotificationType,
    val title: String,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: Long,
    val isRead: Boolean = false,
    val targetId: String? = null,
    /** Account that owns this inbox entry — prevents cross-account bleed on shared devices. */
    val ownerUid: String = "",
    /** applicationId that produced this row — isolates Stawi / Vigil AI on shared data. */
    val appPackage: String = "",
)

enum class NotificationType {
    VIDEO, CHAT, SMS, P2P, CONNECTION, MEETING, APP_UPDATE
}
