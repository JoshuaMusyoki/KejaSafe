package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.havenpad.PickedImage
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    return remember {
        {
            val dialog = FileDialog(null as Frame?, "Select a PDF", FileDialog.LOAD).apply {
                setFilenameFilter { _, name -> name.lowercase().endsWith(".pdf") }
                isVisible = true
            }
            val directory = dialog.directory ?: return@remember
            val name = dialog.file ?: return@remember
            val file = File(directory, name)
            if (!file.exists()) {
                onError("Couldn't open that PDF. Try another file.")
                return@remember
            }
            onPdfsPicked(
                listOf(
                    PickedImage(
                        fileName = name,
                        bytes = file.readBytes(),
                        mimeType = "application/pdf",
                    )
                )
            )
        }
    }
}
