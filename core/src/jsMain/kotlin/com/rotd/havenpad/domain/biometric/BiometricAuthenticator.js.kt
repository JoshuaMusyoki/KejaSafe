package com.rotd.havenpad.domain.biometric

actual fun createBiometricAuthenticator(): BiometricAuthenticator = UnsupportedBiometricAuthenticator()
