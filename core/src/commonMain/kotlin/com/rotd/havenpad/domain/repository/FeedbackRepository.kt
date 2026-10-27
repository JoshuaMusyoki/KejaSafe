package com.rotd.havenpad.domain.repository

import com.rotd.keja_safe.domain.model.Feedback
import kotlinx.coroutines.flow.Flow

interface FeedbackRepository {
    suspend fun sendFeedback(content: String, category: String, screenshotUri: String?): Result<Unit>
    suspend fun syncPendingFeedback(): Result<Unit>
    fun getFeedbacks(): Flow<List<Feedback>>
    suspend fun deleteFeedback(id: String): Result<Unit>
}
