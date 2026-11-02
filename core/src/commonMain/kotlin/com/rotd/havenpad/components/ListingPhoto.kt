package com.rotd.havenpad.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.rotd.havenpad.media.ImageBitmapCache
import com.rotd.havenpad.media.decodeToImageBitmap
import com.rotd.havenpad.network.ListingClient
import kotlinx.coroutines.CancellationException

/**
 * Loads a listing photo from a relative `/uploads/...` path or absolute URL.
 */
@Composable
fun ListingPhoto(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val resolvedUrl = remember(imageUrl) { ListingClient.resolveMediaUrl(imageUrl) }
    var bitmap by remember(resolvedUrl) {
        mutableStateOf(resolvedUrl?.let { ImageBitmapCache.get(it) })
    }
    var loading by remember(resolvedUrl) {
        mutableStateOf(bitmap == null && !resolvedUrl.isNullOrBlank())
    }
    var failed by remember(resolvedUrl) {
        mutableStateOf(resolvedUrl.isNullOrBlank())
    }

    LaunchedEffect(resolvedUrl) {
        if (resolvedUrl.isNullOrBlank()) {
            bitmap = null
            loading = false
            failed = true
            return@LaunchedEffect
        }
        val cached = ImageBitmapCache.get(resolvedUrl)
        if (cached != null) {
            bitmap = cached
            loading = false
            failed = false
            return@LaunchedEffect
        }
        loading = true
        failed = false
        try {
            val decoded = ListingClient.fetchImageBytes(resolvedUrl).decodeToImageBitmap()
            if (decoded != null) {
                ImageBitmapCache.put(resolvedUrl, decoded)
                bitmap = decoded
                failed = false
            } else {
                failed = true
            }
        } catch (e: CancellationException) {
            // Composition left / new image — do not treat as failure.
            throw e
        } catch (_: Exception) {
            // Includes client/server channel closed when the previous request was aborted.
            failed = true
            bitmap = null
        } finally {
            loading = false
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.size(48.dp)
                )
                if (imageUrl.isNullOrBlank()) {
                    Text(
                        text = "No photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    )
                }
            }
        }
        overlay()
    }
}
