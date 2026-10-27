package com.rotd.keja_safe.domain.util

import kotlinx.coroutines.flow.Flow

interface NearbyConnectionManager {
    fun startAdvertising()
    fun stopAdvertising()
    fun startDiscovery()
    fun stopDiscovery()
    suspend fun sendPayload(endpointId: String, payload: String): Result<Unit>
    val incomingPayloads: Flow<String>
    val connectedEndpoints: Flow<List<String>>
}


