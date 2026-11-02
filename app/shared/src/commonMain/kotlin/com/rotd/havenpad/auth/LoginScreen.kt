package com.rotd.havenpad.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeadline
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
        onForgotPassword = { viewModel.sendPasswordReset() },
        onLoginWithGoogle = { viewModel.prepareGoogleSignIn(launchGoogle) },
        onLoginWithFingerprint = { viewModel.loginWithFingerprint() },
        onConfirmBiometric = { viewModel.confirmEnableBiometric() },
        onSkipBiometric = { viewModel.skipEnableBiometric() }
    )

    LaunchedEffect(Unit) {
        viewModel.refreshBiometricState()
        viewModel.warmGoogleSignIn()
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
    onForgotPassword: () -> Unit,
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

    val fieldText = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 24.sp)
    val actionText = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)

    Scaffold(
        topBar = {
            HavenPadHeader(
                title = "Log In",
                onBackClick = onBack
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state is LoginState.AwaitingApproval) {
                        HavenPadHeadline(text = "Check your other device")
                        HavenPadBody(
                            text = "A sign-in from this device is waiting. Open HavenPad on a device that is already signed in and allow it.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = state.deviceName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        return@Column
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    Text(
                        text = "HavenPad",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HavenPadHeadline(text = "Welcome Back")
                    HavenPadBody(
                        text = "Log in to continue your stay.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    HavenPadTextField(
                        value = identifier,
                        onValueChange = onIdentifierChange,
                        label = "Email or username",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = fieldText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HavenPadTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        textStyle = fieldText,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                )
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onForgotPassword,
                            enabled = state !is LoginState.Loading,
                        ) {
                            Text("Forgot password?", style = actionText, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (state is LoginState.Error) {
                        Text(
                            text = state.error,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    HavenPadPrimaryButton(
                        text = if (state is LoginState.Loading) "Signing in..." else "Sign in",
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state !is LoginState.Loading,
                        content = {
                            Text(
                                if (state is LoginState.Loading) "Signing in..." else "Sign in",
                                style = actionText,
                            )
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "or continue with",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HavenPadOutlinedButton(
                            text = "Google",
                            onClick = onLoginWithGoogle,
                            modifier = Modifier.weight(1f),
                            enabled = state !is LoginState.Loading,
                            content = { Text("Google", style = actionText) },
                        )
                        if (biometricEnabled) {
                            IconButton(
                                onClick = onLoginWithFingerprint,
                                modifier = Modifier.size(56.dp),
                                enabled = state !is LoginState.Loading,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint Login",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onContinueAsGuest,
                        enabled = state !is LoginState.Loading,
                    ) {
                        Text("Continue as guest", style = actionText, color = MaterialTheme.colorScheme.primary)
                    }
                    }
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Don't have an account? ", style = MaterialTheme.typography.bodyLarge)
                        Text("Sign up", style = actionText, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
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
            onForgotPassword = {},
            onLoginWithGoogle = {},
            onLoginWithFingerprint = {},
            onConfirmBiometric = {},
            onSkipBiometric = {}
        )
    }
}
