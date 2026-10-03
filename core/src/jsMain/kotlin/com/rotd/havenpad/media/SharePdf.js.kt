package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPdfShareHandler(): (fileName: String, bytes: ByteArray) -> String {
    val export = rememberMediaExport()
    return remember(export) {
        { fileName, bytes -> export.download(fileName, "application/pdf", bytes) }
    }
}
