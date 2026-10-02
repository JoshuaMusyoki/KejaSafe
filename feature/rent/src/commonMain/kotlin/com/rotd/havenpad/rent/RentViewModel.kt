package com.rotd.havenpad.rent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rotd.havenpad.BillItem
import com.rotd.havenpad.BillStatus
import com.rotd.havenpad.RentBill
import com.rotd.havenpad.UpdateBillRequest
import com.rotd.havenpad.data.RentRepository
import com.rotd.havenpad.error.userMessage
import com.rotd.havenpad.media.InvoicePdfGenerator
import com.rotd.havenpad.network.ConnectivityMonitor
import com.rotd.havenpad.network.ListingClient
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.delay
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
        val isSavingEdit: Boolean = false,
        /** Bill opened in invoice preview (shown immediately). */
        val previewBill: RentBill? = null,
        val previewPdfBytes: ByteArray? = null,
        val previewPdfFileName: String? = null,
        val isLoadingPreviewPdf: Boolean = false,
        val previewPdfError: String? = null
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
                    val paying = (_state.value as? RentState.Success)?.payingBillId
                    _state.value = RentState.Success(
                        bills = offline.value,
                        syncWarning = offline.syncWarning,
                        payingBillId = paying,
                        message = (_state.value as? RentState.Success)?.message
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

    fun payBill(billId: String, phoneNumber: String, amount: Double = 0.0) {
        viewModelScope.launch {
            val current = _state.value as? RentState.Success ?: return@launch
            val phone = phoneNumber.trim()
            if (phone.length < 9) {
                _state.value = current.copy(message = "Enter a valid M-Pesa phone number")
                return@launch
            }
            _state.value = current.copy(payingBillId = billId, message = null)
            ListingClient.payBill(billId, phoneNumber = phone, amount = amount)
                .onSuccess { response ->
                    val paidBefore = current.bills.find { it.id == billId }?.amountPaid ?: 0.0
                    _state.value = current.copy(
                        payingBillId = billId,
                        message = response.message
                    )
                    pollBillPaid(billId, paidBefore)
                }
                .onFailure { e ->
                    _state.value = current.copy(
                        payingBillId = null,
                        message = e.userMessage()
                    )
                }
        }
    }

    private fun pollBillPaid(billId: String, paidBefore: Double) {
        viewModelScope.launch {
            repeat(40) {
                delay(3_000)
                val bills = rentRepository.getBills().getOrNull()?.value ?: return@repeat
                val bill = bills.find { it.id == billId }
                val current = _state.value as? RentState.Success
                if (bill?.status == BillStatus.PAID && !bill.mpesaReceiptCode.isNullOrBlank()) {
                    _state.value = RentState.Success(
                        bills = bills,
                        message = "Payment received. M-Pesa code: ${bill.mpesaReceiptCode}",
                        syncWarning = current?.syncWarning
                    )
                    return@launch
                }
                if (
                    bill?.status == BillStatus.PARTIALLY_PAID &&
                    bill.amountPaid > paidBefore &&
                    current?.payingBillId == billId
                ) {
                    _state.value = RentState.Success(
                        bills = bills,
                        message = "Partial payment received. Remaining KES ${bill.remainingAmount.toLong()}",
                        syncWarning = current.syncWarning
                    )
                    return@launch
                }
                if (
                    bill != null &&
                    current?.payingBillId == billId &&
                    bill.status in listOf(BillStatus.FAILED, BillStatus.CANCELLED)
                ) {
                    val reason = bill.paymentFailureReason
                        ?: when (bill.status) {
                            BillStatus.CANCELLED -> "Payment cancelled"
                            else -> "Payment failed"
                        }
                    _state.value = RentState.Success(
                        bills = bills,
                        message = reason,
                        syncWarning = current.syncWarning
                    )
                    return@launch
                }
                if (current != null) {
                    _state.value = current.copy(bills = bills, payingBillId = billId)
                }
            }
            val current = _state.value as? RentState.Success ?: return@launch
            _state.value = current.copy(
                payingBillId = null,
                message = "Still waiting for M-Pesa confirmation. Refresh bills in a moment."
            )
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

    fun previewInvoice(billId: String) {
        viewModelScope.launch {
            val current = _state.value as? RentState.Success ?: return@launch
            val bill = current.bills.find { it.id == billId } ?: return@launch
            val fileName = "HavenPad-Invoice-${bill.year}-${bill.month.toString().padStart(2, '0')}.pdf"
            // Build PDF on-device immediately so preview/download never depend on the API.
            val localPdf = runCatching {
                InvoicePdfGenerator.render(
                    bill = bill,
                    tenantName = bill.tenantName.ifBlank { "Tenant" }
                )
            }.getOrNull()

            _state.value = current.copy(
                previewBill = bill,
                previewPdfBytes = localPdf,
                previewPdfFileName = fileName,
                isLoadingPreviewPdf = localPdf == null,
                previewPdfError = if (localPdf == null) "Could not build invoice PDF" else null,
                message = null
            )

            // Optional: refresh from server if local build failed.
            if (localPdf == null) {
                ListingClient.downloadBillPdf(billId)
                    .onSuccess { bytes ->
                        val latest = _state.value as? RentState.Success ?: return@onSuccess
                        if (latest.previewBill?.id != billId) return@onSuccess
                        _state.value = latest.copy(
                            previewPdfBytes = bytes,
                            isLoadingPreviewPdf = false,
                            previewPdfError = null
                        )
                    }
                    .onFailure { e ->
                        val latest = _state.value as? RentState.Success ?: return@onFailure
                        if (latest.previewBill?.id != billId) return@onFailure
                        _state.value = latest.copy(
                            isLoadingPreviewPdf = false,
                            previewPdfError = e.userMessage()
                        )
                    }
            }
        }
    }

    fun dismissInvoicePreview() {
        val current = _state.value as? RentState.Success ?: return
        _state.value = current.copy(
            previewBill = null,
            previewPdfBytes = null,
            previewPdfFileName = null,
            isLoadingPreviewPdf = false,
            previewPdfError = null
        )
    }

    fun defaultPayPhone(): String =
        SessionManager.currentUser.value?.phone.orEmpty()
}
