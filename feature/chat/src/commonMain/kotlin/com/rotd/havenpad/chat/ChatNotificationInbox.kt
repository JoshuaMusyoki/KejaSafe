package com.rotd.havenpad.chat

import com.rotd.havenpad.data.platformEpochMillis
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.network.SessionManager
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
    /** Account that received this notice. Blank rows are from before accounts were separated. */
    val ownerUserId: String = "",
)

fun ChatNotice.toPeer(): ApartmentChatPeer = ApartmentChatPeer(
    userId = peerUserId,
    displayName = displayName.ifBlank { title },
    listingId = listingId,
    listingTitle = listingTitle,
    bookingId = bookingId,
    peerIsLandlord = peerIsLandlord,
)

object ChatNotificationInbox {
    private const val INBOX_KEY = "chat_notification_inbox"
    private const val SEEN_KEY = "chat_notification_seen"
    private const val PRIMED_KEY = "chat_notifications_primed"
    private const val MAX_ITEMS = 80
    private const val MAX_SEEN = 400

    private val json = Json { ignoreUnknownKeys = true }
    private var accountId: String = ""
    private var allItems: List<ChatNotice> = load()
    private val _items = MutableStateFlow(noticesForAccount(allItems, accountId))
    val items = _items.asStateFlow()

    /** Show only the notices stored for this signed-in account. */
    fun showAccount(userId: String?) {
        accountId = userId.orEmpty()
        publish()
    }

    fun isPrimed(): Boolean {
        val key = primedKey() ?: return false
        return LocalSettings.getString(key) == "1"
    }

    fun markPrimed() {
        val key = primedKey() ?: return
        LocalSettings.putString(key, "1")
    }

    fun hasSeen(id: String): Boolean = seenIds().contains(id)

    fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val key = seenKey() ?: return
        val next = (seenIds() + ids).toList().takeLast(MAX_SEEN)
        LocalSettings.putString(key, json.encodeToString(next))
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
            ownerUserId = currentAccountId(),
        )
        remember(notice)
        return notice
    }

    fun recordNotice(
        id: String,
        title: String,
        body: String,
        peerUserId: String = "",
        listingId: String = "",
        listingTitle: String = "",
        bookingId: String = "",
        displayName: String = "HavenPad Billing",
        peerIsLandlord: Boolean = false
    ): ChatNotice {
        val notice = ChatNotice(
            id = id,
            title = title,
            body = body,
            timestamp = platformEpochMillis(),
            peerUserId = peerUserId,
            listingId = listingId,
            listingTitle = listingTitle,
            bookingId = bookingId,
            displayName = displayName,
            peerIsLandlord = peerIsLandlord,
            ownerUserId = currentAccountId(),
        )
        remember(notice)
        return notice
    }

    fun markRead(id: String) {
        val me = currentAccountId()
        saveAll(allItems.map { notice ->
            if (notice.id == id && notice.ownerUserId == me) notice.copy(read = true) else notice
        })
    }

    fun markAllRead() {
        val me = currentAccountId()
        saveAll(allItems.map { notice ->
            if (notice.ownerUserId == me) notice.copy(read = true) else notice
        })
    }

    fun unreadCount(): Int = _items.value.count { !it.read }

    private fun currentAccountId(): String {
        if (accountId.isNotBlank()) return accountId
        return SessionManager.currentUser.value?.id.orEmpty().also { accountId = it }
    }

    private fun seenKey(): String? {
        val id = currentAccountId()
        return if (id.isBlank()) null else "${SEEN_KEY}_$id"
    }

    private fun primedKey(): String? {
        val id = currentAccountId()
        return if (id.isBlank()) null else "${PRIMED_KEY}_$id"
    }

    private fun seenIds(): Set<String> {
        val key = seenKey() ?: return emptySet()
        val raw = LocalSettings.getString(key) ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList()).toSet()
    }

    private fun remember(notice: ChatNotice) {
        if (notice.ownerUserId.isBlank()) return
        val next = listOf(notice) + allItems.filterNot {
            it.id == notice.id && it.ownerUserId == notice.ownerUserId
        }
        saveAll(next)
    }

    private fun load(): List<ChatNotice> {
        val raw = LocalSettings.getString(INBOX_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatNotice>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveAll(items: List<ChatNotice>) {
        val capped = items
            .groupBy { it.ownerUserId }
            .flatMap { (_, rows) -> rows.take(MAX_ITEMS) }
        allItems = capped
        publish()
        LocalSettings.putString(INBOX_KEY, json.encodeToString(capped))
    }

    private fun publish() {
        _items.value = noticesForAccount(allItems, accountId)
    }
}

internal fun noticesForAccount(notices: List<ChatNotice>, accountId: String): List<ChatNotice> =
    if (accountId.isBlank()) emptyList() else notices.filter { it.ownerUserId == accountId }
