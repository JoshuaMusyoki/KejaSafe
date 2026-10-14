package com.rotd.havenpad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationPermissionState(onResult: (Boolean) -> Unit): LocationPermissionState {
    return remember(onResult) {
        object : LocationPermissionState {
            override val isGranted: Boolean
                get() = locationAccessGranted()

            override fun launchPermissionRequest() {
                requestLocationAccess(onResult)
            }
        }
    }
}
