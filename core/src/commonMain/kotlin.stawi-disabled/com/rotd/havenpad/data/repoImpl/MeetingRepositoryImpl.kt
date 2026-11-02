package com.rotd.stawi.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.rotd.stawi.data.BuildConfig
import com.rotd.stawi.data.livekit.LiveKitTokenFactory
import com.rotd.stawi.domain.model.IncomingCall
import com.rotd.stawi.domain.model.LiveKitConnectionDetails
import com.rotd.stawi.domain.model.Meeting
import com.rotd.stawi.domain.model.MeetingStatus
import com.rotd.stawi.domain.model.MeetingType
import com.rotd.stawi.domain.model.ParticipantStatus
import com.rotd.stawi.domain.repository.AuthRepository
import com.rotd.stawi.domain.repository.MeetingInvite
import com.rotd.stawi.domain.repository.MeetingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val functions: FirebaseFunctions,
    /** Install applicationId for cross-flavor FCM targeting. */
    private val appPackageName: String,
) : MeetingRepository {

    override fun getMeetings(): Flow<List<Meeting>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptyList())

        callbackFlow {
            val subscription = firestore.collection("meetings")
                .whereArrayContains("participantIds", user.uid)
                .addSnapshotListener { snapshot, _ ->
                    val meetings = snapshot?.documents?.mapNotNull { it.toMeeting() } ?: emptyList()
                    trySend(meetings)
                }
            awaitClose { subscription.remove() }
        }
    }

    override suspend fun createMeeting(meeting: Meeting): Result<String> {
        return try {
            val payload = hashMapOf(
                "title" to meeting.title,
                "hostId" to meeting.hostId,
                "participantIds" to meeting.participantIds,
                "participantStatuses" to meeting.participantStatuses.mapValues { (_, status) ->
                    status.toFirestoreMap()
                },
                "startTime" to meeting.startTime,
                "type" to meeting.type.name,
                "status" to meeting.status.name,
                "meetingUrl" to meeting.meetingUrl,
                "agenda" to meeting.agenda,
            )
            val docRef = firestore.collection("meetings").add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMeetingStatus(meetingId: String, status: MeetingStatus): Result<Unit> {
        return try {
            firestore.collection("meetings").document(meetingId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addParticipant(meetingId: String, userId: String): Result<Unit> {
        return try {
            val docRef = firestore.collection("meetings").document(meetingId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val participantIds = snapshot.get("participantIds") as? List<*> ?: emptyList<String>()
                if (!participantIds.contains(userId)) {
                    transaction.update(docRef, "participantIds", participantIds + userId)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateScheduledMeeting(
        meetingId: String,
        title: String,
        agenda: String,
        startTime: Long,
        type: MeetingType,
        participantIds: List<String>,
    ): Result<Unit> {
        return try {
            firestore.collection("meetings").document(meetingId)
                .update(
                    mapOf(
                        "title" to title,
                        "agenda" to agenda,
                        "startTime" to startTime,
                        "type" to type.name,
                        "participantIds" to participantIds.distinct(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateParticipantStatus(
        meetingId: String,
        userId: String,
        status: ParticipantStatus
    ): Result<Unit> {
        return try {
            // Write a plain map so nested status fields deserialize reliably for peers.
            firestore.collection("meetings").document(meetingId)
                .update("participantStatuses.$userId", status.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMeetingInvites(): Flow<List<MeetingInvite>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())

            callbackFlow {
                val subscription = firestore.collection("users")
                    .document(user.uid)
                    .collection("meetingInvites")
                    .addSnapshotListener { snapshot, _ ->
                        val invites = snapshot?.documents?.mapNotNull { doc ->
                            val invitePackage = doc.getString("appPackage").orEmpty()
                            if (invitePackage.isNotBlank() && invitePackage != appPackageName) {
                                return@mapNotNull null
                            }
                            MeetingInvite(
                                meetingId = doc.getString("meetingId") ?: doc.id,
                                title = doc.getString("title").orEmpty(),
                                hostId = doc.getString("hostId").orEmpty(),
                                hostName = doc.getString("hostName").orEmpty(),
                                startTime = doc.getLong("startTime") ?: 0L,
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                read = doc.getBoolean("read") == true,
                            )
                        } ?: emptyList()
                        trySend(invites)
                    }
                awaitClose { subscription.remove() }
            }
        }

    override fun getIncomingCalls(): Flow<List<IncomingCall>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())

            callbackFlow {
                val subscription = firestore.collection("users")
                    .document(user.uid)
                    .collection("incomingCalls")
                    .addSnapshotListener { snapshot, _ ->
                        val calls = snapshot?.documents?.mapNotNull { doc ->
                            val callPackage = doc.getString("appPackage").orEmpty()
                            if (callPackage.isNotBlank() && callPackage != appPackageName) {
                                return@mapNotNull null
                            }
                            val status = doc.getString("status").orEmpty()
                            if (status == "ended" || status == "declined" || status == "accepted") {
                                return@mapNotNull null
                            }
                            IncomingCall(
                                meetingId = doc.getString("meetingId") ?: doc.id,
                                callerId = doc.getString("callerId").orEmpty(),
                                callerName = doc.getString("callerName").orEmpty(),
                                meetingTitle = doc.getString("meetingTitle").orEmpty(),
                                meetingType = runCatching {
                                    MeetingType.valueOf(
                                        doc.getString("meetingType") ?: MeetingType.VIDEO.name
                                    )
                                }.getOrDefault(MeetingType.VIDEO),
                                createdAt = doc.getLong("createdAt") ?: 0L,
                            )
                        } ?: emptyList()
                        trySend(calls.sortedByDescending { it.createdAt })
                    }
                awaitClose { subscription.remove() }
            }
        }

    override suspend fun submitMeetingRating(
        meetingId: String,
        userId: String,
        rating: Int
    ): Result<Unit> {
        return try {
            firestore.collection("meetings").document(meetingId)
                .update(
                    mapOf(
                        "ratings.$userId" to rating.coerceIn(1, 5),
                        "ratedAt.$userId" to System.currentTimeMillis(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun notifyScheduledInvitees(
        meetingId: String,
        title: String,
        hostId: String,
        hostName: String,
        startTime: Long,
        inviteeIds: List<String>,
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val createdAt = System.currentTimeMillis()
            inviteeIds.distinct()
                .filter { it.isNotBlank() && it != hostId }
                .forEach { inviteeId ->
                    val ref = firestore.collection("users")
                        .document(inviteeId)
                        .collection("meetingInvites")
                        .document(meetingId)
                    batch.set(
                        ref,
                        mapOf(
                            "meetingId" to meetingId,
                            "title" to title,
                            "hostId" to hostId,
                            "hostName" to hostName,
                            "startTime" to startTime,
                            "createdAt" to createdAt,
                            "read" to false,
                            "appPackage" to appPackageName,
                        )
                    )
                }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markMeetingInviteRead(meetingId: String): Result<Unit> {
        return try {
            val uid = authRepository.currentUser.first()?.uid
                ?: return Result.failure(IllegalStateException("Not signed in"))
            firestore.collection("users")
                .document(uid)
                .collection("meetingInvites")
                .document(meetingId)
                .update("read", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendIncomingCall(
        calleeId: String,
        call: IncomingCall,
    ): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(calleeId)
                .collection("incomingCalls")
                .document(call.meetingId)
                .set(
                    mapOf(
                        "meetingId" to call.meetingId,
                        "callerId" to call.callerId,
                        "callerName" to call.callerName,
                        "meetingTitle" to call.meetingTitle,
                        "meetingType" to call.meetingType.name,
                        "createdAt" to call.createdAt,
                        "status" to "ringing",
                        "appPackage" to appPackageName,
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearIncomingCall(calleeId: String, meetingId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(calleeId)
                .collection("incomingCalls")
                .document(meetingId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteMeeting(meetingId: String): Result<Unit> {
        return try {
            firestore.collection("meetings").document(meetingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mints a short-lived LiveKit JWT.
     *
     * Uses local API key/secret when present (works without Firebase Blaze).
     * Falls back to Cloud Function `createLiveKitToken` when keys are absent.
     */
    override suspend fun fetchLiveKitConnection(
        roomId: String,
        userId: String,
        displayName: String,
    ): Result<LiveKitConnectionDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val localKey = BuildConfig.LIVEKIT_API_KEY
            val localSecret = BuildConfig.LIVEKIT_API_SECRET
            val serverUrl = BuildConfig.LIVEKIT_URL
            val name = displayName.ifBlank { userId }

            if (localKey.isNotBlank() && localSecret.isNotBlank() && serverUrl.isNotBlank()) {
                val token = LiveKitTokenFactory.create(
                    apiKey = localKey,
                    apiSecret = localSecret,
                    identity = userId,
                    roomName = roomId,
                    displayName = name,
                )
                return@runCatching LiveKitConnectionDetails(
                    serverUrl = serverUrl,
                    participantToken = token
                )
            }

            val result = try {
                functions
                    .getHttpsCallable("createLiveKitToken")
                    .call(
                        hashMapOf(
                            "roomId" to roomId,
                            "participantName" to name,
                        )
                    )
                    .await()
            } catch (e: FirebaseFunctionsException) {
                val hint = when (e.code) {
                    FirebaseFunctionsException.Code.NOT_FOUND ->
                        "Add LIVEKIT_API_KEY / LIVEKIT_API_SECRET to local.properties, " +
                            "or enable Firebase Blaze and deploy createLiveKitToken."
                    FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                        "Sign in again to join the meeting."
                    FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                        e.message ?: "LiveKit server secrets are not configured."
                    else -> e.message ?: "Failed to fetch LiveKit token (${e.code})"
                }
                throw IllegalStateException(hint, e)
            }

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>
                ?: throw IllegalStateException("Empty LiveKit token response")

            val token = data["participantToken"] as? String
                ?: throw IllegalStateException("Missing participantToken")
            val remoteUrl = (data["serverUrl"] as? String)?.takeIf { it.isNotBlank() }
                ?: serverUrl

            if (remoteUrl.isBlank()) {
                throw IllegalStateException("LIVEKIT_URL is not configured")
            }

            LiveKitConnectionDetails(
                serverUrl = remoteUrl,
                participantToken = token
            )
        }
    }

    private fun DocumentSnapshot.toMeeting(): Meeting? {
        if (!exists()) return null
        val participantIds = (get("participantIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
        return Meeting(
            id = id,
            title = getString("title").orEmpty(),
            hostId = getString("hostId").orEmpty(),
            participantIds = participantIds,
            participantStatuses = parseParticipantStatuses(get("participantStatuses")),
            startTime = getLong("startTime") ?: 0L,
            type = runCatching {
                MeetingType.valueOf(getString("type") ?: MeetingType.VIDEO.name)
            }.getOrDefault(MeetingType.VIDEO),
            status = runCatching {
                MeetingStatus.valueOf(getString("status") ?: MeetingStatus.SCHEDULED.name)
            }.getOrDefault(MeetingStatus.SCHEDULED),
            meetingUrl = getString("meetingUrl").orEmpty(),
            agenda = getString("agenda").orEmpty(),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseParticipantStatuses(raw: Any?): Map<String, ParticipantStatus> {
        val outer = raw as? Map<*, *> ?: return emptyMap()
        return outer.mapNotNull { (key, value) ->
            val userId = key as? String ?: return@mapNotNull null
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            userId to ParticipantStatus(
                isMicOn = fields["isMicOn"] as? Boolean ?: true,
                isCameraOn = fields["isCameraOn"] as? Boolean ?: true,
                isHandRaised = fields["isHandRaised"] as? Boolean ?: false,
                lastActive = (fields["lastActive"] as? Number)?.toLong()
                    ?: System.currentTimeMillis(),
            )
        }.toMap()
    }

    private fun ParticipantStatus.toFirestoreMap(): Map<String, Any> = mapOf(
        "isMicOn" to isMicOn,
        "isCameraOn" to isCameraOn,
        "isHandRaised" to isHandRaised,
        "lastActive" to lastActive,
    )
}



