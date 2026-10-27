package com.rotd.keja_safe.domain.usecase

import com.rotd.keja_safe.domain.repository.NotificationRepository

class MarkAllNotificationsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke() {
        repository.markAllAsRead()
    }
}


