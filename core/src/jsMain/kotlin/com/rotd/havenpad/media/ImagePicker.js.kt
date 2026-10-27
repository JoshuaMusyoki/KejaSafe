package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.rotd.havenpad.PickedImage

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
