package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
actual fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit {
    val context = LocalContext.current
    val onTokenState by rememberUpdatedState(onToken)
    val onErrorState by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    return { clientId ->
        if (clientId.isBlank()) {
            onErrorState("Google sign-in is not set up on this server yet.")
        } else scope.launch {
            runCatching {
                val manager = CredentialManager.create(context)
                val option = GetSignInWithGoogleOption.Builder(clientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                val result = manager.getCredential(context, request)
                GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            }.fold(
                onSuccess = { token ->
                    if (token.isBlank()) onErrorState("Google did not return a sign-in")
                    else onTokenState(token)
                },
                onFailure = { error ->
                    val message = if (error is GetCredentialCancellationException) {
                        "Google sign-in was cancelled"
                    } else {
                        error.message ?: "Google sign-in failed"
                    }
                    onErrorState(message)
                },
            )
        }
    }
}
