package com.rotd.kejasafe.domain.chat

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class User(
    val uid: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val profilePictureUrl: String? = null,
    val appPin: String? = null,
    val isActive: Boolean = false,
    val activeAppId: String? = null,
    val lastSeen: Long = 0,
    val points: Int = 0,
    val isAdmin: Boolean = false,
    /** Anonymous / guest session — no Chat or Community (Groups). */
    val isGuest: Boolean = false,
    /** What you do professionally (role, field, or organization). */
    val professionalBackground: String = "",
    /** Goals or what you aspire to. */
    val ambitions: String = "",
    /** Topics you care about — shown on Discover and profile. */
    val interests: List<String> = emptyList(),
) {
    /** Secondary line for Discover / people lists — never email or phone. */
    fun publicSubtitle(): String {
        professionalBackground.trim().takeIf { it.isNotEmpty() }?.let { return it }
        ambitions.trim().takeIf { it.isNotEmpty() }?.let { return it }
        if (interests.isNotEmpty()) {
            return interests.take(3).joinToString(" · ")
        }
        return "Learning together on Stawi"
    }

    fun hasAboutSection(): Boolean =
        professionalBackground.isNotBlank() ||
            ambitions.isNotBlank() ||
            interests.isNotEmpty()
}
