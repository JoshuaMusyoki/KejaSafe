package com.rotd.havenpad.data

import com.rotd.havenpad.*
import com.rotd.havenpad.network.EscrowClient
import kotlinx.coroutines.flow.Flow

class EscrowRepository {

    suspend fun initiatePayment(request: InitiatePaymentRequest): Result<PaymentStatusResponse> {
        return EscrowClient.initiatePayment(request)
    }

    suspend fun getEscrowStatus(transactionId: String): Result<PaymentStatusResponse> {
        return EscrowClient.getStatus(transactionId)
    }

    suspend fun releaseEscrow(transactionId: String, otp: String): Result<PaymentStatusResponse> {
        return EscrowClient.releaseEscrow(ReleaseEscrowRequest(transactionId, otp))
    }

    suspend fun disputeEscrow(transactionId: String, reason: String): Result<PaymentStatusResponse> {
        return EscrowClient.disputeEscrow(DisputeEscrowRequest(transactionId, reason))
    }

    fun observeTransaction(transactionId: String): Flow<PaymentStatusResponse> {
        return EscrowClient.observeStatus(transactionId)
    }
}
