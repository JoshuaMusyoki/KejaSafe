package com.rotd.kejasafe.chat.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.domain.chat.MessageStatus

private val ReadTickBlue = Color(0xFF34B7F1)

@Composable
fun MessageReceiptTicks(
    status: MessageStatus,
    modifier: Modifier = Modifier,
    contentDescription: String? = receiptDescription(status),
) {
    val tint = when (status) {
        MessageStatus.READ -> ReadTickBlue
        MessageStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
    val icon = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule
        MessageStatus.SENT, MessageStatus.FAILED -> Icons.Default.Done
        MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

fun receiptDescription(status: MessageStatus): String = when (status) {
    MessageStatus.SENDING -> "Sending"
    MessageStatus.SENT -> "Sent"
    MessageStatus.DELIVERED -> "Delivered"
    MessageStatus.READ -> "Read"
    MessageStatus.FAILED -> "Failed"
}
