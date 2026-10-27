package com.rotd.havenpad.call

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WebLiveTourMediaSession(
    bridge: RtcJsBridge,
) : LiveTourMediaSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = BrowserRtcEngine(bridge)
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
                    onRemoteVideo = { _hasRemoteVideo.value = it },
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
