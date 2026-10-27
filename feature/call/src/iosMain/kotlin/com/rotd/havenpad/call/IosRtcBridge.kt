package com.rotd.havenpad.call

import com.rotd.havenpad.RtcIceCandidate
import com.rotd.havenpad.RtcSessionDescription
import kotlin.concurrent.Volatile
import platform.Foundation.NSLock
import platform.UIKit.UIView

/**
 * Swift (LiveKitWebRTC) pushes call state here. Kotlin drives the same
 * offer/answer loop Android and the website already use.
 */
object IosRtcBridge {
    var onPrepare: ((Boolean, Boolean) -> Unit)? = null
    var onCreateOffer: (() -> Unit)? = null
    var onAcceptOffer: ((String, String) -> Unit)? = null
    var onAcceptAnswer: ((String, String) -> Unit)? = null
    var onAddIce: ((String, String, Int) -> Unit)? = null
    var onSetMic: ((Boolean) -> Unit)? = null
    var onSetCamera: ((Boolean) -> Unit)? = null
    var onHangUp: (() -> Unit)? = null
    var onAttachVideo: ((UIView, Boolean) -> Unit)? = null
    var onDetachVideo: ((UIView, Boolean) -> Unit)? = null

    @Volatile
    var phase: String = "idle"
        private set

    @Volatile
    var error: String = ""
        private set

    @Volatile
    var offer: RtcSessionDescription? = null
        private set

    @Volatile
    var answer: RtcSessionDescription? = null
        private set

    @Volatile
    var remoteVideo: Boolean = false
        private set

    private val iceLock = NSLock()
    private val localIce = mutableListOf<RtcIceCandidate>()

    fun prepare(camera: Boolean, mic: Boolean) {
        reset()
        phase = "preparing"
        val start = onPrepare
        if (start == null) {
            noteFailed("Live tour media is not linked. Open the Xcode workspace after pod install.")
        } else {
            start(camera, mic)
        }
    }

    fun createOffer() {
        offer = null
        onCreateOffer?.invoke()
    }

    fun acceptOffer(type: String, sdp: String) {
        answer = null
        onAcceptOffer?.invoke(type, sdp)
    }

    fun acceptAnswer(type: String, sdp: String) {
        onAcceptAnswer?.invoke(type, sdp)
    }

    fun addIce(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        onAddIce?.invoke(candidate, sdpMid, sdpMLineIndex)
    }

    fun setMic(enabled: Boolean) {
        onSetMic?.invoke(enabled)
    }

    fun setCamera(enabled: Boolean) {
        onSetCamera?.invoke(enabled)
    }

    fun hangUp() {
        onHangUp?.invoke()
        reset()
    }

    fun attach(view: UIView, remote: Boolean) {
        onAttachVideo?.invoke(view, remote)
    }

    fun detach(view: UIView, remote: Boolean) {
        onDetachVideo?.invoke(view, remote)
    }

    fun noteReady() {
        if (phase != "error") phase = "ready"
    }

    fun noteFailed(message: String) {
        error = message.ifBlank { "Call failed" }
        phase = "error"
    }

    fun noteOffer(type: String, sdp: String) {
        if (sdp.isBlank()) return
        offer = RtcSessionDescription(type.ifBlank { "offer" }, sdp)
    }

    fun noteAnswer(type: String, sdp: String) {
        if (sdp.isBlank()) return
        answer = RtcSessionDescription(type.ifBlank { "answer" }, sdp)
    }

    fun noteIce(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        if (candidate.isBlank()) return
        iceLock.lock()
        localIce.add(
            RtcIceCandidate(
                candidate = candidate,
                sdpMid = sdpMid.ifBlank { null },
                sdpMLineIndex = sdpMLineIndex,
            )
        )
        iceLock.unlock()
    }

    fun drainIce(): List<RtcIceCandidate> {
        iceLock.lock()
        if (localIce.isEmpty()) {
            iceLock.unlock()
            return emptyList()
        }
        val copy = localIce.toList()
        localIce.clear()
        iceLock.unlock()
        return copy
    }

    fun noteRemoteVideo(visible: Boolean) {
        remoteVideo = visible
    }

    private fun reset() {
        phase = "idle"
        error = ""
        offer = null
        answer = null
        remoteVideo = false
        iceLock.lock()
        localIce.clear()
        iceLock.unlock()
    }
}
