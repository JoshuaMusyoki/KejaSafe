package com.rotd.havenpad.profile

import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.UserRole

internal fun kycStatusLabel(status: KycStatus): String = when (status) {
    KycStatus.VERIFIED -> "Verified"
    KycStatus.PENDING -> "Pending"
    KycStatus.REJECTED -> "Rejected"
    KycStatus.NOT_SUBMITTED -> "Not submitted"
}

internal fun nameInitials(fullName: String): String {
    val parts = fullName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> "${parts.first().first()}${parts[1].first()}".uppercase()
    }
}

internal fun shortValue(value: String?, empty: String = "—"): String {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return empty
    return if (text.length <= 14) text else text.take(13).trimEnd() + "…"
}

internal fun UserRole.opensKyc(): Boolean = this == UserRole.STUDENT || this == UserRole.LANDLORD
