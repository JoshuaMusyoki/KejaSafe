package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.havenpad.PickedImage

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    val onPicked = rememberUpdatedState(onPdfsPicked)
    val onFailed = rememberUpdatedState(onError)
    return remember {
        {
            openBrowserFilePicker(accept = "application/pdf,.pdf") { files ->
                if (files.isEmpty()) onFailed.value("Couldn't open that PDF. Try another file.")
                else onPicked.value(files)
            }
        }
    }
}
