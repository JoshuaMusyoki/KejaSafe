package com.rotd.keja_safe.domain.usecase

import com.rotd.keja_safe.domain.repository.NotificationRepository

class MarkNotificationReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.markAsRead(id)
    }
}


