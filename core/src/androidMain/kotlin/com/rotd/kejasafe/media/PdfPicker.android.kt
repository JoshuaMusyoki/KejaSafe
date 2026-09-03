package com.rotd.kejasafe.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rotd.kejasafe.PickedImage

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = runCatching {
            val resolver = context.contentResolver
            val bytes = resolver.readBytesFully(uri) ?: return@runCatching null
            val fileName = resolver.displayName(uri)
                ?: "kejasafe-${System.currentTimeMillis()}.pdf"
            val mimeType = resolver.getType(uri) ?: "application/pdf"
            PickedImage(
                fileName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf",
                bytes = bytes,
                mimeType = mimeType,
            )
        }.getOrNull()
        if (file != null) onPdfsPicked(listOf(file))
    }
    return remember(launcher) {
        { launcher.launch(arrayOf("application/pdf", "application/x-pdf")) }
    }
}

private fun ContentResolver.readBytesFully(uri: Uri): ByteArray? {
    openInputStream(uri)?.use { input ->
        return input.readBytes()
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
