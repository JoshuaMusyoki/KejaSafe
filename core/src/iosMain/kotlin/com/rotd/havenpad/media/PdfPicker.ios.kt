package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.havenpad.PickedImage
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val onPicked by rememberUpdatedState(onPdfsPicked)
    val onFailed by rememberUpdatedState(onError)
    return remember {
        {
            presentPdfPicker(
                onPdfsPicked = { onPicked(it) },
                onError = { onFailed(it) },
            )
        }
    }
}

private var pdfPickerDelegate: NSObject? = null

private fun presentPdfPicker(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
) {
    val picker = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypePDF),
        asCopy = true,
    )
    picker.allowsMultipleSelection = false
    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            pdfPickerDelegate = null
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            if (url == null) {
                report(onError, "Couldn't open that PDF. Try another file.")
                return
            }
            val picked = readPdf(url)
            if (picked == null) {
                report(onError, "Couldn't open that PDF. Try another file.")
                return
            }
            when {
                picked.bytes.size > MAX_CHAT_ATTACHMENT_BYTES ->
                    report(onError, "That PDF is too large. Choose one under $MAX_CHAT_ATTACHMENT_MB MB.")
                !picked.bytes.looksLikePdf() ->
                    report(onError, "That file is not a readable PDF. Try another file.")
                else -> dispatch_async(dispatch_get_main_queue()) { onPdfsPicked(listOf(picked)) }
            }
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            pdfPickerDelegate = null
        }
    }
    pdfPickerDelegate = delegate
    picker.delegate = delegate
    if (topViewController() == null) {
        onError("Couldn't open the file picker.")
        return
    }
    present(picker)
}

private fun readPdf(url: NSURL): PickedImage? {
    val accessed = url.startAccessingSecurityScopedResource()
    val data = NSData.dataWithContentsOfURL(url)
    if (accessed) url.stopAccessingSecurityScopedResource()
    val bytes = data?.toByteArray() ?: return null
    if (bytes.isEmpty()) return null
    val name = url.lastPathComponent ?: "havenpad.pdf"
    val fileName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
    return PickedImage(fileName = fileName, bytes = bytes, mimeType = "application/pdf")
}

private fun report(onError: (String) -> Unit, message: String) {
    dispatch_async(dispatch_get_main_queue()) { onError(message) }
}
