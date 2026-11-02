package com.rotd.havenpad.network

/**
 * The iOS simulator shares the Mac's localhost. A physical iPhone needs the Mac's LAN address
 * instead, for example `http://192.168.1.20:8080`.
 */
internal actual fun platformApiBaseUrl(): String = "http://localhost:8080"
