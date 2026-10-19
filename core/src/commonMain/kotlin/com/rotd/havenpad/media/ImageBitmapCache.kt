package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Small in-memory image cache so listing cards don't re-hit the server
 * on every recomposition.
 */
object ImageBitmapCache {
    private const val MAX_ENTRIES = 96
    private val entries = mutableMapOf<String, ImageBitmap>()

    fun get(url: String): ImageBitmap? {
        return entries[url]
    }

    fun put(url: String, bitmap: ImageBitmap) {
        if (entries.containsKey(url)) {
            entries.remove(url) // Re-insert to maintain insertion order
        } else if (entries.size >= MAX_ENTRIES) {
            val oldestKey = entries.keys.firstOrNull()
            if (oldestKey != null) {
                entries.remove(oldestKey)
            }
        }
        entries[url] = bitmap
    }
}