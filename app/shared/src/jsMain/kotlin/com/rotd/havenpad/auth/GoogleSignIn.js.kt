package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private val evalHavenGoogle: (String) -> Unit = js("function(code) { eval(code); }")
private val bindGoogleDone: ((String) -> Unit) -> Unit = js(
    "function(callback) { globalThis.__havenGoogleDone = callback; }"
)
private val startGoogle: (String) -> Unit = js(
    "function(clientId) { globalThis.__havenGoogleStart(clientId); }"
)

@Composable
actual fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit {
    val onTokenState by rememberUpdatedState(onToken)
    val onErrorState by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    val bridge = remember { JsGoogleBridge() }
    return { clientId ->
        if (clientId.isBlank()) {
            onErrorState("Google sign-in is not set up on this server yet.")
        } else {
            scope.launch {
                val (token, error) = runCatching { bridge.request(clientId) }
                    .getOrElse { "" to (it.message ?: "Google sign-in failed") }
                if (token.isNotBlank()) onTokenState(token)
                else onErrorState(error.ifBlank { "Google sign-in failed" })
            }
        }
    }
}

private class JsGoogleBridge {
    private var ready = false
    private var pending: CompletableDeferred<Pair<String, String>>? = null

    suspend fun request(clientId: String): Pair<String, String> {
        if (!ready) {
            evalHavenGoogle(HAVEN_GOOGLE_SIGN_IN_SCRIPT)
            bindGoogleDone { payload ->
                val split = payload.indexOf('\n')
                val token = if (split < 0) payload else payload.substring(0, split)
                val error = if (split < 0) "" else payload.substring(split + 1)
                pending?.complete(token to error)
                pending = null
            }
            ready = true
        }
        val deferred = CompletableDeferred<Pair<String, String>>()
        pending?.complete("" to "Google sign-in was replaced")
        pending = deferred
        startGoogle(clientId)
        return withTimeout(120_000) { deferred.await() }
    }
}
