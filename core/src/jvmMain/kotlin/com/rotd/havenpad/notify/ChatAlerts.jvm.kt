package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable

actual fun requestChatNotificationPermission() = Unit

actual fun chatNotificationsAllowed(): Boolean = false

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
) = Unit

actual fun showIncomingHouseTourRing(
    callerName: String,
    listingTitle: String,
    sessionId: String,
    peerUserId: String,
    listingId: String,
    peerIsLandlord: Boolean,
) = Unit

actual fun cancelHouseTourRing(sessionId: String) = Unit

actual fun startHouseTourRingSound() = Unit

actual fun stopHouseTourRingSound() = Unit

actual fun onSessionSaved() = Unit

actual fun onSessionCleared() = Unit

actual fun consumeClickedChatNotification(): String = ""

@Composable
actual fun ChatNotificationPermissionEffect() = Unit
