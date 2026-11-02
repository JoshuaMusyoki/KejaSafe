package com.rotd.keja_safe.domain.repository

import com.rotd.keja_safe.domain.model.IncomingCall
import com.rotd.keja_safe.domain.model.LiveKitConnectionDetails
import com.rotd.keja_safe.domain.model.Meeting
import com.rotd.keja_safe.domain.model.MeetingStatus
import com.rotd.keja_safe.domain.model.MeetingType
import com.rotd.keja_safe.domain.model.ParticipantStatus
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun getMeetings(): Flow<List<Meeting>>
    /** Unread scheduled-meeting invites for the signed-in user. */
    fun getMeetingInvites(): Flow<List<MeetingInvite>>
    /** Active ringing incoming calls for the signed-in user. */
    fun getIncomingCalls(): Flow<List<IncomingCall>>
    suspend fun createMeeting(meeting: Meeting): Result<String>
    suspend fun updateMeetingStatus(meetingId: String, status: MeetingStatus): Result<Unit>
    suspend fun addParticipant(meetingId: String, userId: String): Result<Unit>
    suspend fun updateScheduledMeeting(
        meetingId: String,
        title: String,
        agenda: String,
        startTime: Long,
        type: MeetingType,
        participantIds: List<String>,
    ): Result<Unit>
    suspend fun updateParticipantStatus(meetingId: String, userId: String, status: ParticipantStatus): Result<Unit>
    suspend fun submitMeetingRating(meetingId: String, userId: String, rating: Int): Result<Unit>
    suspend fun notifyScheduledInvitees(
        meetingId: String,
        title: String,
        hostId: String,
        hostName: String,
        startTime: Long,
        inviteeIds: List<String>,
    ): Result<Unit>
    suspend fun markMeetingInviteRead(meetingId: String): Result<Unit>
    suspend fun sendIncomingCall(
        calleeId: String,
        call: IncomingCall,
    ): Result<Unit>
    suspend fun clearIncomingCall(calleeId: String, meetingId: String): Result<Unit>
    suspend fun fetchLiveKitConnection(
        roomId: String,
        userId: String,
        displayName: String = userId,
    ): Result<LiveKitConnectionDetails>
    suspend fun deleteMeeting(meetingId: String): Result<Unit>
}

data class MeetingInvite(
    val meetingId: String,
    val title: String,
    val hostId: String,
    val hostName: String,
    val startTime: Long,
    val createdAt: Long,
    val read: Boolean = false,
)


