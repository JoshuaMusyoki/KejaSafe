package com.rotd.havenpad.data.repoImpl

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.data.UserProfileDataSource
import com.rotd.havenpad.domain.biometric.BiometricAuthSupport
import com.rotd.havenpad.domain.biometric.BiometricAuthenticator
import com.rotd.havenpad.domain.biometric.createBiometricAuthenticator
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.domain.storage.SecureStorage
import com.rotd.havenpad.domain.storage.createSecureStorage
import com.rotd.havenpad.network.SessionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth + SQL Connect profile ([UserProfileDataSource] / [com.rotd.havenpad.data.SqlConnectUserProfileDataSource]).
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val profiles: UserProfileDataSource,
    biometricAuthenticator: BiometricAuthenticator = createBiometricAuthenticator(),
    secureStorage: SecureStorage = createSecureStorage(),
) : AuthRepository {
    private val biometricSupport = BiometricAuthSupport(biometricAuthenticator, secureStorage)

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
                return@AuthStateListener
            }
            profiles.notifySignedIn(firebaseUser.uid)
            val cached = SessionManager.currentUser.value
                ?.takeIf { it.id == firebaseUser.uid }
            trySend(cached)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        fullName: String,
        role: UserRole,
    ): Result<User> {
        return try {
            val trimmedEmail = email.trim()
            auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Registration succeeded but no uid"))
            profiles.notifySignedIn(uid)
            val profile = profiles.upsertMyUser(
                email = trimmedEmail,
                username = username.trim(),
                fullName = fullName.trim(),
                role = role,
            ).getOrElse { return Result.failure(it) }
            commitSession(profile)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "registerWithEmail failed", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            loadOrBootstrapProfile(
                email = email.trim(),
                usernameFallback = email.substringBefore("@"),
                fullNameFallback = email.substringBefore("@"),
                role = UserRole.STUDENT,
            )
        } catch (e: Exception) {
            Log.e(TAG, "signInWithEmail failed", e)
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun signInWithIdentifier(identifier: String, password: String): Result<User> {
        val trimmed = identifier.trim()
        val email = if (trimmed.contains("@")) {
            trimmed
        } else {
            profiles.findEmailByUsername(trimmed)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "No account found for that username. Sign in with email after migrating to Firebase Auth."
                    )
                )
        }
        return signInWithEmail(email, password)
    }

    override suspend fun signInWithGoogle(idToken: String, role: UserRole): Result<User> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            if (auth.currentUser != null) {
                val alreadyGoogle = auth.currentUser?.providerData?.any {
                    it.providerId == GoogleAuthProvider.PROVIDER_ID
                } == true
                if (!alreadyGoogle) {
                    auth.signOut()
                }
            }
            auth.signInWithCredential(credential).await()
            val firebaseUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("Google sign-in failed"))
            val email = firebaseUser.email
                ?: firebaseUser.providerData
                    .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                    ?.email
                ?: return Result.failure(IllegalStateException("Google account has no email"))
            profiles.notifySignedIn(firebaseUser.uid)
            val existing = profiles.getMyUser()
            val profile = if (existing != null) {
                existing
            } else {
                val username = email.substringBefore("@").take(40)
                profiles.upsertMyUser(
                    email = email,
                    username = username,
                    fullName = firebaseUser.displayName?.ifBlank { username } ?: username,
                    role = role,
                    googleId = firebaseUser.uid,
                ).getOrElse { return Result.failure(it) }
            }
            commitSession(profile)
            Result.success(profile)
        } catch (e: FirebaseAuthUserCollisionException) {
            recoverGoogleSignIn(credential, role, e)
        } catch (e: Exception) {
            if (isCredentialAlreadyInUse(e) || isAccountExistsWithDifferentCredential(e)) {
                recoverGoogleSignIn(credential, role, e)
            } else {
                Result.failure(friendlyAuthError(e))
            }
        }
    }

    private suspend fun recoverGoogleSignIn(
        credential: com.google.firebase.auth.AuthCredential,
        role: UserRole,
        original: Exception,
    ): Result<User> {
        return try {
            auth.signOut()
            auth.signInWithCredential(credential).await()
            val firebaseUser = auth.currentUser
                ?: return Result.failure(friendlyAuthError(original))
            val email = firebaseUser.email
                ?: return Result.failure(IllegalStateException("Google account has no email"))
            profiles.notifySignedIn(firebaseUser.uid)
            val profile = profiles.getMyUser() ?: profiles.upsertMyUser(
                email = email,
                username = email.substringBefore("@").take(40),
                fullName = firebaseUser.displayName ?: email.substringBefore("@"),
                role = role,
                googleId = firebaseUser.uid,
            ).getOrElse { return Result.failure(it) }
            commitSession(profile)
            Result.success(profile)
        } catch (retry: Exception) {
            Result.failure(friendlyAuthError(retry.takeIf { it.message != null } ?: original))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        return try {
            auth.sendPasswordResetEmail(trimmed).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun refreshProfile(): Result<User> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        profiles.notifySignedIn(uid)
        val user = profiles.getMyUser()
            ?: return Result.failure(IllegalStateException("Profile not found"))
        commitSession(user)
        return Result.success(user)
    }

    override suspend fun updateProfile(request: UpdateProfileRequest): Result<User> {
        return profiles.updateMyProfile(request).onSuccess { commitSession(it) }
    }

    override suspend fun updateProfilePicture(url: String): Result<User> {
        return profiles.updateProfilePicture(url).onSuccess { commitSession(it) }
    }

    override suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus> {
        return profiles.submitKyc(
            documentType = documentType,
            documentNumber = documentNumber,
            documentFrontImageUrl = documentFrontImageUrl,
            documentBackImageUrl = documentBackImageUrl,
            selfieImageUrl = selfieImageUrl,
        ).onSuccess { status ->
            profiles.getMyUser()?.let { commitSession(it.copy(kycStatus = status)) }
        }
    }

    override suspend fun submitLandlordKyc(request: com.rotd.havenpad.LandlordKycRequest): Result<User> =
        com.rotd.havenpad.network.AuthClient.submitLandlordKyc(request).onSuccess { commitSession(it) }

    override suspend fun submitStudentKyc(request: com.rotd.havenpad.StudentKycRequest): Result<User> =
        com.rotd.havenpad.network.AuthClient.submitStudentKyc(request).onSuccess { commitSession(it) }

    override suspend fun signOut() {
        SessionManager.clearSession()
        profiles.notifySignedOut()
        auth.signOut()
    }

    override fun isSignedIn(): Boolean = auth.currentUser != null

    override fun isBiometricAvailable(): Boolean = biometricSupport.isAvailable()

    override fun isBiometricEnabled(): Boolean = biometricSupport.isEnabled()

    override suspend fun enableBiometricLogin(identifier: String, password: String): Result<Unit> =
        biometricSupport.enable(identifier, password)

    override suspend fun disableBiometricLogin() {
        biometricSupport.disable()
    }

    override suspend fun signInWithBiometrics(): Result<User> {
        val credentials = biometricSupport.unlockCredentials().getOrElse { return Result.failure(it) }
        return signInWithIdentifier(credentials.first, credentials.second)
    }

    private suspend fun loadOrBootstrapProfile(
        email: String,
        usernameFallback: String,
        fullNameFallback: String,
        role: UserRole,
    ): Result<User> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        profiles.notifySignedIn(uid)
        val existing = profiles.getMyUser()
        val profile = existing ?: profiles.upsertMyUser(
            email = email,
            username = usernameFallback,
            fullName = fullNameFallback,
            role = role,
        ).getOrElse { return Result.failure(it) }
        commitSession(profile)
        return Result.success(profile)
    }

    private suspend fun commitSession(user: User) {
        val token = runCatching {
            auth.currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull().orEmpty()
        SessionManager.saveSession(token.ifBlank { "firebase:${user.id}" }, user)
    }

    private fun isCredentialAlreadyInUse(e: Exception): Boolean {
        val message = e.message.orEmpty()
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        return code.contains("ERROR_CREDENTIAL_ALREADY_IN_USE", ignoreCase = true) ||
            code.contains("CREDENTIAL_ALREADY_IN_USE", ignoreCase = true) ||
            message.contains("credential is already associated", ignoreCase = true) ||
            message.contains("already linked", ignoreCase = true)
    }

    private fun isAccountExistsWithDifferentCredential(e: Exception): Boolean {
        val message = e.message.orEmpty()
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        return code.contains("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", ignoreCase = true) ||
            message.contains("account-exists-with-different-credential", ignoreCase = true) ||
            message.contains("email address is already in use", ignoreCase = true)
    }

    private fun friendlyAuthError(e: Exception): Exception {
        val message = e.message.orEmpty()
        return when {
            isAccountExistsWithDifferentCredential(e) -> IllegalStateException(
                "This email is already registered. Sign in with that method, or continue with Google after signing out."
            )
            isCredentialAlreadyInUse(e) -> IllegalStateException(
                "This Google account is already registered. Please try signing in again."
            )
            else -> e
        }
    }

    private fun mapAuthException(e: Exception): Exception {
        val raw = buildString {
            append(e.message.orEmpty())
            e.cause?.message?.let { append(' ').append(it) }
        }
        when {
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                return Exception(
                    "Firebase Auth isn’t configured for project “havenpad”. " +
                        "Open Firebase Console → Authentication → Get started, then enable Email/Password."
                )
            raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                raw.contains("PASSWORD_LOGIN_DISABLED", ignoreCase = true) ->
                return Exception("Email/Password sign-in is not enabled in Firebase Console → Authentication → Sign-in method.")
            raw.contains("API_KEY_NOT_VALID", ignoreCase = true) ||
                raw.contains("INVALID_API_KEY", ignoreCase = true) ->
                return Exception("Invalid Firebase API key. Re-download google-services.json for project havenpad.")
            raw.contains("PROJECT_NOT_FOUND", ignoreCase = true) ->
                return Exception("Firebase project “havenpad” was not found. Check google-services.json.")
            raw.contains("network error", ignoreCase = true) ||
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("ERROR_NETWORK_REQUEST_FAILED", ignoreCase = true) ->
                return Exception(
                    "Can’t reach Firebase Auth. Check internet, disable VPN if needed, " +
                        "and confirm Identity Toolkit API is enabled for project havenpad."
                )
        }

        if (e !is FirebaseAuthException) return e
        return when (e.errorCode) {
            "ERROR_WRONG_PASSWORD" -> Exception("Incorrect password for this email.")
            "ERROR_USER_NOT_FOUND" -> Exception("No account found with this email.")
            "ERROR_INVALID_EMAIL" -> Exception("The email address is badly formatted.")
            "ERROR_WEAK_PASSWORD" -> Exception("The password must be 6 characters long or more.")
            "ERROR_USER_DISABLED" -> Exception("This user account has been disabled.")
            "ERROR_TOO_MANY_REQUESTS" -> Exception("Too many attempts. Please try again later.")
            "ERROR_OPERATION_NOT_ALLOWED" -> Exception("Email/Password sign-in is not enabled in Firebase.")
            "ERROR_NETWORK_REQUEST_FAILED" -> Exception(
                "Can’t reach Firebase Auth. Check internet / VPN, then retry."
            )
            "ERROR_INVALID_CREDENTIAL" -> Exception("Invalid email or password. Try again or reset your password.")
            "ERROR_EMAIL_ALREADY_IN_USE" -> Exception("This email is already registered. Try signing in.")
            "ERROR_INTERNAL_ERROR" -> Exception(
                e.message?.substringAfter('[')?.substringBefore(']')?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { "Firebase Auth error: $it" }
                    ?: "Firebase Auth failed. Check Authentication is enabled in Firebase Console."
            )
            else -> e
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
