package com.rotd.havenpad.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HavenPadHeadline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.headlineMedium
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}

@Composable
fun HavenPadBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}

@Composable
fun HavenPadLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}
