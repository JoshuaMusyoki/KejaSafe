package com.rotd.havenpad.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Drawn as icons so web, which has no star glyphs in its font, does not show empty boxes. */
@Composable
fun ReviewStars(
    rating: Int,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
) {
    val clamped = rating.coerceIn(0, 5)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (value in 1..5) {
            val filled = value <= clamped
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder
            val tint = if (filled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (onSelect == null) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (value == 1) "$clamped of 5 stars" else null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                IconButton(
                    onClick = { onSelect(value) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Rate $value of 5",
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
