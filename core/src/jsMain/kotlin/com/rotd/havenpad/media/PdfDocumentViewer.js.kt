package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PdfDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier,
) {
    PagedPdfPreview(bytes = bytes, modifier = modifier)
}
