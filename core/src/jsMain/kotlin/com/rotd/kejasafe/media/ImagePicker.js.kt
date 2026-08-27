package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.kejasafe.PickedImage

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    return remember { { onImagesPicked(emptyList()) } }
}
