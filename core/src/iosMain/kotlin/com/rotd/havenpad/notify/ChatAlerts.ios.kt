package com.rotd.havenpad.notify

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSTimer
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private var notificationsGranted = false
private var clickedNotificationId = ""
private var ringTimer: NSTimer? = null
private val noticeDelegate = IosNoticeDelegate()

private class IosNoticeDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        clickedNotificationId = didReceiveNotificationResponse.notification.request.identifier
        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (platform.UserNotifications.UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound,
        )
    }
}

private fun center(): UNUserNotificationCenter {
    val current = UNUserNotificationCenter.currentNotificationCenter()
    current.delegate = noticeDelegate
    return current
}

actual fun requestChatNotificationPermission() {
    center().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
    ) { granted, _ ->
        notificationsGranted = granted
    }
}

actual fun chatNotificationsAllowed(): Boolean = notificationsGranted

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
    showLocal(title = title, body = body, identifier = notificationKey)
}

actual fun showIncomingHouseTourRing(
    callerName: String,
    listingTitle: String,
    sessionId: String,
    peerUserId: String,
    listingId: String,
    peerIsLandlord: Boolean,
) {
    startHouseTourRingSound()
    val place = listingTitle.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    showLocal(
        title = callerName.ifBlank { "House tour" },
        body = "Incoming house tour$place",
        identifier = "call-$sessionId",
    )
}

actual fun cancelHouseTourRing(sessionId: String) {
    stopHouseTourRingSound()
    val ids = listOf("call-$sessionId")
    center().removeDeliveredNotificationsWithIdentifiers(ids)
    center().removePendingNotificationRequestsWithIdentifiers(ids)
}

actual fun startHouseTourRingSound() {
    if (ringTimer != null) return
    playRing()
    ringTimer = NSTimer.scheduledTimerWithTimeInterval(2.4, true) { _ -> playRing() }
}

actual fun stopHouseTourRingSound() {
    ringTimer?.invalidate()
    ringTimer = null
}

private fun playRing() {
    AudioServicesPlaySystemSound(1005u)
}

actual fun onSessionClearing() = Unit

actual fun onSessionSaved() = Unit

actual fun onSessionCleared() {
    stopHouseTourRingSound()
}

actual fun consumeClickedChatNotification(): String {
    val id = clickedNotificationId
    clickedNotificationId = ""
    return id
}

@Composable
actual fun ChatNotificationPermissionEffect() {
    LaunchedEffect(Unit) {
        requestChatNotificationPermission()
    }
}

private fun showLocal(title: String, body: String, identifier: String) {
    val content = UNMutableNotificationContent()
    content.title = title
    content.body = body
    content.sound = UNNotificationSound.defaultSound()
    val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.1, false)
    val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
    center().addNotificationRequest(request) { }
}
