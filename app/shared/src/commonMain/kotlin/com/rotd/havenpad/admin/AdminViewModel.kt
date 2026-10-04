package com.rotd.havenpad.admin

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.AdminUserDetail
import com.rotd.havenpad.AdminUserSummary
import com.rotd.havenpad.CreateLandlordRequest
import com.rotd.havenpad.StayReview
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.media.decodeToImageBitmap
import com.rotd.havenpad.network.AdminClient
import com.rotd.havenpad.network.ReviewClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminListSection { LANDLORDS, STUDENTS, REPORTS }

data class AdminUiState(
    val role: UserRole = UserRole.LANDLORD,
    val section: AdminListSection = AdminListSection.LANDLORDS,
    val reports: List<StayReview> = emptyList(),
    val loading: Boolean = true,
    val users: List<AdminUserSummary> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val detail: AdminUserDetail? = null,
    val detailLoading: Boolean = false,
    val idImage: ImageBitmap? = null,
    val addressImage: ImageBitmap? = null,
    val busy: Boolean = false,
)

class AdminViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    init {
        load(UserRole.LANDLORD)
    }

    fun load(role: UserRole) {
        viewModelScope.launch {
            val section = if (role == UserRole.STUDENT) AdminListSection.STUDENTS else AdminListSection.LANDLORDS
            _state.update { it.copy(role = role, section = section, loading = true, error = null, detail = null) }
            AdminClient.listUsers(role).fold(
                onSuccess = { users -> _state.update { it.copy(loading = false, users = users) } },
                onFailure = { error -> _state.update { it.copy(loading = false, error = error.userMessage()) } },
            )
        }
    }

    fun open(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(detailLoading = true, error = null, idImage = null, addressImage = null) }
            AdminClient.userDetail(userId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(detailLoading = false, detail = detail) }
                    loadImages(detail)
                },
                onFailure = { error ->
                    _state.update { it.copy(detailLoading = false, error = error.userMessage()) }
                },
            )
        }
    }

    fun closeDetail() {
        _state.update { it.copy(detail = null, idImage = null, addressImage = null, message = null) }
    }

    fun verify() {
        val id = _state.value.detail?.user?.id ?: return
        act(id) { AdminClient.verify(id) }
    }

    fun reject(note: String) {
        val id = _state.value.detail?.user?.id ?: return
        act(id) { AdminClient.reject(id, note) }
    }

    fun remove() {
        val id = _state.value.detail?.user?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            AdminClient.remove(id).fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, detail = null, message = "Account removed") }
                    load(_state.value.role)
                },
                onFailure = { error ->
                    _state.update { it.copy(busy = false, error = error.userMessage()) }
                },
            )
        }
    }

    fun addLandlord(fullName: String, username: String, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            AdminClient.addLandlord(
                CreateLandlordRequest(fullName.trim(), username.trim(), email.trim(), password)
            ).fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, message = "Landlord added", role = UserRole.LANDLORD) }
                    load(UserRole.LANDLORD)
                },
                onFailure = { error ->
                    _state.update { it.copy(busy = false, error = error.userMessage()) }
                },
            )
        }
    }

    fun openReports() {
        viewModelScope.launch {
            _state.update {
                it.copy(section = AdminListSection.REPORTS, loading = true, error = null, detail = null)
            }
            ReviewClient.reports().fold(
                onSuccess = { reports -> _state.update { it.copy(loading = false, reports = reports) } },
                onFailure = { error -> _state.update { it.copy(loading = false, error = error.userMessage()) } },
            )
        }
    }

    fun followUp(reviewId: String, note: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            ReviewClient.followUp(reviewId, note).fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, message = "Follow-up saved for the student.") }
                    openReports()
                },
                onFailure = { error -> _state.update { it.copy(busy = false, error = error.userMessage()) } },
            )
        }
    }

    fun dismissReport(reviewId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            ReviewClient.dismiss(reviewId).fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, message = "Review kept on HavenPad.") }
                    openReports()
                },
                onFailure = { error -> _state.update { it.copy(busy = false, error = error.userMessage()) } },
            )
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            ReviewClient.delete(reviewId).fold(
                onSuccess = {
                    _state.update { it.copy(busy = false, message = "Review deleted.") }
                    openReports()
                },
                onFailure = { error -> _state.update { it.copy(busy = false, error = error.userMessage()) } },
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    private fun act(id: String, call: suspend () -> Result<AdminUserDetail>) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            call().fold(
                onSuccess = { detail ->
                    _state.update { it.copy(busy = false, detail = detail, message = "Review saved") }
                    loadImages(detail)
                },
                onFailure = { error ->
                    _state.update { it.copy(busy = false, error = error.userMessage()) }
                },
            )
        }
    }

    private fun loadImages(detail: AdminUserDetail) {
        viewModelScope.launch {
            val (idBitmap, addressBitmap) = coroutineScope {
                val id = async {
                    if (!detail.hasIdCopy) null
                    else AdminClient.kycImage(detail.user.id, "id").getOrNull()?.decodeToImageBitmap()
                }
                val address = async {
                    if (!detail.hasAddressProof) null
                    else AdminClient.kycImage(detail.user.id, "address").getOrNull()?.decodeToImageBitmap()
                }
                id.await() to address.await()
            }
            if (_state.value.detail?.user?.id == detail.user.id) {
                _state.update { it.copy(idImage = idBitmap, addressImage = addressBitmap) }
            }
        }
    }
}
