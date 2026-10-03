package com.rotd.havenpad.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.rotd.havenpad.PickedImage
import java.io.FileInputStream

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val onPicked = rememberUpdatedState(onPdfsPicked)
    val onFailed = rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        val bytes = runCatching { resolver.readPdfBytes(uri) }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            onFailed.value("Couldn't open that PDF. Try another file.")
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > MAX_CHAT_ATTACHMENT_BYTES) {
            onFailed.value("That PDF is too large. Choose one under $MAX_CHAT_ATTACHMENT_MB MB.")
            return@rememberLauncherForActivityResult
        }
        if (!bytes.looksLikePdf()) {
            onFailed.value("That file is not a readable PDF. Try another file.")
            return@rememberLauncherForActivityResult
        }
        val fileName = resolver.displayName(uri)
            ?: "havenpad-${System.currentTimeMillis()}.pdf"
        onPicked.value(
            listOf(
                PickedImage(
                    fileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf",
                    bytes = bytes,
                    mimeType = "application/pdf",
                ),
            ),
        )
    }
    return remember(launcher) {
        { launcher.launch("application/pdf") }
    }
}

private fun ContentResolver.readPdfBytes(uri: Uri): ByteArray? {
    openInputStream(uri)?.use { input ->
        return input.readBytes().takeIf { it.isNotEmpty() }
    }
    openFileDescriptor(uri, "r")?.use { descriptor ->
        return FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }.takeIf { it.isNotEmpty() }
    }
    return null
}

private fun ContentResolver.displayName(uri: Uri): String? {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)?.takeIf { it.isNotBlank() }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
}
