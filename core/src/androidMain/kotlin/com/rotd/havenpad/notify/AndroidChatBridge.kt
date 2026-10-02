package com.rotd.havenpad.notify

/**
 * Android app module installs the real notification and WorkManager hooks.
 * Other code in core only calls these.
 */
object AndroidChatBridge {
    var showNotification: ((ChatNotifyRequest) -> Unit)? = null
    var onSessionSaved: (() -> Unit)? = null
    var onSessionCleared: (() -> Unit)? = null
}

data class ChatNotifyRequest(
    val title: String,
    val body: String,
    val notificationKey: String,
    val peerUserId: String,
    val listingId: String,
    val listingTitle: String,
    val bookingId: String,
    val displayName: String,
    val peerIsLandlord: Boolean,
)
