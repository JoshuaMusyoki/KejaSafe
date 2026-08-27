package com.rotd.stawi.data.repository

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.rotd.stawi.data.local.dao.OrbitDao
import com.rotd.stawi.data.local.entities.toEntity
import com.rotd.stawi.data.worker.FeedbackSyncWorker
import com.rotd.stawi.domain.model.Feedback
import com.rotd.stawi.domain.repository.AuthRepository
import com.rotd.stawi.domain.repository.FeedbackRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.File

class FeedbackRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val authRepository: AuthRepository,
    private val dao: OrbitDao,
    private val workManager: WorkManager,
    private val context: android.content.Context
) : FeedbackRepository {

    override suspend fun sendFeedback(content: String, category: String, screenshotUri: Uri?): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()

            var localPath: String? = null
            if (screenshotUri != null && screenshotUri.scheme != "data") {
                // Cache a compressed JPEG locally for sync (keeps Storage + Firestore fallback small)
                val bytes = compressScreenshot(screenshotUri)
                if (bytes != null) {
                    val file = File(context.filesDir, "feedback_${System.currentTimeMillis()}.jpg")
                    file.writeBytes(bytes)
                    localPath = file.absolutePath
                }
            }

            val feedback = Feedback(
                userId = user?.uid ?: "anonymous",
                username = user?.username ?: "Anonymous",
                email = user?.email,
                content = content,
                category = category,
                timestamp = System.currentTimeMillis(),
                synced = false
            )

            val entity = feedback.toEntity().copy(localScreenshotPath = localPath)

            // 1. Save locally
            dao.insertFeedback(entity)

            // 2. Schedule sync
            scheduleSync()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPendingFeedback(): Result<Unit> {
        return try {
            val pending = dao.getUnsyncedFeedbacks()
            Log.d("FeedbackRepo", "Syncing ${pending.size} pending items")

            var allSuccessful = true

            for (entity in pending) {
                var screenshotUrl = entity.screenshotUrl

                // 1. Resolve screenshot (Storage first, then inline data URI fallback)
                if (screenshotUrl == null && entity.localScreenshotPath != null) {
                    val localFile = resolveLocalScreenshotFile(entity.localScreenshotPath)
                    if (localFile == null || !localFile.exists()) {
                        Log.w("FeedbackRepo", "Local screenshot missing for feedback ${entity.localId}")
                    } else {
                        screenshotUrl = uploadScreenshotOrInline(entity.userId, localFile)
                        if (screenshotUrl == null) {
                            Log.e("FeedbackRepo", "Screenshot resolve failed for feedback ${entity.localId}")
                            // Continue without screenshot rather than blocking the text feedback forever
                        }
                    }
                }

                // 2. Prepare clean map for Firestore (exclude localId, synced, and id)
                val payload = hashMapOf(
                    "userId" to entity.userId,
                    "username" to entity.username,
                    "email" to entity.email,
                    "content" to entity.content,
                    "category" to entity.category,
                    "timestamp" to entity.timestamp,
                    "screenshotUrl" to screenshotUrl
                )

                try {
                    Log.d("FeedbackRepo", "Adding feedback to Firestore: ${entity.content.take(20)}...")
                    firestore.collection(FEEDBACK_COLLECTION).add(payload).await()
                    dao.markFeedbackSynced(entity.localId)
                    Log.d("FeedbackRepo", "Feedback ${entity.localId} synced successfully")
                } catch (e: Exception) {
                    Log.e("FeedbackRepo", "Firestore add failed for feedback ${entity.localId}", e)
                    allSuccessful = false
                }
            }

            if (allSuccessful) Result.success(Unit) else Result.failure(Exception("Some items failed to sync"))
        } catch (e: Exception) {
            Log.e("FeedbackRepo", "Critical sync error", e)
            Result.failure(e)
        }
    }

    /**
     * Prefer Firebase Storage. If the bucket/rules/network fail (common 404 on resumable upload),
     * embed a small JPEG as a data URI so admin UI can still show the screenshot.
     */
    private suspend fun uploadScreenshotOrInline(userId: String, localFile: File): String? {
        val bytes = try {
            localFile.readBytes()
        } catch (e: Exception) {
            Log.e("FeedbackRepo", "Failed to read local screenshot", e)
            return null
        }

        // Skip Storage for anonymous — rules require request.auth.uid == userId
        if (userId != "anonymous") {
            val storageUrl = uploadToFirebaseStorage(userId, localFile, bytes)
            if (storageUrl != null) return storageUrl
            Log.w("FeedbackRepo", "Storage upload failed, trying inline data URI fallback")
        }

        return toInlineDataUri(bytes)
    }

    private suspend fun uploadToFirebaseStorage(
        userId: String,
        localFile: File,
        bytes: ByteArray
    ): String? {
        val remotePath = "feedback_media/$userId/${localFile.name}"
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        val ref = storage.reference.child(remotePath)

        return try {
            Log.d("FeedbackRepo", "Uploading screenshot to $remotePath (${bytes.size} bytes)")
            // putBytes avoids some putFile/file:// edge cases on resumable uploads
            ref.putBytes(bytes, metadata).await()
            val url = ref.downloadUrl.await().toString()
            Log.d("FeedbackRepo", "Uploaded screenshot: $url")
            url
        } catch (e: Exception) {
            Log.e("FeedbackRepo", "Screenshot Storage upload failed", e)
            null
        }
    }

    private fun toInlineDataUri(bytes: ByteArray): String? {
        val maxInlineBytes = 800_000 // stay under Firestore's ~1MB doc limit
        if (bytes.size > maxInlineBytes) {
            Log.w("FeedbackRepo", "Screenshot too large for inline fallback (${bytes.size} bytes)")
            return null
        }
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    private fun resolveLocalScreenshotFile(pathOrUri: String): File? {
        return try {
            when {
                pathOrUri.startsWith("file:") -> File(Uri.parse(pathOrUri).path ?: return null)
                pathOrUri.startsWith("/") -> File(pathOrUri)
                else -> File(Uri.parse(pathOrUri).path ?: pathOrUri)
            }
        } catch (e: Exception) {
            Log.e("FeedbackRepo", "Invalid local screenshot path: $pathOrUri", e)
            null
        }
    }

    private fun compressScreenshot(uri: Uri): ByteArray? {
        return try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }

            val maxDimension = 1280
            var sample = 1
            if (bounds.outHeight > maxDimension || bounds.outWidth > maxDimension) {
                val halfH = bounds.outHeight / 2
                val halfW = bounds.outWidth / 2
                while (halfH / sample >= maxDimension && halfW / sample >= maxDimension) {
                    sample *= 2
                }
            }

            val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val out = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
            bitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.e("FeedbackRepo", "Screenshot compress failed", e)
            context.contentResolver.openInputStream(uri)?.readBytes()
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<FeedbackSyncWorker>()
            .setConstraints(constraints)
            .addTag("feedback_sync")
            .build()

        workManager.enqueueUniqueWork(
            "feedback_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun getFeedbacks(): Flow<List<Feedback>> = callbackFlow {
        val registration = firestore.collection(FEEDBACK_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val feedbacks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Feedback::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(feedbacks)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun deleteFeedback(id: String): Result<Unit> {
        return try {
            firestore.collection(FEEDBACK_COLLECTION).document(id).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val FEEDBACK_COLLECTION = "feedback"
    }
}
