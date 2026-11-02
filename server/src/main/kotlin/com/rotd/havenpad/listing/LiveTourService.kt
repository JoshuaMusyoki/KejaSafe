package com.rotd.havenpad.listing

import com.rotd.havenpad.CallLogEntry
import com.rotd.havenpad.CreateLiveTourRequest
import com.rotd.havenpad.IncomingRequest
import com.rotd.havenpad.LiveKitConnectionDetails
import com.rotd.havenpad.LiveTourMediaType
import com.rotd.havenpad.LiveTourSession
import com.rotd.havenpad.LiveTourStatus
import com.rotd.havenpad.RequestType
import com.rotd.havenpad.RescheduleLiveTourRequest
import com.rotd.havenpad.RtcIceCandidate
import com.rotd.havenpad.RtcRoomSnapshot
import com.rotd.havenpad.RtcSessionDescription
import com.rotd.havenpad.db.ListingsTable
import com.rotd.havenpad.db.ListingRequestsTable
import com.rotd.havenpad.db.LiveTourIceTable
import com.rotd.havenpad.db.LiveTourRtcTable
import com.rotd.havenpad.db.HiddenCallLogsTable
import com.rotd.havenpad.db.LiveTourSessionsTable
import com.rotd.havenpad.db.UsersTable
import com.rotd.havenpad.livekit.LiveKitTokenService
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

object LiveTourService {

    fun createTour(
        listingId: String,
        tenantId: String,
        request: CreateLiveTourRequest,
    ): LiveTourSession = transaction {
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")
        val landlordId = listing[ListingsTable.landlordId]
        if (landlordId == tenantId) error("You cannot request a live tour of your own listing")

        val tenantName = userName(tenantId)
        val title = listing[ListingsTable.title]
        val note = request.preferredTimeNote.trim()
        val requestId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        ListingRequestsTable.insert {
            it[id] = requestId
            it[ListingRequestsTable.listingId] = listingId
            it[ListingRequestsTable.landlordId] = landlordId
            it[requesterName] = tenantName
            it[type] = RequestType.LIVE_INSPECTION
            it[propertyTitle] = title
            it[detail] = note.ifBlank { "Requested a live house tour" }
            it[timeLabel] = "Awaiting confirm"
            it[createdAt] = now
        }

        LiveTourSessionsTable.insert {
            it[id] = sessionId
            it[LiveTourSessionsTable.listingId] = listingId
            it[LiveTourSessionsTable.landlordId] = landlordId
            it[LiveTourSessionsTable.tenantId] = tenantId
            it[LiveTourSessionsTable.requestId] = requestId
            it[roomName] = ""
            it[mediaType] = request.mediaType
            it[status] = LiveTourStatus.SCHEDULED
            it[preferredTimeNote] = note
            it[createdAt] = now
            it[startedAt] = null
            it[endedAt] = null
        }

        loadSession(sessionId) ?: error("Failed to create live tour")
    }

