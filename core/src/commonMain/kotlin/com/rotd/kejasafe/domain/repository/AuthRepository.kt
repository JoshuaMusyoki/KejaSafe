package com.rotd.kejasafe.domain.repository

import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * KejaSafe authentication against Firebase Auth + SQL Connect profile row.
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

    suspend fun signOut()

    fun isSignedIn(): Boolean
}
