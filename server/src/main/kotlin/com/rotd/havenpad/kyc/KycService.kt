package com.rotd.havenpad.kyc

import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.KycSubmission
import com.rotd.havenpad.LandlordKycRecord
import com.rotd.havenpad.LandlordKycRequest
import com.rotd.havenpad.StudentKycRequest
import com.rotd.havenpad.User
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.auth.AuthService
import com.rotd.havenpad.config.ServerConfig
import com.rotd.havenpad.db.KycTable
import com.rotd.havenpad.db.UsersTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.io.File
import java.util.Base64
import java.util.UUID

object KycService {
    private const val MAX_BYTES = 3 * 1024 * 1024

    fun submitKyc(submission: KycSubmission) = transaction {
        KycTable.insert {
            it[userId] = submission.userId
            it[documentType] = submission.documentType
            it[documentNumber] = submission.documentNumber
            it[documentFrontImageUrl] = submission.documentFrontImageUrl
            it[documentBackImageUrl] = submission.documentBackImageUrl
            it[selfieImageUrl] = submission.selfieImageUrl
            it[submittedAt] = System.currentTimeMillis()
        }

        UsersTable.update({ UsersTable.id eq submission.userId }) {
            it[kycStatus] = KycStatus.PENDING
        }
    }

    fun submitLandlordKyc(userId: String, request: LandlordKycRequest): User {
        val address = request.residentialAddress.trim()
        val number = request.documentNumber.trim()
        val type = request.documentType.trim().ifBlank { "National ID" }
        require(address.length >= 8) { "Enter the full residential address." }
        require(number.length >= 4) { "Enter the ID number." }
        require(type == "National ID" || type == "Passport") { "Choose a National ID or Passport." }

        val idBytes = decodeImage(request.idImageBase64)
        val proofBytes = decodeImage(request.addressProofBase64)
        val idPath = store(userId, "id", request.idImageName, idBytes)
        val proofPath = store(userId, "address", request.addressProofName, proofBytes)

        transaction {
            KycTable.deleteWhere { KycTable.userId eq userId }
            KycTable.insert {
                it[KycTable.userId] = userId
                it[documentType] = type
                it[documentNumber] = number
                it[documentFrontImageUrl] = idPath
                it[selfieImageUrl] = ""
                it[residentialAddress] = address
                it[addressProofImageUrl] = proofPath
                it[reviewNote] = null
                it[reviewedAt] = null
                it[submittedAt] = System.currentTimeMillis()
            }
            UsersTable.update({ UsersTable.id eq userId }) {
                it[kycStatus] = KycStatus.PENDING
            }
        }
        return AuthService.getUserById(userId)
            ?: error("Account was not found after saving documents.")
    }

