package com.rotd.havenpad

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isWeb: Boolean = false
}

actual fun getPlatform(): Platform = JVMPlatform()
