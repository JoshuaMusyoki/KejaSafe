package com.rotd.havenpad.listing

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.PropertyListing
import com.rotd.havenpad.components.HavenPadOutlinedButton
import com.rotd.havenpad.components.HavenPadPrimaryButton
import com.rotd.havenpad.components.ListingPhoto

@Composable
internal fun HouseSheetActions(
    signedIn: Boolean,
    canMessage: Boolean,
    onTextLandlord: () -> Unit,
    onInspect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (canMessage) {
            HavenPadPrimaryButton(
                text = "Text the landlord",
                onClick = onTextLandlord,
                modifier = Modifier.fillMaxWidth(),
            )
            HavenPadOutlinedButton(
                text = "Inspect listing",
                onClick = onInspect,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (!signedIn) {
            Text(
                text = "You're browsing as a guest. Sign in to text the landlord.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HavenPadPrimaryButton(
                text = "Inspect listing",
                onClick = onInspect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

internal fun PropertyListing.housePhotoUrls(): List<String> {
    val fromRecords = photos.map { it.url }.filter { it.isNotBlank() }
    if (fromRecords.isNotEmpty()) return fromRecords
    if (imageUrls.isNotEmpty()) return imageUrls.filter { it.isNotBlank() }
    return listOfNotNull(imageUrl?.takeIf { it.isNotBlank() })
}

@Composable
internal fun HousePhotoGallery(
    listing: PropertyListing,
    modifier: Modifier = Modifier,
    heroHeight: androidx.compose.ui.unit.Dp = 340.dp,
) {
    val urls = remember(listing.id, listing.photos, listing.imageUrls, listing.imageUrl) {
        listing.housePhotoUrls()
    }
    var index by remember(listing.id) { mutableIntStateOf(0) }
    val safeIndex = index.coerceIn(0, (urls.size - 1).coerceAtLeast(0))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clip(RoundedCornerShape(0.dp))
                .pointerInput(urls.size) {
                    if (urls.size <= 1) return@pointerInput
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = size.width * 0.15f
                            when {
                                totalDrag < -threshold && index < urls.lastIndex -> {
                                    index += 1
                                }
                                totalDrag > threshold && index > 0 -> {
                                    index -= 1
                                }
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        },
                    )
                },
        ) {
            ListingPhoto(
                imageUrl = urls.getOrNull(safeIndex),
                contentDescription = listing.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (urls.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "${safeIndex + 1} / ${urls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
