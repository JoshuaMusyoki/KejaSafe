package com.rotd.kejasafe.storage

private val memory = mutableMapOf<String, String>()

actual object LocalSettings {
    actual fun getString(key: String): String? = memory[key]

    actual fun putString(key: String, value: String?) {
        if (value == null) memory.remove(key) else memory[key] = value
    }
}
