package com.rotd.kejasafe.data

import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole

/**
 * Profile persistence keyed by Firebase Auth uid.
 * Android uses SQL Connect; other targets use a local cache.
 */
interface UserProfileDataSource {
    /** Call after Firebase Auth signs in so the active uid is known. */
    fun notifySignedIn(uid: String) {}

    fun notifySignedOut() {}

    suspend fun getMyUser(): User?

    suspend fun upsertMyUser(
        email: String,
        username: String,
        fullName: String,
        role: UserRole,
        googleId: String? = null,
        phone: String? = null,
    ): Result<User>

    suspend fun updateMyProfile(request: UpdateProfileRequest): Result<User>

    suspend fun updateProfilePicture(url: String): Result<User>

    suspend fun findEmailByUsername(username: String): String?

    suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus>
}
