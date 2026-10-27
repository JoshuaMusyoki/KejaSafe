package com.rotd.havenpad.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

private external fun eval(code: String): Any?

private class JsRtcBridge : RtcJsBridge {
    override fun install() {
        eval(HAVEN_RTC_INSTALL)
    }

    override fun prepare(video: Boolean, audio: Boolean) {
        eval("globalThis.__haven.prepare(${video}, ${audio})")
    }

    override fun phase(): String = jsState("phase")

    override fun error(): String = jsState("error")

    override fun makeOffer() {
        eval("globalThis.__haven.makeOffer()")
    }

    override fun takeOffer(json: String) {
        eval("globalThis.__haven.takeOffer(${json.quote()})")
    }

    override fun takeAnswer(json: String) {
        eval("globalThis.__haven.takeAnswer(${json.quote()})")
    }

    override fun addIce(json: String) {
        eval("globalThis.__haven.addIce(${json.quote()})")
    }

    override fun drainIce(): String = evalString("globalThis.__haven.drainIce()")

    override fun peekOffer(): String = jsState("offer")

    override fun peekAnswer(): String = jsState("answer")

    override fun setMic(on: Boolean) {
        eval("globalThis.__haven.setMic($on)")
    }

    override fun setCam(on: Boolean) {
        eval("globalThis.__haven.setCam($on)")
    }

    override fun hangup() {
        eval("if (globalThis.__haven) globalThis.__haven.hangup()")
    }

    override fun hasRemote(): Boolean = evalString("globalThis.__haven && globalThis.__haven.state.remote ? '1' : '0'") == "1"

    private fun jsState(field: String): String =
        evalString("globalThis.__haven ? String(globalThis.__haven.state.$field || '') : ''")

    private fun evalString(code: String): String =
        eval("(function(){ return $code; })()").toString().removeSurrounding("\"")
}

private fun String.quote(): String =
    buildString {
        append('"')
        for (ch in this@quote) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(ch)
            }
        }
        append('"')
    }

@Composable
actual fun rememberLiveTourMediaSession(): LiveTourMediaSession {
    val session = remember { WebLiveTourMediaSession(JsRtcBridge()) }
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
    StubLiveTourVideoPane(showRemote = showRemote, modifier = modifier)
}
