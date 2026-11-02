package com.rotd.havenpad.storage

import android.content.Context

/**
 * Application [Context] for Android-only services (Room).
 * Initialized from [com.rotd.havenpad.HavenPadApplication].
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
