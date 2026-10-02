package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable

/** Ask the system to allow message notifications. Safe to call more than once. */
expect fun requestChatNotificationPermission()

/** True when the platform will actually display a notification. */
expect fun chatNotificationsAllowed(): Boolean

expect fun showIncomingChatNotification(
    title: String,
    body: String,
    notificationKey: String,
    peerUserId: String = "",
    listingId: String = "",
    listingTitle: String = "",
    bookingId: String = "",
    displayName: String = "",
    peerIsLandlord: Boolean = false,
)

/** Start background delivery after login. */
expect fun onSessionSaved()

/** Stop background delivery after logout. */
expect fun onSessionCleared()

/** Android 13+ permission dialog. Other platforms do nothing. */
@Composable
expect fun ChatNotificationPermissionEffect()
