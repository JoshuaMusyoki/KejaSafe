package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.kejasafe.PickedImage

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
): () -> Unit {
    val onPicked = rememberUpdatedState(onPdfsPicked)
    return remember {
        {
            openBrowserFilePicker(accept = "application/pdf,.pdf") { files ->
                onPicked.value(files)
            }
        }
    }
}
