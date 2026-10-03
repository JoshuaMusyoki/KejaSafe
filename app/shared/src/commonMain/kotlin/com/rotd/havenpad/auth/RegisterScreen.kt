package com.rotd.havenpad.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
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
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()

    RegisterScreenContent(
        state = state,
        fullName = fullName,
        username = username,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        role = role,
        onNavigateToLogin = onNavigateToLogin,
        onBack = onBack,
        onFullNameChange = { viewModel.fullName.value = it },
        onUsernameChange = { viewModel.username.value = it },
        onEmailChange = { viewModel.email.value = it },
        onPasswordChange = { viewModel.password.value = it },
        onConfirmPasswordChange = { viewModel.confirmPassword.value = it },
        onRoleChange = { viewModel.role.value = it },
        onRegister = { viewModel.register() }
    )

    // Activity-scoped VM can retain Success after logout; only navigate when session is live.
    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            if (SessionManager.isLogged()) {
                onRegistrationSuccess()
            }
            viewModel.consumeSuccess()
        }
    }
}

@Composable
private fun RegisterScreenContent(
    state: RegisterState,
    fullName: String,
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    role: UserRole,
    onNavigateToLogin: () -> Unit,
    onBack: (() -> Unit)?,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HavenPadHeader(
                title = "Create Account",
                onBackClick = onBack ?: onNavigateToLogin
            )
        }
    ) { padding ->
        AuthFormScaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            HavenPadHeadline(text = "Join HavenPad")
            HavenPadBody(text = "Find your safe sanctuary today.")

            Spacer(modifier = Modifier.height(16.dp))

            HavenPadTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = "Full Name",
                modifier = Modifier.fillMaxWidth()
            )

            HavenPadTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = "Username",
                modifier = Modifier.fillMaxWidth()
            )

            HavenPadTextField(
                value = email,
                onValueChange = onEmailChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                label = "Email Address",
                modifier = Modifier.fillMaxWidth()
            )

            HavenPadTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password (8+ characters)",
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

            HavenPadTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HavenPadLabel(text = "I am a:")
                RadioButton(
                    selected = role == UserRole.STUDENT,
                    onClick = { onRoleChange(UserRole.STUDENT) }
                )
                HavenPadLabel(text = "Student")
                RadioButton(
                    selected = role == UserRole.LANDLORD,
                    onClick = { onRoleChange(UserRole.LANDLORD) }
                )
                HavenPadLabel(text = "Landlord")
            }

            if (state is RegisterState.Error) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HavenPadPrimaryButton(
                text = if (state is RegisterState.Loading) "Registering..." else "Create Account",
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is RegisterState.Loading
            )

            HavenPadOutlinedButton(
                text = "Sign up with Google",
                onClick = { /* Handle Google Sign In */ },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = onNavigateToLogin) {
                HavenPadLabel(text = "Already have an account? Log In")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@ScreenPreviews
@Composable
fun RegisterScreenPreview() {
    HavenPadTheme {
        RegisterScreenContent(
            state = RegisterState.Idle,
            fullName = "John Doe",
            username = "johndoe",
            email = "john@example.com",
            password = "",
            confirmPassword = "",
            role = UserRole.STUDENT,
            onNavigateToLogin = {},
            onBack = {},
            onFullNameChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onRoleChange = {},
            onRegister = {}
        )
    }
}
