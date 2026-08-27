package com.rotd.stawi.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rotd.stawi.domain.model.AttachmentType
import com.rotd.stawi.domain.model.CommunityKind
import com.rotd.stawi.domain.model.Discussion
import com.rotd.stawi.domain.model.Group
import com.rotd.stawi.domain.model.GroupInvite
import com.rotd.stawi.domain.model.GroupInviteStatus
import com.rotd.stawi.domain.model.GroupMessage
import com.rotd.stawi.domain.model.MessageStatus
import com.rotd.stawi.domain.repository.AuthRepository
import com.rotd.stawi.domain.repository.GroupRepository
import com.rotd.stawi.domain.util.Notifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class GroupRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val notifier: Notifier,
) : GroupRepository {

    private val notifiedInviteIds = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    override fun getGroups(): Flow<List<Group>> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) return@flatMapLatest flowOf(emptyList())

        callbackFlow {
            val registration = firestore.collection(GROUPS)
                .whereArrayContains("memberIds", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val groups = snapshot?.documents
                        ?.mapNotNull { it.toGroup() }
                        ?.filter { !it.closed }
                        ?.sortedByDescending { it.lastMessageAt.takeIf { t -> t > 0L } ?: it.createdAt }
                        ?: emptyList()
                    trySend(groups)
                }
            awaitClose { registration.remove() }
        }
    }

    override fun getPendingInvites(): Flow<List<GroupInvite>> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(emptyList())

            callbackFlow {
                val registration = firestore.collection(GROUP_INVITES)
                    .whereEqualTo("inviteeId", user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        val invites = snapshot?.documents
                            ?.mapNotNull { it.toGroupInvite() }
                            ?.filter { it.status == GroupInviteStatus.PENDING }
                            ?.sortedByDescending { it.createdAt }
                            ?: emptyList()
                        invites.forEach { invite ->
                            if (notifiedInviteIds.add(invite.id)) {
                                val kindLabel =
                                    if (invite.groupKind == CommunityKind.ROOM) "idea room" else "learning group"
                                val inviter = invite.invitedByName.ifBlank { "Someone" }
                                notifier.showChatMessageNotification(
                                    sender = "Learning group invite",
                                    message = "$inviter invited you to join ${invite.groupName.ifBlank { "a $kindLabel" }}",
                                    peerId = invite.groupId.ifBlank { invite.id },
                                )
                            }
                        }
                        trySend(invites)
                    }
                awaitClose { registration.remove() }
            }
        }

    override fun getGroupMessages(groupId: String): Flow<List<GroupMessage>> =
        observeMessages(groupRef(groupId).collection(MESSAGES), groupId, discussionId = null)

    override fun getDiscussions(groupId: String): Flow<List<Discussion>> = callbackFlow {
        val registration = groupRef(groupId).collection(DISCUSSIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val discussions = snapshot?.documents
                    ?.mapNotNull { it.toDiscussion(groupId) }
                    ?.sortedByDescending { it.lastMessageAt.takeIf { t -> t > 0L } ?: it.createdAt }
                    ?: emptyList()
                trySend(discussions)
            }
        awaitClose { registration.remove() }
    }

    override fun getDiscussionMessages(groupId: String, discussionId: String): Flow<List<GroupMessage>> =
        observeMessages(
            groupRef(groupId).collection(DISCUSSIONS).document(discussionId).collection(MESSAGES),
            groupId,
            discussionId,
        )

    override suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        kind: CommunityKind,
    ): Result<String> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val now = System.currentTimeMillis()
            val payload = hashMapOf(
                "name" to name.trim(),
                "description" to description.trim(),
                "createdBy" to user.uid,
                "memberIds" to listOf(user.uid),
                "createdAt" to now,
                "lastMessage" to "",
                "lastMessageAt" to 0L,
                "kind" to kind.name,
                "closed" to false,
            )
            val docRef = firestore.collection(GROUPS).add(payload).await()
            val invitees = memberIds.filter { it != user.uid }.distinct()
            if (invitees.isNotEmpty()) {
                inviteMembersInternal(
                    groupId = docRef.id,
                    groupName = name.trim(),
                    groupKind = kind,
                    invitedBy = user.uid,
                    invitedByName = user.username.ifBlank { user.phoneNumber },
                    inviteeIds = invitees,
                )
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createDiscussion(groupId: String, title: String): Result<String> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val trimmed = title.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Discussion title required"))
            }
            val now = System.currentTimeMillis()
            val payload = hashMapOf(
                "title" to trimmed,
                "createdBy" to user.uid,
                "createdAt" to now,
                "lastMessage" to "",
                "lastMessageAt" to 0L,
            )
            val docRef = groupRef(groupId).collection(DISCUSSIONS).add(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: GroupMessage): Result<Unit> {
        return try {
            val groupRef = groupRef(message.groupId)
            val discussionId = message.discussionId
            val messageCollection = if (discussionId.isNullOrBlank()) {
                groupRef.collection(MESSAGES)
            } else {
                groupRef.collection(DISCUSSIONS).document(discussionId).collection(MESSAGES)
            }
            val messageRef = messageCollection.document(
                message.id.ifBlank { firestore.collection(GROUPS).document().id }
            )
            val preview = messagePreview(message)
            val payload = hashMapOf<String, Any?>(
                "senderId" to message.senderId,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "status" to message.status.name,
                "attachmentUrl" to message.attachmentUrl,
                "attachmentType" to message.attachmentType?.name,
                "fileName" to message.fileName,
                "discussionId" to discussionId,
            )
            firestore.runBatch { batch ->
                batch.set(messageRef, payload)
                if (discussionId.isNullOrBlank()) {
                    batch.update(
                        groupRef,
                        mapOf(
                            "lastMessage" to preview,
                            "lastMessageAt" to message.timestamp,
                        )
                    )
                } else {
                    batch.update(
                        groupRef.collection(DISCUSSIONS).document(discussionId),
                        mapOf(
                            "lastMessage" to preview,
                            "lastMessageAt" to message.timestamp,
                        )
                    )
                    batch.update(
                        groupRef,
                        mapOf(
                            "lastMessage" to preview,
                            "lastMessageAt" to message.timestamp,
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun inviteMembers(groupId: String, memberIds: List<String>): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val groupSnap = groupRef(groupId).get().await()
            val group = groupSnap.toGroup()
                ?: return Result.failure(IllegalStateException("Community not found"))
            if (group.closed) {
                return Result.failure(IllegalStateException("Community is closed"))
            }
            if (user.uid !in group.memberIds) {
                return Result.failure(IllegalStateException("Only members can invite"))
            }
            val invitees = memberIds
                .filter { it != user.uid && it !in group.memberIds }
                .distinct()
            if (invitees.isEmpty()) {
                return Result.failure(IllegalArgumentException("No new members to invite"))
            }
            inviteMembersInternal(
                groupId = groupId,
                groupName = group.name,
                groupKind = group.kind,
                invitedBy = user.uid,
                invitedByName = user.username.ifBlank { user.phoneNumber },
                inviteeIds = invitees,
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun acceptInvite(inviteId: String): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val inviteRef = firestore.collection(GROUP_INVITES).document(inviteId)
            val inviteSnap = inviteRef.get().await()
            val invite = inviteSnap.toGroupInvite()
                ?: return Result.failure(IllegalStateException("Invite not found"))
            if (invite.inviteeId != user.uid) {
                return Result.failure(IllegalStateException("Not your invite"))
            }
            if (invite.status != GroupInviteStatus.PENDING) {
                return Result.failure(IllegalStateException("Invite already handled"))
            }
            val groupRef = groupRef(invite.groupId)
            val groupSnap = groupRef.get().await()
            val group = groupSnap.toGroup()
            if (group == null || group.closed) {
                inviteRef.update("status", GroupInviteStatus.DECLINED.name).await()
                return Result.failure(IllegalStateException("Community is no longer available"))
            }
            firestore.runBatch { batch ->
                batch.update(inviteRef, "status", GroupInviteStatus.ACCEPTED.name)
                batch.update(groupRef, "memberIds", FieldValue.arrayUnion(user.uid))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun declineInvite(inviteId: String): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val inviteRef = firestore.collection(GROUP_INVITES).document(inviteId)
            val invite = inviteRef.get().await().toGroupInvite()
                ?: return Result.failure(IllegalStateException("Invite not found"))
            if (invite.inviteeId != user.uid) {
                return Result.failure(IllegalStateException("Not your invite"))
            }
            inviteRef.update("status", GroupInviteStatus.DECLINED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(groupId: String, memberId: String): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val group = groupRef(groupId).get().await().toGroup()
                ?: return Result.failure(IllegalStateException("Community not found"))
            if (group.createdBy != user.uid) {
                return Result.failure(IllegalStateException("Only the admin can remove members"))
            }
            if (memberId == group.createdBy) {
                return Result.failure(IllegalStateException("Cannot remove the admin"))
            }
            groupRef(groupId)
                .update("memberIds", FieldValue.arrayRemove(memberId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val group = groupRef(groupId).get().await().toGroup()
                ?: return Result.failure(IllegalStateException("Community not found"))
            if (group.createdBy == user.uid) {
                return Result.failure(
                    IllegalStateException("Admin cannot leave — close the group instead")
                )
            }
            firestore.collection(GROUPS).document(groupId)
                .update("memberIds", FieldValue.arrayRemove(user.uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disbandGroup(groupId: String): Result<Unit> {
        return try {
            val user = authRepository.currentUser.first()
                ?: return Result.failure(IllegalStateException("Not signed in"))
            val group = groupRef(groupId).get().await().toGroup()
                ?: return Result.failure(IllegalStateException("Community not found"))
            if (group.createdBy != user.uid) {
                return Result.failure(IllegalStateException("Only the admin can close the group"))
            }
            firestore.runBatch { batch ->
                batch.update(
                    groupRef(groupId),
                    mapOf(
                        "closed" to true,
                        "memberIds" to emptyList<String>(),
                        "lastMessage" to "Group closed",
                    )
                )
            }.await()
            // Scope by inviter so list rules (invitedBy == auth) allow the query.
            val pendingInvites = firestore.collection(GROUP_INVITES)
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("invitedBy", user.uid)
                .get()
                .await()
            val pendingDocs = pendingInvites.documents.filter {
                it.getString("status") == GroupInviteStatus.PENDING.name
            }
            if (pendingDocs.isNotEmpty()) {
                firestore.runBatch { batch ->
                    pendingDocs.forEach { doc ->
                        batch.update(doc.reference, "status", GroupInviteStatus.DECLINED.name)
                    }
                }.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun inviteMembersInternal(
        groupId: String,
        groupName: String,
        groupKind: CommunityKind,
        invitedBy: String,
        invitedByName: String,
        inviteeIds: List<String>,
    ) {
        // Deterministic doc ids — no list query needed (avoids rules that block
        // "all invites for groupId" while create-with-members is in flight).
        val now = System.currentTimeMillis()
        val targets = inviteeIds.distinct()
        if (targets.isEmpty()) return

        firestore.runBatch { batch ->
            targets.forEach { inviteeId ->
                val docId = "${groupId}_$inviteeId"
                val ref = firestore.collection(GROUP_INVITES).document(docId)
                batch.set(
                    ref,
                    mapOf(
                        "groupId" to groupId,
                        "groupName" to groupName,
                        "groupKind" to groupKind.name,
                        "invitedBy" to invitedBy,
                        "invitedByName" to invitedByName,
                        "inviteeId" to inviteeId,
                        "status" to GroupInviteStatus.PENDING.name,
                        "createdAt" to now,
                    )
                )
            }
        }.await()
    }

    private fun observeMessages(
        collection: com.google.firebase.firestore.CollectionReference,
        groupId: String,
        discussionId: String?,
    ): Flow<List<GroupMessage>> = callbackFlow {
        val registration = collection
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents
                    ?.mapNotNull { it.toGroupMessage(groupId, discussionId) }
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    private fun groupRef(groupId: String) = firestore.collection(GROUPS).document(groupId)

    private fun messagePreview(message: GroupMessage): String {
        if (message.content.isNotBlank()) return message.content
        return when (message.attachmentType) {
            AttachmentType.IMAGE -> "📷 Photo"
            AttachmentType.VIDEO -> "🎥 Video"
            AttachmentType.PDF -> "📄 ${message.fileName ?: "Document"}"
            AttachmentType.STICKER -> "🎭 Sticker"
            null -> ""
        }
    }

    private fun DocumentSnapshot.toGroup(): Group? {
        val data = data ?: return null
        @Suppress("UNCHECKED_CAST")
        val memberIds = data["memberIds"] as? List<String> ?: emptyList()
        val kindName = data["kind"] as? String ?: CommunityKind.GROUP.name
        return Group(
            id = id,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            createdBy = data["createdBy"] as? String ?: "",
            memberIds = memberIds,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            lastMessage = data["lastMessage"] as? String ?: "",
            lastMessageAt = (data["lastMessageAt"] as? Number)?.toLong() ?: 0L,
            kind = runCatching { CommunityKind.valueOf(kindName) }.getOrDefault(CommunityKind.GROUP),
            closed = data["closed"] as? Boolean ?: false,
        )
    }

    private fun DocumentSnapshot.toGroupInvite(): GroupInvite? {
        val data = data ?: return null
        val kindName = data["groupKind"] as? String ?: CommunityKind.GROUP.name
        val statusName = data["status"] as? String ?: GroupInviteStatus.PENDING.name
        return GroupInvite(
            id = id,
            groupId = data["groupId"] as? String ?: "",
            groupName = data["groupName"] as? String ?: "",
            groupKind = runCatching { CommunityKind.valueOf(kindName) }.getOrDefault(CommunityKind.GROUP),
            invitedBy = data["invitedBy"] as? String ?: "",
            invitedByName = data["invitedByName"] as? String ?: "",
            inviteeId = data["inviteeId"] as? String ?: "",
            status = runCatching { GroupInviteStatus.valueOf(statusName) }
                .getOrDefault(GroupInviteStatus.PENDING),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun DocumentSnapshot.toDiscussion(groupId: String): Discussion? {
        val data = data ?: return null
        return Discussion(
            id = id,
            groupId = groupId,
            title = data["title"] as? String ?: "",
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            lastMessage = data["lastMessage"] as? String ?: "",
            lastMessageAt = (data["lastMessageAt"] as? Number)?.toLong() ?: 0L,
        )
    }

    private fun DocumentSnapshot.toGroupMessage(
        groupId: String,
        discussionId: String?,
    ): GroupMessage? {
        val data = data ?: return null
        val statusName = data["status"] as? String ?: MessageStatus.SENT.name
        val typeName = data["attachmentType"] as? String
        return GroupMessage(
            id = id,
            groupId = groupId,
            discussionId = discussionId ?: data["discussionId"] as? String,
            senderId = data["senderId"] as? String ?: "",
            content = data["content"] as? String ?: "",
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L,
            status = runCatching { MessageStatus.valueOf(statusName) }.getOrDefault(MessageStatus.SENT),
            attachmentUrl = data["attachmentUrl"] as? String,
            attachmentType = typeName?.let {
                runCatching { AttachmentType.valueOf(it) }.getOrNull()
            },
            fileName = data["fileName"] as? String,
        )
    }

    companion object {
        private const val GROUPS = "groups"
        private const val GROUP_INVITES = "groupInvites"
        private const val MESSAGES = "messages"
        private const val DISCUSSIONS = "discussions"
    }
}
