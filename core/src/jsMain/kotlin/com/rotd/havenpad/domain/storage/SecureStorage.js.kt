package com.rotd.havenpad.domain.storage

actual fun createSecureStorage(): SecureStorage = MemorySecureStorage()
