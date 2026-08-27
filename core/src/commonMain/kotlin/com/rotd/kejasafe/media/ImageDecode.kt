package com.rotd.kejasafe.media

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes image bytes into a Compose [ImageBitmap], or null if unsupported/corrupt.
 */
expect fun ByteArray.decodeToImageBitmap(): ImageBitmap?
