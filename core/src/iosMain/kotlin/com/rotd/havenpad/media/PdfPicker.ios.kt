package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.havenpad.PickedImage

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
): () -> Unit {
    return remember { { onPdfsPicked(emptyList()) } }
}
