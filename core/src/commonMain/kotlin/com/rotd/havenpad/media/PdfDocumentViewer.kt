package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * In-app PDF page viewer. Android uses [android.graphics.pdf.PdfRenderer];
 * iOS uses PDFKit. Other targets show a fallback.
 */
@Composable
expect fun PdfDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier = Modifier,
)
