package com.rotd.keja_safe.domain.util

/**
 * Prevents duplicate system-tray notifications when both FCM and the live
 * Firestore listener deliver the same chat message (common when the app is open).
 */
class MessageNotificationTracker {
    private val claimedIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * @return true if this message id has not been claimed yet (caller should show the tray).
     */
    fun tryClaim(messageId: String): Boolean {
        val id = messageId.trim()
        if (id.isEmpty()) return true
        val claimed = claimedIds.add(id)
        trimIfNeeded()
        return claimed
    }

    private fun trimIfNeeded() {
        if (claimedIds.size <= MAX_ENTRIES) return
        val overflow = claimedIds.size - KEEP_ENTRIES
        val iterator = claimedIds.iterator()
        var removed = 0
        while (iterator.hasNext() && removed < overflow) {
            iterator.next()
            iterator.remove()
            removed++
        }
    }

    companion object {
        private const val MAX_ENTRIES = 2_000
        private const val KEEP_ENTRIES = 1_000
    }
}
