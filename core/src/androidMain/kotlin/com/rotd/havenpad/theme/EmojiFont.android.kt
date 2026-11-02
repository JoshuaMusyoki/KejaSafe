package com.rotd.havenpad.theme

import androidx.compose.runtime.Composable

@Composable
actual fun ProvideEmojiFont(content: @Composable () -> Unit) {
    content()
}
