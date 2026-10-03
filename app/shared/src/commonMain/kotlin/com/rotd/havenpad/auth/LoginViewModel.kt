package com.rotd.havenpad.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.auth.SignInAwaitingApproval
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.error.authUserMessage
import com.rotd.havenpad.network.AuthClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
    data class AwaitingApproval(val challengeId: String, val deviceName: String) : LoginState
    data class Success(val message: String) : LoginState
    data class Error(val error: String) : LoginState
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    var identifier = MutableStateFlow("") // email or username
    var password = MutableStateFlow("")

    val biometricAvailable = MutableStateFlow(false)
    val biometricEnabled = MutableStateFlow(false)
    val offerEnableBiometric = MutableStateFlow(false)

    init {
        refreshBiometricState()
    }

    fun refreshBiometricState() {
        biometricAvailable.value = authRepository.isBiometricAvailable()
        biometricEnabled.value = authRepository.isBiometricEnabled()
    }

    fun login() {
        if (identifier.value.isBlank() || password.value.isBlank()) {
            _state.value = LoginState.Error("Please enter both identifier and password")
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithIdentifier(
                identifier = identifier.value,
                password = password.value,
            ).fold(
                onSuccess = { onPasswordLoginSuccess() },
                onFailure = { error -> handleSignInFailure(error) }
            )
        }
    }

    fun prepareGoogleSignIn(onClientId: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            AuthClient.googleClientId().fold(
                onSuccess = { clientId ->
                    if (clientId.isBlank()) {
                        _state.value = LoginState.Error(
                            "Google sign-in is not set up on this server yet. Continue as a guest, or sign in with email.",
                        )
                    } else {
                        _state.value = LoginState.Idle
                        onClientId(clientId)
                    }
                },
                onFailure = {
                    _state.value = LoginState.Error(it.authUserMessage())
                },
            )
        }
    }

    fun reportError(message: String) {
        _state.value = LoginState.Error(message)
    }

    fun loginWithGoogle(idToken: String, role: UserRole = UserRole.STUDENT) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithGoogle(idToken, role).fold(
                onSuccess = {
                    offerEnableBiometric.value = false
                    _state.value = LoginState.Success("Login successful!")
                },
                onFailure = { error -> handleSignInFailure(error) }
            )
        }
    }

    fun loginWithFingerprint() {
        if (!authRepository.isBiometricEnabled()) {
            _state.value = LoginState.Error(
                "Biometric login is not set up. Sign in with your password first.",
            )
            return
        }
        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithBiometrics().fold(
                onSuccess = {
                    offerEnableBiometric.value = false
                    refreshBiometricState()
                    _state.value = LoginState.Success("Login successful!")
                },
                onFailure = {
                    _state.value = LoginState.Error(it.authUserMessage())
                }
            )
        }
    }

    fun confirmEnableBiometric() {
        viewModelScope.launch {
            authRepository.enableBiometricLogin(identifier.value, password.value)
            offerEnableBiometric.value = false
            refreshBiometricState()
            _state.value = LoginState.Success("Login successful!")
        }
    }

    fun skipEnableBiometric() {
        offerEnableBiometric.value = false
        if (_state.value !is LoginState.Success) {
            _state.value = LoginState.Success("Login successful!")
        }
    }

    /** Call after handling [LoginState.Success] so re-opening Login after logout does not auto-navigate home. */
    fun consumeSuccess() {
        if (_state.value is LoginState.Success) {
            _state.value = LoginState.Idle
        }
        offerEnableBiometric.value = false
    }

    fun reset() {
        _state.value = LoginState.Idle
        identifier.value = ""
        password.value = ""
        offerEnableBiometric.value = false
        refreshBiometricState()
    }

    private fun handleSignInFailure(error: Throwable) {
        val waiting = error as? SignInAwaitingApproval
        if (waiting != null) {
            _state.value = LoginState.AwaitingApproval(waiting.challengeId, waiting.deviceName)
            waitForApproval(waiting.challengeId)
        } else {
            _state.value = LoginState.Error(error.authUserMessage())
        }
    }

    private fun waitForApproval(challengeId: String) {
        viewModelScope.launch {
            while (isActive && _state.value is LoginState.AwaitingApproval) {
                delay(2000)
                val body = AuthClient.pollSignIn(challengeId).getOrNull() ?: continue
                when {
                    body.pending -> Unit
                    body.token.isNotBlank() && body.user != null -> {
                        val signedIn = body.user ?: return@launch
                        SessionManager.saveSession(body.token, signedIn)
                        onPasswordLoginSuccess()
                    }
                    body.denied -> _state.value = LoginState.Error(
                        "This sign-in was blocked from your other device."
                    )
                }
            }
        }
    }

    private fun onPasswordLoginSuccess() {
        refreshBiometricState()
        val shouldOffer = biometricAvailable.value && !biometricEnabled.value
        offerEnableBiometric.value = shouldOffer
        _state.value = LoginState.Success("Login successful!")
    }
}
