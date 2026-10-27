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

/** Full-screen house-tour ring. Android turns the screen on; other platforms show a notification. */
expect fun showIncomingHouseTourRing(
    callerName: String,
    listingTitle: String,
    sessionId: String,
    peerUserId: String = "",
    listingId: String = "",
    peerIsLandlord: Boolean = false,
)

expect fun cancelHouseTourRing(sessionId: String)

expect fun startHouseTourRingSound()

expect fun stopHouseTourRingSound()

/** Start background delivery after login. */
expect fun onSessionSaved()

/** Drop this phone's push token while the login is still valid. */
expect fun onSessionClearing()

/** Stop background delivery after logout. */
expect fun onSessionCleared()

/** Android 13+ permission dialog. Other platforms do nothing. */
@Composable
expect fun ChatNotificationPermissionEffect()

/** Message id from a system notification the user just clicked. Empty when nothing is waiting. */
expect fun consumeClickedChatNotification(): String
