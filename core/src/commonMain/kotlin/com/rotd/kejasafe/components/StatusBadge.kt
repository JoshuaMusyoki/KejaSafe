package com.rotd.kejasafe.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.kejasafe.ListingStatus
import com.rotd.kejasafe.RequestType
import com.rotd.kejasafe.theme.Emerald700
import com.rotd.kejasafe.theme.Gold600

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ListingStatusBadge(
    status: ListingStatus,
    modifier: Modifier = Modifier
) {
    val (container, content, label) = when (status) {
        ListingStatus.VERIFIED -> Triple(
            Emerald700.copy(alpha = 0.14f),
            Emerald700,
            "Verified"
        )
        ListingStatus.PENDING -> Triple(
            Gold600.copy(alpha = 0.16f),
            Gold600,
            "In review"
        )
        ListingStatus.DRAFT -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Draft"
        )
        ListingStatus.REJECTED -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            "Rejected"
        )
    }
    StatusBadge(
        text = label,
        modifier = modifier,
        containerColor = container,
        contentColor = content
    )
}

@Composable
fun RequestTypeBadge(
    type: RequestType,
    modifier: Modifier = Modifier
) {
    val (container, content, label) = when (type) {
        RequestType.LIVE_INSPECTION -> Triple(
            Emerald700.copy(alpha = 0.12f),
            Emerald700,
            "Live Inspection"
        )
        RequestType.ROOMMATE_SPLIT -> Triple(
            Gold600.copy(alpha = 0.14f),
            Gold600,
            "Roommate Split"
        )
        RequestType.BOOKING -> Triple(
            Emerald700.copy(alpha = 0.12f),
            Emerald700,
            "Booking"
        )
        RequestType.GENERAL -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Request"
        )
    }
    StatusBadge(
        text = label,
        modifier = modifier,
        containerColor = container,
        contentColor = content
    )
}
