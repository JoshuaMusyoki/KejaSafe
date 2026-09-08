package com.rotd.kejasafe.data

actual fun platformEpochMillis(): Long =
    kotlin.js.Date.now().toLong()
