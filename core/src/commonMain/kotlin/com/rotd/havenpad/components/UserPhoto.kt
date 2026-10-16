package com.rotd.havenpad.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.rotd.havenpad.media.ProfilePhotoLoader

fun photoInitials(name: String): String =
    name.split(" ")
        .mapNotNull { part -> part.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifBlank { "?" }

@Composable
fun UserPhoto(
    imageUrl: String?,
    fallback: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    // Keep the last good bitmap across brief null URL flashes during peer refresh.
    var bitmap by remember { mutableStateOf(ProfilePhotoLoader.peek(imageUrl)) }
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        ProfilePhotoLoader.peek(imageUrl)?.let { bitmap = it }
        val loaded = ProfilePhotoLoader.load(imageUrl) ?: return@LaunchedEffect
        bitmap = loaded
    }
    val photo: ImageBitmap? = ProfilePhotoLoader.peek(imageUrl) ?: bitmap
    Box(
        modifier = modifier.clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = fallback,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
