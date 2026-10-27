package com.rotd.havenpad

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val isWeb: Boolean = false
}

actual fun getPlatform(): Platform = AndroidPlatform()
