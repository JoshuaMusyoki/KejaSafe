package com.rotd.keja_safe.domain.usecase

import com.rotd.keja_safe.domain.model.Notification
import com.rotd.keja_safe.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

class GetNotificationsUseCase(
    private val repository: NotificationRepository
) {
    operator fun invoke(): Flow<List<Notification>> = repository.getNotifications()
}


