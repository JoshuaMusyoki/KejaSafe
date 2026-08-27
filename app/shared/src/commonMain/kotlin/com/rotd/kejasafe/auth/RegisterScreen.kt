package com.rotd.kejasafe.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.components.*
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.theme.KejaSafeTheme

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val fullName by viewModel.fullName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()
    val role by viewModel.role.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Activity-scoped VM can retain Success after logout; only navigate when session is live.
    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            if (SessionManager.isLogged()) {
                onRegistrationSuccess()
            }
            viewModel.consumeSuccess()
        }
    }

    Scaffold(
        topBar = {
            KejaSafeHeader(
                title = "Create Account",
                onBackClick = onNavigateToLogin
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = KejaSafeTheme.spacing.containerMarginMobile)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KejaSafeTheme.spacing.elementGap)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            KejaSafeHeadline(text = "Join KejaSafe")
            KejaSafeBody(text = "Find your safe sanctuary today.")

            Spacer(modifier = Modifier.height(16.dp))

            KejaSafeTextField(
                value = fullName,
                onValueChange = { viewModel.fullName.value = it },
                label = "Full Name",
                modifier = Modifier.fillMaxWidth()
            )

            KejaSafeTextField(
                value = username,
                onValueChange = { viewModel.username.value = it },
                label = "Username",
                modifier = Modifier.fillMaxWidth()
            )

            KejaSafeTextField(
                value = email,
                onValueChange = { viewModel.email.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                label = "Email Address",
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

            KejaSafeTextField(
                value = confirmPassword,
                onValueChange = { viewModel.confirmPassword.value = it },
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
                KejaSafeLabel(text = "I am a:")
                RadioButton(
                    selected = role == UserRole.STUDENT,
                    onClick = { viewModel.role.value = UserRole.STUDENT }
                )
                KejaSafeLabel(text = "Student")
                RadioButton(
                    selected = role == UserRole.LANDLORD,
                    onClick = { viewModel.role.value = UserRole.LANDLORD }
                )
                KejaSafeLabel(text = "Landlord")
            }

            if (state is RegisterState.Error) {
                Text(
                    text = (state as RegisterState.Error).error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            KejaSafePrimaryButton(
                text = if (state is RegisterState.Loading) "Registering..." else "Create Account",
                onClick = { viewModel.register() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is RegisterState.Loading
            )

            KejaSafeOutlinedButton(
                text = "Sign up with Google",
                onClick = { /* Handle Google Sign In */ },
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = onNavigateToLogin) {
                KejaSafeLabel(text = "Already have an account? Log In")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
