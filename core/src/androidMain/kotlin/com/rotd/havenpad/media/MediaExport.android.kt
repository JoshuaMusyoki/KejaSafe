package com.rotd.havenpad.media

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberMediaExport(): MediaExport {
    val context = LocalContext.current
    return remember(context) {
        MediaExport(
            download = { fileName, mimeType, bytes ->
                saveToPhone(context, fileName, mimeType, bytes)
                if (mimeType.startsWith("image/")) "Saved to your photos" else "Saved to Downloads"
            },
            share = { fileName, mimeType, bytes ->
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    insertMediaStore(context, fileName, mimeType, bytes)
                } else {
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    val file = File(dir, fileName)
                    file.writeBytes(bytes)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = android.content.ClipData.newRawUri(fileName, uri)
                }
                context.startActivity(
                    Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Choose WhatsApp or another app"
            },
        )
    }
}

@Composable
actual fun ExpandedPdfPreview(bytes: ByteArray, modifier: Modifier) {
    PdfDocumentViewer(bytes = bytes, modifier = modifier)
}

private fun saveToPhone(
    context: android.content.Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        insertMediaStore(context, fileName, mimeType, bytes)
        return
    }
    val image = mimeType.startsWith("image/")
    @Suppress("DEPRECATION")
    val dir = Environment.getExternalStoragePublicDirectory(
        if (image) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS
    )
    if (!dir.exists()) dir.mkdirs()
    File(dir, fileName).writeBytes(bytes)
}

private fun insertMediaStore(
    context: android.content.Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
): android.net.Uri {
    val image = mimeType.startsWith("image/")
    val collection = if (image) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    }
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            if (image) Environment.DIRECTORY_PICTURES + "/HavenPad" else Environment.DIRECTORY_DOWNLOADS
        )
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(collection, values) ?: error("Could not save the file")
    resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not write the file")
    values.clear()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    return uri
}
