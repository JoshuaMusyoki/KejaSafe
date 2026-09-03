package com.rotd.kejasafe.domain.storage

actual fun createSecureStorage(): SecureStorage = MemorySecureStorage()
