package com.rotd.keja_safe.domain.repository

import com.rotd.keja_safe.domain.model.CommunityKind
import com.rotd.keja_safe.domain.model.Discussion
import com.rotd.keja_safe.domain.model.Group
import com.rotd.keja_safe.domain.model.GroupInvite
import com.rotd.keja_safe.domain.model.GroupMessage
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<Group>>
    fun getPendingInvites(): Flow<List<GroupInvite>>
    fun getGroupMessages(groupId: String): Flow<List<GroupMessage>>
    fun getDiscussions(groupId: String): Flow<List<Discussion>>
    fun getDiscussionMessages(groupId: String, discussionId: String): Flow<List<GroupMessage>>

    suspend fun createGroup(
        name: String,
        description: String,
        memberIds: List<String>,
        kind: CommunityKind = CommunityKind.GROUP,
    ): Result<String>

    suspend fun createDiscussion(groupId: String, title: String): Result<String>
    suspend fun sendMessage(message: GroupMessage): Result<Unit>

    /** Invite colleagues; they must accept before joining. */
    suspend fun inviteMembers(groupId: String, memberIds: List<String>): Result<Unit>

    suspend fun acceptInvite(inviteId: String): Result<Unit>
    suspend fun declineInvite(inviteId: String): Result<Unit>

    /** Admin-only: remove a member from the community. */
    suspend fun removeMember(groupId: String, memberId: String): Result<Unit>

    suspend fun leaveGroup(groupId: String): Result<Unit>

    /** Admin-only: close/disband the community for everyone. */
    suspend fun disbandGroup(groupId: String): Result<Unit>
}
