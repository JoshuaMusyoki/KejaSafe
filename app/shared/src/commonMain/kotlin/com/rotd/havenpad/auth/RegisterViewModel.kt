package com.rotd.havenpad.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.error.authUserMessage
import com.rotd.havenpad.onboardingError
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
    var phone = MutableStateFlow("")
    var password = MutableStateFlow("")
    var confirmPassword = MutableStateFlow("")
    var role = MutableStateFlow(UserRole.STUDENT)
    var university = MutableStateFlow("")
    var studentNumber = MutableStateFlow("")
    var major = MutableStateFlow("")
    var yearOfStudy = MutableStateFlow("")
    var businessName = MutableStateFlow("")
    var nationalIdNumber = MutableStateFlow("")
    var residentialAddress = MutableStateFlow("")
    var licenseNumber = MutableStateFlow("")
    var serviceArea = MutableStateFlow("")

    private var preparedRole: UserRole? = null

    fun prepare(role: UserRole) {
        if (preparedRole == role) return
        reset()
        this.role.value = role
        preparedRole = role
    }

    fun register() {
        val request = currentRequest()
        if (password.value != confirmPassword.value) {
            _state.value = RegisterState.Error("Passwords do not match")
            return
        }
        request.onboardingError()?.let { message ->
            _state.value = RegisterState.Error(message)
            return
        }

        viewModelScope.launch {
            _state.value = RegisterState.Loading
            authRepository.registerWithEmail(
                email = request.email,
                password = request.password,
                username = request.username,
                fullName = request.fullName,
                role = request.role,
                details = request,
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

    private fun currentRequest(): RegistrationRequest = RegistrationRequest(
        email = email.value.trim(),
        username = username.value.trim(),
        fullName = fullName.value.trim(),
        password = password.value,
        role = role.value,
        phone = phone.value.trim(),
        university = university.value.trim(),
        studentNumber = studentNumber.value.trim(),
        major = major.value.trim(),
        yearOfStudy = yearOfStudy.value.trim().toIntOrNull(),
        businessName = businessName.value.trim(),
        nationalIdNumber = nationalIdNumber.value.trim(),
        residentialAddress = residentialAddress.value.trim(),
        licenseNumber = licenseNumber.value.trim(),
        serviceArea = serviceArea.value.trim(),
    )

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
        phone.value = ""
        password.value = ""
        confirmPassword.value = ""
        role.value = UserRole.STUDENT
        university.value = ""
        studentNumber.value = ""
        major.value = ""
        yearOfStudy.value = ""
        businessName.value = ""
        nationalIdNumber.value = ""
        residentialAddress.value = ""
        licenseNumber.value = ""
        serviceArea.value = ""
    }
}
