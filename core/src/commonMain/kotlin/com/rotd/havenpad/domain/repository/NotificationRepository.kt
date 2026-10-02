package com.rotd.keja_safe.domain.repository

import com.rotd.keja_safe.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(): Flow<List<Notification>>
    suspend fun markAsRead(id: Long)
    suspend fun markAllAsRead()
    /** Mark in-app chat/SMS rows for a peer as read (when the conversation is opened). */
    suspend fun markTargetAsRead(targetId: String)
    suspend fun addNotification(notification: Notification)
}
