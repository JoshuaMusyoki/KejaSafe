package com.rotd.havenpad.data

actual fun platformEpochMillis(): Long =
    kotlin.js.Date.now().toLong()
