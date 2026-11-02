package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.io.File

@Composable
actual fun rememberMediaExport(): MediaExport {
    return remember {
        MediaExport(
            download = { fileName, _, bytes ->
                val dir = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeBytes(bytes)
                "Saved to ${file.absolutePath}"
            },
            share = { fileName, _, bytes ->
                val dir = File(System.getProperty("user.home"), "Downloads").apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeBytes(bytes)
                "Saved to ${file.absolutePath}. Share it from there."
            },
        )
    }
}

@Composable
actual fun ExpandedPdfPreview(bytes: ByteArray, modifier: Modifier) {
    PdfDocumentViewer(bytes = bytes, modifier = modifier)
}
