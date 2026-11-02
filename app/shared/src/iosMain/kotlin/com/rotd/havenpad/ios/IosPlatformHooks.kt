package com.rotd.havenpad.ios

import com.rotd.havenpad.call.IosRtcBridge
import platform.UIKit.UIView

/**
 * Entry points the Swift app sets once, before Compose appears.
 * Kotlin calls back into those handlers for Google sign-in and live-tour media.
 */
object IosPlatformHooks {
    fun onGoogleSignIn(handler: (String, IosGoogleRequest) -> Unit) {
        IosGoogleSignInBridge.install(handler)
    }

    fun onPrepareCall(handler: (Boolean, Boolean) -> Unit) {
        IosRtcBridge.onPrepare = handler
    }

    fun onCreateOffer(handler: () -> Unit) {
        IosRtcBridge.onCreateOffer = handler
    }

    fun onAcceptOffer(handler: (String, String) -> Unit) {
        IosRtcBridge.onAcceptOffer = handler
    }

    fun onAcceptAnswer(handler: (String, String) -> Unit) {
        IosRtcBridge.onAcceptAnswer = handler
    }

    fun onAddIce(handler: (String, String, Int) -> Unit) {
        IosRtcBridge.onAddIce = handler
    }

    fun onSetMic(handler: (Boolean) -> Unit) {
        IosRtcBridge.onSetMic = handler
    }

    fun onSetCamera(handler: (Boolean) -> Unit) {
        IosRtcBridge.onSetCamera = handler
    }

    fun onHangUp(handler: () -> Unit) {
        IosRtcBridge.onHangUp = handler
    }

    fun onAttachVideo(handler: (UIView, Boolean) -> Unit) {
        IosRtcBridge.onAttachVideo = handler
    }

    fun onDetachVideo(handler: (UIView, Boolean) -> Unit) {
        IosRtcBridge.onDetachVideo = handler
    }

    fun rtcReady() = IosRtcBridge.noteReady()

    fun rtcFailed(message: String) = IosRtcBridge.noteFailed(message)

    fun rtcOffer(type: String, sdp: String) = IosRtcBridge.noteOffer(type, sdp)

    fun rtcAnswer(type: String, sdp: String) = IosRtcBridge.noteAnswer(type, sdp)

    fun rtcIce(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        IosRtcBridge.noteIce(candidate, sdpMid, sdpMLineIndex)
    }

    fun rtcRemoteVideo(visible: Boolean) = IosRtcBridge.noteRemoteVideo(visible)
}

class IosGoogleRequest internal constructor(
    private val onToken: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var finished = false

    fun succeed(token: String) {
        if (finished) return
        finished = true
        onToken(token)
    }

    fun fail(message: String) {
        if (finished) return
        finished = true
        onError(message)
    }
}

internal object IosGoogleSignInBridge {
    private var start: ((String, IosGoogleRequest) -> Unit)? = null

    fun install(handler: (String, IosGoogleRequest) -> Unit) {
        start = handler
    }

    fun launch(clientId: String, onToken: (String) -> Unit, onError: (String) -> Unit) {
        val handler = start
        if (handler == null) {
            onError("Google sign-in is not linked in this iOS build. Open the Xcode workspace after pod install.")
            return
        }
        handler(clientId, IosGoogleRequest(onToken, onError))
    }
}
