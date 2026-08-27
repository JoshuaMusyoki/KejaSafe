package com.rotd.stawi.data.repository

import com.rotd.stawi.data.local.dao.OrbitDao
import com.rotd.stawi.data.mapper.toDomain
import com.rotd.stawi.data.mapper.toEntity
import com.rotd.stawi.domain.model.Notification
import com.rotd.stawi.domain.model.NotificationType
import com.rotd.stawi.domain.repository.AuthRepository
import com.rotd.stawi.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first

class NotificationRepositoryImpl(
    private val dao: OrbitDao,
    private val authRepository: AuthRepository,
    /** applicationId — isolates Stawi / Vigil AI inboxes when sharing Firebase. */
    private val appPackage: String,
) : NotificationRepository {

    override fun getNotifications(): Flow<List<Notification>> {
        return authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                dao.getNotificationsForUser(user.uid, appPackage)
                    .onStart {
                        runCatching { dao.deleteDuplicateNotifications(user.uid, appPackage) }
                    }
                    .map { entities ->
                        entities
                            .map { it.toDomain() }
                            .dedupeForDisplay()
                    }
            }
        }
    }

    override suspend fun markAsRead(id: Long) {
        val uid = authRepository.currentUser.first()?.uid ?: return
        dao.markAsRead(id, uid, appPackage)
    }

    override suspend fun markAllAsRead() {
        val uid = authRepository.currentUser.first()?.uid ?: return
        dao.markAllAsRead(uid, appPackage)
    }

    override suspend fun markTargetAsRead(targetId: String) {
        if (targetId.isBlank()) return
        val uid = authRepository.currentUser.first()?.uid ?: return
        dao.markTargetAsRead(uid, appPackage, targetId)
    }

    override suspend fun addNotification(notification: Notification) {
        val uid = authRepository.currentUser.first()?.uid ?: return
        val owned = notification.copy(
            ownerUid = notification.ownerUid.ifBlank { uid },
            appPackage = notification.appPackage.ifBlank { appPackage },
        )
        if (owned.ownerUid != uid) return
        if (owned.appPackage != appPackage) return

        // Avoid stacking identical unread chat/SMS rows for the same sender + body
        if (owned.type == NotificationType.CHAT || owned.type == NotificationType.SMS) {
            val existing = dao.findSimilarNotification(
                ownerUid = owned.ownerUid,
                appPackage = owned.appPackage,
                type = owned.type.name,
                targetId = owned.targetId,
                content = owned.content
            )
            if (existing != null) {
                // Refresh timestamp on the existing row instead of inserting another
                dao.insertNotification(
                    existing.copy(
                        title = owned.title,
                        timestamp = maxOf(existing.timestamp, owned.timestamp),
                        isRead = false
                    )
                )
                return
            }
        }
        dao.insertNotification(owned.toEntity())
    }

    private fun List<Notification>.dedupeForDisplay(): List<Notification> {
        // Keep newest per type + sender + message body (UI still ordered by timestamp DESC)
        return distinctBy { notification ->
            when (notification.type) {
                NotificationType.CHAT, NotificationType.SMS, NotificationType.P2P, NotificationType.CONNECTION, NotificationType.MEETING ->
                    "${notification.type}|${notification.targetId.orEmpty()}|${notification.content.trim()}"
                NotificationType.VIDEO, NotificationType.APP_UPDATE ->
                    "${notification.type}|${notification.targetId.orEmpty()}|${notification.title}|${notification.content}"
            }
        }
    }
}
