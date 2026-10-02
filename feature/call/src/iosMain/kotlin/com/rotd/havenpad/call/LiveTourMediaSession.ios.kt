package com.rotd.havenpad.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS media actual: uses [StubLiveTourMediaSession] until LiveKit Swift is bridged.
 * See `app/iosApp/Podfile` and `LiveTourLiveKitBridge.swift`.
 */
@Composable
actual fun rememberLiveTourMediaSession(): LiveTourMediaSession =
    rememberStubLiveTourMediaSession("iOS")

@Composable
actual fun LiveTourVideoPane(
    session: LiveTourMediaSession,
    showRemote: Boolean,
    modifier: Modifier,
) {
    StubLiveTourVideoPane(showRemote = showRemote, modifier = modifier)
}
