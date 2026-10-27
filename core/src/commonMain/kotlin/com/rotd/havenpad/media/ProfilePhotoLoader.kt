package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap
import com.rotd.havenpad.network.AuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps profile photos decoded so chat, the home top bar, and profile can paint
 * them on the first frame instead of waiting on a fresh download.
 */
object ProfilePhotoLoader {
    fun peek(url: String?): ImageBitmap? {
        val key = AuthClient.resolvePublicUrl(url) ?: return null
        return ImageBitmapCache.get(key)
    }

    suspend fun load(url: String?): ImageBitmap? {
        val key = AuthClient.resolvePublicUrl(url) ?: return null
        ImageBitmapCache.get(key)?.let { return it }
        val bytes = AuthClient.fetchPublicBytes(key) ?: return null
        val bitmap = withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() } ?: return null
        ImageBitmapCache.put(key, bitmap)
        return bitmap
    }

    suspend fun prefetch(urls: Iterable<String?>) {
        coroutineScope {
            urls.distinct().forEach { url ->
                if (url.isNullOrBlank()) return@forEach
                if (peek(url) != null) return@forEach
                launch { load(url) }
            }
        }
    }
}
