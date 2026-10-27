package com.rotd.havenpad

interface Platform {
    val name: String
    val isWeb: Boolean
}

expect fun getPlatform(): Platform
