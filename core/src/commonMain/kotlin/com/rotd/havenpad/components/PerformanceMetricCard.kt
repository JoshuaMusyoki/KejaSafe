package com.rotd.havenpad.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.theme.Emerald700
import com.rotd.havenpad.theme.Gold600
import com.rotd.havenpad.theme.Slate200

/**
 * Full-width performance metric (analytics / detailed views).
 */
@Composable
fun PerformanceMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Emerald700,
    accentColor: Color? = null,
    badge: @Composable (() -> Unit)? = null,
    footer: String? = null,
    progress: Float? = null,
    segmentedProgress: Pair<Int, Int>? = null,
    trailingLabel: String? = null
) {
    HavenPadCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(96.dp)
                        .background(accentColor)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trailingLabel != null) {
                        Text(
                            text = trailingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    badge?.invoke()
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )

                if (footer != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = footer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (progress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressTrack(progress = progress, color = Emerald700)
                }

                if (segmentedProgress != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SegmentedProgress(
                        filled = segmentedProgress.first,
                        total = segmentedProgress.second,
                        color = Emerald700
                    )
                }
            }
        }
    }
}

/**
 * Dense marketplace-style metric tile for landlord home grids.
 * Same density language as property cards: compact header, hero value, short footer.
 */
@Composable
fun CompactPerformanceStatCard(
    title: String,
    value: String,
    valueColor: Color,
    footer: String,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
    accentColor: Color? = null,
    trailingLabel: String? = null,
    progress: Float? = null,
    segmentedProgress: Pair<Int, Int>? = null
) {
    HavenPadCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(108.dp)
                        .background(accentColor)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (trailingLabel != null) {
                        Text(
                            text = trailingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    badge?.invoke()
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                if (progress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressTrack(progress = progress, color = valueColor)
                }

                if (segmentedProgress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SegmentedProgress(
                        filled = segmentedProgress.first,
                        total = segmentedProgress.second,
                        color = valueColor
                    )
                }
            }
        }
    }
}

@Composable
fun ViewsMetricBadge(deltaPercent: Int) {
    SurfacePill(
        containerColor = Emerald700.copy(alpha = 0.12f),
        contentColor = Emerald700
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "+$deltaPercent%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NewInquiriesBadge() {
    SurfacePill(
        containerColor = Gold600.copy(alpha = 0.14f),
        contentColor = Gold600
    ) {
        Icon(
            imageVector = Icons.Default.MailOutline,
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "New",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SurfacePill(
    containerColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
            content = content
        )
    }
}

@Composable
private fun LinearProgressTrack(progress: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Slate200)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
private fun SegmentedProgress(filled: Int, total: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index < filled) color else Color(0xFFE8ECE9))
            )
        }
    }
}
