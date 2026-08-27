package com.rotd.kejasafe.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rotd.kejasafe.PickedImage

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val images = uris.mapNotNull { uri ->
            runCatching {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "image/jpeg"
                val fileName = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.ifBlank { null }
                    ?: "house_${System.currentTimeMillis()}.jpg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@mapNotNull null
                PickedImage(
                    fileName = fileName,
                    bytes = bytes,
                    mimeType = mimeType
                )
            }.getOrNull()
        }
        if (images.isNotEmpty()) onImagesPicked(images)
    }

    return remember(launcher) {
        { launcher.launch("image/*") }
    }
}
