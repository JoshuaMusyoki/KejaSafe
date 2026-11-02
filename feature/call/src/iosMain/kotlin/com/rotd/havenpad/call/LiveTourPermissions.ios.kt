package com.rotd.havenpad.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLiveTourPermissionGate(onGranted: () -> Unit): () -> Unit =
    remember(onGranted) { onGranted }
