package com.rotd.kejasafe

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.rotd.kejasafe.data.DataConnectRuntime
import com.rotd.kejasafe.di.initKoin
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.network.startConnectivityMonitor
import com.rotd.kejasafe.storage.AndroidAppContext
import com.rotd.kejasafe.storage.LocalSettings
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class KejaSafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalSettings.init(this)
        AndroidAppContext.init(this)
        startConnectivityMonitor()
        // Require login after process death (user swipes app away / force-stop).
        // In-memory session still survives Activity recreates (e.g. theme) in the same process.
        SessionManager.clearSession()
        runCatching { FirebaseAuth.getInstance().signOut() }
        // Cloud SQL / Firebase Data Connect is paused — profiles use local cache;
        // listings/rent use :server H2 via Ktor. Auth uses KtorBridge (HS256 JWTs).
        DataConnectRuntime.cloudEnabled = false
        // When Cloud SQL is provisioned again:
        //   1. firebase deploy --only dataconnect  (from repo root)
        //   2. Switch createUserProfileDataSource() to SqlConnectUserProfileDataSource
        //   3. Switch createAuthRepository() back to AuthRepositoryImpl (Firebase)
        //   4. DataConnectRuntime.cloudEnabled = true
        // DataConnectRuntime.useEmulator = true  // only with local emulator
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@KejaSafeApplication)
        }
    }
}
