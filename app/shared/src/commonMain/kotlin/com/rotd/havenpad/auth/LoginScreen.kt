package com.rotd.havenpad.auth

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
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeadline
import com.rotd.havenpad.components.HavenPadLabel
import com.rotd.havenpad.components.HavenPadOutlinedButton
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.HavenPadTextField
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.theme.HavenPadTheme
import com.rotd.havenpad.theme.ScreenPreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val identifier by viewModel.identifier.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val offerEnableBiometric by viewModel.offerEnableBiometric.collectAsStateWithLifecycle()

    val launchGoogle = rememberGoogleIdTokenLauncher(
        onToken = { token -> viewModel.loginWithGoogle(token, UserRole.STUDENT) },
        onError = viewModel::reportError,
    )

    LoginScreenContent(
        state = state,
        identifier = identifier,
        password = password,
        biometricEnabled = biometricEnabled,
        offerEnableBiometric = offerEnableBiometric,
        onNavigateToRegister = onNavigateToRegister,
        onContinueAsGuest = onContinueAsGuest,
        onBack = onBack,
        onIdentifierChange = { viewModel.identifier.value = it },
        onPasswordChange = { viewModel.password.value = it },
        onLogin = { viewModel.login() },
        onLoginWithGoogle = { viewModel.prepareGoogleSignIn(launchGoogle) },
        onLoginWithFingerprint = { viewModel.loginWithFingerprint() },
        onConfirmBiometric = { viewModel.confirmEnableBiometric() },
        onSkipBiometric = { viewModel.skipEnableBiometric() }
    )

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
}

@Composable
private fun LoginScreenContent(
    state: LoginState,
    identifier: String,
    password: String,
    biometricEnabled: Boolean,
    offerEnableBiometric: Boolean,
    onNavigateToRegister: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onBack: (() -> Unit)?,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onLoginWithGoogle: () -> Unit,
    onLoginWithFingerprint: () -> Unit,
    onConfirmBiometric: () -> Unit,
    onSkipBiometric: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    if (offerEnableBiometric) {
        AlertDialog(
            onDismissRequest = onSkipBiometric,
            title = { Text("Enable biometric login?") },
            text = {
                Text("Use fingerprint or Face ID next time instead of typing your password on this device.")
            },
            confirmButton = {
                TextButton(onClick = onConfirmBiometric) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = onSkipBiometric) {
                    Text("Not now")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            HavenPadHeader(
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

            if (state is LoginState.AwaitingApproval) {
                HavenPadHeadline(text = "Check your other device")
                HavenPadBody(
                    text = "A sign-in from this device is waiting. Open HavenPad on a device that is already signed in and allow it, the same way Google asks before a new device can continue."
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (state is LoginState.AwaitingApproval) {
                    Text(
                        text = state.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                return@AuthFormScaffold
            }

            HavenPadHeadline(text = "Welcome Back")
            HavenPadBody(text = "Enter your username or email to continue.")

            Spacer(modifier = Modifier.height(24.dp))

            HavenPadTextField(
                value = identifier,
                onValueChange = onIdentifierChange,
                label = "Username or Email",
                modifier = Modifier.fillMaxWidth()
            )

            HavenPadTextField(
                value = password,
                onValueChange = onPasswordChange,
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
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HavenPadPrimaryButton(
                text = if (state is LoginState.Loading) "Logging in..." else "Log In",
                onClick = onLogin,
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

            HavenPadOutlinedButton(
                text = "Sign in with Google",
                onClick = onLoginWithGoogle,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.Loading
            )

            HavenPadOutlinedButton(
                text = "Continue as a guest",
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is LoginState.Loading
            )

            if (biometricEnabled) {
                IconButton(
                    onClick = onLoginWithFingerprint,
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
                HavenPadLabel(text = "Don't have an account? Sign Up")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@ScreenPreviews
@Composable
fun LoginScreenPreview() {
    HavenPadTheme {
        LoginScreenContent(
            state = LoginState.Idle,
            identifier = "",
            password = "",
            biometricEnabled = true,
            offerEnableBiometric = false,
            onNavigateToRegister = {},
            onContinueAsGuest = {},
            onBack = {},
            onIdentifierChange = {},
            onPasswordChange = {},
            onLogin = {},
            onLoginWithGoogle = {},
            onLoginWithFingerprint = {},
            onConfirmBiometric = {},
            onSkipBiometric = {}
        )
    }
}
