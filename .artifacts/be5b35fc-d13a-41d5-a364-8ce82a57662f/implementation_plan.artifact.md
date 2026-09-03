# Implement Fingerprint Login for Android and iOS

Implement biometric authentication (fingerprint/FaceID) to allow users to sign in securely on both platforms.

## User Review Required

> [!IMPORTANT]
> Biometric data is device-specific. Users must first log in with their email/password once to "enable" biometric login for that device.
> We will store credentials (or a persistent token) securely in the device's secure enclave (EncryptedSharedPreferences on Android, Keychain on iOS).

## Proposed Changes

### [Core Module]

#### [NEW] [BiometricAuthenticator.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/domain/biometric/BiometricAuthenticator.kt)
Define a common interface for biometric operations.

#### [NEW] [SecureStorage.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/domain/storage/SecureStorage.kt)
Define a common interface for storing sensitive data like passwords or tokens.

#### [MODIFY] [AuthRepository.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/domain/repository/AuthRepository.kt)
Add methods for biometric login and enabling/disabling it.

#### [MODIFY] [KtorBridgeAuthRepository.kt](file:///C:/TranssionProjects/KejaSafe/core/src/commonMain/kotlin/com/rotd/kejasafe/data/repoImpl/KtorBridgeAuthRepository.kt)
Implement the new biometric methods using injected `BiometricAuthenticator` and `SecureStorage`.

#### [MODIFY] [build.gradle.kts](file:///C:/TranssionProjects/KejaSafe/core/build.gradle.kts)
Add `androidx.biometric` dependency for Android.

#### [NEW] [BiometricAuthenticator.android.kt](file:///C:/TranssionProjects/KejaSafe/core/src/androidMain/kotlin/com/rotd/kejasafe/domain/biometric/BiometricAuthenticator.android.kt)
Android implementation using `androidx.biometric.BiometricPrompt`.

#### [NEW] [SecureStorage.android.kt](file:///C:/TranssionProjects/KejaSafe/core/src/androidMain/kotlin/com/rotd/kejasafe/domain/storage/SecureStorage.android.kt)
Android implementation using `EncryptedSharedPreferences`.

#### [NEW] [BiometricAuthenticator.ios.kt](file:///C:/TranssionProjects/KejaSafe/core/src/iosMain/kotlin/com/rotd/kejasafe/domain/biometric/BiometricAuthenticator.ios.kt)
iOS implementation using `LocalAuthentication`.

#### [NEW] [SecureStorage.ios.kt](file:///C:/TranssionProjects/KejaSafe/core/src/iosMain/kotlin/com/rotd/kejasafe/domain/storage/SecureStorage.ios.kt)
iOS implementation using `Keychain`.

### [App Shared Module]

#### [MODIFY] [LoginViewModel.kt](file:///C:/TranssionProjects/KejaSafe/app/shared/src/commonMain/kotlin/com/rotd/kejasafe/auth/LoginViewModel.kt)
Integrate biometric login logic.

## Verification Plan

### Automated Tests
- Unit tests for `BiometricAuthenticator` behavior (mocked).
- Integration tests for `AuthRepository` biometric flow.

### Manual Verification
1. Log in with email/password.
2. Enable biometric login in settings (or via a prompt).
3. Log out.
4. Tap the fingerprint icon on the login screen.
5. Verify biometric prompt appears and successful authentication logs the user in.
6. Verify failed biometric authentication (wrong finger) shows an error.
