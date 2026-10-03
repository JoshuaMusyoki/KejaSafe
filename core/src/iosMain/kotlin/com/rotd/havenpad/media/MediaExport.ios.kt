package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
actual fun rememberMediaExport(): MediaExport = remember {
    MediaExport(
        download = { _, _, _ -> "Saving files is available in the phone app and the web app" },
        share = { _, _, _ -> "Sharing is available in the phone app and the web app" },
    )
}

@Composable
actual fun ExpandedPdfPreview(bytes: ByteArray, modifier: Modifier) {
    PdfDocumentViewer(bytes = bytes, modifier = modifier)
}
