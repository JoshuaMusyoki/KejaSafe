package com.rotd.havenpad.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Page-by-page PDF view used on the web, matching the phone reader. */
@Composable
internal fun PagedPdfPreview(
    bytes: ByteArray,
    modifier: Modifier = Modifier,
) {
    var pageIndex by remember(bytes.size) { mutableIntStateOf(0) }
    var pageCount by remember(bytes.size) { mutableIntStateOf(0) }
    var pageBitmap by remember(bytes.size) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(bytes.size) { mutableStateOf<String?>(null) }
    var loading by remember(bytes.size) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (pageCount > 1) 72.dp else 8.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .pointerInput(pageIndex, pageCount) {
                    if (pageCount <= 1) return@pointerInput
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = size.width * 0.18f
                            when {
                                totalDrag < -threshold && pageIndex < pageCount - 1 -> pageIndex += 1
                                totalDrag > threshold && pageIndex > 0 -> pageIndex -= 1
                            }
                            totalDrag = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val targetW = constraints.maxWidth.coerceAtLeast(1)
            LaunchedEffect(bytes, pageIndex, targetW) {
                loading = true
                val page = runCatching {
                    renderPdfPageForPreview(bytes, pageIndex, targetW)
                }.getOrElse {
                    error = it.message ?: "Could not open this PDF."
                    null
                }
                if (page != null) {
                    pageCount = page.pageCount
                    pageBitmap = page.bitmap
                    error = null
                    val last = page.pageCount - 1
                    if (pageIndex > last) pageIndex = last.coerceAtLeast(0)
                } else if (error == null) {
                    error = "Could not open this PDF."
                }
                loading = false
            }
            val bitmap = pageBitmap
            when {
                loading && bitmap == null -> CircularProgressIndicator()
                bitmap != null -> Image(
                    bitmap = bitmap,
                    contentDescription = "PDF page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                )
                else -> Text(
                    text = error ?: "Could not open this PDF.",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (pageCount > 1) {
            PdfPageControls(
                pageIndex = pageIndex,
                pageCount = pageCount,
                onPrevious = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                onNext = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PdfPageControls(
    pageIndex: Int,
    pageCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canGoBack = pageIndex > 0
    val canGoForward = pageIndex < pageCount - 1
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = canGoBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.size(44.dp).clip(CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
            }
            Text(
                text = "${pageIndex + 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge,
            )
            IconButton(
                onClick = onNext,
                enabled = canGoForward,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
                modifier = Modifier.size(44.dp).clip(CircleShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
            }
        }
    }
}
