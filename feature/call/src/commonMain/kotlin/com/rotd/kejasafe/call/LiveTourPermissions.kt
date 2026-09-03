package com.rotd.kejasafe.call

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that requests camera + mic (Android) then invokes [onGranted].
 * On iOS/other platforms the system prompts on first media use, so [onGranted] runs immediately.
 */
@Composable
expect fun rememberLiveTourPermissionGate(onGranted: () -> Unit): () -> Unit
