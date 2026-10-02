package com.rotd.havenpad.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(code) => { eval(code); return null; }")
private external fun evalHaven(code: String): JsAny?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(code) => eval(code)")
private external fun evalHavenString(code: String): String

private class WasmRtcBridge : RtcJsBridge {
    override fun install() {
        evalHaven(HAVEN_RTC_INSTALL)
    }

    override fun prepare(video: Boolean, audio: Boolean) {
        evalHaven("globalThis.__haven.prepare(${video}, ${audio})")
    }

    override fun phase(): String = evalHavenString("globalThis.__haven ? String(globalThis.__haven.state.phase || '') : ''")

    override fun error(): String = evalHavenString("globalThis.__haven ? String(globalThis.__haven.state.error || '') : ''")

    override fun makeOffer() {
        evalHaven("globalThis.__haven.makeOffer()")
    }

    override fun takeOffer(json: String) {
        evalHaven("globalThis.__haven.takeOffer(${json.quote()})")
    }

    override fun takeAnswer(json: String) {
        evalHaven("globalThis.__haven.takeAnswer(${json.quote()})")
    }

    override fun addIce(json: String) {
        evalHaven("globalThis.__haven.addIce(${json.quote()})")
    }

    override fun drainIce(): String =
        evalHavenString("globalThis.__haven ? globalThis.__haven.drainIce() : '[]'")

    override fun peekOffer(): String =
        evalHavenString("globalThis.__haven ? String(globalThis.__haven.state.offer || '') : ''")

    override fun peekAnswer(): String =
        evalHavenString("globalThis.__haven ? String(globalThis.__haven.state.answer || '') : ''")

    override fun setMic(on: Boolean) {
        evalHaven("globalThis.__haven.setMic($on)")
    }

    override fun setCam(on: Boolean) {
        evalHaven("globalThis.__haven.setCam($on)")
    }

    override fun hangup() {
        evalHaven("if (globalThis.__haven) globalThis.__haven.hangup()")
    }

    override fun hasRemote(): Boolean =
        evalHavenString("globalThis.__haven && globalThis.__haven.state.remote ? '1' : '0'") == "1"
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
    val session = remember { WebLiveTourMediaSession(WasmRtcBridge()) }
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
