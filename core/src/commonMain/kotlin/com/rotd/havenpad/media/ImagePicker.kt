package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import com.rotd.havenpad.PickedImage

/**
 * Returns a launcher that opens the platform image picker (multi-select when supported).
 * On platforms without a picker, the returned function is a no-op.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImage>) -> Unit
): () -> Unit
