package com.rotd.havenpad.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.RoommatePreferences
import com.rotd.havenpad.UpdateProfileRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.Collections
import java.util.Date
import java.util.UUID

object AuthService {
    private val jwtSecret = ServerConfig.jwtSecret
    private val jwtIssuer = ServerConfig.jwtIssuer
    private val jwtAudience = ServerConfig.jwtAudience
    private val googleClientId = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com"

    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
        .setAudience(Collections.singletonList(googleClientId))
        .build()

    fun generateToken(user: User): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", user.id)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    fun registerUser(request: RegistrationRequest): User? = transaction {
        val existingUser = UsersTable.selectAll()
            .where { (UsersTable.email eq request.email) or (UsersTable.username eq request.username) }
            .singleOrNull()
        if (existingUser != null) return@transaction null

        val id = UUID.randomUUID().toString()
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = request.email
            it[UsersTable.username] = request.username
            it[UsersTable.fullName] = request.fullName
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = request.role
        }

        User(
            id = id,
            email = request.email,
            username = request.username,
            fullName = request.fullName,
            role = request.role
        )
    }

    fun loginUser(identifier: String, password: String): User? = transaction {
        val key = identifier.trim()
        if (key.isEmpty()) return@transaction null
        val userRow = UsersTable.selectAll()
            .where {
                (UsersTable.email.lowerCase() eq key.lowercase()) or
                    (UsersTable.username.lowerCase() eq key.lowercase())
            }
            .singleOrNull()
            ?: return@transaction null
        val passwordHash = userRow[UsersTable.passwordHash] ?: return@transaction null

        val result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash)
        if (result.verified) userRow.toUser() else null
    }

    fun authenticateWithGoogle(idToken: String, role: UserRole): User? = transaction {
        val token = verifier.verify(idToken) ?: return@transaction null
        val payload = token.payload
        val googleId = payload.subject
        val email = payload.email
        val name = payload["name"] as String
        val pictureUrl = payload["picture"] as? String

        val existingUser = UsersTable.selectAll()
            .where { UsersTable.googleId eq googleId }
            .singleOrNull()

        if (existingUser != null) {
            existingUser.toUser()
        } else {
            val id = UUID.randomUUID().toString()
            val username = email.substringBefore("@")
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.email] = email
                it[UsersTable.username] = username
                it[UsersTable.fullName] = name
                it[UsersTable.role] = role
                it[UsersTable.googleId] = googleId
                it[UsersTable.profilePictureUrl] = pictureUrl
            }
            User(
                id = id,
                email = email,
                username = username,
                fullName = name,
                role = role,
                profilePictureUrl = pictureUrl
            )
        }
    }

    fun getUserById(userId: String): User? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
    }

    fun updateProfile(userId: String, request: UpdateProfileRequest): User? = transaction {
        val fullName = request.fullName.trim()
        val username = request.username.trim()
        if (fullName.isBlank() || username.isBlank()) return@transaction null

        val usernameTaken = UsersTable.selectAll()
            .where { (UsersTable.username eq username) and (UsersTable.id neq userId) }
            .singleOrNull() != null
        if (usernameTaken) return@transaction null

        val prefs = RoommatePreferences.encode(request.roommatePreferences)

        val updated = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.fullName] = fullName
            it[UsersTable.username] = username
            it[UsersTable.phone] = request.phone?.trim()?.ifBlank { null }
            it[UsersTable.businessName] = request.businessName?.trim()?.ifBlank { null }
            it[UsersTable.major] = request.major?.trim()?.ifBlank { null }
            it[UsersTable.yearOfStudy] = request.yearOfStudy
            it[UsersTable.roommatePreferences] = prefs
        }
        if (updated == 0) return@transaction null
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
    }

    private fun ResultRow.toUser(): User {
        val prefs = RoommatePreferences.normalize(this[UsersTable.roommatePreferences])

        return User(
            id = this[UsersTable.id],
            email = this[UsersTable.email],
            username = this[UsersTable.username],
            fullName = this[UsersTable.fullName],
            role = this[UsersTable.role],
            kycStatus = this[UsersTable.kycStatus],
            profilePictureUrl = this[UsersTable.profilePictureUrl],
            major = this[UsersTable.major],
            yearOfStudy = this[UsersTable.yearOfStudy],
            roommatePreferences = prefs,
            phone = this[UsersTable.phone],
            businessName = this[UsersTable.businessName]
        )
    }
}
