package com.rotd.kejasafe.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.EscrowState
import com.rotd.kejasafe.EscrowSummary
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.utils.formatEpoch

@Composable
fun EscrowDetailsDialog(
    summary: EscrowSummary?,
    isActing: Boolean,
    error: String?,
    onDispute: ((String, String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showDisputeForm by remember { mutableStateOf(false) }
    var disputeReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isActing) onDismiss() },
        title = { Text("Escrow Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (summary == null) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text("Transaction ID: ${summary.transactionId}", style = MaterialTheme.typography.labelSmall)
                    Text("State: ${summary.state.name.replace('_', ' ')}", fontWeight = FontWeight.SemiBold)
                    
                    KejaSafeCard {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            EscrowAmountRow("Total Amount", "KES ${(summary.totalAmountCents / 100)}")
                            
                            // Only show KejaSafe fee to the landlord (business logic transparency)
                            val user = SessionManager.currentUser.value
                            if (user?.role == UserRole.LANDLORD || user?.role == UserRole.ADMIN) {
                                EscrowAmountRow("KejaSafe Fee (5%)", "KES ${(summary.commissionCents / 100)}")
                                EscrowAmountRow("Landlord Payout", "KES ${(summary.landlordShareCents / 100)}", isHighlight = true)
                            } else {
                                Text(
                                    "This amount is held safely in escrow and will be released to the landlord only after you confirm move-in.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    if (summary.logs.isNotEmpty()) {
                        Text("Audit Trail", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            summary.logs.take(5).forEach { log ->
                                Column {
                                    Text(log.message, style = MaterialTheme.typography.bodySmall)
                                    Text(formatEpoch(log.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (showDisputeForm && onDispute != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Raise a Dispute", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        OutlinedTextField(
                            value = disputeReason,
                            onValueChange = { disputeReason = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Reason for dispute...") },
                            enabled = !isActing
                        )
                    }

                    if (!error.isNullOrBlank()) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (summary != null && onDispute != null) {
                if (showDisputeForm) {
                    TextButton(onClick = { onDispute(summary.transactionId, disputeReason) }, enabled = !isActing && disputeReason.isNotBlank()) {
                        Text("Submit Dispute", color = MaterialTheme.colorScheme.error)
                    }
                } else if (summary.state == EscrowState.HELD_IN_ESCROW || summary.state == EscrowState.RELEASE_PENDING) {
                    TextButton(onClick = { showDisputeForm = true }, enabled = !isActing) {
                        Text("Dispute Payment", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isActing) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun EscrowAmountRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium)
    }
}

