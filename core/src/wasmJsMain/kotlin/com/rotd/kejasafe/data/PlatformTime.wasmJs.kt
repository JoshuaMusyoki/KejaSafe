package com.rotd.kejasafe.data

actual fun platformEpochMillis(): Long = jsDateNow().toLong()

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double
