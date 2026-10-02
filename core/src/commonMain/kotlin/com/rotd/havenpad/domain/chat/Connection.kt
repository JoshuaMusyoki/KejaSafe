package com.rotd.havenpad.domain.chat

import com.rotd.havenpad.data.platformEpochMillis
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
    val timestamp: Long = platformEpochMillis()
)


