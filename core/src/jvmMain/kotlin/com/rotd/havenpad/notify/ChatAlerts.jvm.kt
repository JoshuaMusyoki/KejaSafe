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

actual fun onSessionSaved() = Unit

actual fun onSessionCleared() = Unit

@Composable
actual fun ChatNotificationPermissionEffect() = Unit
