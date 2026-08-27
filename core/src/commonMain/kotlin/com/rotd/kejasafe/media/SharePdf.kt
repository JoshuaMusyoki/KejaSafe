package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable

/**
 * Platform handler that persists / shares a PDF and returns a short user message.
 */
@Composable
expect fun rememberPdfShareHandler(): (fileName: String, bytes: ByteArray) -> String