    /**
     * Instant call from chat. The other person is rung until they answer or someone hangs up.
     */
    fun startDirectTour(
        listingId: String,
        callerId: String,
        peerUserId: String,
        mediaType: LiveTourMediaType = LiveTourMediaType.VIDEO,
    ): LiveTourSession = transaction {
        if (peerUserId.isBlank()) error("Peer is required")
        if (callerId == peerUserId) error("Cannot call yourself")

        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
            ?: error("Listing not found")
        val landlordId = listing[ListingsTable.landlordId]
        val tenantId = when {
            callerId == landlordId -> peerUserId
            peerUserId == landlordId -> callerId
            else -> error("Live calls are between the landlord and a tenant on this listing")
        }
        if (tenantId == landlordId) error("Invalid call participants")

        val existing = LiveTourSessionsTable.selectAll()
            .where {
                (LiveTourSessionsTable.listingId eq listingId) and
                    (LiveTourSessionsTable.landlordId eq landlordId) and
                    (LiveTourSessionsTable.tenantId eq tenantId) and
                    (LiveTourSessionsTable.status inList listOf(
                        LiveTourStatus.SCHEDULED,
                        LiveTourStatus.READY,
                        LiveTourStatus.ACTIVE,
                    ))
            }
            .maxByOrNull { it[LiveTourSessionsTable.createdAt] }

        if (existing != null) {
            val sessionId = existing[LiveTourSessionsTable.id]
            val now = System.currentTimeMillis()
            clearRtc(sessionId)
            LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
                it[LiveTourSessionsTable.roomName] = "webrtc-${sessionId.take(8)}"
                it[status] = LiveTourStatus.READY
                it[startedAt] = now
                it[LiveTourSessionsTable.mediaType] = mediaType
                it[LiveTourSessionsTable.callerId] = callerId
                it[answeredAt] = null
                it[missed] = false
                it[endedAt] = null
            }
            ListingRequestsTable.update({
                ListingRequestsTable.id eq existing[LiveTourSessionsTable.requestId]
            }) {
                it[timeLabel] = "Ready to join"
                it[detail] = "Direct call from chat"
            }
            return@transaction loadSession(sessionId) ?: error("Tour disappeared")
        }

        val title = listing[ListingsTable.title]
        val requestId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        val roomName = "tour-${sessionId.take(8)}"
        val now = System.currentTimeMillis()
        val callerName = userName(callerId)

        ListingRequestsTable.insert {
            it[id] = requestId
            it[ListingRequestsTable.listingId] = listingId
            it[ListingRequestsTable.landlordId] = landlordId
            it[requesterName] = callerName
            it[type] = RequestType.LIVE_INSPECTION
            it[propertyTitle] = title
            it[detail] = "Direct call from chat"
            it[timeLabel] = "Ready to join"
            it[createdAt] = now
        }

        LiveTourSessionsTable.insert {
            it[id] = sessionId
            it[LiveTourSessionsTable.listingId] = listingId
            it[LiveTourSessionsTable.landlordId] = landlordId
            it[LiveTourSessionsTable.tenantId] = tenantId
            it[LiveTourSessionsTable.requestId] = requestId
            it[LiveTourSessionsTable.roomName] = roomName
            it[LiveTourSessionsTable.mediaType] = mediaType
            it[status] = LiveTourStatus.READY
            it[preferredTimeNote] = "Direct call from chat"
            it[createdAt] = now
            it[startedAt] = now
            it[endedAt] = null
            it[LiveTourSessionsTable.callerId] = callerId
            it[answeredAt] = null
            it[missed] = false
        }

