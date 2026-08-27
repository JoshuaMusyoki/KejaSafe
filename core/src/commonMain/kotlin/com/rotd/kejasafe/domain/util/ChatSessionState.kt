package com.rotd.keja_safe.domain.util

/**
 * Process-wide chat UI state used to decide when tray notifications should be suppressed
 * (e.g. user is already inside the conversation) and which account is active.
 */
class ChatSessionState {
    @Volatile
    var activePeerId: String? = null
        private set

    @Volatile
    var isAppForeground: Boolean = false
        private set

    @Volatile
    var currentUserId: String? = null
        private set

    fun setActivePeer(peerId: String?) {
        activePeerId = peerId
    }

    fun setForeground(foreground: Boolean) {
        isAppForeground = foreground
    }

    fun setCurrentUser(userId: String?) {
        currentUserId = userId
    }

    /** Status-bar notification for this peer should be skipped while already viewing them. */
    fun shouldSuppressTrayForPeer(peerId: String): Boolean {
        if (peerId.isBlank()) return false
        return isAppForeground && activePeerId == peerId
    }

    /** True when push is intended for a different account than the one signed in. */
    fun isForDifferentAccount(targetUserId: String?): Boolean {
        val current = currentUserId ?: return false
        if (targetUserId.isNullOrBlank()) return false
        return targetUserId != current
    }
}
