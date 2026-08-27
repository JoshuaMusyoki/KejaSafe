package com.rotd.kejasafe.domain.chat

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}

@Serializable
data class Connection(
    val id: String = "", // senderId_receiverId
    val senderId: String,
    val receiverId: String,
    val status: ConnectionStatus,
    val timestamp: Long = System.currentTimeMillis()
)


