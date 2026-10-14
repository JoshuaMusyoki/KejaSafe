package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
actual fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit {
    val onTokenState by rememberUpdatedState(onToken)
    val onErrorState by rememberUpdatedState(onError)
    val scope = rememberCoroutineScope()
    val bridge = remember { WasmGoogleBridge() }
    LaunchedEffect(Unit) { bridge.warm() }
    return { clientId ->
        if (clientId.isBlank()) {
            onErrorState("Google sign-in is not set up on this server yet.")
        } else {
            val pending = bridge.begin(clientId)
            scope.launch {
                val (token, error) = runCatching { withTimeout(120_000) { pending.await() } }
                    .getOrElse { "" to (it.message ?: "Google sign-in failed") }
                if (token.isNotBlank()) onTokenState(token)
                else onErrorState(error.ifBlank { "Google sign-in failed" })
            }
        }
    }
}

private class WasmGoogleBridge {
    private var ready = false
    private var pending: CompletableDeferred<Pair<String, String>>? = null

    fun warm() {
        install()
    }

    /** Starts Google in the click turn so the browser can show the account list. */
    fun begin(clientId: String): CompletableDeferred<Pair<String, String>> {
        install()
        val deferred = CompletableDeferred<Pair<String, String>>()
        pending?.complete("" to "Google sign-in was replaced")
        pending = deferred
        startGoogle(clientId)
        return deferred
    }

    private fun install() {
        if (ready) return
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
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(code) => { eval(code); }")
private external fun evalHavenGoogle(code: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(callback) => { globalThis.__havenGoogleDone = callback; }")
private external fun bindGoogleDone(callback: (String) -> Unit)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(clientId) => { globalThis.__havenGoogleStart(clientId); }")
private external fun startGoogle(clientId: String)
