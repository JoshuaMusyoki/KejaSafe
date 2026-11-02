package com.rotd.havenpad.domain.storage

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rotd.havenpad.storage.AndroidAppContext

actual fun createSecureStorage(): SecureStorage = AndroidSecureStorage()

class AndroidSecureStorage : SecureStorage {
    private val prefs: SharedPreferences by lazy { createPrefs() }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
            apply()
        }
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun createPrefs(): SharedPreferences {
        val context = AndroidAppContext.require()
        return try {
            encryptedPrefs(context)
        } catch (_: Exception) {
            context.deleteSharedPreferences(FILE_NAME)
            encryptedPrefs(context)
        }
    }

    private fun encryptedPrefs(context: android.content.Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        private const val FILE_NAME = "havenpad_secure_prefs"
    }
}
