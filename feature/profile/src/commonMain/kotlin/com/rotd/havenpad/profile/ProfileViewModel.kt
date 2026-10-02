package com.rotd.havenpad.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.RoommatePreferences
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.data.ProfileRepository
import com.rotd.havenpad.error.ErrorMessages
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.network.ConnectivityMonitor
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileEditDraft(
    val fullName: String = "",
    val username: String = "",
    val phone: String = "",
    val businessName: String = "",
    val major: String = "",
    val yearOfStudy: String = "",
    val roommatePreferencesText: String = ""
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val error: String? = null,
    val syncWarning: String? = null,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricMessage: String? = null,
    val accountMessage: String? = null,
    val showBiometricPasswordPrompt: Boolean = false,
)

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val draft: ProfileEditDraft = ProfileEditDraft(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val completed: Boolean = false
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: com.rotd.havenpad.domain.repository.AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        // Keep UI in lock-step with session updates (e.g. after Edit Profile saves).
        viewModelScope.launch {
            SessionManager.currentUser.collect { sessionUser ->
                if (sessionUser != null) {
                    _state.update {
                        it.copy(user = sessionUser, isLoading = false, error = null)
                    }
                }
            }
        }
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                loadProfile()
            }
        }
        loadProfile()
        refreshBiometricState()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val sessionUser = SessionManager.currentUser.value
            if (sessionUser != null) {
                _state.update { it.copy(user = sessionUser, isLoading = false, error = null) }
            } else {
                _state.update { it.copy(isLoading = true, error = null, syncWarning = null) }
            }

            profileRepository.getProfile(
                onCacheHit = { cached ->
                    SessionManager.updateUser(cached)
                    _state.update {
                        it.copy(user = cached, isLoading = false, error = null)
                    }
                }
            )
                .onSuccess { offline ->
                    _state.update {
                        it.copy(
                            user = offline.value,
                            isLoading = false,
                            error = null,
                            syncWarning = offline.syncWarning
                        )
                    }
                }
                .onFailure { e ->
                    if (sessionUser != null || _state.value.user != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                syncWarning = e.userMessage().takeIf {
                                    it == ErrorMessages.OFFLINE_CACHED ||
                                        it == ErrorMessages.SYNC_FAILED_CACHED ||
                                        it == ErrorMessages.NO_INTERNET
                                } ?: ErrorMessages.SYNC_FAILED_CACHED
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = e.userMessage(),
                                syncWarning = null
                            )
                        }
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun onBiometricToggle(enabled: Boolean) {
        if (!enabled) {
            viewModelScope.launch {
                authRepository.disableBiometricLogin()
                _state.update {
                    it.copy(
                        biometricEnabled = false,
                        biometricMessage = null,
                        showBiometricPasswordPrompt = false,
                    )
                }
            }
            return
        }
        if (!authRepository.isBiometricAvailable()) {
            _state.update {
                it.copy(biometricMessage = "Biometrics are not available on this device.")
            }
            return
        }
        _state.update {
            it.copy(
                showBiometricPasswordPrompt = true,
                biometricMessage = null,
            )
        }
    }

    fun confirmEnableBiometric(password: String) {
        val user = _state.value.user ?: SessionManager.currentUser.value
        val identifier = user?.email?.takeIf { it.isNotBlank() }
            ?: user?.username.orEmpty()
        viewModelScope.launch {
            authRepository.enableBiometricLogin(identifier, password).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            biometricEnabled = true,
                            showBiometricPasswordPrompt = false,
                            biometricMessage = "Biometric login enabled on this device.",
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            biometricEnabled = false,
                            showBiometricPasswordPrompt = false,
                            biometricMessage = e.message?.trim()?.takeIf { msg -> msg.isNotEmpty() }
                                ?: e.userMessage(),
                        )
                    }
                }
            )
        }
    }

    fun dismissBiometricPasswordPrompt() {
        _state.update { it.copy(showBiometricPasswordPrompt = false) }
    }

    fun sendPasswordReset() {
        val email = _state.value.user?.email?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    _state.update {
                        it.copy(accountMessage = "Password reset email sent to $email.")
                    }
                },
                onFailure = { error ->
                    val message = if (error is UnsupportedOperationException) {
                        "Password reset is available in the HavenPad phone app."
                    } else {
                        error.userMessage()
                    }
                    _state.update { it.copy(accountMessage = message) }
                }
            )
        }
    }

    private fun refreshBiometricState() {
        _state.update {
            it.copy(
                biometricAvailable = authRepository.isBiometricAvailable(),
                biometricEnabled = authRepository.isBiometricEnabled(),
            )
        }
    }
}

class EditProfileViewModel(
    private val authRepository: com.rotd.havenpad.domain.repository.AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private val _savedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvents = _savedEvents.asSharedFlow()

    init {
        load()
    }

    /** Clear sticky post-save flag so reopening Edit Profile does not flash away. */
    fun onScreenVisible() {
        if (_state.value.completed) {
            _state.update { it.copy(completed = false, isSaving = false) }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, completed = false) }
            val sessionUser = SessionManager.currentUser.value
            if (sessionUser != null) {
                applyUser(sessionUser)
            }
            authRepository.refreshProfile()
                .onSuccess { user ->
                    applyUser(user)
                }
                .onFailure { e ->
                    if (sessionUser == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = e.userMessage()
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    private fun applyUser(user: User) {
        _state.update {
            it.copy(
                isLoading = false,
                user = user,
                draft = ProfileEditDraft(
                    fullName = user.fullName,
                    username = user.username,
                    phone = user.phone.orEmpty(),
                    businessName = user.businessName.orEmpty(),
                    major = user.major.orEmpty(),
                    yearOfStudy = user.yearOfStudy?.toString().orEmpty(),
                    roommatePreferencesText = user.roommatePreferences.joinToString(", ")
                ),
                completed = false
            )
        }
    }

    fun updateDraft(transform: (ProfileEditDraft) -> ProfileEditDraft) {
        _state.update {
            it.copy(draft = transform(it.draft), error = null)
        }
    }

    fun saveProfile() {
        val draft = _state.value.draft
        val user = _state.value.user ?: return
        if (draft.fullName.isBlank() || draft.username.isBlank()) {
            _state.update { it.copy(error = "Name and username are required") }
            return
        }
        if (_state.value.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val request = UpdateProfileRequest(
                fullName = draft.fullName.trim(),
                username = draft.username.trim(),
                phone = draft.phone.trim().ifBlank { null },
                businessName = when (user.role) {
                    UserRole.LANDLORD, UserRole.ADMIN ->
                        draft.businessName.trim().ifBlank { null }
                    else -> null
                },
                major = when (user.role) {
                    UserRole.STUDENT -> draft.major.trim().ifBlank { null }
                    else -> null
                },
                yearOfStudy = when (user.role) {
                    UserRole.STUDENT -> draft.yearOfStudy.trim().toIntOrNull()
                    else -> null
                },
                roommatePreferences = when (user.role) {
                    UserRole.STUDENT -> RoommatePreferences.normalize(draft.roommatePreferencesText)
                    else -> emptyList()
                }
            )

            authRepository.updateProfile(request)
                .onSuccess { updated ->
                    _state.update {
                        it.copy(
                            user = updated,
                            isSaving = false,
                            completed = true
                        )
                    }
                    _savedEvents.tryEmit(Unit)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            error = e.userMessage()
                        )
                    }
                }
        }
    }
}
