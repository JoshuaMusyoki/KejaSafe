package com.rotd.kejasafe.storage

import kotlinx.browser.localStorage

actual object LocalSettings {
    actual fun getString(key: String): String? = localStorage.getItem(key)

    actual fun putString(key: String, value: String?) {
        if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
    }
}
