package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.havenpad.PickedImage

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit {
    // Wire PHPicker when iOS media picking is prioritized.
    return remember { { onImagesPicked(emptyList()) } }
}
