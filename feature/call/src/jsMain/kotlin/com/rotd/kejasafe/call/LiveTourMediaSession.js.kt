package com.rotd.kejasafe.call

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun rememberLiveTourMediaSession(): LiveTourMediaSession =
    rememberStubLiveTourMediaSession("Web")

@Composable
actual fun LiveTourVideoPane(
    session: LiveTourMediaSession,
    showRemote: Boolean,
    modifier: Modifier,
) {
    StubLiveTourVideoPane(showRemote = showRemote, modifier = modifier)
}
