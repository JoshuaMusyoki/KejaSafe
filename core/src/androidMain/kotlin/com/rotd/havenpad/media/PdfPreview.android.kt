package com.rotd.havenpad.media

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File

actual fun renderPdfFirstPage(
    bytes: ByteArray,
    maxWidthPx: Int,
    maxHeightPx: Int,
): ImageBitmap? {
    val file = File.createTempFile("havenpad_pdf_thumb_", ".pdf")
    val session = AndroidPdfSession.open(file, bytes)
    return try {
        if (session.error != null || session.pageCount <= 0) {
            null
        } else {
            session.renderPage(
                index = 0,
                targetWidthPx = maxWidthPx.coerceAtLeast(1),
                targetHeightPx = maxHeightPx.coerceAtLeast(1),
            )
        }
    } catch (_: Exception) {
        null
    } finally {
        session.close()
    }
}
