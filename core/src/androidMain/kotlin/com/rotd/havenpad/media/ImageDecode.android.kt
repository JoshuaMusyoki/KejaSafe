package com.rotd.havenpad.media

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.decodeToImageBitmap(): ImageBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size) ?: return null
    return bitmap.asImageBitmap()
}
