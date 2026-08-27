package com.rotd.kejasafe.domain.call

data class IncomingCall(
    val meetingId: String,
    val callerId: String,
    val callerName: String,
    val meetingTitle: String,
    val meetingType: MeetingType = MeetingType.VIDEO,
    val createdAt: Long = System.currentTimeMillis(),
)


