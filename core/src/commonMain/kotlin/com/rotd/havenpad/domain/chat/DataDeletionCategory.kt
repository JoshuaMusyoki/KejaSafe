package com.rotd.havenpad.domain.chat

/**
 * Categories available for partial data deletion while keeping the account.
 * Used by Settings > Privacy & data.
 */
object DataDeletionCategory {
    const val PROFILE_PHOTO = "profile_photo"
    const val OPTIONAL_CONTACT = "optional_contact"
    const val CHAT_HISTORY = "chat_history"
    const val MEETINGS = "meetings"
    const val OTHER = "other"
}
