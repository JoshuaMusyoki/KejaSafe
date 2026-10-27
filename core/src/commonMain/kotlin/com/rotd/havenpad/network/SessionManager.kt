package com.rotd.havenpad.network

import com.rotd.havenpad.User
import com.rotd.havenpad.storage.CacheKeys
import com.rotd.havenpad.storage.JsonCache
import com.rotd.havenpad.storage.LocalSettings
import com.rotd.havenpad.notify.onSessionCleared
import com.rotd.havenpad.notify.onSessionClearing
import com.rotd.havenpad.notify.onSessionSaved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Session for the current process only. [dropSavedSession] runs once at startup so a
 * killed app, a closed browser tab, or a reloaded page must sign in with a password again.
 * The token is still stored while this process is alive so API calls keep working.
 */
object SessionManager {
    private const val KEY_TOKEN = "session_token"
    private const val KEY_USER = "session_user_json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _token = MutableStateFlow<String?>(null)
    val token = _token.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    private var restored = false
    private var droppedPreviousRun = false

    /**
     * Forget the login left by the previous run. Safe to call from every entry point;
     * only the first call in this process clears anything.
     */
    fun dropSavedSession() {
        if (droppedPreviousRun) return
        droppedPreviousRun = true
        LocalSettings.putString(KEY_TOKEN, null)
        LocalSettings.putString(KEY_USER, null)
        _token.value = null
        _currentUser.value = null
        restored = true
    }

    /** Load token/user from disk. Safe to call more than once. */
    fun restore() {
        if (restored && _token.value != null) return
        restored = true
        val token = LocalSettings.getString(KEY_TOKEN)
        val userJson = LocalSettings.getString(KEY_USER)
        if (token.isNullOrBlank() || userJson.isNullOrBlank()) {
            _token.value = null
            _currentUser.value = null
            return
        }
        val user = runCatching { json.decodeFromString(User.serializer(), userJson) }.getOrNull()
        if (user == null) {
            clearSession()
            return
        }
        _token.value = token
        _currentUser.value = user
    }

    fun saveSession(token: String, user: User) {
        _token.value = token
        _currentUser.value = user
        LocalSettings.putString(KEY_TOKEN, token)
        LocalSettings.putString(KEY_USER, json.encodeToString(User.serializer(), user))
        onSessionSaved()
    }

    fun clearSession() {
        onSessionClearing()
        val userId = _currentUser.value?.id
        _token.value = null
        _currentUser.value = null
        LocalSettings.putString(KEY_TOKEN, null)
        LocalSettings.putString(KEY_USER, null)
        if (userId != null) {
            CacheKeys.userScoped(userId).forEach { JsonCache.remove(it) }
        }
        JsonCache.remove(CacheKeys.BROWSE_LISTINGS)
        onSessionCleared()
    }

    fun updateUser(user: User) {
        _currentUser.value = user
        LocalSettings.putString(KEY_USER, json.encodeToString(User.serializer(), user))
    }

    fun isLogged(): Boolean {
        restore()
        return _token.value != null && _currentUser.value != null
    }
}
