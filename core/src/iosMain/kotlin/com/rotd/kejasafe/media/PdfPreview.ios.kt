package com.rotd.kejasafe.media

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxMediaBox
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun renderPdfFirstPage(
    bytes: ByteArray,
    maxWidthPx: Int,
    maxHeightPx: Int,
): ImageBitmap? = runCatching {
    if (!bytes.looksLikePdf()) return null
    val document = PDFDocument(data = bytes.toNsData()) ?: return null
    val page = document.pageAtIndex(0u) ?: return null
    val thumb = page.thumbnailOfSize(
        CGSizeMake(
            maxWidthPx.coerceAtLeast(1).toDouble(),
            maxHeightPx.coerceAtLeast(1).toDouble(),
        ),
        forBox = kPDFDisplayBoxMediaBox,
    )
    val png = UIImagePNGRepresentation(thumb) ?: return null
    png.toByteArray().decodeToImageBitmap()
}.getOrNull()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNsData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val dest = ByteArray(size)
    dest.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, size.toULong())
    }
    return dest
}
