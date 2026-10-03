package com.rotd.havenpad.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.error.authUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterState {
    data object Idle : RegisterState
    data object Loading : RegisterState
    data class Success(val message: String) : RegisterState
    data class Error(val error: String) : RegisterState
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    var fullName = MutableStateFlow("")
    var username = MutableStateFlow("")
    var email = MutableStateFlow("")
    var password = MutableStateFlow("")
    var confirmPassword = MutableStateFlow("")
    var role = MutableStateFlow(UserRole.STUDENT)

    fun register() {
        if (fullName.value.isBlank() || username.value.isBlank() || email.value.isBlank() || password.value.isBlank() || confirmPassword.value.isBlank()) {
            _state.value = RegisterState.Error("All fields are required")
            return
        }

        if (password.value != confirmPassword.value) {
            _state.value = RegisterState.Error("Passwords do not match")
            return
        }

        if (password.value.length < 8) {
            _state.value = RegisterState.Error("Password must be at least 8 characters.")
            return
        }

        viewModelScope.launch {
            _state.value = RegisterState.Loading
            authRepository.registerWithEmail(
                email = email.value,
                password = password.value,
                username = username.value,
                fullName = fullName.value,
                role = role.value,
            ).fold(
                onSuccess = {
                    _state.value = RegisterState.Success("Registration successful!")
                },
                onFailure = {
                    _state.value = RegisterState.Error(it.authUserMessage())
                }
            )
        }
    }

    /** Call after handling [RegisterState.Success] so re-opening Register does not auto-navigate. */
    fun consumeSuccess() {
        if (_state.value is RegisterState.Success) {
            _state.value = RegisterState.Idle
        }
    }

    fun reset() {
        _state.value = RegisterState.Idle
        fullName.value = ""
        username.value = ""
        email.value = ""
        password.value = ""
        confirmPassword.value = ""
        role.value = UserRole.STUDENT
    }
}
