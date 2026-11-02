package com.rotd.havenpad.rent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.rotd.havenpad.BillStatus
import com.rotd.havenpad.ElectricityBilling
import com.rotd.havenpad.RentBill
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.components.BillsListShimmer
import com.rotd.havenpad.components.HavenPadBody
import com.rotd.havenpad.components.HavenPadCard
import com.rotd.havenpad.components.HavenPadErrorState
import com.rotd.havenpad.components.HavenPadFloatingNavBar
import com.rotd.havenpad.components.HavenPadHeader
import com.rotd.havenpad.components.HavenPadHeadline
import com.rotd.havenpad.components.HavenPadLabel
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.HavenPadOfflineBanner
import com.rotd.havenpad.components.HavenPadOutlinedButton
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.media.rememberContactPhonePicker
import com.rotd.havenpad.media.rememberPdfShareHandler
import com.rotd.havenpad.network.SessionManager
import kotlin.random.Random

@Composable
fun RentScreen(
    selectedTab: HavenPadNavTab = HavenPadNavTab.BILLS,
    onTabSelected: (HavenPadNavTab) -> Unit,
    onCreateListingClick: (() -> Unit)? = null,
    viewModel: RentViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val user by SessionManager.currentUser.collectAsState()
    val isLandlord = user?.role == UserRole.LANDLORD || user?.role == UserRole.ADMIN
    val savePdf = rememberPdfShareHandler()
    var payConfirmBill by remember { mutableStateOf<RentBill?>(null) }
    var payPhone by remember { mutableStateOf("") }
    var payAmountText by remember { mutableStateOf("") }
    var editBill by remember { mutableStateOf<RentBill?>(null) }
    val pickContactPhone = rememberContactPhonePicker { number ->
        payPhone = number.filter { it.isDigit() || it == '+' }
    }

    LaunchedEffect(Unit) {
        viewModel.loadBills()
    }

    // Preview first — download only when user taps Download on this screen.
    (state as? RentState.Success)?.previewBill?.let { bill ->
        val success = state as RentState.Success
        val needsPay = !isLandlord &&
            (bill.status != BillStatus.PAID || bill.mpesaReceiptCode.isNullOrBlank())
        InvoicePreviewScreen(
            bill = bill,
            pdfBytes = success.previewPdfBytes,
            isLoadingPdf = success.isLoadingPreviewPdf,
            pdfError = success.previewPdfError,
            canRetryPayment = needsPay,
            onBack = viewModel::dismissInvoicePreview,
            onDownload = {
                val latest = viewModel.state.value as? RentState.Success
                val bytes = latest?.previewPdfBytes
                    ?: error("PDF is still loading")
                val name = latest.previewPdfFileName ?: "HavenPad-Invoice.pdf"
                savePdf(name, bytes)
            },
            onRetryPayment = {
                viewModel.dismissInvoicePreview()
                payPhone = viewModel.defaultPayPhone()
                payAmountText = bill.remainingAmount.toLong().coerceAtLeast(1L).toString()
                payConfirmBill = bill
            }
        )
        return
    }

    payConfirmBill?.let { bill ->
        val remaining = bill.remainingAmount.toLong().coerceAtLeast(1L)
        AlertDialog(
            onDismissRequest = { payConfirmBill = null },
            title = { Text("Pay with M-Pesa", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        buildString {
                            append("Balance KES $remaining for ${bill.propertyTitle.ifBlank { "this bill" }}.")
                            if (bill.amountPaid > 0) {
                                append(" Already paid KES ${bill.amountPaid.toLong()}.")
                            }
                            append(" You'll get an STK Push — enter your M-Pesa PIN on your phone.")
                        }
                    )
                    bill.paymentFailureReason?.takeIf { it.isNotBlank() }?.let { reason ->
                        Text(
                            reason,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { value ->
                            payAmountText = value.filter { it.isDigit() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (KES)") },
                        placeholder = { Text(remaining.toString()) },
                        supportingText = { Text("Pay in full or enter a smaller amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = payPhone,
                        onValueChange = { payPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("M-Pesa phone") },
                        placeholder = { Text("07xxxxxxxx") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        trailingIcon = {
                            TextButton(onClick = pickContactPhone) {
                                Text("Contacts")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phone = payPhone
                        val id = bill.id
                        val amount = payAmountText.toDoubleOrNull()
                            ?.takeIf { it > 0 }
                            ?: bill.remainingAmount
                        payConfirmBill = null
                        viewModel.payBill(id, phoneNumber = phone, amount = amount)
                    }
                ) {
                    Text("Send M-Pesa prompt")
                }
            },
            dismissButton = {
                TextButton(onClick = { payConfirmBill = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    editBill?.let { bill ->
        val success = state as? RentState.Success
        var awaitingSave by remember(bill.id) { mutableStateOf(false) }
        EditBillDialog(
            bill = bill,
            isSaving = success?.isSavingEdit == true && success.editingBillId == bill.id,
            onDismiss = { if (!awaitingSave) editBill = null },
            onSave = { lines, note ->
                awaitingSave = true
                viewModel.updateBill(bill.id, lines, note, onSuccess = {
                    editBill = null
                })
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HavenPadHeader(
                title = if (isLandlord) "Issued Bills" else "Rent & Bills"
            )
        },
        bottomBar = {
            HavenPadFloatingNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onCreateListingClick = if (isLandlord) onCreateListingClick else null
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is RentState.Loading -> {
                    BillsListShimmer(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopCenter)
                    )
                }
                is RentState.Error -> {
                    HavenPadErrorState(
                        message = s.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        onRetry = { viewModel.loadBills() }
                    )
                }
                is RentState.Success -> {
                    if (s.bills.isEmpty()) {
                        HavenPadBody(
                            text = if (isLandlord) {
                                "No bills yet. Set charges on each listing, approve a stay, then generate a bill."
                            } else {
                                "No bills yet. After your landlord approves your booking, they can issue rent."
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (!s.syncWarning.isNullOrBlank()) {
                                item {
                                    HavenPadOfflineBanner(
                                        message = s.syncWarning.orEmpty(),
                                        onRetry = { viewModel.loadBills() }
                                    )
                                }
                            }
                            if (!s.message.isNullOrBlank()) {
                                item {
                                    Text(
                                        text = s.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            items(s.bills, key = { it.id }) { bill ->
                                val unpaid = bill.status != BillStatus.PAID ||
                                    bill.mpesaReceiptCode.isNullOrBlank()
                                BillCard(
                                    bill = bill,
                                    isLandlord = isLandlord,
                                    canPay = !isLandlord && unpaid,
                                    paying = s.payingBillId == bill.id,
                                    requesting = s.requestingBillId == bill.id,
                                    onPay = {
                                        payPhone = viewModel.defaultPayPhone()
                                        payAmountText = bill.remainingAmount.toLong()
                                            .coerceAtLeast(1L)
                                            .toString()
                                        payConfirmBill = bill
                                    },
                                    onRequestPayment = { viewModel.requestPayment(bill.id) },
                                    onEdit = { editBill = bill },
                                    onViewInvoice = { viewModel.previewInvoice(bill.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditBillDialog(
    bill: RentBill,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<EditableBillLine>, String?) -> Unit
) {
    val existingWaterAmount = remember(bill.id) {
        bill.items.firstOrNull { it.name.contains("water", ignoreCase = true) }?.amount ?: 0.0
    }
    val initialWaterUnits = if (bill.waterPricePerUnit > 0 && existingWaterAmount > 0) {
        (existingWaterAmount / bill.waterPricePerUnit).toInt().toString()
    } else {
        if (existingWaterAmount > 0) existingWaterAmount.toInt().toString() else "0"
    }
    var waterUnits by remember(bill.id) { mutableStateOf(initialWaterUnits) }

    val existingElecAmount = remember(bill.id) {
        bill.items.firstOrNull { it.name.contains("electricity", ignoreCase = true) && !it.name.contains("token", ignoreCase = true) }?.amount ?: 0.0
    }
    var electricityAmount by remember(bill.id) { mutableStateOf(existingElecAmount.toInt().toString()) }

    val otherLines = remember(bill.id) {
        mutableStateListOf<EditableBillLine>().apply {
            addAll(
                bill.items
                    .filter { !it.name.contains("water", ignoreCase = true) && !it.name.contains("electricity", ignoreCase = true) }
                    .mapIndexed { index, item ->
                        EditableBillLine(
                            id = "line_$index",
                            name = item.name,
                            amount = if (item.amount == 0.0) "0" else item.amount.toInt().toString()
                        )
                    }
            )
            if (none { it.name.contains("rent", ignoreCase = true) }) {
                add(0, EditableBillLine(id = "rent", name = "Base rent", amount = ""))
            }
        }
    }

    var electricityNote by remember(bill.id) {
        mutableStateOf(bill.electricityNote.orEmpty())
    }

    val calculatedWaterAmount = (waterUnits.toDoubleOrNull() ?: 0.0) * (if (bill.waterPricePerUnit > 0) bill.waterPricePerUnit else 0.0)
    val calculatedElecAmount = if (bill.electricityBilling == ElectricityBilling.MONTHLY) {
        electricityAmount.toDoubleOrNull() ?: 0.0
    } else 0.0

    val previewTotal = calculatedWaterAmount +
        calculatedElecAmount +
        otherLines.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Column {
                Text("Modify invoice & utilities", fontWeight = FontWeight.SemiBold)
                Text(
                    text = buildString {
                        append("${monthName(bill.month)} ${bill.year}")
                        if (bill.propertyTitle.isNotBlank()) append(" · ${bill.propertyTitle}")
                        if (bill.tenantName.isNotBlank()) append(" · ${bill.tenantName}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HavenPadLabel(
                    text = "Enter units consumed for water and monthly charges for electricity (if not token)."
                )

                if (bill.waterPricePerUnit > 0) {
                    OutlinedTextField(
                        value = waterUnits,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.all { it.isDigit() }) {
                                waterUnits = value
                            }
                        },
                        label = { Text("Water units consumed") },
                        supportingText = { Text("Rate: KES ${bill.waterPricePerUnit.toInt()} per unit · Total: KES ${calculatedWaterAmount.toInt()}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    var fallbackWaterAmount by remember(bill.id) { mutableStateOf(existingWaterAmount.toInt().toString()) }
                    OutlinedTextField(
                        value = fallbackWaterAmount,
                        onValueChange = { fallbackWaterAmount = it },
                        label = { Text("Water amount (KES)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                if (bill.electricityBilling == ElectricityBilling.MONTHLY) {
                    OutlinedTextField(
                        value = electricityAmount,
                        onValueChange = { electricityAmount = it },
                        label = { Text("Electricity (KES / mo)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                } else {
                    Text(
                        text = "Electricity: Prepaid token (tenant tops up separately)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()
                Text("Other Charges & Rent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                otherLines.forEachIndexed { index, line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = line.name,
                            onValueChange = { otherLines[index] = line.copy(name = it) },
                            label = { Text("Item") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = line.amount,
                            onValueChange = { value ->
                                if (value.isEmpty() || value.all { it.isDigit() || it == '.' }) {
                                    otherLines[index] = line.copy(amount = value)
                                }
                            },
                            label = { Text("KES") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
                TextButton(
                    onClick = {
                        otherLines.add(
                            EditableBillLine(
                                id = "line_${Random.nextLong().toULong().toString(16)}",
                                name = "",
                                amount = ""
                            )
                        )
                    }
                ) {
                    Text("Add line item")
                }

                OutlinedTextField(
                    value = electricityNote,
                    onValueChange = { electricityNote = it },
                    label = { Text("Invoice / Electricity note (optional)") },
                    placeholder = { Text("e.g. meter reading or token details") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
                Text(
                    text = "New invoice total: KES ${previewTotal.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalLines = buildList {
                        val wUnits = waterUnits.toDoubleOrNull() ?: 0.0
                        val wAmt = if (bill.waterPricePerUnit > 0) wUnits * bill.waterPricePerUnit else (waterUnits.toDoubleOrNull() ?: 0.0)
                        if (wAmt > 0 || bill.waterPricePerUnit > 0) {
                            add(EditableBillLine("water", "Water (${wUnits.toInt()} units @ ${bill.waterPricePerUnit.toInt()}/u)", wAmt.toString()))
                        }
                        if (bill.electricityBilling == ElectricityBilling.MONTHLY) {
                            val eAmt = electricityAmount.toDoubleOrNull() ?: 0.0
                            if (eAmt > 0) {
                                add(EditableBillLine("elec", "Electricity", eAmt.toString()))
                            }
                        }
                        addAll(otherLines)
                    }
                    onSave(finalLines, electricityNote)
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving…" else "Send updated invoice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}



@Composable
private fun BillCard(
    bill: RentBill,
    isLandlord: Boolean,
    canPay: Boolean,
    paying: Boolean,
    requesting: Boolean,
    onPay: () -> Unit,
    onRequestPayment: () -> Unit,
    onEdit: () -> Unit,
    onViewInvoice: () -> Unit
) {
    HavenPadCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    HavenPadHeadline(text = "KES ${bill.totalAmount.toInt()}")
                    if (bill.amountPaid > 0 && bill.status != BillStatus.PAID) {
                        HavenPadLabel(text = "Paid KES ${bill.amountPaid.toLong()} · Left KES ${bill.remainingAmount.toLong()}")
                    }
                }
                StatusBadge(bill.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            val propertyLine = listOfNotNull(
                bill.propertyTitle.takeIf { it.isNotBlank() },
                bill.tenantName.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (propertyLine.isNotBlank()) {
                HavenPadBody(text = propertyLine)
                Spacer(modifier = Modifier.height(4.dp))
            }
            HavenPadLabel(text = "${monthName(bill.month)} ${bill.year} · Due: ${formatDue(bill.dueDate)}")
            bill.electricityNote?.let {
                Spacer(modifier = Modifier.height(4.dp))
                HavenPadLabel(text = it)
            }
            if (bill.depositIncluded) {
                Spacer(modifier = Modifier.height(4.dp))
                HavenPadLabel(text = "Includes security deposit")
            }
            bill.paymentFailureReason
                ?.takeIf {
                    it.isNotBlank() &&
                        bill.status in listOf(BillStatus.FAILED, BillStatus.CANCELLED)
                }
                ?.let { reason ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

            if (bill.paymentRequested && bill.status != BillStatus.PAID) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isLandlord) {
                        "Payment requested — waiting on tenant"
                    } else {
                        "Your landlord requested payment for this bill"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            bill.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HavenPadBody(text = item.name)
                    HavenPadBody(
                        text = if (item.amount <= 0.0 && item.name.contains("token", ignoreCase = true)) {
                            "TOKEN"
                        } else {
                            "KES ${item.amount.toInt()}"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HavenPadOutlinedButton(
                text = "Preview invoice",
                onClick = onViewInvoice,
                modifier = Modifier.fillMaxWidth()
            )

            if (canPay) {
                Spacer(modifier = Modifier.height(10.dp))
                HavenPadPrimaryButton(
                    text = if (paying) {
                        "Waiting for M-Pesa…"
                    } else if (bill.paymentRequested) {
                        "Pay now (requested)"
                    } else {
                        "Pay with M-PESA"
                    },
                    onClick = onPay,
                    enabled = !paying,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isLandlord && bill.status != BillStatus.PAID) {
                Spacer(modifier = Modifier.height(10.dp))
                HavenPadOutlinedButton(
                    text = "Edit utilities",
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (bill.paymentRequested) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Payment requested")
                    }
                } else {
                    HavenPadPrimaryButton(
                        text = if (requesting) "Sending…" else "Request payment",
                        onClick = onRequestPayment,
                        enabled = !requesting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BillStatus) {
    val (label, container) = when (status) {
        BillStatus.PAID -> "Paid" to MaterialTheme.colorScheme.primaryContainer
        BillStatus.PENDING_PAYMENT -> "Awaiting M-Pesa" to MaterialTheme.colorScheme.tertiaryContainer
        BillStatus.PARTIALLY_PAID -> "Partially paid" to MaterialTheme.colorScheme.secondaryContainer
        BillStatus.OVERDUE -> "Overdue" to MaterialTheme.colorScheme.errorContainer
        BillStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.errorContainer
        BillStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.errorContainer
        BillStatus.UNPAID -> "Unpaid" to MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(
        color = container,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    12 -> "December"
    else -> "Month $month"
}

internal fun formatDue(epochMs: Long): String {
    var z = epochMs / 86_400_000L + 719_468L
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
    var y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + if (mp < 10) 3 else -9
    y += if (m <= 2) 1 else 0
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}
