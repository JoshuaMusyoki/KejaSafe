package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Small in-memory image cache so listing cards don't re-hit the server
 * (and cancel mid-write) on every recomposition.
 */
object ImageBitmapCache {
    private const val maxEntries = 72
    private val entries = LinkedHashMap<String, ImageBitmap>()

    fun get(url: String): ImageBitmap? {
        return entries[url]
    }

    fun put(url: String, bitmap: ImageBitmap) {
        entries[url] = bitmap
        if (entries.size > maxEntries) {
            val eldestKey = entries.keys.iterator().next()
            entries.remove(eldestKey)
        }
    }
}
