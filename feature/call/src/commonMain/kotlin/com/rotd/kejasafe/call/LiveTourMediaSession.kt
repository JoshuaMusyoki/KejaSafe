package com.rotd.kejasafe.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

enum class LiveTourConnectionState {
    Idle,
    Connecting,
    Connected,
    Disconnected,
    Failed,
}

interface LiveTourMediaSession {
    val connectionState: StateFlow<LiveTourConnectionState>
    val statusMessage: StateFlow<String?>
    val hasRemoteVideo: StateFlow<Boolean>
    val micEnabled: StateFlow<Boolean>
    val cameraEnabled: StateFlow<Boolean>

    suspend fun connect(
        serverUrl: String,
        token: String,
        enableCamera: Boolean,
        enableMic: Boolean,
    )

    fun setMicEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun disconnect()
}

@Composable
expect fun rememberLiveTourMediaSession(): LiveTourMediaSession

@Composable
expect fun LiveTourVideoPane(
    session: LiveTourMediaSession,
    showRemote: Boolean,
    modifier: Modifier = Modifier,
)
