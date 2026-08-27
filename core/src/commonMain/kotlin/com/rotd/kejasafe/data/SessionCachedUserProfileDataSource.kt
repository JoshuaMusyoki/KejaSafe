package com.rotd.kejasafe.data

import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.storage.LocalSettings
import kotlinx.serialization.json.Json

/**
 * Local profile mirror keyed by Firebase uid.
 * Used standalone on non-Android targets, and as an offline cache under SQL Connect on Android.
 */
class SessionCachedUserProfileDataSource : UserProfileDataSource {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun notifySignedIn(uid: String) {
        LocalSettings.putString(KEY_ACTIVE_UID, uid)
    }

    override fun notifySignedOut() {
        LocalSettings.putString(KEY_ACTIVE_UID, null)
    }

    /** Write-through helper for SQL Connect / session sync. */
    fun replaceCachedUser(user: User) {
        writeUser(user)
        indexUsername(user.username, user.email)
    }

    override suspend fun getMyUser(): User? {
        val uid = LocalSettings.getString(KEY_ACTIVE_UID) ?: return null
        return readUser(uid)
    }

    override suspend fun upsertMyUser(
        email: String,
        username: String,
        fullName: String,
        role: UserRole,
        googleId: String?,
        phone: String?,
    ): Result<User> {
        val uid = LocalSettings.getString(KEY_ACTIVE_UID)
            ?: return Result.failure(IllegalStateException("No signed-in user"))
        val existing = readUser(uid)
        val user = User(
            id = uid,
            email = email,
            username = username,
            fullName = fullName,
            role = role,
            kycStatus = existing?.kycStatus ?: KycStatus.NOT_SUBMITTED,
            profilePictureUrl = existing?.profilePictureUrl,
            major = existing?.major,
            yearOfStudy = existing?.yearOfStudy,
            roommatePreferences = existing?.roommatePreferences.orEmpty(),
            phone = phone ?: existing?.phone,
            businessName = existing?.businessName,
        )
        writeUser(user)
        indexUsername(username, email)
        return Result.success(user)
    }

    override suspend fun updateMyProfile(request: UpdateProfileRequest): Result<User> {
        val uid = LocalSettings.getString(KEY_ACTIVE_UID)
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = readUser(uid)
            ?: return Result.failure(IllegalStateException("Profile not found"))
        val updated = existing.copy(
            fullName = request.fullName.trim(),
            username = request.username.trim(),
            major = request.major?.trim()?.ifBlank { null },
            yearOfStudy = request.yearOfStudy,
            roommatePreferences = request.roommatePreferences,
            phone = request.phone?.trim()?.ifBlank { null },
            businessName = request.businessName?.trim()?.ifBlank { null },
        )
        writeUser(updated)
        indexUsername(updated.username, updated.email)
        return Result.success(updated)
    }

    override suspend fun updateProfilePicture(url: String): Result<User> {
        val uid = LocalSettings.getString(KEY_ACTIVE_UID)
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = readUser(uid)
            ?: return Result.failure(IllegalStateException("Profile not found"))
        val updated = existing.copy(profilePictureUrl = url)
        writeUser(updated)
        return Result.success(updated)
    }

    override suspend fun findEmailByUsername(username: String): String? {
        return LocalSettings.getString(usernameKey(username.trim().lowercase()))
    }

    override suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus> {
        val uid = LocalSettings.getString(KEY_ACTIVE_UID)
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val existing = readUser(uid)
            ?: return Result.failure(IllegalStateException("Profile not found"))
        writeUser(existing.copy(kycStatus = KycStatus.PENDING))
        return Result.success(KycStatus.PENDING)
    }

    @Deprecated("Use notifySignedIn", ReplaceWith("notifySignedIn(uid)"))
    fun bindUid(uid: String) = notifySignedIn(uid)

    @Deprecated("Use notifySignedOut", ReplaceWith("notifySignedOut()"))
    fun clearActiveUid() = notifySignedOut()

    private fun readUser(uid: String): User? {
        val raw = LocalSettings.getString(userKey(uid)) ?: return null
        return runCatching { json.decodeFromString(User.serializer(), raw) }.getOrNull()
    }

    private fun writeUser(user: User) {
        LocalSettings.putString(userKey(user.id), json.encodeToString(User.serializer(), user))
        LocalSettings.putString(KEY_ACTIVE_UID, user.id)
    }

    private fun indexUsername(username: String, email: String) {
        LocalSettings.putString(usernameKey(username.lowercase()), email)
    }

    private fun userKey(uid: String) = "sql_profile_$uid"
    private fun usernameKey(username: String) = "sql_username_$username"

    companion object {
        private const val KEY_ACTIVE_UID = "sql_profile_active_uid"
    }
}
