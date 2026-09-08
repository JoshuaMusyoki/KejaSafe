package com.rotd.kejasafe.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.kejasafe.components.KejaSafeBody
import com.rotd.kejasafe.components.KejaSafeHeader
import com.rotd.kejasafe.components.KejaSafeHeadline
import com.rotd.kejasafe.components.KejaSafeLabel
import com.rotd.kejasafe.components.KejaSafeOutlinedButton
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.components.KejaSafeTextField
import com.rotd.kejasafe.network.SessionManager
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val identifier by viewModel.identifier.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val offerEnableBiometric by viewModel.offerEnableBiometric.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshBiometricState()
    }

    // ViewModels can be activity-scoped and retain Success after logout; only navigate when session is live.
    LaunchedEffect(state, offerEnableBiometric) {
        if (state is LoginState.Success && !offerEnableBiometric) {
            if (SessionManager.isLogged()) {
                onLoginSuccess()
            }
            viewModel.consumeSuccess()
        }
    }

    if (offerEnableBiometric) {
        AlertDialog(
            onDismissRequest = { viewModel.skipEnableBiometric() },
            title = { Text("Enable biometric login?") },
            text = {
                Text("Use fingerprint or Face ID next time instead of typing your password on this device.")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmEnableBiometric() }) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.skipEnableBiometric() }) {
                    Text("Not now")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            KejaSafeHeader(
                title = "Log In",
                onBackClick = onBack
            )
        }
    ) { padding ->
        AuthFormScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            KejaSafeHeadline(text = "Welcome Back")
            KejaSafeBody(text = "Enter your username or email to continue.")

            Spacer(modifier = Modifier.height(24.dp))

            KejaSafeTextField(
                value = identifier,
                onValueChange = { viewModel.identifier.value = it },
                label = "Username or Email",
                modifier = Modifier.fillMaxWidth()
            )

            KejaSafeTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = "Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            if (state is LoginState.Error) {
                Text(
                    text = (state as LoginState.Error).error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            KejaSafePrimaryButton(
                text = if (state is LoginState.Loading) "Logging in..." else "Log In",
                onClick = { viewModel.login() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.Loading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    " OR ",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outline
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            KejaSafeOutlinedButton(
                text = "Sign in with Google",
                onClick = { viewModel.loginWithGoogle() },
                modifier = Modifier.fillMaxWidth()
            )

            if (biometricEnabled) {
                IconButton(
                    onClick = { viewModel.loginWithFingerprint() },
                    modifier = Modifier.size(64.dp),
                    enabled = state !is LoginState.Loading
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Login",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            TextButton(onClick = onNavigateToRegister) {
                KejaSafeLabel(text = "Don't have an account? Sign Up")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
