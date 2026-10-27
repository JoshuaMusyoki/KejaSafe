package com.rotd.havenpad.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import havenpad.core.generated.resources.Res
import havenpad.core.generated.resources.haven_pad_logo
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun HavenPadLogoImage(modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.haven_pad_logo),
        contentDescription = "HavenPad",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
