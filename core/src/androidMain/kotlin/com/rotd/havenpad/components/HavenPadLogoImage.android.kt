package com.rotd.havenpad.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rotd.havenpad.core.R

@Composable
actual fun HavenPadLogoImage(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.haven_pad_logo),
        contentDescription = "HavenPad",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
