package com.rotd.kejasafe.domain.biometric

actual fun createBiometricAuthenticator(): BiometricAuthenticator = UnsupportedBiometricAuthenticator()
