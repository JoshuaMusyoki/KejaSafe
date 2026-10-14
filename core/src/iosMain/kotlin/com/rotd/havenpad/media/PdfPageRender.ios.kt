package com.rotd.havenpad.media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGSizeMake
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIImagePNGRepresentation

@OptIn(ExperimentalForeignApi::class)
actual suspend fun renderPdfPageForPreview(
    bytes: ByteArray,
    pageIndex: Int,
    maxWidthPx: Int,
): PdfPageImage? {
    if (!bytes.looksLikePdf()) return null
    val document = PDFDocument(data = bytes.toNsData()) ?: return null
    val pageCount = document.pageCount.toInt()
    if (pageCount <= 0) return null
    val index = pageIndex.coerceIn(0, pageCount - 1)
    val page = document.pageAtIndex(index.toULong()) ?: return null
    val targetWidth = maxWidthPx.coerceIn(1, 1600).toDouble()
    val targetHeight = (targetWidth * 1.414).coerceAtMost(1600.0)
    val thumb = page.thumbnailOfSize(
        CGSizeMake(targetWidth, targetHeight),
        forBox = kPDFDisplayBoxMediaBox,
    )
    val png = UIImagePNGRepresentation(thumb) ?: return null
    val bitmap = png.toByteArray().decodeToImageBitmap() ?: return null
    return PdfPageImage(bitmap = bitmap, pageCount = pageCount)
}
