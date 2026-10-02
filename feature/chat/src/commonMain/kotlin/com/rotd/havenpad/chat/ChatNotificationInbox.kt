package com.rotd.havenpad.chat

import com.rotd.havenpad.data.platformEpochMillis
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.storage.LocalSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChatNotice(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val peerUserId: String,
    val listingId: String,
    val listingTitle: String,
    val bookingId: String,
    val displayName: String,
    val peerIsLandlord: Boolean,
    val read: Boolean = false,
)

object ChatNotificationInbox {
    private const val INBOX_KEY = "chat_notification_inbox"
    private const val SEEN_KEY = "chat_notification_seen"
    private const val PRIMED_KEY = "chat_notifications_primed"
    private const val MAX_ITEMS = 80
    private const val MAX_SEEN = 400

    private val json = Json { ignoreUnknownKeys = true }
    private val _items = MutableStateFlow(load())
    val items = _items.asStateFlow()

    fun isPrimed(): Boolean = LocalSettings.getString(PRIMED_KEY) == "1"

    fun markPrimed() {
        LocalSettings.putString(PRIMED_KEY, "1")
    }

    fun hasSeen(id: String): Boolean = seenIds().contains(id)

    fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val next = (seenIds() + ids).toList().takeLast(MAX_SEEN)
        LocalSettings.putString(SEEN_KEY, json.encodeToString(next))
    }

    fun record(peer: ApartmentChatPeer, messageId: String, body: String): ChatNotice {
        val notice = ChatNotice(
            id = messageId,
            title = peer.displayName.ifBlank { "New message" },
            body = body,
            timestamp = platformEpochMillis(),
            peerUserId = peer.userId,
            listingId = peer.listingId,
            listingTitle = peer.listingTitle,
            bookingId = peer.bookingId,
            displayName = peer.displayName,
            peerIsLandlord = peer.peerIsLandlord,
        )
        val next = listOf(notice) + _items.value.filterNot { it.id == notice.id }
        save(next.take(MAX_ITEMS))
        return notice
    }

    fun markRead(id: String) {
        save(_items.value.map { if (it.id == id) it.copy(read = true) else it })
    }

    fun markAllRead() {
        save(_items.value.map { it.copy(read = true) })
    }

    fun unreadCount(): Int = _items.value.count { !it.read }

    private fun seenIds(): Set<String> {
        val raw = LocalSettings.getString(SEEN_KEY) ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList()).toSet()
    }

    private fun load(): List<ChatNotice> {
        val raw = LocalSettings.getString(INBOX_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatNotice>>(raw) }.getOrDefault(emptyList())
    }

    private fun save(items: List<ChatNotice>) {
        _items.value = items
        LocalSettings.putString(INBOX_KEY, json.encodeToString(items))
    }
}
