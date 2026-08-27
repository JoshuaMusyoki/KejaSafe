package com.rotd.kejasafe.storage

import platform.Foundation.NSUserDefaults

actual object LocalSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? =
        defaults.stringForKey(key)

    actual fun putString(key: String, value: String?) {
        if (value == null) {
            defaults.removeObjectForKey(key)
        } else {
            defaults.setObject(value, forKey = key)
        }
        defaults.synchronize()
    }
}
