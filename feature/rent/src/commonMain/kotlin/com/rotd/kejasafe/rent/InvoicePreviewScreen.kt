package com.rotd.kejasafe.rent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.BillStatus
import com.rotd.kejasafe.RentBill
import com.rotd.kejasafe.components.KejaSafePrimaryButton
import com.rotd.kejasafe.media.PdfDocumentViewer
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600
import kotlinx.coroutines.launch

/**
 * In-app invoice preview. Summary shows immediately; PDF document view appears when
 * bytes are ready. Download only runs when the user taps Download.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewScreen(
    bill: RentBill,
    pdfBytes: ByteArray?,
    isLoadingPdf: Boolean,
    pdfError: String?,
    canRetryPayment: Boolean,
    onBack: () -> Unit,
    onDownload: () -> String,
    onRetryPayment: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showPdfDocument by remember { mutableStateOf(false) }

    LaunchedEffect(pdfBytes) {
        if (pdfBytes != null) showPdfDocument = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Invoice preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pdfBytes != null) {
                        IconButton(
                            onClick = {
                                val message = runCatching { onDownload() }
                                    .getOrElse { it.message ?: "Could not save PDF" }
                                scope.launch { snackbar.showSnackbar(message) }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download PDF")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pdfBytes != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !showPdfDocument,
                                onClick = { showPdfDocument = false },
                                label = { Text("Summary") }
                            )
                            FilterChip(
                                selected = showPdfDocument,
                                onClick = { showPdfDocument = true },
                                label = { Text("PDF") }
                            )
                        }
                    }

                    val receipt = bill.mpesaReceiptCode?.takeIf { it.isNotBlank() }
                    val isPaid = bill.status == BillStatus.PAID && receipt != null
                    if (isPaid) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald700.copy(alpha = 0.12f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Payment confirmed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Emerald700,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "M-Pesa code: $receipt",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else if (canRetryPayment) {
                        KejaSafePrimaryButton(
                            text = when (bill.status) {
                                BillStatus.PENDING_PAYMENT -> "Retry M-Pesa payment"
                                else -> "Pay with M-Pesa"
                            },
                            onClick = onRetryPayment,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    com.rotd.kejasafe.components.KejaSafeOutlinedButton(
                        text = when {
                            isLoadingPdf -> "Preparing PDF…"
                            pdfBytes == null -> "Download unavailable"
                            else -> "Download PDF"
                        },
                        onClick = {
                            val message = runCatching { onDownload() }
                                .getOrElse { it.message ?: "Could not save PDF" }
                            scope.launch { snackbar.showSnackbar(message) }
                        },
                        enabled = pdfBytes != null && !isLoadingPdf,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showPdfDocument && pdfBytes != null) {
                PdfDocumentViewer(
                    bytes = pdfBytes,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                InvoiceSummaryPreview(
                    bill = bill,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }

            if (isLoadingPdf && pdfBytes == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Loading PDF…",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            if (!pdfError.isNullOrBlank() && pdfBytes == null) {
                Text(
                    text = pdfError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InvoiceSummaryPreview(
    bill: RentBill,
    modifier: Modifier = Modifier
) {
    val invoiceNo = buildString {
        append("INV-")
        append(bill.year)
        append(bill.month.toString().padStart(2, '0'))
        append("-")
        append(bill.id.filter { it.isLetterOrDigit() }.takeLast(6).uppercase())
    }
    val statusColor = when (bill.status) {
        BillStatus.PAID -> Emerald700
        BillStatus.PENDING_PAYMENT -> MaterialTheme.colorScheme.tertiary
        BillStatus.OVERDUE -> MaterialTheme.colorScheme.error
        BillStatus.UNPAID -> Gold600
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald700)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "KejaSafe",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "INVOICE",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    invoiceNo,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${invoiceMonthName(bill.month)} ${bill.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    bill.status.name,
                    color = statusColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoPanel(
                title = "Bill to",
                lines = listOf(
                    bill.tenantName.ifBlank { "Tenant" },
                    "Due: ${invoiceFormatDue(bill.dueDate)}"
                ),
                modifier = Modifier.weight(1f)
            )
            InfoPanel(
                title = "Property",
                lines = listOf(
                    bill.propertyTitle.ifBlank { bill.propertyId },
                    if (bill.depositIncluded) "Includes deposit" else ""
                ).filter { it.isNotBlank() },
                modifier = Modifier.weight(1f)
            )
        }

        Text("Charges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bill.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (item.amount <= 0.0 && item.name.contains("token", ignoreCase = true)) {
                            "TOKEN"
                        } else {
                            "KES ${item.amount.toInt()}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total due", fontWeight = FontWeight.Bold)
                Text(
                    "KES ${bill.totalAmount.toInt()}",
                    fontWeight = FontWeight.Bold,
                    color = Emerald700
                )
            }
        }

        bill.mpesaReceiptCode?.takeIf { it.isNotBlank() && bill.status == BillStatus.PAID }?.let { code ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Emerald700.copy(alpha = 0.12f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "M-Pesa confirmation",
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald700,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        bill.electricityNote?.takeIf { it.isNotBlank() }?.let {
            Text(
                "Note: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            "Preview first, then tap Download PDF when you want a file copy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoPanel(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Emerald700
        )
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun invoiceMonthName(month: Int): String = when (month) {
    1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
    5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
    9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
    else -> month.toString()
}

private fun invoiceFormatDue(epochMs: Long): String {
    val day = epochMs / 86_400_000L
    var days = day.toInt()
    var year = 1970
    while (true) {
        val diy = if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 366 else 365
        if (days < diy) break
        days -= diy
        year++
    }
    val leaps = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    val lengths = intArrayOf(31, if (leaps) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (len in lengths) {
        if (days < len) break
        days -= len
        month++
    }
    val d = days + 1
    return "${d.toString().padStart(2, '0')} ${invoiceMonthName(month).take(3)} $year"
}
