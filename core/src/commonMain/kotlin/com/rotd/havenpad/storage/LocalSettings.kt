package com.rotd.havenpad.storage

/**
 * Tiny multiplatform key-value prefs for session / theme survival across process recreation
 * (e.g. system dark-mode changes).
 */
expect object LocalSettings {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
}
