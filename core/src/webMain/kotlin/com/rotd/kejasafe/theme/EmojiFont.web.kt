package com.rotd.kejasafe.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import kejasafe.core.generated.resources.Res
import kejasafe.core.generated.resources.twemoji_mozilla
import org.jetbrains.compose.resources.Font

/**
 * Skia on web does not see CSS emoji fonts. Twemoji Mozilla (COLR) is loaded
 * here and applied to emoji runs in chat text.
 */
@Composable
actual fun ProvideEmojiFont(content: @Composable () -> Unit) {
    val emojiFont = Font(Res.font.twemoji_mozilla, weight = FontWeight.Normal)
    val family = remember(emojiFont) { FontFamily(emojiFont) }
    val resolver = LocalFontFamilyResolver.current
    resolver.resolve(family, FontWeight.Normal, FontStyle.Normal, FontSynthesis.All)
    CompositionLocalProvider(LocalEmojiFontFamily provides family, content = content)
}
