package com.rotd.kejasafe.db

import com.rotd.kejasafe.EscrowState
import org.jetbrains.exposed.v1.core.Table

object EscrowTransactionsTable : Table("escrow_transactions") {
    val id = varchar("id", 50)
    val bookingId = varchar("booking_id", 50).references(ListingBookingsTable.id)
    val studentId = varchar("student_id", 50).references(UsersTable.id)
    val landlordId = varchar("landlord_id", 50).references(UsersTable.id)
    val state = enumerationByName("state", 30, EscrowState::class)
    val totalAmountCents = long("total_amount_cents")
    val commissionCents = long("commission_cents")
    val landlordShareCents = long("landlord_share_cents")
    val verificationOtp = varchar("verification_otp", 10).nullable()
    val mpesaCheckoutRequestId = varchar("mpesa_checkout_request_id", 100).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}

object EscrowAuditLogTable : Table("escrow_audit_log") {
    val id = integer("id").autoIncrement()
    val transactionId = varchar("transaction_id", 50).references(EscrowTransactionsTable.id)
    val fromState = enumerationByName("from_state", 30, EscrowState::class).nullable()
    val toState = enumerationByName("to_state", 30, EscrowState::class)
    val message = varchar("message", 500)
    val timestamp = long("timestamp")

    override val primaryKey = PrimaryKey(id)
}
