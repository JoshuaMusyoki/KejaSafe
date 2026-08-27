package com.rotd.kejasafe.rent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.kejasafe.BillItem
import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.UpdateBillRequest
import com.rotd.kejasafe.data.RentRepository
import com.rotd.kejasafe.error.userMessage
import com.rotd.kejasafe.network.ConnectivityMonitor
import com.rotd.kejasafe.network.ListingClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditableBillLine(
    val id: String,
    val name: String,
    val amount: String
)

sealed interface RentState {
    data object Loading : RentState
    data class Success(
        val bills: List<RentBill>,
        val message: String? = null,
        val syncWarning: String? = null,
        val payingBillId: String? = null,
        val requestingBillId: String? = null,
        val editingBillId: String? = null,
        val isSavingEdit: Boolean = false
    ) : RentState
    data class Error(val message: String) : RentState
}

class RentViewModel(
    private val rentRepository: RentRepository
) : ViewModel() {
    private val _state = MutableStateFlow<RentState>(RentState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ConnectivityMonitor.becameOnline.collect {
                loadBills()
            }
        }
    }

    fun loadBills() {
        viewModelScope.launch {
            val hadData = (_state.value as? RentState.Success)?.bills?.isNotEmpty() == true
            if (!hadData) {
                _state.value = RentState.Loading
            }
            rentRepository.getBills(
                onCacheHit = { cached ->
                    _state.value = RentState.Success(bills = cached)
                }
            )
                .onSuccess { offline ->
                    _state.value = RentState.Success(
                        bills = offline.value,
                        syncWarning = offline.syncWarning
                    )
                }
                .onFailure { e ->
                    val current = _state.value as? RentState.Success
                    if (current != null && current.bills.isNotEmpty()) {
                        _state.value = current.copy(syncWarning = e.userMessage())
                    } else {
                        _state.value = RentState.Error(e.userMessage())
                    }
                }
        }
    }

    fun payBill(billId: String) {
        viewModelScope.launch {
            val current = _state.value as? RentState.Success ?: return@launch
            _state.value = current.copy(payingBillId = billId, message = null)
            ListingClient.payBill(billId)
                .onSuccess {
                    rentRepository.getBills()
                        .onSuccess { offline ->
                            _state.value = RentState.Success(
                                bills = offline.value,
                                message = "Payment received. Thank you!",
                                syncWarning = offline.syncWarning
                            )
                        }
                        .onFailure {
                            loadBills()
                        }
                }
                .onFailure { e ->
                    _state.value = current.copy(
                        payingBillId = null,
                        message = e.userMessage()
                    )
                }
        }
    }

    fun requestPayment(billId: String) {
        viewModelScope.launch {
            val current = _state.value as? RentState.Success ?: return@launch
            _state.value = current.copy(requestingBillId = billId, message = null)
            ListingClient.requestPayment(billId)
                .onSuccess { updated ->
                    val bills = current.bills.map { if (it.id == updated.id) updated else it }
                    _state.value = RentState.Success(
                        bills = bills,
                        message = "Payment request sent to tenant"
                    )
                }
                .onFailure { e ->
                    _state.value = current.copy(
                        requestingBillId = null,
                        message = e.userMessage()
                    )
                }
        }
    }

    fun updateBill(
        billId: String,
        lines: List<EditableBillLine>,
        electricityNote: String?
    ) {
        viewModelScope.launch {
            val current = _state.value as? RentState.Success ?: return@launch
            val items = lines.mapNotNull { line ->
                val name = line.name.trim()
                val amount = line.amount.trim().toDoubleOrNull() ?: return@mapNotNull null
                if (name.isBlank()) null else BillItem(name, amount)
            }
            if (items.isEmpty()) {
                _state.value = current.copy(message = "Add at least one bill line with an amount")
                return@launch
            }
            _state.value = current.copy(isSavingEdit = true, editingBillId = billId, message = null)
            ListingClient.updateBill(
                billId = billId,
                request = UpdateBillRequest(
                    items = items,
                    electricityNote = electricityNote?.trim()?.ifBlank { null }
                )
            )
                .onSuccess { updated ->
                    val bills = current.bills.map { if (it.id == updated.id) updated else it }
                    _state.value = RentState.Success(
                        bills = bills,
                        message = "Bill updated — water & utilities saved"
                    )
                }
                .onFailure { e ->
                    _state.value = current.copy(
                        isSavingEdit = false,
                        editingBillId = null,
                        message = e.userMessage()
                    )
                }
        }
    }

    fun downloadPdf(billId: String, save: (String, ByteArray) -> String) {
        viewModelScope.launch {
            val current = _state.value
            ListingClient.downloadBillPdf(billId)
                .onSuccess { bytes ->
                    val message = runCatching {
                        save("kejasafe-bill-$billId.pdf", bytes)
                    }.getOrElse { e -> e.userMessage() }
                    val bills = (current as? RentState.Success)?.bills
                        ?: rentRepository.getBills().getOrNull()?.value.orEmpty()
                    _state.value = RentState.Success(bills, message)
                }
                .onFailure { e ->
                    _state.value = RentState.Error(e.userMessage())
                }
        }
    }
}
