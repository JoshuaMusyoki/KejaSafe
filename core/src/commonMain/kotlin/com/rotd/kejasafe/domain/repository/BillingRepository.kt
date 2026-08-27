package com.rotd.kejasafe.domain.repository

interface BillingRepository {
    suspend fun initiateMpesaStkPush(phoneNumber: String, amount: Double): Result<Boolean>
    suspend fun verifySubscription(cardToken: String): Result<Boolean>
    suspend fun isPremiumActive(): Boolean
}


