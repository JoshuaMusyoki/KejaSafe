package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPdfShareHandler(): (fileName: String, bytes: ByteArray) -> String {
    return remember {
        { fileName, bytes ->
            val dir = java.io.File(System.getProperty("user.home"), "Downloads")
            if (!dir.exists()) dir.mkdirs()
            val file = java.io.File(dir, fileName)
            file.writeBytes(bytes)
            "Saved to ${file.absolutePath}"
        }
    }
}
