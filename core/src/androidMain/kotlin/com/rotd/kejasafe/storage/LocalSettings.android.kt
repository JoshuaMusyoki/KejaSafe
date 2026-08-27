package com.rotd.kejasafe.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("StaticFieldLeak")
actual object LocalSettings {
    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("kejasafe_prefs", Context.MODE_PRIVATE)
        }
    }

    actual fun getString(key: String): String? =
        prefs?.getString(key, null)

    actual fun putString(key: String, value: String?) {
        val p = prefs ?: return
        p.edit().apply {
            if (value == null) remove(key) else putString(key, value)
            apply()
        }
    }
}
