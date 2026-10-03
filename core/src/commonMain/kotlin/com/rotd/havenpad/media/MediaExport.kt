package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Saves a file onto the device, or opens the system share sheet (WhatsApp and the rest).
 * Returns a short message for the user.
 */
class MediaExport(
    val download: (fileName: String, mimeType: String, bytes: ByteArray) -> String,
    val share: (fileName: String, mimeType: String, bytes: ByteArray) -> String,
)

@Composable
expect fun rememberMediaExport(): MediaExport

/**
 * Full-size PDF. Phone apps draw pages in Compose. The browser shows its own PDF viewer
 * in this space so the page can be read, saved, and shared.
 */
@Composable
expect fun ExpandedPdfPreview(
    bytes: ByteArray,
    modifier: Modifier = Modifier,
)
