package com.rotd.kejasafe.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform connectivity signals.
 * Android also registers a system NetworkCallback; all targets update via [reportOffline]/[reportOnline]
 * from network request outcomes.
 */
object ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _becameOnline = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when connectivity transitions from offline → online. */
    val becameOnline: SharedFlow<Unit> = _becameOnline.asSharedFlow()

    fun reportOffline() {
        _isOnline.value = false
    }

    fun reportOnline() {
        val wasOffline = !_isOnline.value
        _isOnline.value = true
        if (wasOffline) {
            _becameOnline.tryEmit(Unit)
        }
    }
}

/** Platform hook (Android registers ConnectivityManager; others no-op). */
expect fun startConnectivityMonitor()
