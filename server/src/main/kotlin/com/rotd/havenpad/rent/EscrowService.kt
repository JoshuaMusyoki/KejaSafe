package com.rotd.havenpad.rent

import com.rotd.havenpad.*
import com.rotd.havenpad.db.*
import com.rotd.havenpad.payment.MpesaService
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EscrowService {

    private val statusObservers = ConcurrentHashMap<String, suspend (EscrowState) -> Unit>()

    fun observeTransaction(transactionId: String, callback: suspend (EscrowState) -> Unit) {
        statusObservers[transactionId] = callback
    }

    fun stopObserving(transactionId: String) {
        statusObservers.remove(transactionId)
    }

    suspend fun initiatePayment(userId: String, request: InitiatePaymentRequest): PaymentStatusResponse {
        val phone = request.phoneNumber.trim()
        if (phone.isBlank()) error("M-Pesa phone number is required")

        val amountKes = (request.amountCents / 100L).coerceAtLeast(1L)

        val prepared = transaction {
            val booking = ListingBookingsTable.selectAll()
                .where { ListingBookingsTable.id eq request.bookingId }
                .singleOrNull() ?: error("Booking not found")

            if (booking[ListingBookingsTable.guestUserId] != userId) {
                error("Unauthorized booking access")
            }

            val transactionId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val commission = (request.amountCents * 5) / 100
            val landlordShare = request.amountCents - commission

            EscrowTransactionsTable.insert {
                it[id] = transactionId
                it[bookingId] = request.bookingId
                it[studentId] = userId
                it[landlordId] = booking[ListingBookingsTable.landlordId]
                it[state] = EscrowState.PENDING_PAYMENT
                it[totalAmountCents] = request.amountCents
                it[commissionCents] = commission
                it[landlordShareCents] = landlordShare
                it[createdAt] = now
                it[updatedAt] = now
            }

            auditLog(
                transactionId,
                null,
                EscrowState.PENDING_PAYMENT,
                "Payment initiated — STK Push to $phone"
            )
            transactionId
        }

        val stk = try {
            MpesaService.initiateStkPush(
                phoneNumber = phone,
                amountKes = amountKes,
                reference = prepared.take(12),
                description = "Escrow deposit"
            )
        } catch (e: Exception) {
            transaction {
                EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq prepared }) {
                    it[state] = EscrowState.REFUNDED
                    it[updatedAt] = System.currentTimeMillis()
                }
                auditLog(
                    prepared,
                    EscrowState.PENDING_PAYMENT,
                    EscrowState.REFUNDED,
                    "STK Push failed: ${e.message}"
                )
            }
            throw e
        }

        if (!stk.accepted) {
            transaction {
                EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq prepared }) {
                    it[state] = EscrowState.REFUNDED
                    it[updatedAt] = System.currentTimeMillis()
                }
                auditLog(
                    prepared,
                    EscrowState.PENDING_PAYMENT,
                    EscrowState.REFUNDED,
                    "STK Push rejected: ${stk.failureMessage}"
                )
            }
            error(stk.failureMessage)
        }

        transaction {
            EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq prepared }) {
                it[mpesaCheckoutRequestId] = stk.CheckoutRequestID
                it[updatedAt] = System.currentTimeMillis()
            }
        }

        return PaymentStatusResponse(
            transactionId = prepared,
            state = EscrowState.PENDING_PAYMENT,
            message = stk.CustomerMessage
                ?: "STK Push sent to $phone. Enter your M-Pesa PIN on your phone."
        )
    }

    fun handleMpesaCallback(checkoutRequestId: String, paid: Boolean, resultDesc: String) = transaction {
        val row = EscrowTransactionsTable.selectAll()
            .where { EscrowTransactionsTable.mpesaCheckoutRequestId eq checkoutRequestId }
            .singleOrNull() ?: return@transaction

        if (row[EscrowTransactionsTable.state] != EscrowState.PENDING_PAYMENT) return@transaction

        val transactionId = row[EscrowTransactionsTable.id]
        if (paid) {
            val otp = (1000..9999).random().toString()
            EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq transactionId }) {
                it[state] = EscrowState.HELD_IN_ESCROW
                it[verificationOtp] = otp
                it[updatedAt] = System.currentTimeMillis()
            }
            auditLog(
                transactionId,
                EscrowState.PENDING_PAYMENT,
                EscrowState.HELD_IN_ESCROW,
                "M-Pesa payment confirmed. Verification OTP generated: $otp"
            )
        } else {
            EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq transactionId }) {
                it[state] = EscrowState.REFUNDED
                it[updatedAt] = System.currentTimeMillis()
            }
            auditLog(
                transactionId,
                EscrowState.PENDING_PAYMENT,
                EscrowState.REFUNDED,
                "M-Pesa payment failed: $resultDesc"
            )
        }
    }

    fun getEscrowStatus(transactionId: String, userId: String): PaymentStatusResponse = transaction {
        val row = EscrowTransactionsTable.selectAll()
            .where { EscrowTransactionsTable.id eq transactionId }
            .singleOrNull() ?: error("Transaction not found")

        if (row[EscrowTransactionsTable.studentId] != userId && row[EscrowTransactionsTable.landlordId] != userId) {
            error("Forbidden")
        }

        PaymentStatusResponse(
            transactionId = transactionId,
            state = row[EscrowTransactionsTable.state],
            summary = toSummary(row, userId)
        )
    }

    fun releaseEscrow(userId: String, request: ReleaseEscrowRequest): PaymentStatusResponse = transaction {
        val row = EscrowTransactionsTable.selectAll()
            .where { EscrowTransactionsTable.id eq request.transactionId }
            .singleOrNull() ?: error("Transaction not found")

        if (row[EscrowTransactionsTable.studentId] != userId) {
            error("Only the student can release funds")
        }

        if (row[EscrowTransactionsTable.state] != EscrowState.HELD_IN_ESCROW &&
            row[EscrowTransactionsTable.state] != EscrowState.RELEASE_PENDING
        ) {
            error("Funds are not in a releasable state")
        }

        val expectedOtp = row[EscrowTransactionsTable.verificationOtp]
        if (expectedOtp != null && expectedOtp != request.verificationOtp) {
            error("Invalid verification code")
        }

        val oldState = row[EscrowTransactionsTable.state]
        val now = System.currentTimeMillis()

        EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq request.transactionId }) {
            it[state] = EscrowState.RELEASED
            it[updatedAt] = now
        }

        val commission = row[EscrowTransactionsTable.commissionCents]
        val payout = row[EscrowTransactionsTable.landlordShareCents]

        auditLog(
            request.transactionId,
            oldState,
            EscrowState.RELEASED,
            "Funds released. HavenPad Revenue: $commission cents. Landlord Payout: $payout cents."
        )

        PaymentStatusResponse(
            transactionId = request.transactionId,
            state = EscrowState.RELEASED,
            message = "Funds successfully disbursed to landlord."
        )
    }

    fun disputeEscrow(userId: String, request: DisputeEscrowRequest): PaymentStatusResponse = transaction {
        val row = EscrowTransactionsTable.selectAll()
            .where { EscrowTransactionsTable.id eq request.transactionId }
            .singleOrNull() ?: error("Transaction not found")

        if (row[EscrowTransactionsTable.studentId] != userId) {
            error("Forbidden")
        }

        if (row[EscrowTransactionsTable.state] != EscrowState.HELD_IN_ESCROW &&
            row[EscrowTransactionsTable.state] != EscrowState.RELEASE_PENDING
        ) {
            error("Only funds in escrow can be disputed")
        }

        val oldState = row[EscrowTransactionsTable.state]
        val now = System.currentTimeMillis()

        EscrowTransactionsTable.update({ EscrowTransactionsTable.id eq request.transactionId }) {
            it[state] = EscrowState.DISPUTED
            it[updatedAt] = now
        }

        auditLog(
            request.transactionId,
            oldState,
            EscrowState.DISPUTED,
            "Tenant raised a dispute: ${request.reason}"
        )

        PaymentStatusResponse(
            transactionId = request.transactionId,
            state = EscrowState.DISPUTED,
            message = "Dispute registered. HavenPad support will contact you within 24 hours."
        )
    }

    private fun auditLog(transactionId: String, from: EscrowState?, to: EscrowState, msg: String) {
        EscrowAuditLogTable.insert {
            it[EscrowAuditLogTable.transactionId] = transactionId
            it[fromState] = from
            it[toState] = to
            it[message] = msg
            it[timestamp] = System.currentTimeMillis()
        }

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            statusObservers[transactionId]?.invoke(to)
        }
    }

    private fun toSummary(row: ResultRow, requesterUserId: String) = EscrowSummary(
        transactionId = row[EscrowTransactionsTable.id],
        bookingId = row[EscrowTransactionsTable.bookingId],
        state = row[EscrowTransactionsTable.state],
        totalAmountCents = row[EscrowTransactionsTable.totalAmountCents],
        commissionCents = row[EscrowTransactionsTable.commissionCents],
        landlordShareCents = row[EscrowTransactionsTable.landlordShareCents],
        logs = EscrowAuditLogTable.selectAll()
            .where { EscrowAuditLogTable.transactionId eq row[EscrowTransactionsTable.id] }
            .map {
                EscrowAuditLog(
                    fromState = it[EscrowAuditLogTable.fromState],
                    toState = it[EscrowAuditLogTable.toState],
                    message = it[EscrowAuditLogTable.message],
                    timestamp = it[EscrowAuditLogTable.timestamp]
                )
            }.map { log ->
                val isStudent = row[EscrowTransactionsTable.studentId] == requesterUserId
                if (isStudent && log.message.contains("OTP", ignoreCase = true)) {
                    log.copy(message = "M-Pesa payment confirmed. Waiting for move-in verification.")
                } else {
                    log
                }
            },
        createdAt = row[EscrowTransactionsTable.createdAt],
        updatedAt = row[EscrowTransactionsTable.updatedAt]
    )
}
