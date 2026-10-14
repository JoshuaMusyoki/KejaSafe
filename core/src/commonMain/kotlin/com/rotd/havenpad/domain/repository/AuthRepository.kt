package com.rotd.havenpad.domain.repository

import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.LandlordKycRequest
import com.rotd.havenpad.StudentKycRequest
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * HavenPad authentication against Firebase Auth + SQL Connect profile row.
 * [User.id] is always the Firebase Auth uid.
 */
interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun registerWithEmail(
        email: String,
        password: String,
        username: String,
        fullName: String,
        role: UserRole,
        details: RegistrationRequest? = null,
    ): Result<User>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** Resolves username → email via SQL Connect (when available), then signs in. */
    suspend fun signInWithIdentifier(identifier: String, password: String): Result<User>

    suspend fun signInWithGoogle(idToken: String, role: UserRole): Result<User>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun refreshProfile(): Result<User>

    suspend fun updateProfile(request: UpdateProfileRequest): Result<User>

    suspend fun updateProfilePicture(url: String): Result<User>

    suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus>

    suspend fun submitLandlordKyc(request: LandlordKycRequest): Result<User>

    suspend fun submitStudentKyc(request: StudentKycRequest): Result<User>

    suspend fun signOut()

    fun isSignedIn(): Boolean

    fun isBiometricAvailable(): Boolean

    fun isBiometricEnabled(): Boolean

    /**
     * After a successful password login, confirm biometrics then store credentials
     * on this device only.
     */
    suspend fun enableBiometricLogin(identifier: String, password: String): Result<Unit>

    suspend fun disableBiometricLogin()

    /** Prompt for fingerprint / Face ID, then sign in with stored credentials. */
    suspend fun signInWithBiometrics(): Result<User>
}
