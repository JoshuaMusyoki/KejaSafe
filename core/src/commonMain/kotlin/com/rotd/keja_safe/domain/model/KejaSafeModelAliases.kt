package com.rotd.keja_safe.domain.model

/**
 * Compatibility aliases for unfinished Stawi→KejaSafe chat port.
 * Real types live under com.rotd.kejasafe.domain.chat / .call.
 */

typealias User = com.rotd.kejasafe.domain.chat.User
typealias ChatMessage = com.rotd.kejasafe.domain.chat.ChatMessage
typealias MessageStatus = com.rotd.kejasafe.domain.chat.MessageStatus
typealias TransportType = com.rotd.kejasafe.domain.chat.TransportType
typealias AttachmentType = com.rotd.kejasafe.domain.chat.AttachmentType
typealias MessageReaction = com.rotd.kejasafe.domain.chat.MessageReaction
typealias Connection = com.rotd.kejasafe.domain.chat.Connection
typealias ConnectionStatus = com.rotd.kejasafe.domain.chat.ConnectionStatus
typealias Notification = com.rotd.kejasafe.domain.chat.Notification
typealias NotificationType = com.rotd.kejasafe.domain.chat.NotificationType
typealias CommunityKind = com.rotd.kejasafe.domain.chat.CommunityKind
typealias Group = com.rotd.kejasafe.domain.chat.Group
typealias GroupInvite = com.rotd.kejasafe.domain.chat.GroupInvite
typealias GroupInviteStatus = com.rotd.kejasafe.domain.chat.GroupInviteStatus
typealias Discussion = com.rotd.kejasafe.domain.chat.Discussion
typealias GroupMessage = com.rotd.kejasafe.domain.chat.GroupMessage
typealias PeopleSearchScope = com.rotd.kejasafe.domain.chat.PeopleSearchScope
typealias PeopleSearchSort = com.rotd.kejasafe.domain.chat.PeopleSearchSort
typealias PeopleSearchFilters = com.rotd.kejasafe.domain.chat.PeopleSearchFilters
typealias SavedPeopleSearch = com.rotd.kejasafe.domain.chat.SavedPeopleSearch

typealias Meeting = com.rotd.kejasafe.domain.call.Meeting
typealias MeetingType = com.rotd.kejasafe.domain.call.MeetingType
typealias MeetingStatus = com.rotd.kejasafe.domain.call.MeetingStatus
typealias ParticipantStatus = com.rotd.kejasafe.domain.call.ParticipantStatus
typealias IncomingCall = com.rotd.kejasafe.domain.call.IncomingCall

typealias LiveKitConnectionDetails = com.rotd.kejasafe.LiveKitConnectionDetails

data class Feedback(
    val id: String = "",
    val content: String = "",
    val category: String = "",
    val screenshotUrl: String? = null,
    val userId: String = "",
    val createdAt: Long = 0L,
    val synced: Boolean = false,
)