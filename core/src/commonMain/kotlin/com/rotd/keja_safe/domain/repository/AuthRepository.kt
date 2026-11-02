package com.rotd.keja_safe.domain.repository

import com.rotd.havenpad.domain.chat.User
import kotlinx.coroutines.flow.Flow

/**
 * Legacy Stawi/chat auth surface (Firestore-oriented).
 * HavenPad app auth uses [com.rotd.havenpad.domain.repository.AuthRepository].
 * Phone OTP requires an Activity on Android; kept as [Any] so commonMain compiles.
 */
interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithPhone(activity: Any, phoneNumber: String): Result<Unit>
    suspend fun verifyOtp(otp: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String, linkIfPhoneSession: Boolean = true): Result<Unit>
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signInAsGuest(): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerUser(username: String): Result<Unit>
    suspend fun updateAppPin(pin: String): Result<Unit>
    suspend fun updateProfilePicture(base64Image: String): Result<Unit>
    suspend fun updateUserProfile(
        username: String,
        email: String?,
        phoneNumber: String?,
        professionalBackground: String? = null,
        ambitions: String? = null,
        interests: List<String>? = null,
    ): Result<Unit>
    suspend fun addPoints(points: Int): Result<Unit>
    suspend fun updateFcmToken(): Result<Unit>
    suspend fun requestDataDeletion(
        categories: Set<String>,
        details: String = ""
    ): Result<Unit>
    suspend fun signOut()
    suspend fun getActiveUsers(): Flow<List<User>>
    suspend fun updatePresence(isActive: Boolean)
    fun hasPhoneProvider(): Boolean
    fun isSignedIn(): Boolean
}
