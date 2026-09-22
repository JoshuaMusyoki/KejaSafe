package com.rotd.kejasafe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationPermissionState(onResult: (Boolean) -> Unit): LocationPermissionState {
    return remember {
        object : LocationPermissionState {
            override val isGranted: Boolean = true
            override fun launchPermissionRequest() {
                onResult(true)
            }
        }
    }
}
