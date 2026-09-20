package com.rotd.kejasafe

import kotlinx.serialization.Serializable

@Serializable
enum class EscrowState {
    /** STK Push / Payment prompt initiated. */
    PENDING_PAYMENT,
    /** Funds successfully collected by server; locked in platform escrow. */
    HELD_IN_ESCROW,
    /** Move-in verification initiated via OTP/QR code. */
    RELEASE_PENDING,
    /** OTP verified; 95% paid to Landlord, 5% retained by KejaSafe. */
    RELEASED,
    /** Tenant raised an issue; payout frozen. */
    DISPUTED,
    /** Transaction cancelled; deposit returned to tenant. */
    REFUNDED
}

@Serializable
data class InitiatePaymentRequest(
    val bookingId: String,
    val phoneNumber: String,
    /** Amount in cents (e.g., 500000 for 5000 KES) */
    val amountCents: Long
)

@Serializable
data class EscrowAuditLog(
    val fromState: EscrowState?,
    val toState: EscrowState,
    val message: String,
    val timestamp: Long
)

@Serializable
data class EscrowSummary(
    val transactionId: String,
    val bookingId: String,
    val state: EscrowState,
    val totalAmountCents: Long,
    val commissionCents: Long,
    val landlordShareCents: Long,
    val logs: List<EscrowAuditLog> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class PaymentStatusResponse(
    val transactionId: String,
    val state: EscrowState,
    val summary: EscrowSummary? = null,
    val message: String? = null
)

@Serializable
data class ReleaseEscrowRequest(
    val transactionId: String,
    val verificationOtp: String
)

@Serializable
data class DisputeEscrowRequest(
    val transactionId: String,
    val reason: String
)
