package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import com.rotd.kejasafe.PickedImage

/**
 * Opens a platform document picker for PDFs.
 * On platforms without a picker the returned function is a no-op.
 */
@Composable
expect fun rememberPdfPickerLauncher(
    onPdfsPicked: (List<PickedImage>) -> Unit,
): () -> Unit
