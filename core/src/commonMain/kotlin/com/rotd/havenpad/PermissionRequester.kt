package com.rotd.havenpad

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLocationPermissionState(onResult: (Boolean) -> Unit): LocationPermissionState

interface LocationPermissionState {
    val isGranted: Boolean
    fun launchPermissionRequest()
}
