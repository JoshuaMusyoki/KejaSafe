package com.rotd.havenpad.media

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
actual fun PdfDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var pageIndex by remember(bytes.size, bytes.contentHashCode()) { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    val session = remember(bytes.size, bytes.contentHashCode()) {
        AndroidPdfSession.open(File(context.cacheDir, "chat_pdf_${System.nanoTime()}.pdf"), bytes)
    }

    DisposableEffect(session) {
        onDispose { session.close() }
    }

    if (error != null) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = error ?: "Could not open this PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

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
                                totalDrag < -threshold && pageIndex < pageCount - 1 -> {
                                    pageIndex += 1
                                }
                                totalDrag > threshold && pageIndex > 0 -> {
                                    pageIndex -= 1
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
            contentAlignment = Alignment.Center,
        ) {
            val targetW = constraints.maxWidth.coerceAtLeast(1)
            val targetH = constraints.maxHeight.coerceAtLeast(1)
            LaunchedEffect(session, pageIndex, targetW, targetH) {
                loading = true
                error = session.error
                pageCount = session.pageCount
                if (session.error == null && pageCount > 0) {
                    val index = pageIndex.coerceIn(0, pageCount - 1)
                    if (index != pageIndex) pageIndex = index
                    pageBitmap = runCatching {
                        session.renderPage(index, targetW, targetH)
                    }
                        .onFailure { error = it.message ?: "Could not render this PDF page." }
                        .getOrNull()
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
                else -> Text("Could not open this PDF.")
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
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous page",
                )
            }
            Text(
                text = "${pageIndex + 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next page",
                )
            }
        }
    }
}

internal class AndroidPdfSession(
    private val renderer: PdfRenderer?,
    private val pfd: ParcelFileDescriptor?,
    private val cacheFile: File,
    val error: String?,
) {
    val pageCount: Int get() = renderer?.pageCount ?: 0

    fun renderPage(
        index: Int,
        targetWidthPx: Int,
        targetHeightPx: Int,
    ): ImageBitmap {
        val pdf = renderer ?: error(error ?: "PDF is not available.")
        pdf.openPage(index).use { page ->
            val srcW = page.width.coerceAtLeast(1).toFloat()
            val srcH = page.height.coerceAtLeast(1).toFloat()
            val fitScale = minOf(
                targetWidthPx.coerceAtLeast(1) / srcW,
                targetHeightPx.coerceAtLeast(1) / srcH,
            ).coerceAtLeast(0.1f)
            val outW = (srcW * fitScale).toInt().coerceAtLeast(1)
            val outH = (srcH * fitScale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            val matrix = Matrix().apply { setScale(fitScale, fitScale) }
            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp.asImageBitmap()
        }
    }

    fun close() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        runCatching { cacheFile.delete() }
    }

    companion object {
        fun open(file: File, bytes: ByteArray): AndroidPdfSession {
            return try {
                if (!bytes.looksLikePdf()) {
                    return AndroidPdfSession(
                        renderer = null,
                        pfd = null,
                        cacheFile = file,
                        error = "This file is not a readable PDF.",
                    )
                }
                file.writeBytes(bytes)
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                AndroidPdfSession(renderer, pfd, file, error = null)
            } catch (e: SecurityException) {
                AndroidPdfSession(
                    renderer = null,
                    pfd = null,
                    cacheFile = file,
                    error = "This PDF is protected and can’t be opened here.",
                )
            } catch (e: Exception) {
                AndroidPdfSession(
                    renderer = null,
                    pfd = null,
                    cacheFile = file,
                    error = e.message?.takeIf { it.isNotBlank() }
                        ?: "Could not open this PDF.",
                )
            }
        }
    }
}
