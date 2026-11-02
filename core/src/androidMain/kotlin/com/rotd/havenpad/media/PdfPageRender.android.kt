package com.rotd.havenpad.media

actual suspend fun renderPdfPageForPreview(
    bytes: ByteArray,
    pageIndex: Int,
    maxWidthPx: Int,
): PdfPageImage? = null
