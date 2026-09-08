package com.rotd.kejasafe

interface Platform {
    val name: String
    val isWeb: Boolean
}

expect fun getPlatform(): Platform
