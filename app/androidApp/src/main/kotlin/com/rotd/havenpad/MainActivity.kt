package com.rotd.havenpad

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.storage.AndroidActivityHolder

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openChatFromNotification(intent)
        openTourFromNotification(intent)
        if (SessionManager.isLogged()) {
            ChatBackgroundSync.startAlerts(this)
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openChatFromNotification(intent)
        openTourFromNotification(intent)
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityHolder.current = this
        if (SessionManager.isLogged()) {
            ChatBackgroundSync.startAlerts(this)
        }
    }

    override fun onPause() {
        if (SessionManager.isLogged()) {
            ChatBackgroundSync.enqueueSoon(this)
        }
        if (AndroidActivityHolder.current === this) {
            AndroidActivityHolder.current = null
        }
        super.onPause()
    }
}
