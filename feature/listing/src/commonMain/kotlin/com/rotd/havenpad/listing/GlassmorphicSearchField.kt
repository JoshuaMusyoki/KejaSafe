package com.rotd.havenpad.listing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicSearchField(
    modifier: Modifier = Modifier,
    query: String,
    placeholder: String = "Start your search",
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = scheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(
            width = 1.dp,
            color = scheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = scheme.onSurface,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = query.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (query.isBlank()) FontWeight.Medium else FontWeight.SemiBold,
                color = if (query.isBlank()) {
                    scheme.onSurfaceVariant
                } else {
                    scheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
