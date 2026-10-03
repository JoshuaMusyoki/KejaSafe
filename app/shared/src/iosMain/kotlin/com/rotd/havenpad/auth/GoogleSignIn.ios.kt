package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit = { _ ->
    onError("Sign in with Google from the HavenPad website or the Android app.")
}
