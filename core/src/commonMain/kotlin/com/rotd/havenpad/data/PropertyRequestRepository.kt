package com.rotd.havenpad.data

import com.rotd.havenpad.domain.listing.PropertyRequest
import com.rotd.havenpad.network.PropertyRequestClient

class PropertyRequestRepository {
    suspend fun submitRequest(phoneNumber: String, request: PropertyRequest): Result<String> {
        return PropertyRequestClient.submitRequest(phoneNumber, request)
    }
}
