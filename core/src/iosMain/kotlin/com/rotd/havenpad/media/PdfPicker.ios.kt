package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.rotd.havenpad.PickedImage

@Composable
actual fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
    onError: (String) -> Unit,
): () -> Unit {
    return remember {
        {
            onError("PDF upload is available on the phone and the website.")
            onPdfsPicked(emptyList())
        }
    }
}
