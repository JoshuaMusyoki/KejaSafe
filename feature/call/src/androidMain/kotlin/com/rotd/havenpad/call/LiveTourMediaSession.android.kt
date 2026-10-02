package com.rotd.havenpad.call

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidLiveTourMediaSession(
    private val appContext: Context,
) : LiveTourMediaSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var room: Room? = null
    private var eventsJob: Job? = null

    private val _connectionState = MutableStateFlow(LiveTourConnectionState.Idle)
    override val connectionState: StateFlow<LiveTourConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    override val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _hasRemoteVideo = MutableStateFlow(false)
    override val hasRemoteVideo: StateFlow<Boolean> = _hasRemoteVideo.asStateFlow()

    private val _micEnabled = MutableStateFlow(true)
    override val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(true)
    override val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    override suspend fun connect(
        serverUrl: String,
        token: String,
        enableCamera: Boolean,
        enableMic: Boolean,
    ) {
        disconnect()
        _connectionState.value = LiveTourConnectionState.Connecting
        _statusMessage.value = "Connecting…"
        _micEnabled.value = enableMic
        _cameraEnabled.value = enableCamera
        try {
            val created = LiveKit.create(appContext.applicationContext)
            room = created
            eventsJob = scope.launch {
                created.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            if (track is RemoteVideoTrack) {
                                _remoteVideoTrack.value = track
                                _hasRemoteVideo.value = true
                            }
                        }
                        is RoomEvent.TrackUnsubscribed -> {
                            if (event.track is RemoteVideoTrack) {
                                _remoteVideoTrack.value = null
                                _hasRemoteVideo.value = false
                            }
                        }
                        is RoomEvent.Disconnected -> {
                            _connectionState.value = LiveTourConnectionState.Disconnected
                            _statusMessage.value = "Disconnected"
                        }
                        is RoomEvent.FailedToConnect -> {
                            _connectionState.value = LiveTourConnectionState.Failed
                            _statusMessage.value = event.error.message ?: "Failed to connect"
                        }
                        else -> Unit
                    }
                }
            }
            created.connect(serverUrl, token)
            created.localParticipant.setMicrophoneEnabled(enableMic)
            created.localParticipant.setCameraEnabled(enableCamera)
            refreshLocalCamera()
            _connectionState.value = LiveTourConnectionState.Connected
            _statusMessage.value = "Connected — this tour is watermarked"
        } catch (t: Throwable) {
            _connectionState.value = LiveTourConnectionState.Failed
            _statusMessage.value = t.message ?: "Connection failed"
            disconnect()
        }
    }

    override fun setMicEnabled(enabled: Boolean) {
        _micEnabled.value = enabled
        scope.launch { room?.localParticipant?.setMicrophoneEnabled(enabled) }
    }

    override fun setCameraEnabled(enabled: Boolean) {
        _cameraEnabled.value = enabled
        scope.launch {
            room?.localParticipant?.setCameraEnabled(enabled)
            refreshLocalCamera()
        }
    }

    override fun disconnect() {
        eventsJob?.cancel()
        eventsJob = null
        runCatching { room?.disconnect() }
        runCatching { room?.release() }
        room = null
        _remoteVideoTrack.value = null
        _localVideoTrack.value = null
        _hasRemoteVideo.value = false
        if (_connectionState.value == LiveTourConnectionState.Connected ||
            _connectionState.value == LiveTourConnectionState.Connecting
        ) {
            _connectionState.value = LiveTourConnectionState.Disconnected
        }
    }

    fun bindRenderer(renderer: TextureViewRenderer) {
        room?.initVideoRenderer(renderer)
    }

    private fun refreshLocalCamera() {
        val localCam = room?.localParticipant
            ?.getTrackPublication(Track.Source.CAMERA)
            ?.track as? VideoTrack
        _localVideoTrack.value = localCam
    }
}

@Composable
actual fun rememberLiveTourMediaSession(): LiveTourMediaSession {
    val context = LocalContext.current.applicationContext
    val session = remember(context) { AndroidLiveTourMediaSession(context) }
    DisposableEffect(session) {
        onDispose { session.disconnect() }
    }
    return session
}

@Composable
actual fun LiveTourVideoPane(
    session: LiveTourMediaSession,
    showRemote: Boolean,
    modifier: Modifier,
) {
    val androidSession = session as? AndroidLiveTourMediaSession
    if (androidSession == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = if (showRemote) "Waiting for landlord camera…" else "Camera off",
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        return
    }
    val track by (if (showRemote) androidSession.remoteVideoTrack else androidSession.localVideoTrack)
        .collectAsState()

    if (track == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = if (showRemote) "Waiting for landlord camera…" else "Camera off",
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        return
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            TextureViewRenderer(ctx).also { renderer ->
                androidSession.bindRenderer(renderer)
                track?.addRenderer(renderer)
            }
        },
        update = { renderer ->
            androidSession.bindRenderer(renderer)
            track?.addRenderer(renderer)
        }
    )
}
