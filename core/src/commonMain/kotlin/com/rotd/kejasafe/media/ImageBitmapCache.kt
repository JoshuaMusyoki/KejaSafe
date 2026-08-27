package com.rotd.kejasafe.media

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Small in-memory image cache so listing cards don't re-hit the server
 * (and cancel mid-write) on every recomposition.
 */
object ImageBitmapCache {
    private const val maxEntries = 48
    private val lock = Any()
    private val entries = LinkedHashMap<String, ImageBitmap>(maxEntries, 0.75f, true)

    fun get(url: String): ImageBitmap? = synchronized(lock) {
        entries[url]
    }

    fun put(url: String, bitmap: ImageBitmap) {
        synchronized(lock) {
            entries[url] = bitmap
            while (entries.size > maxEntries) {
                val eldest = entries.entries.iterator().next()
                entries.remove(eldest.key)
            }
        }
    }
}
