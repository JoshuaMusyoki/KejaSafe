package com.rotd.kejasafe.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.domain.repository.AuthRepository
import com.rotd.kejasafe.error.authUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginState {
    data object Idle : LoginState
    data object Loading : LoginState
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
                onFailure = {
                    _state.value = LoginState.Error(it.authUserMessage())
                }
            )
        }
    }

    fun loginWithGoogle() {
        _state.value = LoginState.Error("Google Sign-In UI not wired yet — use email/password for now.")
    }

    fun loginWithGoogle(idToken: String, role: com.rotd.kejasafe.UserRole) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            authRepository.signInWithGoogle(idToken, role).fold(
                onSuccess = {
                    offerEnableBiometric.value = false
                    _state.value = LoginState.Success("Login successful!")
                },
                onFailure = {
                    _state.value = LoginState.Error(it.authUserMessage())
                }
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

    private fun onPasswordLoginSuccess() {
        refreshBiometricState()
        val shouldOffer = biometricAvailable.value && !biometricEnabled.value
        offerEnableBiometric.value = shouldOffer
        _state.value = LoginState.Success("Login successful!")
    }
}
