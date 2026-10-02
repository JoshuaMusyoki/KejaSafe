package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(title, body, tag) => { if (typeof Notification !== 'undefined' && Notification.permission === 'granted') { new Notification(title, { body: body, tag: tag }); } }"
)
private external fun showBrowserNotification(title: String, body: String, tag: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => (typeof Notification !== 'undefined' && Notification.permission === 'granted')")
private external fun notificationsGranted(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "() => { if (typeof Notification !== 'undefined' && Notification.permission === 'default') { Notification.requestPermission(); } }"
)
private external fun requestBrowserPermission()

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
