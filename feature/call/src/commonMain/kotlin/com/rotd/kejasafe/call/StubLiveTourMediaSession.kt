package com.rotd.kejasafe.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS placeholder until LiveKit Swift is linked in Xcode (pod `LiveKit`).
 * UI + watermark still work; media connect marks Connected for flow testing.
 */
class StubLiveTourMediaSession(
    private val platformLabel: String,
) : LiveTourMediaSession {
    private val _connectionState = MutableStateFlow(LiveTourConnectionState.Idle)
    override val connectionState: StateFlow<LiveTourConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(
        "$platformLabel: add LiveKit SDK for camera/mic"
    )
    override val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _hasRemoteVideo = MutableStateFlow(false)
    override val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private val _micEnabled = MutableStateFlow(true)
    override val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(true)
    override val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    override suspend fun connect(
        serverUrl: String,
        token: String,
        enableCamera: Boolean,
        enableMic: Boolean,
    ) {
        _micEnabled.value = enableMic
        _cameraEnabled.value = enableCamera
        _connectionState.value = LiveTourConnectionState.Connecting
        _statusMessage.value = "Token received — wire LiveKit on $platformLabel to publish media"
        _connectionState.value = LiveTourConnectionState.Connected
        _hasRemoteVideo.value = false
    }

    override fun setMicEnabled(enabled: Boolean) {
        _micEnabled.value = enabled
    }

    override fun setCameraEnabled(enabled: Boolean) {
        _cameraEnabled.value = enabled
    }

    override fun disconnect() {
        _connectionState.value = LiveTourConnectionState.Disconnected
        _hasRemoteVideo.value = false
    }
}

@Composable
fun rememberStubLiveTourMediaSession(platformLabel: String): LiveTourMediaSession {
    val session = remember(platformLabel) { StubLiveTourMediaSession(platformLabel) }
    DisposableEffect(session) {
        onDispose { session.disconnect() }
    }
    return session
}

@Composable
fun StubLiveTourVideoPane(
    showRemote: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (showRemote) "Remote video (LiveKit pending)" else "Local preview (LiveKit pending)",
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}