    fun submitStudentKyc(userId: String, request: StudentKycRequest): User {
        val role = transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.get(UsersTable.role)
        }
        require(role == UserRole.STUDENT) { "Student ID verification is for student accounts." }
        val university = request.university.trim()
        val number = request.studentNumber.trim()
        require(university.length >= 2) { "Enter your university." }
        require(number.length >= 3) { "Enter your student number." }
        val idBytes = decodeImage(request.idImageBase64)
        val idPath = store(userId, "id", request.idImageName, idBytes)
        transaction {
            KycTable.deleteWhere { KycTable.userId eq userId }
            KycTable.insert {
                it[KycTable.userId] = userId
                it[documentType] = "Student ID"
                it[documentNumber] = number
                it[documentFrontImageUrl] = idPath
                it[selfieImageUrl] = ""
                it[residentialAddress] = university
                it[addressProofImageUrl] = ""
                it[reviewNote] = null
                it[reviewedAt] = null
                it[submittedAt] = System.currentTimeMillis()
            }
            UsersTable.update({ UsersTable.id eq userId }) {
                it[kycStatus] = KycStatus.PENDING
            }
        }
        return AuthService.getUserById(userId)
            ?: error("Account was not found after saving documents.")
    }

    fun getMine(userId: String): LandlordKycRecord = transaction {
        val status = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.get(UsersTable.kycStatus)
            ?: KycStatus.NOT_SUBMITTED
        val row = latestRow(userId)
        if (row == null) {
            LandlordKycRecord(status = status)
        } else {
            LandlordKycRecord(
                status = status,
                documentType = row[KycTable.documentType],
                documentNumber = row[KycTable.documentNumber],
                residentialAddress = row[KycTable.residentialAddress],
                hasIdCopy = row[KycTable.documentFrontImageUrl].isNotBlank(),
                hasAddressProof = row[KycTable.addressProofImageUrl].isNotBlank(),
                reviewNote = row[KycTable.reviewNote],
            )
        }
    }

    fun getKycStatus(userId: String): KycStatus = transaction {
        UsersTable.selectAll().where { UsersTable.id eq userId }
            .map { it.get(UsersTable.kycStatus) }
            .singleOrNull() ?: KycStatus.NOT_SUBMITTED
    }

    fun review(userId: String, status: KycStatus, note: String?) {
        require(status == KycStatus.VERIFIED || status == KycStatus.REJECTED) {
            "Choose verify or reject."
        }
        transaction {
            val updated = UsersTable.update({ UsersTable.id eq userId }) {
                it[kycStatus] = status
            }
            if (updated == 0) error("Account was not found.")
            val row = latestRow(userId)
            if (row != null) {
                KycTable.update({ KycTable.id eq row[KycTable.id] }) {
                    it[reviewNote] = note?.trim()?.ifBlank { null }
                    it[reviewedAt] = System.currentTimeMillis()
                }
            }
        }
    }

    fun readFile(userId: String, kind: String): Pair<ByteArray, String>? {
        val relative = transaction {
            val row = latestRow(userId) ?: return@transaction null
            when (kind) {
                "id" -> row[KycTable.documentFrontImageUrl]
                "address" -> row[KycTable.addressProofImageUrl]
                else -> null
            }
        } ?: return null
        val file = resolve(relative) ?: return null
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        return file.readBytes() to mime
    }

    private fun latestRow(userId: String) = KycTable.selectAll()
        .where { KycTable.userId eq userId }
        .orderBy(KycTable.submittedAt to SortOrder.DESC)
        .limit(1)
        .singleOrNull()

    private fun decodeImage(base64: String): ByteArray {
        val cleaned = base64.substringAfter("base64,", base64).trim()
        val bytes = runCatching { Base64.getDecoder().decode(cleaned) }.getOrElse {
            throw IllegalArgumentException("Could not read that image.")
        }
        require(bytes.isNotEmpty()) { "That image is empty." }
        require(bytes.size <= MAX_BYTES) { "Each image must be 3MB or smaller." }
        return bytes
    }

    private fun store(userId: String, kind: String, fileName: String, bytes: ByteArray): String {
        val ext = fileName.substringAfterLast('.', "jpg").lowercase().filter { it.isLetter() }.take(4)
        val safeExt = if (ext in setOf("jpg", "jpeg", "png", "webp")) ext else "jpg"
        val folderName = userId.filter { it.isLetterOrDigit() || it == '-' }
        val dir = File(kycRoot(), folderName).apply { mkdirs() }
        val stored = "${kind}_${UUID.randomUUID()}.$safeExt"
        File(dir, stored).writeBytes(bytes)
        return "$folderName/$stored"
    }

    private fun resolve(relative: String): File? {
        if (relative.isBlank() || relative.contains("..")) return null
        val root = kycRoot().canonicalFile
        val file = File(root, relative).canonicalFile
        if (!file.path.startsWith(root.path)) return null
        if (!file.isFile) return null
        return file
    }

    private fun kycRoot(): File = File(ServerConfig.kycDir).apply { mkdirs() }
}
