package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.havenpad.ios.IosGoogleSignInBridge

@Composable
actual fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit {
    val onTokenState by rememberUpdatedState(onToken)
    val onErrorState by rememberUpdatedState(onError)
    return { clientId ->
        if (clientId.isBlank()) {
            onErrorState("Google sign-in is not set up on this server yet.")
        } else {
            IosGoogleSignInBridge.launch(clientId, onTokenState, onErrorState)
        }
    }
}
