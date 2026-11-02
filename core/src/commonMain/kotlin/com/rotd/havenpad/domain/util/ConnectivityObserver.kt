package com.rotd.keja_safe.domain.util

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    val status: Flow<Status>

    enum class Status {
        Available, Unavailable, Losing, Lost
    }
}


