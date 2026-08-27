package com.rotd.kejasafe.data

internal actual fun platformEpochMillis(): Long =
    (js("Date.now()") as Double).toLong()
