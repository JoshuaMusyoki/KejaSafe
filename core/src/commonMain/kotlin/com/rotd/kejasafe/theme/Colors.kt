package com.rotd.kejasafe.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand Colors - Emerald
val Emerald700 = Color(0xFF006948)
val Emerald800 = Color(0xFF005137)
val Emerald900 = Color(0xFF002114)
val EmeraldContainer = Color(0xFF00855D)
val OnEmeraldContainer = Color(0xFFF5FFF7)
val EmeraldInverse = Color(0xFF68DBA9)

// Brand Colors - Gold
val Gold600 = Color(0xFF735C00)
val GoldContainer = Color(0xFFFED65B)
val OnGoldContainer = Color(0xFF745C00)

// Neutral Palette
val Slate50 = Color(0xFFF7F9FB)
val Slate100 = Color(0xFFF2F4F6)
val Slate200 = Color(0xFFECEEF0)
val Slate300 = Color(0xFFE6E8EA)
val Slate400 = Color(0xFFE0E3E5)
val Slate700 = Color(0xFF3D4A42)
val Slate900 = Color(0xFF191C1E)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

internal val LightColorScheme = lightColorScheme(
    primary = Emerald700,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    inversePrimary = EmeraldInverse,
    
    secondary = Gold600,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    
    background = Slate50,
    onBackground = Slate900,
    
    surface = Slate50,
    onSurface = Slate900,
    surfaceVariant = Slate400,
    onSurfaceVariant = Slate700,
    
    outline = Color(0xFF6D7A72),
    outlineVariant = Color(0xFFBCCAC0),
    
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

internal val DarkColorScheme = darkColorScheme(
    primary = EmeraldInverse,
    onPrimary = Emerald900,
    primaryContainer = Emerald700,
    onPrimaryContainer = EmeraldInverse,
    
    secondary = Color(0xFFE9C349), 
    onSecondary = Color(0xFF241A00),
    
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE0E3E5),
    
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE0E3E5),
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    outline = Slate400,
    outlineVariant = Slate700,
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)
