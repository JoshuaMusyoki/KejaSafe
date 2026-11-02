package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage

@Composable
actual fun rememberMediaExport(): MediaExport = remember {
    MediaExport(
        download = { fileName, mimeType, bytes ->
            if (mimeType.startsWith("image/")) savePhoto(bytes) else exportToFiles(fileName, bytes)
        },
        share = { fileName, _, bytes ->
            shareFile(fileName, bytes)
        },
    )
}

@Composable
actual fun ExpandedPdfPreview(bytes: ByteArray, modifier: Modifier) {
    PdfDocumentViewer(bytes = bytes, modifier = modifier)
}

private fun savePhoto(bytes: ByteArray): String {
    val image = UIImage.imageWithData(bytes.toNsData())
        ?: return "Couldn't save that photo"
    PHPhotoLibrary.sharedPhotoLibrary().performChanges({
        PHAssetChangeRequest.creationRequestForAssetFromImage(image)
    }, completionHandler = null)
    return "Saved to your photos"
}

private fun exportToFiles(fileName: String, bytes: ByteArray): String {
    val url = writeSharedFile(fileName, bytes) ?: return "Couldn't save that file"
    val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
    present(picker)
    return "Choose where to save the file"
}

private fun shareFile(fileName: String, bytes: ByteArray): String {
    val url = writeSharedFile(fileName, bytes) ?: return "Couldn't share that file"
    val host = topViewController() ?: return "Couldn't open sharing"
    val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    sheet.popoverPresentationController?.let { popover ->
        popover.sourceView = host.view
        popover.sourceRect = host.view.bounds
    }
    host.presentViewController(sheet, animated = true, completion = null)
    return "Choose an app to share with"
}
