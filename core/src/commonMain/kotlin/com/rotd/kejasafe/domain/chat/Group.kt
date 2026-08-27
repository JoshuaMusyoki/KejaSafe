package com.rotd.kejasafe.domain.chat

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class CommunityKind {
    /** Named community group with discussions. */
    GROUP,
    /** Ad-hoc multi-recipient shared chat room. */
    ROOM,
}

@Keep
@Serializable
enum class GroupInviteStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

@Keep
@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val kind: CommunityKind = CommunityKind.GROUP,
    /** When true, the community is closed by admin and hidden from members. */
    val closed: Boolean = false,
)

@Keep
@Serializable
data class GroupInvite(
    val id: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val groupKind: CommunityKind = CommunityKind.GROUP,
    val invitedBy: String = "",
    val invitedByName: String = "",
    val inviteeId: String = "",
    val status: GroupInviteStatus = GroupInviteStatus.PENDING,
    val createdAt: Long = 0L,
)

@Keep
@Serializable
data class Discussion(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
)

@Keep
@Serializable
data class GroupMessage(
    val id: String = "",
    val groupId: String = "",
    /** Null = main group/room chat; set for discussion thread messages. */
    val discussionId: String? = null,
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val status: MessageStatus = MessageStatus.SENT,
    val attachmentUrl: String? = null,
    val attachmentType: AttachmentType? = null,
    val fileName: String? = null,
)
