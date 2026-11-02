package com.rotd.havenpad.call

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
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.UIKit.UIColor
import platform.UIKit.UIView

private class IosLiveTourMediaSession : LiveTourMediaSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val engine = IosRtcEngine()
    private var job: Job? = null

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

    override suspend fun connect(
        sessionId: String,
        asCaller: Boolean,
        enableCamera: Boolean,
        enableMic: Boolean,
    ) {
        disconnect()
        _micEnabled.value = enableMic
        _cameraEnabled.value = enableCamera
        job = scope.launch {
            try {
                runWebRtcSignaling(
                    sessionId = sessionId,
                    asCaller = asCaller,
                    enableCamera = enableCamera,
                    enableMic = enableMic,
                    engine = engine,
                    onState = { state, message ->
                        _connectionState.value = state
                        _statusMessage.value = message
                    },
                    onRemoteVideo = { _hasRemoteVideo.value = it || IosRtcBridge.remoteVideo },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _connectionState.value = LiveTourConnectionState.Failed
                _statusMessage.value = error.message ?: "Call failed"
            }
        }
    }

    override fun setMicEnabled(enabled: Boolean) {
        _micEnabled.value = enabled
        engine.setMicEnabled(enabled)
    }

    override fun setCameraEnabled(enabled: Boolean) {
        _cameraEnabled.value = enabled
        engine.setCameraEnabled(enabled)
    }

    override fun disconnect() {
        job?.cancel()
        job = null
        engine.close()
        _hasRemoteVideo.value = false
        if (_connectionState.value == LiveTourConnectionState.Connected ||
            _connectionState.value == LiveTourConnectionState.Connecting
        ) {
            _connectionState.value = LiveTourConnectionState.Disconnected
            _statusMessage.value = "Disconnected"
        }
    }
}

private class IosRtcEngine : RtcMediaEngine {
    override suspend fun prepare(enableCamera: Boolean, enableMic: Boolean) {
        IosRtcBridge.prepare(enableCamera, enableMic)
        waitUntil { it == "ready" }
    }

    override suspend fun createOffer(): RtcSessionDescription {
        IosRtcBridge.createOffer()
        return waitOffer()
    }

    override suspend fun acceptRemoteOffer(offer: RtcSessionDescription): RtcSessionDescription {
        IosRtcBridge.acceptOffer(offer.type, offer.sdp)
        return waitAnswer()
    }

    override suspend fun acceptRemoteAnswer(answer: RtcSessionDescription) {
        IosRtcBridge.acceptAnswer(answer.type, answer.sdp)
        delay(300)
    }

    override suspend fun addIceCandidate(candidate: RtcIceCandidate) {
        IosRtcBridge.addIce(
            candidate.candidate,
            candidate.sdpMid.orEmpty(),
            candidate.sdpMLineIndex ?: 0,
        )
    }

    override fun drainLocalIce(): List<RtcIceCandidate> = IosRtcBridge.drainIce()

    override fun setMicEnabled(enabled: Boolean) = IosRtcBridge.setMic(enabled)

    override fun setCameraEnabled(enabled: Boolean) = IosRtcBridge.setCamera(enabled)

    override fun hasRemoteVideo(): Boolean = IosRtcBridge.remoteVideo

    override fun close() = IosRtcBridge.hangUp()

    private suspend fun waitUntil(ready: (String) -> Boolean) {
        var waited = 0
        while (waited < 20_000) {
            val phase = IosRtcBridge.phase
            if (phase == "error") {
                throw IllegalStateException(IosRtcBridge.error.ifBlank { "Could not start the call" })
            }
            if (ready(phase)) return
            delay(200)
            waited += 200
        }
        throw IllegalStateException(IosRtcBridge.error.ifBlank { "The call took too long to start" })
    }

    private suspend fun waitOffer(): RtcSessionDescription {
        var waited = 0
        while (waited < 20_000) {
            if (IosRtcBridge.phase == "error") {
                throw IllegalStateException(IosRtcBridge.error.ifBlank { "Call setup failed" })
            }
            IosRtcBridge.offer?.let { return it }
            delay(200)
            waited += 200
        }
        throw IllegalStateException("The call did not produce an offer")
    }

    private suspend fun waitAnswer(): RtcSessionDescription {
        var waited = 0
        while (waited < 20_000) {
            if (IosRtcBridge.phase == "error") {
                throw IllegalStateException(IosRtcBridge.error.ifBlank { "Call setup failed" })
            }
            IosRtcBridge.answer?.let { return it }
            delay(200)
            waited += 200
        }
        throw IllegalStateException("The other person did not finish connecting")
    }
}

@Composable
actual fun rememberLiveTourMediaSession(): LiveTourMediaSession {
    val session = remember { IosLiveTourMediaSession() }
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
    val cameraOn by session.cameraEnabled.collectAsState()
    val remoteOn by session.hasRemoteVideo.collectAsState()
    if (!showRemote && !cameraOn) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Camera off", color = Color.White.copy(alpha = 0.8f))
        }
        return
    }
    Box(modifier = modifier) {
        UIKitView(
            factory = {
                val view = UIView()
                view.backgroundColor = UIColor.blackColor
                IosRtcBridge.attach(view, remote = showRemote)
                view
            },
            modifier = Modifier.fillMaxSize(),
            update = { view -> IosRtcBridge.attach(view, remote = showRemote) },
            onRelease = { view -> IosRtcBridge.detach(view, remote = showRemote) },
            interactive = false,
        )
        if (showRemote && !remoteOn) {
            Text(
                text = "Waiting for the other camera…",
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
