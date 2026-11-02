package com.rotd.havenpad.device

import android.os.Build

internal actual fun platformDeviceName(): String {
    val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
    val model = Build.MODEL?.trim().orEmpty().ifBlank { "Android" }
    val prettyMaker = manufacturer.replaceFirstChar { it.titlecase() }
    return if (manufacturer.isBlank() || model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$prettyMaker $model"
    }
}

internal actual fun platformLabel(): String = "android"
