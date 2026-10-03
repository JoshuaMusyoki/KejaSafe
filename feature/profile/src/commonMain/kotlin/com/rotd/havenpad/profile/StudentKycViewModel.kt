package com.rotd.havenpad.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.LandlordKycRecord
import com.rotd.havenpad.StudentKycRequest
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.AuthClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentKycUiState(
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val record: LandlordKycRecord? = null,
    val error: String? = null,
    val done: Boolean = false,
)

class StudentKycViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StudentKycUiState())
    val state: StateFlow<StudentKycUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            AuthClient.myKyc().fold(
                onSuccess = { record ->
                    _state.update { it.copy(loading = false, record = record) }
                },
                onFailure = { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                },
            )
        }
    }

    fun submit(request: StudentKycRequest) {
        if (_state.value.submitting) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            authRepository.submitStudentKyc(request).fold(
                onSuccess = {
                    _state.update { it.copy(submitting = false, done = true) }
                },
                onFailure = { error ->
                    _state.update { it.copy(submitting = false, error = error.userMessage()) }
                },
            )
        }
    }
}
