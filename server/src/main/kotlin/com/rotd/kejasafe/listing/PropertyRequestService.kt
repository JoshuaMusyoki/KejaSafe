package com.rotd.kejasafe.listing

import com.rotd.kejasafe.db.PropertyRequestsTable
import com.rotd.kejasafe.domain.listing.PropertyRequest
import com.rotd.kejasafe.payment.MpesaService
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.core.eq
import java.util.UUID

object PropertyRequestService {

    fun createRequest(userId: String, request: PropertyRequest): String = transaction {
        val requestId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        PropertyRequestsTable.insert {
            it[PropertyRequestsTable.id] = requestId
            it[PropertyRequestsTable.userId] = userId
            it[PropertyRequestsTable.size] = request.size
            it[PropertyRequestsTable.houseType] = request.houseType
            it[PropertyRequestsTable.listingType] = request.listingType
            it[PropertyRequestsTable.area] = request.area
            it[PropertyRequestsTable.distance] = request.distance
            it[PropertyRequestsTable.budgetFrom] = request.budgetFrom
            it[PropertyRequestsTable.budgetTo] = request.budgetTo
            it[PropertyRequestsTable.additionalInfo] = request.additionalInfo
            it[PropertyRequestsTable.agentsCanCall] = request.agentsCanCall
            it[PropertyRequestsTable.paymentStatus] = "PENDING_MPESA"
            it[PropertyRequestsTable.createdAt] = now
        }

        requestId
    }

    suspend fun initiatePayment(requestId: String, phoneNumber: String) {
        // Amount is fixed at 200 KES as per requirement
        val response = MpesaService.initiateStkPush(
            phoneNumber = phoneNumber,
            amountKes = 200,
            reference = requestId,
            description = "Property request"
        )

        if (!response.accepted) {
            error(response.failureMessage)
        }

        transaction {
            PropertyRequestsTable.update({ PropertyRequestsTable.id eq requestId }) {
                it[mpesaCheckoutRequestId] = response.CheckoutRequestID
            }
        }
    }

    fun handleMpesaCallback(checkoutRequestId: String, resultCode: Int, resultDesc: String) = transaction {
        val status = if (resultCode == 0) "PAID" else "FAILED"
        PropertyRequestsTable.update({ PropertyRequestsTable.mpesaCheckoutRequestId eq checkoutRequestId }) {
            it[paymentStatus] = status
        }
        
        // Log the result
        println("M-Pesa Callback for $checkoutRequestId: $resultCode - $resultDesc")
    }
}
