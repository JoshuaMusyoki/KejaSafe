package com.rotd.havenpad.media

import androidx.compose.runtime.Composable
import com.rotd.havenpad.PickedImage

/**
 * Opens a platform document picker for PDFs.
 * On platforms without a picker the returned function is a no-op.
 */
@Composable
expect fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
): () -> Unit
