package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap

/** First-page thumbnail for chat bubbles. Returns null if the PDF cannot be rendered. */
expect fun renderPdfFirstPage(
    bytes: ByteArray,
    maxWidthPx: Int,
    maxHeightPx: Int,
): ImageBitmap?
