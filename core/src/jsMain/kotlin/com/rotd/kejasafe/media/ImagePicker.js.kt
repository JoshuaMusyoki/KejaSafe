package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.kejasafe.PickedImage

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    val onPicked = rememberUpdatedState(onImagesPicked)
    return remember {
        {
            openBrowserFilePicker(accept = "image/*") { files ->
                onPicked.value(files)
            }
        }
    }
}
