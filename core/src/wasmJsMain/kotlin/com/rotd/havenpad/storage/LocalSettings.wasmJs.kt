package com.rotd.havenpad.storage

import kotlin.js.ExperimentalWasmJsInterop

actual object LocalSettings {
    actual fun getString(key: String): String? = jsGetItem(key)

    actual fun putString(key: String, value: String?) {
        if (value == null) jsRemoveItem(key) else jsSetItem(key, value)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => localStorage.getItem(key)")
private external fun jsGetItem(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => localStorage.setItem(key, value)")
private external fun jsSetItem(key: String, value: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => localStorage.removeItem(key)")
private external fun jsRemoveItem(key: String)
