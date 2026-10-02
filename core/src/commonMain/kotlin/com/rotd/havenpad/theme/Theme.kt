package com.rotd.havenpad.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class HavenPadSpacing(
    val base: Dp = 8.dp,
    val containerMarginMobile: Dp = 16.dp,
    val containerMarginDesktop: Dp = 48.dp,
    val gutter: Dp = 24.dp,
    val sectionGap: Dp = 64.dp,
    val elementGap: Dp = 16.dp
)

val LocalHavenPadSpacing = staticCompositionLocalOf { HavenPadSpacing() }

@Composable
fun HavenPadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalHavenPadSpacing provides HavenPadSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HavenPadTypography,
            shapes = HavenPadShapes,
            content = content
        )
    }
}

/** Resolve [ThemeMode] against the device setting. */
@Composable
fun ThemeMode.resolveDarkTheme(systemDark: Boolean = isSystemInDarkTheme()): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Accessor object for custom theme attributes like spacing.
 */
object HavenPadTheme {
    val spacing: HavenPadSpacing
        @Composable
        get() = LocalHavenPadSpacing.current
}
