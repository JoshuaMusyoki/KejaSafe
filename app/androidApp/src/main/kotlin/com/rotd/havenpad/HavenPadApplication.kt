package com.rotd.havenpad

import android.app.Application
import com.rotd.havenpad.data.DataConnectRuntime
import com.rotd.havenpad.di.initKoin
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.network.startConnectivityMonitor
import com.rotd.havenpad.storage.AndroidAppContext
import com.rotd.havenpad.storage.LocalSettings
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class HavenPadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalSettings.init(this)
        AndroidAppContext.init(this)
        ChatBackgroundSync.install(this)
        startConnectivityMonitor()
        DataConnectRuntime.cloudEnabled = false
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@HavenPadApplication)
        }
        SessionManager.dropSavedSession()
    }
}
