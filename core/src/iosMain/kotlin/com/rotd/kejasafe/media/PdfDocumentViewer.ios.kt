package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFView
import platform.PDFKit.kPDFDisplaySinglePageContinuous

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PdfDocumentViewer(
    bytes: ByteArray,
    modifier: Modifier,
) {
    val data = remember(bytes) { bytes.toNsData() }
    UIKitView(
        modifier = modifier,
        factory = {
            PDFView().apply {
                document = PDFDocument(data = data)
                autoScales = true
                displayMode = kPDFDisplaySinglePageContinuous
            }
        },
        update = { view ->
            view.document = PDFDocument(data = data)
            view.autoScales = true
        },
    )
}
