package com.rotd.keja_safe.domain.util

interface Notifier {
    fun showIncomingCall(callerName: String, meetingId: String, isVideo: Boolean = true)
    fun cancelIncomingCall(meetingId: String)
    fun showMeetingInvite(title: String, body: String, meetingId: String)
    fun showMeetingReminder(title: String, meetingId: String)

    /**
     * @param sender display name / title
     * @param message body preview
     * @param peerId stable id used for grouping, cancel, and deep-link open (user uid or phone)
     */
    fun showChatMessageNotification(
        sender: String,
        message: String,
        peerId: String = "",
    )

    fun cancelChatNotificationsForPeer(peerId: String)
    fun cancelAllChatNotifications()
    fun showNewVideoNotification(creatorName: String, videoTitle: String)

    /** System tray notification when a new Play Store version is released. */
    fun showAppUpdateNotification(title: String, body: String, versionName: String? = null)
}
