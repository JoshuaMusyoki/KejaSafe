package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable

private val showBrowserNotification: (String, String, String) -> Unit = js(
    "function(title, body, tag) { if (typeof Notification !== 'undefined' && Notification.permission === 'granted') { new Notification(title, { body: body, tag: tag }); } }"
)

private val notificationsGranted: () -> Boolean = js(
    "function() { return typeof Notification !== 'undefined' && Notification.permission === 'granted'; }"
)

private val requestBrowserPermission: () -> Unit = js(
    "function() { if (typeof Notification !== 'undefined' && Notification.permission === 'default') { Notification.requestPermission(); } }"
)

actual fun requestChatNotificationPermission() {
    requestBrowserPermission()
}

actual fun chatNotificationsAllowed(): Boolean = notificationsGranted()

actual fun showIncomingChatNotification(
    title: String,
    body: String,
    notificationKey: String,
    peerUserId: String,
    listingId: String,
    listingTitle: String,
    bookingId: String,
    displayName: String,
    peerIsLandlord: Boolean,
) {
    showBrowserNotification(title, body, notificationKey)
}

actual fun onSessionSaved() = Unit

actual fun onSessionCleared() = Unit

@Composable
actual fun ChatNotificationPermissionEffect() = Unit
