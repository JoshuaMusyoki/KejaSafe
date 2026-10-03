package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(title, body, tag) => { if (typeof Notification !== 'undefined' && Notification.permission === 'granted') { var n = new Notification(title, { body: body, tag: tag }); n.onclick = function () { globalThis.__havenNoticeOpen = tag; n.close(); if (window.focus) window.focus(); }; } }"
)
private external fun showBrowserNotification(title: String, body: String, tag: String)

@JsFun("() => { var id = globalThis.__havenNoticeOpen || ''; globalThis.__havenNoticeOpen = ''; return id; }")
private external fun takeClickedNotification(): String

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

actual fun showIncomingHouseTourRing(
    callerName: String,
    listingTitle: String,
    sessionId: String,
    peerUserId: String,
    listingId: String,
    peerIsLandlord: Boolean,
) {
    showBrowserNotification(
        callerName.ifBlank { "House tour" },
        "Incoming house tour${if (listingTitle.isBlank()) "" else " · $listingTitle"}",
        "call-$sessionId",
    )
}

actual fun cancelHouseTourRing(sessionId: String) = Unit

actual fun startHouseTourRingSound() = Unit

actual fun stopHouseTourRingSound() = Unit

actual fun onSessionClearing() = Unit

actual fun onSessionSaved() = Unit

actual fun onSessionCleared() = Unit

@Composable
actual fun ChatNotificationPermissionEffect() = Unit

actual fun consumeClickedChatNotification(): String = takeClickedNotification()
