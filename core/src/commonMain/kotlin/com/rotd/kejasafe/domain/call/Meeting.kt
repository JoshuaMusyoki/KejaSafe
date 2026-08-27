package com.rotd.kejasafe.domain.call

import kotlinx.serialization.Serializable

@Serializable
data class ParticipantStatus(
    val isMicOn: Boolean = true,
    val isCameraOn: Boolean = true,
    val isHandRaised: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)

@Serializable
data class Meeting(
    val id: String = "",
    val title: String = "",
    val hostId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantStatuses: Map<String, ParticipantStatus> = emptyMap(),
    val startTime: Long = 0L,
    val type: MeetingType = MeetingType.VIDEO,
    val status: MeetingStatus = MeetingStatus.SCHEDULED,
    val meetingUrl: String = "",
    val agenda: String = "",
)

@Serializable
enum class MeetingType {
    VIDEO, AUDIO
}

@Serializable
enum class MeetingStatus {
    SCHEDULED, ACTIVE, ENDED
}


