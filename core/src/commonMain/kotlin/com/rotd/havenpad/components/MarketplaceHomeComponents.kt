package com.rotd.havenpad.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rotd.havenpad.ListingStatus
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.reviewRankLabel
import com.rotd.havenpad.PropertyType

enum class MarketplaceTileTone {
    PRIMARY,
    SECONDARY,
    NEUTRAL
}

@Composable
fun marketplaceTileColors(tone: MarketplaceTileTone): MarketplaceTileColorSet {
    val scheme = MaterialTheme.colorScheme
    val light = scheme.background.luminance() > 0.5f
    fun edge(fill: Color, darkBorder: Color): Color =
        if (light) fill.over(scheme.surface).darken(0.06f) else darkBorder

    return when (tone) {
        MarketplaceTileTone.PRIMARY -> {
            val fill = scheme.primaryContainer.copy(alpha = 0.55f)
            MarketplaceTileColorSet(
                container = fill,
                border = edge(fill, scheme.primaryContainer.copy(alpha = 0.35f)),
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                actionFill = scheme.primary.copy(alpha = 0.16f)
            )
        }
        MarketplaceTileTone.SECONDARY -> {
            val fill = scheme.secondaryContainer.copy(alpha = 0.65f)
            MarketplaceTileColorSet(
                container = fill,
                border = edge(fill, scheme.secondaryContainer.copy(alpha = 0.35f)),
                accent = scheme.secondary,
                onAccent = scheme.onSecondary,
                actionFill = scheme.secondary.copy(alpha = 0.16f)
            )
        }
        MarketplaceTileTone.NEUTRAL -> {
            val fill = scheme.surfaceVariant.copy(alpha = 0.75f)
            MarketplaceTileColorSet(
                container = fill,
                border = edge(fill, scheme.outlineVariant.copy(alpha = 0.45f)),
                accent = scheme.onSurface,
                onAccent = scheme.surface,
                actionFill = scheme.primary.copy(alpha = 0.14f)
            )
        }
    }
}

/** Opaque result of drawing this color over [background]. */
private fun Color.over(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1f - a),
        green = green * a + background.green * (1f - a),
        blue = blue * a + background.blue * (1f - a),
        alpha = 1f
    )
}

private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = 1f
)

data class MarketplaceTileColorSet(
    val container: Color,
    val border: Color,
    val accent: Color,
    val onAccent: Color,
    val actionFill: Color
)

/**
 * HavenPad-style compact dashboard tile: icon, headline value, short subtitle, pill action.
 * Colors follow Material theme so light/dark stay readable.
 */
@Composable
fun MarketplaceTileCard(
    title: String,
    value: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MarketplaceTileTone = MarketplaceTileTone.PRIMARY,
    icon: ImageVector? = null,
    badge: @Composable (() -> Unit)? = null
) {
    val colors = marketplaceTileColors(tone)

    Card(
        modifier = modifier.widthIn(min = 156.dp, max = 220.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.container),
        border = BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                badge?.invoke()
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            TextButton(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(colors.actionFill),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accent),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Centers marketplace tiles when they fit; scrolls horizontally when they overflow (mobile).
 */
@Composable
fun CenteredMarketplaceTileRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .widthIn(min = maxWidth)
                .horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Centered dialog on wide screens, bottom sheet on mobile — Uzilopp “floating detail card”.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HavenPadDetailPopup(
    title: String,
    onDismiss: () -> Unit,
    useDialog: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable (ColumnScope.() -> Unit)? = null
) {
    if (useDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HavenPadCard(
                    modifier = modifier
                        .widthIn(min = 320.dp, max = 480.dp)
                        .padding(24.dp)
                ) {
                    PopupHeader(title = title, onDismiss = onDismiss)
                    Column(
                        modifier = Modifier
                            .heightIn(max = 560.dp)
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .imePadding()
                            .verticalScroll(rememberScrollState()),
                        content = content
                    )
                    footer?.let {
                        Column(
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                            content = it
                        )
                    }
                }
            }
        }
    } else {
        HavenPadBottomSheet(onDismissRequest = onDismiss) {
            PopupHeader(title = title, onDismiss = onDismiss)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
            footer?.invoke(this)
        }
    }
}

@Composable
private fun PopupHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
fun MarketplaceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/**
 * Compact marketplace listing card for home grids and horizontal rows.
 */
@Composable
fun CompactListingCard(
    listing: PropertyListing,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
    distanceLabel: String? = null,
    trailingBadge: @Composable (() -> Unit)? = null,
    fullWidth: Boolean = false
) {
    val priceText = listing.pricePerMonth?.let {
        "${listing.currency} ${it.toInt()}/mo"
    } ?: "Price on request"
    val meta = listOfNotNull(
        listing.propertyType?.let { compactTypeLabel(it) },
        listing.unitsCount?.let { n -> "$n unit${if (n == 1) "" else "s"}" },
        distanceLabel
    ).joinToString(" · ")
    val scheme = MaterialTheme.colorScheme
    val sizedModifier = if (fullWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier.widthIn(min = 160.dp, max = 280.dp)
    }

    Card(
        modifier = sizedModifier
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.8f), MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = scheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            ListingPhoto(
                imageUrl = listing.imageUrl,
                contentDescription = listing.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                when {
                    trailingBadge != null -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            trailingBadge()
                        }
                    }
                    listing.status == ListingStatus.VERIFIED -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            ListingStatusBadge(status = ListingStatus.VERIFIED)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface
                )
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
                Text(
                    text = listing.reviewRankLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (listing.reviewCount > 0) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onView,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primary.copy(alpha = 0.14f)),
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.primary),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("View", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun <T> CenteredListingCardRow(
    items: List<T>,
    key: ((T) -> Any)? = null,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (key != null) {
            items(items, key = key) { itemContent(it) }
        } else {
            items(items) { itemContent(it) }
        }
    }
}

private fun compactTypeLabel(type: PropertyType): String = when (type) {
    PropertyType.SINGLE_ROOM -> "Single room"
    PropertyType.BEDSITTER -> "Bedsitter"
    PropertyType.ONE_BEDROOM -> "1 bedroom"
    PropertyType.TWO_BEDROOM -> "2 bedroom"
    PropertyType.THREE_BEDROOM -> "3 bedroom"
    PropertyType.MAISONETTE -> "Maisonette"
}
