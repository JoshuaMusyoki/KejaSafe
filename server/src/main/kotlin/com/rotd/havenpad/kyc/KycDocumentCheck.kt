package com.rotd.havenpad.kyc

import com.rotd.havenpad.DocumentCheck
import com.rotd.havenpad.UserRole
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Checks that an uploaded ID or proof of address is a real, readable photo
 * and that the typed details are complete. The printed name is not read from
 * the image, so a person still confirms the document before the account is verified.
 */
object KycDocumentCheck {
    fun assess(
        fullName: String,
        role: UserRole,
        documentType: String,
        documentNumber: String,
        address: String,
        idBytes: ByteArray?,
        proofBytes: ByteArray?,
    ): DocumentCheck {
        val findings = mutableListOf<String>()
        var earned = 0
        var possible = 0
        var blocked = false

        fun check(passed: Boolean, points: Int, ok: String, problem: String) {
            possible += points
            if (passed) {
                earned += points
                findings += ok
            } else {
                blocked = true
                findings += problem
            }
        }

        val idLabel = if (role == UserRole.STUDENT) "Student ID" else documentType.ifBlank { "ID" }
        val idPhoto = inspectPhoto(idBytes)
        check(
            idPhoto.readable,
            40,
            "$idLabel photo is large enough to read.",
            idPhoto.problem ?: "$idLabel photo needs a clearer picture.",
        )
        check(
            numberLooksValid(documentType, documentNumber),
            20,
            "The number on the form has a normal shape.",
            "The document number does not look like a ${documentType.ifBlank { "ID" }} number.",
        )
        val nameParts = fullName.trim().split(Regex("\\s+")).filter { it.length >= 2 }
        check(
            nameParts.size >= 2,
            10,
            "The profile has a first and last name to compare with the document.",
            "The profile name is only one word. Compare it carefully with the document.",
        )
        if (role != UserRole.STUDENT) {
            check(
                address.trim().length >= 12 && address.any { it.isLetter() },
                10,
                "A residential address was entered.",
                "The residential address is too short to match a proof of address.",
            )
            val proof = inspectPhoto(proofBytes)
            check(
                proof.readable,
                20,
                "Proof of address photo is large enough to read.",
                proof.problem ?: "Proof of address needs a clearer picture.",
            )
        }

        val score = if (possible == 0) 0 else ((earned * 100.0) / possible).roundToInt()
        val decision = if (!blocked && score >= 85) {
            "Photos look readable. Confirm the printed name and address, then verify."
        } else {
            "Needs your review before this account can be verified."
        }
        return DocumentCheck(score = score, decision = decision, findings = findings)
    }

    private fun numberLooksValid(documentType: String, number: String): Boolean {
        val value = number.trim()
        return when (documentType) {
            "Passport" -> value.matches(Regex("^[A-Za-z0-9]{6,12}$"))
            "Student ID" -> value.length >= 3
            else -> value.matches(Regex("^\\d{6,10}$"))
        }
    }

    private fun inspectPhoto(bytes: ByteArray?): PhotoInspection {
        if (bytes == null || bytes.isEmpty()) {
            return PhotoInspection(false, "The photo was not uploaded.")
        }
        if (bytes.size < 15_000) {
            return PhotoInspection(false, "The photo file is too small to show a real document.")
        }
        if (looksLikeWebp(bytes)) {
            return PhotoInspection(true, null)
        }
        val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
            ?: return PhotoInspection(false, "The file is not a readable photo.")
        if (image.width < 640 || image.height < 400) {
            return PhotoInspection(false, "The photo is too small. Ask for a closer picture of the whole document.")
        }
        val stepX = (image.width / 48).coerceAtLeast(1)
        val stepY = (image.height / 48).coerceAtLeast(1)
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        var y = 0
        while (y < image.height) {
            var x = 0
            while (x < image.width) {
                val color = image.getRGB(x, y)
                val red = (color shr 16) and 0xff
                val green = (color shr 8) and 0xff
                val blue = color and 0xff
                val luma = (red * 3 + green * 6 + blue) / 10.0
                sum += luma
                sumSq += luma * luma
                count++
                x += stepX
            }
            y += stepY
        }
        if (count == 0) return PhotoInspection(false, "The photo could not be read.")
        val mean = sum / count
        val variance = sumSq / count - mean * mean
        if (variance < 40.0 || mean < 12.0 || mean > 248.0) {
            return PhotoInspection(false, "The photo looks blank, too dark, or washed out.")
        }
        return PhotoInspection(true, null)
    }

    private fun looksLikeWebp(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val riff = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte()
        val webp = bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        return riff && webp
    }

    private data class PhotoInspection(val readable: Boolean, val problem: String?)
}
