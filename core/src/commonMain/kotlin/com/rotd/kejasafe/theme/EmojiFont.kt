package com.rotd.kejasafe.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/**
 * Color emoji face used by web chat. Android, iOS, and desktop already fall back
 * to a system emoji font, so this stays null there.
 */
val LocalEmojiFontFamily = staticCompositionLocalOf<FontFamily?> { null }

@Composable
expect fun ProvideEmojiFont(content: @Composable () -> Unit)
