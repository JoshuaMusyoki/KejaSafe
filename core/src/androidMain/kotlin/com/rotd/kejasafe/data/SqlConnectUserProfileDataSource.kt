package com.rotd.kejasafe.data

import com.rotd.kejasafe.KycStatus
import com.rotd.kejasafe.UpdateProfileRequest
import com.rotd.kejasafe.User
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.dataconnect.GetMyUserQuery

/**
 * Optional Firebase Data Connect (Cloud SQL) profile sync.
 *
 * **Paused:** the app uses [SessionCachedUserProfileDataSource] via
 * [createUserProfileDataSource] while `:server` + H2 handles API data.
 * Schema lives under `server/dataconnect/` for a future Cloud SQL cutover.
 *
 * To re-enable: wire this class in [createUserProfileDataSource], uncomment the
 * Cloud SQL blocks below, and set [DataConnectRuntime.cloudEnabled] = true after
 * `firebase deploy --only dataconnect`.
 */
class SqlConnectUserProfileDataSource(
    private val cache: SessionCachedUserProfileDataSource = SessionCachedUserProfileDataSource(),
) : UserProfileDataSource {

    // CLOUD SQL: private val connector get() = DataConnectRuntime.connector()

    override fun notifySignedIn(uid: String) {
        cache.notifySignedIn(uid)
    }

    override fun notifySignedOut() {
        cache.notifySignedOut()
    }

    override suspend fun getMyUser(): User? {
        val local = cache.getMyUser()
        // --- Cloud SQL (Data Connect) — paused while on H2 ---
        // if (!DataConnectRuntime.cloudEnabled) return local
        // return try {
        //     val remote = connector.getMyUser.execute().data.user?.toDomain()
        //     if (remote != null) {
        //         cache.replaceCachedUser(remote)
        //         remote
        //     } else local
        // } catch (e: Throwable) {
        //     local
        // }
        return local
    }

    override suspend fun upsertMyUser(
        email: String,
        username: String,
        fullName: String,
        role: UserRole,
        googleId: String?,
        phone: String?,
    ): Result<User> {
        val local = cache.upsertMyUser(email, username, fullName, role, googleId, phone)
            .getOrElse { return Result.failure(it) }
        // --- Cloud SQL (Data Connect) upsert — paused (needs Cloud SQL instance) ---
        // if (DataConnectRuntime.cloudEnabled) {
        //     connector.upsertMyUser.execute(email, username, fullName, role.name) {
        //         this.googleId = googleId
        //         this.phone = phone
        //     }
        // }
        return Result.success(local)
    }

    override suspend fun updateMyProfile(request: UpdateProfileRequest): Result<User> {
        val local = cache.updateMyProfile(request).getOrElse { return Result.failure(it) }
        // --- Cloud SQL (Data Connect) updateMyProfile — paused ---
        // if (DataConnectRuntime.cloudEnabled) {
        //     connector.updateMyProfile.execute(request.fullName.trim(), request.username.trim()) { ... }
        // }
        return Result.success(local)
    }

    override suspend fun updateProfilePicture(url: String): Result<User> {
        val local = cache.updateProfilePicture(url).getOrElse { return Result.failure(it) }
        // --- Cloud SQL (Data Connect) profile picture — paused ---
        // if (DataConnectRuntime.cloudEnabled) {
        //     connector.updateMyProfile.execute(local.fullName, local.username) { profilePictureUrl = url; ... }
        // }
        return Result.success(local)
    }

    override suspend fun findEmailByUsername(username: String): String? {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return null
        cache.findEmailByUsername(trimmed)?.let { return it }
        // --- Cloud SQL (Data Connect) GetUserByUsername — paused ---
        // if (DataConnectRuntime.cloudEnabled) {
        //     return connector.getUserByUsername.execute(trimmed).data.users.firstOrNull()?.email
        // }
        return null
    }

    override suspend fun submitKyc(
        documentType: String,
        documentNumber: String,
        documentFrontImageUrl: String,
        documentBackImageUrl: String?,
        selfieImageUrl: String,
    ): Result<KycStatus> {
        val local = cache.submitKyc(
            documentType,
            documentNumber,
            documentFrontImageUrl,
            documentBackImageUrl,
            selfieImageUrl,
        ).getOrElse { return Result.failure(it) }
        // --- Cloud SQL (Data Connect) SubmitKyc — paused ---
        // if (DataConnectRuntime.cloudEnabled) {
        //     connector.submitKyc.execute(...) { documentBackImageUrl = ... }
        // }
        return Result.success(local)
    }

    /** Mapping helper for when Cloud SQL getMyUser is uncommented. */
    @Suppress("unused")
    private fun GetMyUserQuery.Data.User.toDomain(): User = User(
        id = id,
        email = email,
        username = username,
        fullName = fullName,
        role = parseRole(role),
        kycStatus = parseKyc(kycStatus),
        profilePictureUrl = profilePictureUrl,
        major = major,
        yearOfStudy = yearOfStudy,
        roommatePreferences = decodeRoommatePreferences(roommatePreferences),
        phone = phone,
        businessName = businessName,
    )

    private fun parseRole(raw: String): UserRole =
        runCatching { UserRole.valueOf(raw) }.getOrDefault(UserRole.STUDENT)

    private fun parseKyc(raw: String): KycStatus =
        runCatching { KycStatus.valueOf(raw) }.getOrDefault(KycStatus.NOT_SUBMITTED)

    companion object {
        private const val PREFS_SEP = "|"

        fun encodeRoommatePreferences(prefs: List<String>): String? =
            prefs.map { it.trim() }.filter { it.isNotEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.joinToString(PREFS_SEP)

        fun decodeRoommatePreferences(raw: String?): List<String> =
            raw?.split(PREFS_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
    }
}
