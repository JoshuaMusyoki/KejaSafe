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
import com.rotd.havenpad.RtcIceCandidate
import com.rotd.havenpad.RtcSessionDescription
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.JavaI420Buffer
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

class AndroidLiveTourMediaSession(
    private val appContext: Context,
) : LiveTourMediaSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private val engine = AndroidRtcEngine(appContext)

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

    val eglBase: EglBase get() = engine.eglBase
    val remoteVideoTrack: StateFlow<VideoTrack?> = engine.remoteVideo
    val localVideoTrack: StateFlow<VideoTrack?> = engine.localVideo

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
                    onRemoteVideo = { visible ->
                        _hasRemoteVideo.value = visible || engine.remoteVideo.value != null
                    },
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

private class AndroidRtcEngine(
    private val context: Context,
) : RtcMediaEngine {
    val eglBase: EglBase = EglBase.create()
    val remoteVideo = MutableStateFlow<VideoTrack?>(null)
    val localVideo = MutableStateFlow<VideoTrack?>(null)

    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var capturer: VideoCapturer? = null
    private var captureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: org.webrtc.AudioTrack? = null
    private val localIce = Collections.synchronizedList(mutableListOf<RtcIceCandidate>())
    private var cameraWanted = true
    @Volatile private var sendingBlack = false
    private var blackPump: ScheduledFuture<*>? = null
    private var blackScheduler = newBlackScheduler()

    override suspend fun prepare(enableCamera: Boolean, enableMic: Boolean) {
        cameraWanted = enableCamera
        ensureFactory()
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        val created = factory?.createPeerConnection(config, observer)
            ?: error("Could not start WebRTC")
        peer = created
        val audioSource = factory?.createAudioSource(MediaConstraints())
        localAudioTrack = factory?.createAudioTrack("haven-audio", audioSource)?.also { track ->
            track.setEnabled(enableMic)
            created.addTrack(track, listOf("haven"))
        }
        if (enableCamera) startCamera(created)
    }

    override suspend fun createOffer(): RtcSessionDescription {
        val connection = peer ?: error("Call is not ready")
        val offer = connection.awaitOffer(receiveVideo = cameraWanted)
        connection.awaitSetLocal(offer)
        return RtcSessionDescription(offer.type.canonicalForm(), offer.description)
    }

    override suspend fun acceptRemoteOffer(offer: RtcSessionDescription): RtcSessionDescription {
        val connection = peer ?: error("Call is not ready")
        connection.awaitSetRemote(SessionDescription(SessionDescription.Type.OFFER, offer.sdp))
        val answer = connection.awaitAnswer(receiveVideo = cameraWanted)
        connection.awaitSetLocal(answer)
        return RtcSessionDescription(answer.type.canonicalForm(), answer.description)
    }

    override suspend fun acceptRemoteAnswer(answer: RtcSessionDescription) {
        val connection = peer ?: error("Call is not ready")
        connection.awaitSetRemote(SessionDescription(SessionDescription.Type.ANSWER, answer.sdp))
    }

    override suspend fun addIceCandidate(candidate: RtcIceCandidate) {
        peer?.addIceCandidate(
            IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex ?: 0, candidate.candidate)
        )
    }

    override fun drainLocalIce(): List<RtcIceCandidate> {
        synchronized(localIce) {
            if (localIce.isEmpty()) return emptyList()
            val copy = localIce.toList()
            localIce.clear()
            return copy
        }
    }

    override fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    override fun setCameraEnabled(enabled: Boolean) {
        // Keep the track live. Disabling it, or only stopping the camera, leaves the
        // last picture on this phone and on the other person's large view.
        localVideoTrack?.setEnabled(true)
        if (enabled) {
            stopBlackFrames()
            runCatching { capturer?.startCapture(1280, 720, 30) }
        } else {
            runCatching { capturer?.stopCapture() }
            startBlackFrames()
        }
    }

    override fun hasRemoteVideo(): Boolean = remoteVideo.value != null

    override fun close() {
        stopBlackFrames()
        blackScheduler.shutdownNow()
        runCatching { capturer?.stopCapture() }
        runCatching { capturer?.dispose() }
        capturer = null
        runCatching { captureHelper?.dispose() }
        captureHelper = null
        localVideoTrack = null
        localAudioTrack = null
        videoSource = null
        localVideo.value = null
        remoteVideo.value = null
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        peer = null
        synchronized(localIce) { localIce.clear() }
    }

    private fun startCamera(connection: PeerConnection) {
        val enumerator = Camera2Enumerator(context)
        val name = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: return
        val camera = enumerator.createCapturer(name, null) ?: return
        val helper = SurfaceTextureHelper.create("HavenCapture", eglBase.eglBaseContext)
        val source = factory?.createVideoSource(camera.isScreencast) ?: return
        videoSource = source
        camera.initialize(helper, context, source.capturerObserver)
        camera.startCapture(1280, 720, 30)
        val track = factory?.createVideoTrack("haven-video", source) ?: return
        track.setEnabled(true)
        connection.addTrack(track, listOf("haven"))
        capturer = camera
        captureHelper = helper
        localVideoTrack = track
        localVideo.value = track
    }

    private fun startBlackFrames() {
        val source = videoSource ?: return
        sendingBlack = true
        if (blackPump != null) return
        if (blackScheduler.isShutdown) blackScheduler = newBlackScheduler()
        blackPump = runCatching {
            blackScheduler.scheduleAtFixedRate({
                if (!sendingBlack) return@scheduleAtFixedRate
                val frame = blackFrame()
                runCatching {
                    source.capturerObserver.onCapturerStarted(true)
                    source.capturerObserver.onFrameCaptured(frame)
                }
                frame.release()
            }, 0, 200, TimeUnit.MILLISECONDS)
        }.getOrNull()
    }

    private fun newBlackScheduler(): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "HavenBlackFrame").apply { isDaemon = true }
        }

    private fun stopBlackFrames() {
        sendingBlack = false
        blackPump?.cancel(false)
        blackPump = null
    }

    private fun blackFrame(): VideoFrame {
        val width = 160
        val height = 120
        val buffer = JavaI420Buffer.allocate(width, height)
        fillPlane(buffer.dataY, 0)
        fillPlane(buffer.dataU, 128)
        fillPlane(buffer.dataV, 128)
        return VideoFrame(buffer, 0, System.nanoTime())
    }

    private fun fillPlane(plane: ByteBuffer, value: Int) {
        val byte = value.toByte()
        for (index in 0 until plane.capacity()) plane.put(index, byte)
        plane.rewind()
    }

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate ?: return
            localIce.add(
                RtcIceCandidate(
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                )
            )
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
        override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
        override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
        override fun onRenegotiationNeeded() = Unit
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) {
            val track = receiver?.track()
            if (track is VideoTrack) remoteVideo.value = track
        }
        override fun onTrack(transceiver: RtpTransceiver?) {
            val track = transceiver?.receiver?.track()
            if (track is VideoTrack) remoteVideo.value = track
        }
    }

    private fun ensureFactory() {
        if (factory != null) return
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
        val audio = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audio)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }
}

