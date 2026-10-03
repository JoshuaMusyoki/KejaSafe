package com.rotd.havenpad.chat

import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.notify.showIncomingChatNotification

/**
 * Pulls each chat the signed-in user can open and raises a notification for
 * messages that arrived since the last check. Shared by the open app and the
 * background worker.
 */
suspend fun pullNewChatMessages(repository: ApartmentChatRepository) {
    SessionManager.restore()
    val me = SessionManager.currentUser.value?.id ?: return
    val peers = repository.refreshEligiblePeers().getOrElse { return }
    val primed = ChatNotificationInbox.isPrimed()
    peers.forEach { peer ->
        repository.syncConversation(peer.listingId, peer.userId)
        val messages = repository.cachedMessages(peer.listingId, peer.userId)
        if (!primed) {
            ChatNotificationInbox.markSeen(messages.map { it.id })
            return@forEach
        }
        val readingThisChat = AppVisibility.inForeground &&
            OpenChat.peerKey == openChatKey(peer.listingId, peer.userId)
        messages.filter { message ->
            message.senderId != me && !ChatNotificationInbox.hasSeen(message.id)
        }.forEach { message ->
            ChatNotificationInbox.markSeen(listOf(message.id))
            val notice = ChatNotificationInbox.record(
                peer = peer,
                messageId = message.id,
                body = message.previewText(),
            )
            if (readingThisChat) {
                ChatNotificationInbox.markRead(notice.id)
                return@forEach
            }
            showIncomingChatNotification(
                title = notice.title,
                body = notice.body,
                notificationKey = notice.id,
                peerUserId = peer.userId,
                listingId = peer.listingId,
                listingTitle = peer.listingTitle,
                bookingId = peer.bookingId,
                displayName = peer.displayName,
                peerIsLandlord = peer.peerIsLandlord,
            )
        }
    }
    if (!primed) ChatNotificationInbox.markPrimed()
}