        loadSession(sessionId) ?: error("Failed to start live call")
    }

    fun confirmTour(sessionId: String, landlordId: String): LiveTourSession = transaction {
        val row = LiveTourSessionsTable.selectAll()
            .where { LiveTourSessionsTable.id eq sessionId }
            .singleOrNull()
            ?: error("Live tour not found")
        if (row[LiveTourSessionsTable.landlordId] != landlordId) {
            error("Not your listing tour")
        }
        val current = row[LiveTourSessionsTable.status]
        if (current == LiveTourStatus.READY || current == LiveTourStatus.ACTIVE) {
            if (row[LiveTourSessionsTable.callerId].isBlank()) {
                LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
                    it[LiveTourSessionsTable.callerId] = landlordId
                    it[answeredAt] = null
                    it[missed] = false
                }
            }
            return@transaction loadSession(sessionId) ?: error("Live tour not found")
        }
        if (current != LiveTourStatus.SCHEDULED) {
            error("This tour can no longer be confirmed")
        }

        val roomName = "webrtc-${sessionId.take(8)}"
        val now = System.currentTimeMillis()
        LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
            it[LiveTourSessionsTable.roomName] = roomName
            it[status] = LiveTourStatus.READY
            it[startedAt] = now
            it[LiveTourSessionsTable.callerId] = landlordId
            it[answeredAt] = null
            it[missed] = false
        }
        ListingRequestsTable.update({ ListingRequestsTable.id eq row[LiveTourSessionsTable.requestId] }) {
            it[timeLabel] = "Ready to join"
            it[detail] = "Live tour confirmed — join when ready"
        }
        loadSession(sessionId) ?: error("Tour disappeared")
    }

    fun rescheduleTour(
        sessionId: String,
        landlordId: String,
        request: RescheduleLiveTourRequest,
    ): LiveTourSession = transaction {
        val row = LiveTourSessionsTable.selectAll()
            .where { LiveTourSessionsTable.id eq sessionId }
            .singleOrNull()
            ?: error("Live tour not found")
        if (row[LiveTourSessionsTable.landlordId] != landlordId) {
            error("Not your listing tour")
        }
        if (row[LiveTourSessionsTable.status] == LiveTourStatus.ENDED) {
            error("Tour already ended")
        }
        val note = request.preferredTimeNote.trim()
        if (note.isBlank()) error("Preferred time note is required")

        LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
            it[preferredTimeNote] = note
            it[status] = LiveTourStatus.SCHEDULED
            it[roomName] = ""
            it[startedAt] = null
        }
        ListingRequestsTable.update({ ListingRequestsTable.id eq row[LiveTourSessionsTable.requestId] }) {
            it[detail] = note
            it[timeLabel] = "Reschedule proposed"
        }
        loadSession(sessionId) ?: error("Tour disappeared")
    }

    fun getTour(sessionId: String, userId: String): LiveTourSession? = transaction {
        val session = loadSession(sessionId) ?: return@transaction null
        if (session.landlordId != userId && session.tenantId != userId) return@transaction null
        session
    }

    fun issueToken(sessionId: String, userId: String): LiveKitConnectionDetails = transaction {
        val session = loadSession(sessionId) ?: error("Live tour not found")
        if (session.landlordId != userId && session.tenantId != userId) {
            error("Not a participant of this tour")
        }
        if (session.status !in listOf(LiveTourStatus.READY, LiveTourStatus.ACTIVE)) {
            error("Tour is not ready yet — landlord must Confirm first")
        }
        if (session.roomName.isBlank()) error("Tour room is not ready")

        val displayName = userName(userId)
        if (session.status == LiveTourStatus.READY) {
            LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
                it[status] = LiveTourStatus.ACTIVE
            }
        }
        val token = LiveKitTokenService.createToken(
            identity = userId,
            displayName = displayName,
            roomName = session.roomName,
            canPublish = true,
        )
        LiveKitConnectionDetails(
            serverUrl = LiveKitTokenService.serverUrl(),
            roomName = session.roomName,
            participantName = displayName,
            participantToken = token,
            sessionId = sessionId,
            listingTitle = session.listingTitle,
            mediaType = session.mediaType,
        )
    }

    fun endTour(sessionId: String, userId: String): LiveTourSession = transaction {
        val session = loadSession(sessionId) ?: error("Live tour not found")
        if (session.landlordId != userId && session.tenantId != userId) {
            error("Not a participant of this tour")
        }
        val now = System.currentTimeMillis()
        val unanswered = session.answeredAt == null && session.callerId.isNotBlank()
        LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
            it[status] = LiveTourStatus.ENDED
            it[endedAt] = now
            it[missed] = unanswered
        }
        clearRtc(sessionId)
        ListingRequestsTable.update({ ListingRequestsTable.id eq session.requestId }) {
            it[timeLabel] = "Ended"
        }
        loadSession(sessionId) ?: error("Tour disappeared")
    }

    /** Open live-tour cards for landlord inbox (id = session id). */
    fun incomingForLandlord(landlordId: String): List<IncomingRequest> = transaction {
        LiveTourSessionsTable.selectAll()
            .where {
                (LiveTourSessionsTable.landlordId eq landlordId) and
                    (LiveTourSessionsTable.status inList listOf(
                        LiveTourStatus.SCHEDULED,
                        LiveTourStatus.READY,
                        LiveTourStatus.ACTIVE,
                    ))
            }
            .map { row ->
                val session = row.toSession()
                IncomingRequest(
                    id = session.id,
                    requesterName = session.tenantName.ifBlank { "Tenant" },
                    type = RequestType.LIVE_INSPECTION,
                    propertyTitle = session.listingTitle,
                    detail = session.preferredTimeNote.ifBlank {
                        when (session.status) {
                            LiveTourStatus.READY, LiveTourStatus.ACTIVE -> "Ready to join live tour"
                            else -> "Requested a live house tour"
                        }
                    },
                    timeLabel = when (session.status) {
                        LiveTourStatus.SCHEDULED -> "Awaiting confirm"
                        LiveTourStatus.READY -> "Ready to join"
                        LiveTourStatus.ACTIVE -> "In progress"
                        LiveTourStatus.ENDED -> "Ended"
                    },
                )
            }
    }

    fun openToursForTenant(tenantId: String): List<LiveTourSession> = transaction {
        LiveTourSessionsTable.selectAll()
            .where {
                (LiveTourSessionsTable.tenantId eq tenantId) and
                    (LiveTourSessionsTable.status inList listOf(
                        LiveTourStatus.SCHEDULED,
                        LiveTourStatus.READY,
                        LiveTourStatus.ACTIVE,
                    ))
            }
            .map { it.toSession() }
            .sortedByDescending { it.createdAt }
    }

    /** Call the signed-in user has not answered. Scheduled tours do not ring. */
    fun incomingRing(userId: String): CallLogEntry? = transaction {
        if (userId.isBlank()) return@transaction null
        LiveTourSessionsTable.selectAll()
            .where {
                ((LiveTourSessionsTable.landlordId eq userId) or (LiveTourSessionsTable.tenantId eq userId)) and
                    (LiveTourSessionsTable.status inList listOf(LiveTourStatus.READY, LiveTourStatus.ACTIVE)) and
                    (LiveTourSessionsTable.callerId neq "") and
                    (LiveTourSessionsTable.callerId neq userId) and
                    LiveTourSessionsTable.answeredAt.isNull()
            }
            .maxByOrNull { it[LiveTourSessionsTable.startedAt] ?: it[LiveTourSessionsTable.createdAt] }
            ?.toSession()
            ?.toLog(userId)
    }

    fun callHistory(userId: String): List<CallLogEntry> = transaction {
        if (userId.isBlank()) return@transaction emptyList()
        val hidden = HiddenCallLogsTable.selectAll()
            .where { HiddenCallLogsTable.userId eq userId }
            .map { it[HiddenCallLogsTable.sessionId] }
            .toSet()
        LiveTourSessionsTable.selectAll()
            .where {
                ((LiveTourSessionsTable.landlordId eq userId) or (LiveTourSessionsTable.tenantId eq userId)) and
                    (LiveTourSessionsTable.status eq LiveTourStatus.ENDED)
            }
            .map { it.toSession().toLog(userId) }
            .filter { it.sessionId !in hidden }
            .sortedByDescending { it.endedAt ?: it.createdAt }
            .take(40)
    }

    /** Removes one call from this person's list only. */
    fun hideCall(userId: String, sessionId: String) {
        transaction {
            participantSession(sessionId, userId)
            val already = HiddenCallLogsTable.selectAll()
                .where {
                    (HiddenCallLogsTable.userId eq userId) and (HiddenCallLogsTable.sessionId eq sessionId)
                }
                .count() > 0
            if (!already) {
                HiddenCallLogsTable.insert {
                    it[HiddenCallLogsTable.userId] = userId
                    it[HiddenCallLogsTable.sessionId] = sessionId
                    it[hiddenAt] = System.currentTimeMillis()
                }
            }
        }
    }

    /** The person who tapped Call or Join. The other participant is rung until they answer. */
    fun ring(sessionId: String, userId: String): LiveTourSession = transaction {
        val session = participantSession(sessionId, userId)
        if (session.answeredAt != null && session.callerId.isNotBlank() && session.callerId != userId) {
            return@transaction session
        }
        val now = System.currentTimeMillis()
        LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
            it[LiveTourSessionsTable.callerId] = userId
            it[answeredAt] = null
            it[missed] = false
            it[startedAt] = now
            if (session.status == LiveTourStatus.SCHEDULED || session.status == LiveTourStatus.ENDED) {
                it[status] = LiveTourStatus.READY
            }
        }
        loadSession(sessionId) ?: error("Live tour not found")
    }

    /** Callee opened the call. Until this is set, hanging up counts as missed. */
    fun markJoined(sessionId: String, userId: String): LiveTourSession = transaction {
        val session = participantSession(sessionId, userId)
        if (session.callerId.isNotBlank() && session.callerId != userId && session.answeredAt == null) {
            LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
                it[answeredAt] = System.currentTimeMillis()
                if (session.status == LiveTourStatus.READY) {
                    it[status] = LiveTourStatus.ACTIVE
                }
            }
        }
        loadSession(sessionId) ?: error("Live tour not found")
    }

    fun rtcRoom(sessionId: String, userId: String): RtcRoomSnapshot = transaction {
        val session = participantSession(sessionId, userId)
        val row = LiveTourRtcTable.selectAll()
            .where { LiveTourRtcTable.sessionId eq sessionId }
            .singleOrNull()
        val ice = LiveTourIceTable.selectAll()
            .where { LiveTourIceTable.sessionId eq sessionId }
            .map { it.toIce() }
        RtcRoomSnapshot(
            role = if (session.landlordId == userId) "caller" else "callee",
            mediaType = session.mediaType,
            offer = row?.get(LiveTourRtcTable.offerSdp)?.let { RtcSessionDescription("offer", it) },
            answer = row?.get(LiveTourRtcTable.answerSdp)?.let { RtcSessionDescription("answer", it) },
            callerCandidates = ice.filter { it.first }.map { it.second },
            calleeCandidates = ice.filter { !it.first }.map { it.second },
        )
    }

    fun saveOffer(sessionId: String, userId: String, description: RtcSessionDescription) = transaction {
        val session = participantSession(sessionId, userId)
        if (session.landlordId != userId) error("Only the landlord starts the WebRTC offer")
        if (session.status !in listOf(LiveTourStatus.READY, LiveTourStatus.ACTIVE)) {
            error("Tour is not ready yet")
        }
        val existing = LiveTourRtcTable.selectAll()
            .where { LiveTourRtcTable.sessionId eq sessionId }
            .singleOrNull()
        if (existing == null) {
            LiveTourRtcTable.insert {
                it[LiveTourRtcTable.sessionId] = sessionId
                it[offerSdp] = description.sdp
                it[answerSdp] = null
            }
        } else if (existing[LiveTourRtcTable.offerSdp].isNullOrBlank()) {
            LiveTourRtcTable.update({ LiveTourRtcTable.sessionId eq sessionId }) {
                it[offerSdp] = description.sdp
            }
        }
        if (session.status == LiveTourStatus.READY) {
            LiveTourSessionsTable.update({ LiveTourSessionsTable.id eq sessionId }) {
                it[status] = LiveTourStatus.ACTIVE
            }
        }
    }

    fun saveAnswer(sessionId: String, userId: String, description: RtcSessionDescription) = transaction {
        val session = participantSession(sessionId, userId)
        if (session.tenantId != userId) error("Only the tenant sends the WebRTC answer")
        val existing = LiveTourRtcTable.selectAll()
            .where { LiveTourRtcTable.sessionId eq sessionId }
            .singleOrNull()
            ?: error("Waiting for the landlord offer")
        if (existing[LiveTourRtcTable.answerSdp].isNullOrBlank()) {
            LiveTourRtcTable.update({ LiveTourRtcTable.sessionId eq sessionId }) {
                it[answerSdp] = description.sdp
            }
        }
    }

    fun addIce(sessionId: String, userId: String, candidate: RtcIceCandidate) = transaction {
        val session = participantSession(sessionId, userId)
        if (candidate.candidate.isBlank()) return@transaction
        LiveTourIceTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[LiveTourIceTable.sessionId] = sessionId
            it[fromCaller] = session.landlordId == userId
            it[LiveTourIceTable.candidate] = candidate.candidate
            it[sdpMid] = candidate.sdpMid
            it[sdpMLineIndex] = candidate.sdpMLineIndex
        }
    }

    private fun participantSession(sessionId: String, userId: String): LiveTourSession {
        val session = loadSession(sessionId) ?: error("Live tour not found")
        if (session.landlordId != userId && session.tenantId != userId) {
            error("Not a participant of this tour")
        }
        return session
    }

    private fun clearRtc(sessionId: String) {
        LiveTourIceTable.deleteWhere { LiveTourIceTable.sessionId eq sessionId }
        LiveTourRtcTable.deleteWhere { LiveTourRtcTable.sessionId eq sessionId }
    }

    private fun ResultRow.toIce(): Pair<Boolean, RtcIceCandidate> =
        this[LiveTourIceTable.fromCaller] to RtcIceCandidate(
            candidate = this[LiveTourIceTable.candidate],
            sdpMid = this[LiveTourIceTable.sdpMid],
            sdpMLineIndex = this[LiveTourIceTable.sdpMLineIndex],
        )

    private fun loadSession(sessionId: String): LiveTourSession? {
        val row = LiveTourSessionsTable.selectAll()
            .where { LiveTourSessionsTable.id eq sessionId }
            .singleOrNull()
            ?: return null
        return row.toSession()
    }

    private fun ResultRow.toSession(): LiveTourSession {
        val listingId = this[LiveTourSessionsTable.listingId]
        val listing = ListingsTable.selectAll()
            .where { ListingsTable.id eq listingId }
            .singleOrNull()
        val landlordId = this[LiveTourSessionsTable.landlordId]
        val tenantId = this[LiveTourSessionsTable.tenantId]
        return LiveTourSession(
            id = this[LiveTourSessionsTable.id],
            listingId = listingId,
            listingTitle = listing?.get(ListingsTable.title).orEmpty(),
            landlordId = landlordId,
            landlordName = userName(landlordId),
            tenantId = tenantId,
            tenantName = userName(tenantId),
            requestId = this[LiveTourSessionsTable.requestId],
            roomName = this[LiveTourSessionsTable.roomName],
            mediaType = this[LiveTourSessionsTable.mediaType],
            status = this[LiveTourSessionsTable.status],
            preferredTimeNote = this[LiveTourSessionsTable.preferredTimeNote],
            createdAt = this[LiveTourSessionsTable.createdAt],
            startedAt = this[LiveTourSessionsTable.startedAt],
            endedAt = this[LiveTourSessionsTable.endedAt],
            callerId = this[LiveTourSessionsTable.callerId],
            answeredAt = this[LiveTourSessionsTable.answeredAt],
            missed = this[LiveTourSessionsTable.missed],
        )
    }

    private fun LiveTourSession.toLog(userId: String): CallLogEntry {
        val peerIsTenant = userId == landlordId
        return CallLogEntry(
            sessionId = id,
            listingId = listingId,
            listingTitle = listingTitle,
            peerUserId = if (peerIsTenant) tenantId else landlordId,
            peerName = if (peerIsTenant) tenantName else landlordName,
            peerIsLandlord = !peerIsTenant,
            mediaType = mediaType,
            missed = missed,
            outgoing = callerId == userId,
            status = status,
            createdAt = createdAt,
            endedAt = endedAt,
        )
    }

    private fun userName(userId: String): String =
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.fullName)
            ?: "User"
}