private suspend fun PeerConnection.awaitOffer(receiveVideo: Boolean): SessionDescription =
    awaitCreate { observer, constraints -> createOffer(observer, constraints) }

private suspend fun PeerConnection.awaitAnswer(receiveVideo: Boolean): SessionDescription =
    awaitCreate { observer, constraints -> createAnswer(observer, constraints) }

private suspend fun PeerConnection.awaitCreate(
    create: PeerConnection.(SdpObserver, MediaConstraints) -> Unit,
): SessionDescription = suspendCancellableCoroutine { cont ->
    val constraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
    }
    create(object : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription?) {
            if (description != null && cont.isActive) cont.resume(description)
        }
        override fun onCreateFailure(error: String?) {
            if (cont.isActive) cont.resumeWithException(IllegalStateException(error ?: "Could not create call description"))
        }
        override fun onSetSuccess() = Unit
        override fun onSetFailure(error: String?) = Unit
    }, constraints)
}

private suspend fun PeerConnection.awaitSetLocal(description: SessionDescription) {
    awaitSet { setLocalDescription(it, description) }
}

private suspend fun PeerConnection.awaitSetRemote(description: SessionDescription) {
    awaitSet { setRemoteDescription(it, description) }
}

private suspend fun PeerConnection.awaitSet(
    set: PeerConnection.(SdpObserver) -> Unit,
) {
    suspendCancellableCoroutine { cont ->
        set(object : SdpObserver {
            override fun onSetSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onSetFailure(error: String?) {
                if (cont.isActive) cont.resumeWithException(IllegalStateException(error ?: "Could not apply call description"))
            }
            override fun onCreateSuccess(description: SessionDescription?) = Unit
            override fun onCreateFailure(error: String?) = Unit
        })
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
    val track by (if (showRemote) {
        androidSession?.remoteVideoTrack
    } else {
        androidSession?.localVideoTrack
    } ?: MutableStateFlow(null)).collectAsState()
    val cameraOn by (androidSession?.cameraEnabled ?: MutableStateFlow(true)).collectAsState()

    if (!showRemote && androidSession != null && !cameraOn) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(text = "Camera off", color = Color.White.copy(alpha = 0.8f))
        }
        return
    }

    if (androidSession == null || track == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = if (showRemote) "Waiting for the other camera…" else "Camera off",
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        return
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(androidSession.eglBase.eglBaseContext, null)
                setMirror(!showRemote)
                setEnableHardwareScaler(true)
                track?.addSink(this)
                tag = track
            }
        },
        update = { renderer ->
            val previous = renderer.tag as? VideoTrack
            val next = track
            if (previous !== next) {
                previous?.removeSink(renderer)
                next?.addSink(renderer)
                renderer.tag = next
            }
        },
        onRelease = { renderer ->
            (renderer.tag as? VideoTrack)?.removeSink(renderer)
            renderer.release()
        }
    )
}
