package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDocumentPickerViewController

@Composable
actual fun rememberPdfShareHandler(): (fileName: String, bytes: ByteArray) -> String {
    return remember {
        { fileName, bytes ->
            val url = writeSharedFile(fileName, bytes) ?: return@remember "Couldn't save that PDF"
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
            present(picker)
            "Choose where to save $fileName"
        }
    }
}
