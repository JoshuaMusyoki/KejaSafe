package com.rotd.kejasafe.data

internal actual fun platformEpochMillis(): Long =
    kotlin.js.Date.now().toLong()
