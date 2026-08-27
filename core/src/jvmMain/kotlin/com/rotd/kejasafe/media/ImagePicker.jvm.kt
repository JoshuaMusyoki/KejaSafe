package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.kejasafe.PickedImage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Select house photos", FileDialog.LOAD).apply {
                isMultipleMode = true
                setFilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                        lower.endsWith(".png") || lower.endsWith(".webp")
                }
                isVisible = true
            }
            val directory = dialog.directory ?: return@remember
            val names = dialog.files?.map { it.name } ?: listOfNotNull(dialog.file)
            val images = names.mapNotNull { name ->
                val file = File(directory, name)
                if (!file.exists()) return@mapNotNull null
                val mime = when {
                    name.endsWith(".png", true) -> "image/png"
                    name.endsWith(".webp", true) -> "image/webp"
                    else -> "image/jpeg"
                }
                PickedImage(fileName = name, bytes = file.readBytes(), mimeType = mime)
            }
            if (images.isNotEmpty()) onImagesPicked(images)
        }
    }
}
