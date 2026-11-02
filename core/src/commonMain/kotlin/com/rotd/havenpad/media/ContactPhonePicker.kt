package com.rotd.havenpad.media

import androidx.compose.runtime.Composable

/**
 * Opens the platform contacts picker and returns a phone number.
 * On platforms without a picker, the returned launcher is a no-op.
 */
@Composable
expect fun rememberContactPhonePicker(
    onPhonePicked: (String) -> Unit
): () -> Unit
