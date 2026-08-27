package com.rotd.kejasafe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform