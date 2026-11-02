package com.rotd.havenpad.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.RegistrationRequest
import com.rotd.havenpad.onboardingError
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.RoommatePreferences
import com.rotd.havenpad.listing.ListingService
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
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.imageio.ImageIO

class AccountRemovedException : RuntimeException(
    "This account has been removed from HavenPad. Contact the HavenPad team if this is a mistake."
)

object AuthService {
    private val jwtSecret = ServerConfig.jwtSecret
    private val jwtIssuer = ServerConfig.jwtIssuer
    private val jwtAudience = ServerConfig.jwtAudience
    private val verifier by lazy {
        val audiences = listOf(ServerConfig.googleClientId, ServerConfig.googleIosClientId)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(audiences.ifEmpty { listOf(ServerConfig.googleClientId) })
            .build()
    }

    fun generateToken(user: User): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", user.id)
            .withClaim("role", user.role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + 86400000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    fun registerUser(request: RegistrationRequest): User? {
        request.onboardingError()?.let { throw IllegalArgumentException(it) }
        return createAccount(request, request.role)
    }

    fun createLandlord(request: com.rotd.havenpad.CreateLandlordRequest): User? =
        createAccount(
            RegistrationRequest(
                email = request.email.trim(),
                username = request.username.trim(),
                fullName = request.fullName.trim(),
                password = request.password,
                role = UserRole.LANDLORD,
            ),
            UserRole.LANDLORD,
        )

    fun ensureBootstrapAdmin() {
        transaction {
            val email = ServerConfig.adminEmail.trim()
            val existing = UsersTable.selectAll()
                .where { UsersTable.email.lowerCase() eq email.lowercase() }
                .singleOrNull()
            if (existing != null) {
                val id = existing[UsersTable.id]
                val hash = existing[UsersTable.passwordHash]
                val passwordOk = hash != null &&
                    BCrypt.verifyer().verify(ServerConfig.adminPassword.toCharArray(), hash).verified
                val usernameTaken = UsersTable.selectAll()
                    .where {
                        (UsersTable.username.lowerCase() eq "admin") and (UsersTable.id neq id)
                    }
                    .singleOrNull() != null
                val needsAdminName = existing[UsersTable.username].lowercase() != "admin" && !usernameTaken
                if (
                    existing[UsersTable.role] != UserRole.ADMIN ||
                    existing[UsersTable.active] == false ||
                    !passwordOk ||
                    needsAdminName
                ) {
                    UsersTable.update({ UsersTable.id eq id }) {
                        it[UsersTable.role] = UserRole.ADMIN
                        it[UsersTable.active] = true
                        if (!passwordOk) {
                            it[UsersTable.passwordHash] = BCrypt.withDefaults()
                                .hashToString(12, ServerConfig.adminPassword.toCharArray())
                        }
                        if (needsAdminName) {
                            it[UsersTable.username] = "admin"
                        }
                    }
                }
                return@transaction
            }
            val username = uniqueUsername("admin")
            val id = UUID.randomUUID().toString()
            val passwordHash = BCrypt.withDefaults().hashToString(12, ServerConfig.adminPassword.toCharArray())
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.email] = email
                it[UsersTable.username] = username
                it[UsersTable.fullName] = "HavenPad Admin"
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.role] = UserRole.ADMIN
                it[UsersTable.kycStatus] = com.rotd.havenpad.KycStatus.VERIFIED
                it[UsersTable.active] = true
                it[UsersTable.createdAt] = System.currentTimeMillis()
            }
        }
    }

    fun isActive(userId: String): Boolean = transaction {
        UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.active) == true
    }

    fun deactivateUser(userId: String) = transaction {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[active] = false
        }
    }

    private fun createAccount(request: RegistrationRequest, role: UserRole): User? = transaction {
        val email = request.email.trim()
        val username = request.username.trim()
        val fullName = request.fullName.trim()
        if (email.isBlank() || username.isBlank() || fullName.isBlank() || request.password.length < 8) {
            return@transaction null
        }
        val existingUser = UsersTable.selectAll()
            .where {
                (UsersTable.email.lowerCase() eq email.lowercase()) or
                    (UsersTable.username.lowerCase() eq username.lowercase())
            }
            .singleOrNull()
        if (existingUser != null) return@transaction null

        val id = UUID.randomUUID().toString()
        val passwordHash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())
        val now = System.currentTimeMillis()
        val kyc = if (role == UserRole.AGENT) KycStatus.PENDING else KycStatus.NOT_SUBMITTED
        val phone = request.phone.trim().ifBlank { null }
        val major = request.major.trim().ifBlank { null }
        val businessName = request.businessName.trim().ifBlank { null }
        val university = request.university.trim().ifBlank { null }
        val studentNumber = request.studentNumber.trim().ifBlank { null }
        val nationalId = request.nationalIdNumber.trim().ifBlank { null }
        val address = request.residentialAddress.trim().ifBlank { null }
        val license = request.licenseNumber.trim().ifBlank { null }
        val area = request.serviceArea.trim().ifBlank { null }

        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.username] = username
            it[UsersTable.fullName] = fullName
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.kycStatus] = kyc
            it[UsersTable.phone] = phone
            it[UsersTable.major] = major
            it[UsersTable.yearOfStudy] = request.yearOfStudy
            it[UsersTable.businessName] = businessName
            it[UsersTable.university] = university
            it[UsersTable.studentNumber] = studentNumber
            it[UsersTable.nationalId] = nationalId
            it[UsersTable.residentialAddress] = address
            it[UsersTable.licenseNumber] = license
            it[UsersTable.serviceArea] = area
            it[UsersTable.active] = true
            it[UsersTable.createdAt] = now
        }

        User(
            id = id,
            email = email,
            username = username,
            fullName = fullName,
            role = role,
            kycStatus = kyc,
            phone = phone,
            major = major,
            yearOfStudy = request.yearOfStudy,
            businessName = businessName,
            university = university,
            studentNumber = studentNumber,
            nationalIdNumber = nationalId,
            residentialAddress = address,
            licenseNumber = license,
            serviceArea = area,
        )
    }

    private fun uniqueUsername(base: String): String {
        var candidate = base
        var n = 1
        while (
            UsersTable.selectAll()
                .where { UsersTable.username.lowerCase() eq candidate.lowercase() }
                .singleOrNull() != null
        ) {
            n += 1
            candidate = "$base-$n"
        }
        return candidate
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
            ?: if (key.equals("admin", ignoreCase = true)) {
                UsersTable.selectAll()
                    .where { UsersTable.email.lowerCase() eq ServerConfig.adminEmail.lowercase() }
                    .singleOrNull()
            } else {
                null
            }
            ?: return@transaction null
        if (!userRow[UsersTable.active]) throw AccountRemovedException()
        val passwordHash = userRow[UsersTable.passwordHash] ?: return@transaction null

        val result = BCrypt.verifyer().verify(password.toCharArray(), passwordHash)
        if (result.verified) userRow.toUser() else null
    }

    fun authenticateWithGoogle(idToken: String, role: UserRole): User? = transaction {
        if (ServerConfig.googleClientId.isBlank()) return@transaction null
        val token = verifier.verify(idToken) ?: return@transaction null
        val payload = token.payload
        val googleId = payload.subject
        val email = payload.email?.trim().orEmpty()
        if (email.isBlank()) return@transaction null
        val name = (payload["name"] as? String)?.trim().orEmpty()
        val pictureUrl = payload["picture"] as? String

        val byGoogle = UsersTable.selectAll()
            .where { UsersTable.googleId eq googleId }
            .singleOrNull()
        if (byGoogle != null) {
            if (!byGoogle[UsersTable.active]) throw AccountRemovedException()
            return@transaction byGoogle.toUser()
        }

        // Same email as an email/password account → link Google and keep that profile.
        val byEmail = UsersTable.selectAll()
            .where { UsersTable.email.lowerCase() eq email.lowercase() }
            .singleOrNull()
        if (byEmail != null) {
            if (!byEmail[UsersTable.active]) throw AccountRemovedException()
            val userId = byEmail[UsersTable.id]
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.googleId] = googleId
                if (byEmail[UsersTable.profilePictureUrl].isNullOrBlank() && !pictureUrl.isNullOrBlank()) {
                    it[UsersTable.profilePictureUrl] = pictureUrl
                }
                if (byEmail[UsersTable.fullName].isBlank() && name.isNotBlank()) {
                    it[UsersTable.fullName] = name
                }
            }
            return@transaction getUserById(userId)
        }

        val id = UUID.randomUUID().toString()
        val username = uniqueUsername(email.substringBefore("@").ifBlank { "haven" })
        val assignedRole = if (role == UserRole.ADMIN) UserRole.STUDENT else role
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.username] = username
            it[UsersTable.fullName] = name.ifBlank { username }
            it[UsersTable.role] = assignedRole
            it[UsersTable.googleId] = googleId
            it[UsersTable.profilePictureUrl] = pictureUrl
            it[UsersTable.active] = true
            it[UsersTable.createdAt] = System.currentTimeMillis()
        }
        getUserById(id)
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

    fun saveProfilePhoto(userId: String, fileName: String, imageBase64: String): User? {
        if (userId.isBlank()) return null
        val original = decodeProfilePhoto(imageBase64)
        val bytes = shrinkPhoto(original)
        val safeId = userId.filter { it.isLetterOrDigit() || it == '-' }
        if (safeId.isBlank()) return null
        val dir = File(ListingService.uploadRoot, "avatars").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.startsWith("$safeId.") }?.forEach { it.delete() }
        File(dir, "$safeId.jpg").writeBytes(bytes)
        val url = "/uploads/avatars/$safeId.jpg"
        val updated = transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.profilePictureUrl] = url
            }
        }
        if (updated == 0) return null
        return getUserById(userId)
    }

    fun shrinkStoredAvatars() {
        val dir = File(ListingService.uploadRoot, "avatars")
        if (!dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (!file.isFile || file.length() < 80_000) return@forEach
            val shrunk = runCatching { shrinkPhoto(file.readBytes()) }.getOrNull() ?: return@forEach
            if (shrunk.size >= file.length()) return@forEach
            val jpg = File(dir, "${file.nameWithoutExtension}.jpg")
            jpg.writeBytes(shrunk)
            if (jpg.name != file.name) file.delete()
            val newUrl = "/uploads/avatars/${jpg.name}"
            val oldUrl = "/uploads/avatars/${file.name}"
            if (newUrl != oldUrl) {
                transaction {
                    UsersTable.update({ UsersTable.profilePictureUrl eq oldUrl }) {
                        it[UsersTable.profilePictureUrl] = newUrl
                    }
                }
            }
        }
    }

    private fun shrinkPhoto(bytes: ByteArray): ByteArray {
        val source = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
        val maxEdge = 512
        val longest = maxOf(source.width, source.height).coerceAtLeast(1)
        val ratio = minOf(1.0, maxEdge.toDouble() / longest)
        val width = (source.width * ratio).toInt().coerceAtLeast(1)
        val height = (source.height * ratio).toInt().coerceAtLeast(1)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, width, height)
        graphics.drawImage(source, 0, 0, width, height, null)
        graphics.dispose()
        val out = ByteArrayOutputStream()
        if (!ImageIO.write(image, "jpg", out)) return bytes
        return out.toByteArray()
    }

    private fun decodeProfilePhoto(base64: String): ByteArray {
        val cleaned = base64.substringAfter("base64,", base64).trim()
        val bytes = runCatching { Base64.getDecoder().decode(cleaned) }.getOrElse {
            throw IllegalArgumentException("Could not read that photo.")
        }
        require(bytes.isNotEmpty()) { "That photo is empty." }
        require(bytes.size <= 3 * 1024 * 1024) { "Choose a photo under 3MB." }
        return bytes
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
            businessName = this[UsersTable.businessName],
            university = this[UsersTable.university],
            studentNumber = this[UsersTable.studentNumber],
            nationalIdNumber = this[UsersTable.nationalId],
            residentialAddress = this[UsersTable.residentialAddress],
            licenseNumber = this[UsersTable.licenseNumber],
            serviceArea = this[UsersTable.serviceArea],
        )
    }
}
