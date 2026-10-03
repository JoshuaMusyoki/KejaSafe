package com.rotd.havenpad.listing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    heroHeight: androidx.compose.ui.unit.Dp = 200.dp,
) {
    val urls = remember(listing.id, listing.photos, listing.imageUrls, listing.imageUrl) {
        listing.housePhotoUrls()
    }
    var index by remember(listing.id) { mutableIntStateOf(0) }
    var expanded by remember(listing.id) { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, (urls.size - 1).coerceAtLeast(0))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = urls.isNotEmpty()) { expanded = true },
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
                        .padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "${safeIndex + 1} / ${urls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (urls.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(urls) { photoIndex, url ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                index = photoIndex
                                expanded = true
                            },
                    ) {
                        ListingPhoto(
                            imageUrl = url,
                            contentDescription = "Photo ${photoIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            Text(
                text = "Tap a photo to view all ${urls.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (expanded && urls.isNotEmpty()) {
        HousePhotoViewer(
            urls = urls,
            startIndex = safeIndex,
            title = listing.title,
            onDismiss = { expanded = false },
            onIndexChange = { index = it },
        )
    }
}

@Composable
private fun HousePhotoViewer(
    urls: List<String>,
    startIndex: Int,
    title: String,
    onDismiss: () -> Unit,
    onIndexChange: (Int) -> Unit,
) {
    var page by remember(urls, startIndex) { mutableIntStateOf(startIndex.coerceIn(0, urls.lastIndex)) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(page, urls.size) {
                    if (urls.size <= 1) return@pointerInput
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = size.width * 0.18f
                            when {
                                totalDrag < -threshold && page < urls.lastIndex -> {
                                    page += 1
                                    onIndexChange(page)
                                }
                                totalDrag > threshold && page > 0 -> {
                                    page -= 1
                                    onIndexChange(page)
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
                imageUrl = urls[page],
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${page + 1} / ${urls.size}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            if (urls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            page = (page - 1).coerceAtLeast(0)
                            onIndexChange(page)
                        },
                        enabled = page > 0,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous photo",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = {
                            page = (page + 1).coerceAtMost(urls.lastIndex)
                            onIndexChange(page)
                        },
                        enabled = page < urls.lastIndex,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next photo",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
