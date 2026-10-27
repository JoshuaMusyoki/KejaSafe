package com.rotd.havenpad.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Set from a notification tap so the open app can jump into that chat. */
object ChatLaunchRequest {
    private val _pending = MutableStateFlow<HavenPadDestination.ChatThread?>(null)
    val pending = _pending.asStateFlow()

    fun request(thread: HavenPadDestination.ChatThread) {
        _pending.value = thread
    }

    fun consume() {
        _pending.value = null
    }
}

/** Set from a full-screen house-tour ring so the open app joins that call. */
object TourLaunchRequest {
    private val _pending = MutableStateFlow<String?>(null)
    val pending = _pending.asStateFlow()

    fun request(sessionId: String) {
        _pending.value = sessionId
    }

    fun consume() {
        _pending.value = null
    }
}

object BillLaunchRequest {
    private val _pending = MutableStateFlow<String?>(null)
    val pending = _pending.asStateFlow()

    fun request(billId: String) {
        _pending.value = billId
    }

    fun consume() {
        _pending.value = null
    }
}
