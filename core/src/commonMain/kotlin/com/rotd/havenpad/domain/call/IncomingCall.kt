package com.rotd.havenpad.domain.call

import com.rotd.havenpad.data.platformEpochMillis

data class IncomingCall(
    val meetingId: String,
    val callerId: String,
    val callerName: String,
    val meetingTitle: String,
    val meetingType: MeetingType = MeetingType.VIDEO,
    val createdAt: Long = platformEpochMillis(),
)


