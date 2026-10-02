package com.rotd.havenpad.data

actual fun platformEpochMillis(): Long = jsDateNow().toLong()

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double
