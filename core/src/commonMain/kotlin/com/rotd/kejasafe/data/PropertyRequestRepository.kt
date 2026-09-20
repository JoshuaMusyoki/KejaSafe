package com.rotd.kejasafe.data

import com.rotd.kejasafe.domain.listing.PropertyRequest
import com.rotd.kejasafe.network.PropertyRequestClient

class PropertyRequestRepository {
    suspend fun submitRequest(phoneNumber: String, request: PropertyRequest): Result<String> {
        return PropertyRequestClient.submitRequest(phoneNumber, request)
    }
}
