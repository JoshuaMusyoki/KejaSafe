package com.rotd.havenpad.web.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// These will adapt to the current MaterialTheme.colorScheme
val LandingBrandGreen @Composable get() = MaterialTheme.colorScheme.primary
val LandingBrandSecondary @Composable get() = MaterialTheme.colorScheme.secondary
val LandingLightGreenBg @Composable get() = if (MaterialTheme.colorScheme.primary == Color(0xFF006948)) Color(
    0xFFF9FBF8
) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)

val LandingLightGreenBg2 @Composable get() = if (MaterialTheme.colorScheme.primary == Color(0xFF006948)) Color(
    0xFFA5EE81
) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
val LandingPillarMint @Composable get() = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
val LandingTextDark @Composable get() = MaterialTheme.colorScheme.onBackground
val LandingTextSecondary @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

// Static colors for reference
val WebBrandGreen = Color(0xFF005043)
val WebBrandSecondary = Color(0xFF02806A)
val WebLightGreenBg = Color(0xFFF8FBFB)
val WebPillarMint = Color(0xFFE9F4F2)
val WebTextDark = Color(0xFF1A1A1A)
val WebTextSecondary = Color(0xFF4B5563)
