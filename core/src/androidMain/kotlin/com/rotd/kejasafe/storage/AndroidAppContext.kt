package com.rotd.kejasafe.storage

import android.content.Context

/**
 * Application [Context] for Android-only services (Room).
 * Initialized from [com.rotd.kejasafe.KejaSafeApplication].
 */
object AndroidAppContext {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun require(): Context =
        appContext ?: error("AndroidAppContext.init() was not called")
}
