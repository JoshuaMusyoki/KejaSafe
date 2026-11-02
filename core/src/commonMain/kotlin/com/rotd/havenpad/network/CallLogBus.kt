package com.rotd.havenpad.network

import com.rotd.havenpad.CallLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tells the chat page to show Calls after someone hangs up. */
object CallLogBus {
    private val _tick = MutableStateFlow(0)
    val tick = _tick.asStateFlow()
    var focusCalls: Boolean = false
        private set

    fun ended() {
        focusCalls = true
        _tick.value = _tick.value + 1
    }

    fun consumeFocus(): Boolean {
        val focus = focusCalls
        focusCalls = false
        return focus
    }
}

/** Ringing call the signed-in user has not answered yet. */
object IncomingCallCenter {
    private val _ringing = MutableStateFlow<CallLogEntry?>(null)
    val ringing = _ringing.asStateFlow()
    var suppressedSessionId: String? = null
        private set

    fun show(entry: CallLogEntry) {
        if (entry.sessionId == suppressedSessionId) return
        _ringing.value = entry
    }

    fun clear() {
        _ringing.value = null
    }

    fun suppress(sessionId: String) {
        suppressedSessionId = sessionId
        if (_ringing.value?.sessionId == sessionId) _ringing.value = null
    }

    fun unsuppress(sessionId: String) {
        if (suppressedSessionId == sessionId) suppressedSessionId = null
    }
}
