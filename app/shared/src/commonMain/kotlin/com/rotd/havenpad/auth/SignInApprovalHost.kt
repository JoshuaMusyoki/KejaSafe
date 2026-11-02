package com.rotd.havenpad.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.rotd.havenpad.network.AuthClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SignInApprovalHost() {
    val prompt by SignInPromptBus.prompt.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (isActive) {
            if (SessionManager.isLogged()) {
                AuthClient.rememberDevice()
                AuthClient.incomingSignIn().onSuccess { incoming ->
                    if (incoming.challengeId.isNotBlank()) SignInPromptBus.offer(incoming)
                }
            }
            delay(6000)
        }
    }

    val request = prompt
    if (request != null && SessionManager.isLogged()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("New sign-in") },
            text = {
                Text(
                    "${request.deviceName} is trying to sign in to your account. Allow it only if this was you."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = request.challengeId
                        SignInPromptBus.clear(id)
                        scope.launch { AuthClient.approveSignIn(id) }
                    }
                ) { Text("Allow") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val id = request.challengeId
                        SignInPromptBus.clear(id)
                        scope.launch { AuthClient.denySignIn(id) }
                    }
                ) { Text("Block") }
            },
        )
    }
}
