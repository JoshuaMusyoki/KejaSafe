package com.rotd.kejasafe.storage

import java.util.prefs.Preferences

actual object LocalSettings {
    private val prefs: Preferences = Preferences.userRoot().node("com.rotd.kejasafe")

    actual fun getString(key: String): String? =
        prefs.get(key, null)

    actual fun putString(key: String, value: String?) {
        if (value == null) prefs.remove(key) else prefs.put(key, value)
        prefs.flush()
    }
}
