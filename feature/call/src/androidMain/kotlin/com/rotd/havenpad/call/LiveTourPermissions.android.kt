package com.rotd.havenpad.call

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberLiveTourPermissionGate(onGranted: () -> Unit): () -> Unit {
    val state = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        )
    )
    var pendingJoin by remember { mutableStateOf(false) }

    LaunchedEffect(state.allPermissionsGranted, pendingJoin) {
        if (pendingJoin && state.allPermissionsGranted) {
            pendingJoin = false
            onGranted()
        }
    }

    return remember(state, onGranted) {
        {
            if (state.allPermissionsGranted) {
                onGranted()
            } else {
                pendingJoin = true
                state.launchMultiplePermissionRequest()
            }
        }
    }
}
