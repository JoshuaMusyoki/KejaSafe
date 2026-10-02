package com.rotd.havenpad.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.rotd.havenpad.CallLogEntry
import com.rotd.havenpad.LiveTourSession
import com.rotd.havenpad.LiveTourStatus
import com.rotd.havenpad.data.platformEpochMillis
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.network.IncomingCallCenter
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.notify.ChatNotificationPermissionEffect
import com.rotd.havenpad.notify.cancelHouseTourRing
import com.rotd.havenpad.notify.showIncomingHouseTourRing
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/** Listing + person currently open, so a notification is skipped while you are reading that chat. */
object OpenChat {
    var peerKey: String? = null
}

fun openChatKey(listingId: String, userId: String): String = "${listingId}_$userId"

private val notifiedCallIds = mutableSetOf<String>()

/** Rings the signed-in user when the other person has started a live call. */
suspend fun pullIncomingCall() {
    val detected = detectIncomingRing()
    if (!detected.checked) return
    val ring = detected.ring
    if (ring != null && ring.sessionId == IncomingCallCenter.suppressedSessionId) {
        return
    }
    if (ring == null) {
        val previous = IncomingCallCenter.ringing.value?.sessionId
        IncomingCallCenter.clear()
        if (previous != null) cancelHouseTourRing(previous)
        return
    }
    IncomingCallCenter.show(ring)
    if (!notifiedCallIds.add(ring.sessionId)) return
    showIncomingHouseTourRing(
        callerName = ring.peerName.ifBlank { "House tour" },
        listingTitle = ring.listingTitle,
        sessionId = ring.sessionId,
        peerUserId = ring.peerUserId,
        listingId = ring.listingId,
        peerIsLandlord = ring.peerIsLandlord,
    )
}

private data class RingCheck(val checked: Boolean, val ring: CallLogEntry?)

private suspend fun detectIncomingRing(): RingCheck {
    val incoming = LiveTourClient.incomingRing()
    if (incoming.isSuccess) return RingCheck(checked = true, ring = incoming.getOrNull())
    val me = SessionManager.currentUser.value?.id ?: return RingCheck(checked = false, ring = null)
    val tours = LiveTourClient.myOpenTours().getOrNull() ?: return RingCheck(checked = false, ring = null)
    val now = platformEpochMillis()
    val session = tours.firstOrNull { tour -> tour.isRingingFor(me, now) }
        ?: return RingCheck(checked = true, ring = null)
    return RingCheck(checked = true, ring = session.toIncomingLog(me))
}

private fun LiveTourSession.isRingingFor(userId: String, now: Long): Boolean {
    if (status != LiveTourStatus.READY && status != LiveTourStatus.ACTIVE) return false
    if (answeredAt != null) return false
    if (callerId == userId) return false
    if (callerId.isNotBlank()) return true
    if (status == LiveTourStatus.ACTIVE) return true
    if (preferredTimeNote.contains("call", ignoreCase = true)) return true
    val started = startedAt ?: return false
    return now - started in 0..180_000L
}

private fun LiveTourSession.toIncomingLog(userId: String): CallLogEntry {
    val iAmTenant = userId == tenantId
    return CallLogEntry(
        sessionId = id,
        listingId = listingId,
        listingTitle = listingTitle,
        peerUserId = if (iAmTenant) landlordId else tenantId,
        peerName = if (iAmTenant) landlordName else tenantName,
        peerIsLandlord = iAmTenant,
        mediaType = mediaType,
        missed = false,
        outgoing = false,
        status = status,
        createdAt = createdAt,
        endedAt = endedAt,
    )
}

/**
 * While this screen is in memory, keep checking for messages.
 * A background worker does the same check after the app is closed.
 */
@Composable
fun ChatIncomingNotifications() {
    val chatRepository = koinInject<ApartmentChatRepository>()
    ChatNotificationPermissionEffect()
    LaunchedEffect(Unit) {
        while (true) {
            if (SessionManager.currentUser.value?.id == null) {
                delay(2_000)
                continue
            }
            runCatching { pullIncomingCall() }
            runCatching { pullNewChatMessages(chatRepository) }
            delay(2_000)
        }
    }
}

@Composable
fun TrackOpenChat(listingId: String?, userId: String?) {
    DisposableEffect(listingId, userId) {
        OpenChat.peerKey = if (listingId.isNullOrBlank() || userId.isNullOrBlank()) {
            null
        } else {
            openChatKey(listingId, userId)
        }
        onDispose {
            if (OpenChat.peerKey == openChatKey(listingId.orEmpty(), userId.orEmpty())) {
                OpenChat.peerKey = null
            }
        }
    }
}
