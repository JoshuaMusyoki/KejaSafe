package com.rotd.havenpad.domain.storage

/**
 * Small encrypted key-value store for secrets (passwords / tokens).
 * Android: EncryptedSharedPreferences. iOS: Keychain.
 */
interface SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun clear()
}

expect fun createSecureStorage(): SecureStorage

class MemorySecureStorage : SecureStorage {
    private val data = mutableMapOf<String, String>()

    override fun getString(key: String): String? = data[key]

    override fun putString(key: String, value: String?) {
        if (value == null) data.remove(key) else data[key] = value
    }

    override fun clear() {
        data.clear()
    }
}
