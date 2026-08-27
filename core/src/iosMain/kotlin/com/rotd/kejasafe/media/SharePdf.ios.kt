package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPdfShareHandler(): (fileName: String, bytes: ByteArray) -> String {
    return remember {
        { _, _ -> "PDF download is available on Android" }
    }
}
