package com.rotd.kejasafe.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberContactPhonePicker(
    onPhonePicked: (String) -> Unit
): () -> Unit = remember { { } }
