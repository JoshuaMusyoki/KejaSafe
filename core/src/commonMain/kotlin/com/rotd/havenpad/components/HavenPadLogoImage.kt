package com.rotd.havenpad.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android loads this from the library drawable so it is in the APK.
 * Other targets use the Compose resource.
 */
@Composable
internal expect fun HavenPadLogoImage(modifier: Modifier = Modifier)
