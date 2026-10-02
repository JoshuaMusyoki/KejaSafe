package com.rotd.havenpad

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rotd.havenpad.call.HouseTourRingScreen
import com.rotd.havenpad.navigation.TourLaunchRequest
import com.rotd.havenpad.network.CallLogBus
import com.rotd.havenpad.network.IncomingCallCenter
import com.rotd.havenpad.network.LiveTourClient
import com.rotd.havenpad.notify.startHouseTourRingSound
import com.rotd.havenpad.notify.stopHouseTourRingSound
import com.rotd.havenpad.theme.HavenPadTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Shown over the lock screen when a house tour rings and the app is in the background. */
class IncomingTourActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        val sessionId = intent.getStringExtra(ChatNotifyContract.EXTRA_SESSION).orEmpty()
        val caller = intent.getStringExtra(ChatNotifyContract.EXTRA_NAME).orEmpty()
        val listing = intent.getStringExtra(ChatNotifyContract.EXTRA_LISTING_TITLE).orEmpty()
        startHouseTourRingSound()
        setContent {
            HavenPadTheme(darkTheme = true) {
                HouseTourRingScreen(
                    callerName = caller.ifBlank { "House tour" },
                    listingTitle = listing,
                    onAnswer = {
                        finishRing(sessionId)
                        IncomingCallCenter.suppress(sessionId)
                        TourLaunchRequest.request(sessionId)
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra(ChatNotifyContract.EXTRA_SESSION, sessionId)
                            }
                        )
                        finish()
                    },
                    onDecline = {
                        finishRing(sessionId)
                        IncomingCallCenter.clear()
                        if (sessionId.isNotBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                LiveTourClient.endTour(sessionId)
                                CallLogBus.ended()
                            }
                        }
                        finish()
                    },
                )
            }
        }
    }

    private fun finishRing(sessionId: String) {
        stopHouseTourRingSound()
        if (sessionId.isBlank()) return
        getSystemService(NotificationManager::class.java)?.cancel(sessionId.hashCode())
    }

    override fun onDestroy() {
        stopHouseTourRingSound()
        super.onDestroy()
    }
}
