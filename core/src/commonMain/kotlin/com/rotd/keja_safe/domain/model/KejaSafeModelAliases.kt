package com.rotd.keja_safe.domain.model

/**
 * Compatibility aliases for unfinished Stawi→HavenPad chat port.
 * Real types live under com.rotd.havenpad.domain.chat / .call.
 */

typealias User = com.rotd.havenpad.domain.chat.User
typealias ChatMessage = com.rotd.havenpad.domain.chat.ChatMessage
typealias MessageStatus = com.rotd.havenpad.domain.chat.MessageStatus
typealias TransportType = com.rotd.havenpad.domain.chat.TransportType
typealias AttachmentType = com.rotd.havenpad.domain.chat.AttachmentType
typealias MessageReaction = com.rotd.havenpad.domain.chat.MessageReaction
typealias Connection = com.rotd.havenpad.domain.chat.Connection
typealias ConnectionStatus = com.rotd.havenpad.domain.chat.ConnectionStatus
typealias Notification = com.rotd.havenpad.domain.chat.Notification
typealias NotificationType = com.rotd.havenpad.domain.chat.NotificationType
typealias CommunityKind = com.rotd.havenpad.domain.chat.CommunityKind
typealias Group = com.rotd.havenpad.domain.chat.Group
typealias GroupInvite = com.rotd.havenpad.domain.chat.GroupInvite
typealias GroupInviteStatus = com.rotd.havenpad.domain.chat.GroupInviteStatus
typealias Discussion = com.rotd.havenpad.domain.chat.Discussion
typealias GroupMessage = com.rotd.havenpad.domain.chat.GroupMessage
typealias PeopleSearchScope = com.rotd.havenpad.domain.chat.PeopleSearchScope
typealias PeopleSearchSort = com.rotd.havenpad.domain.chat.PeopleSearchSort
typealias PeopleSearchFilters = com.rotd.havenpad.domain.chat.PeopleSearchFilters
typealias SavedPeopleSearch = com.rotd.havenpad.domain.chat.SavedPeopleSearch

typealias Meeting = com.rotd.havenpad.domain.call.Meeting
typealias MeetingType = com.rotd.havenpad.domain.call.MeetingType
typealias MeetingStatus = com.rotd.havenpad.domain.call.MeetingStatus
typealias ParticipantStatus = com.rotd.havenpad.domain.call.ParticipantStatus
typealias IncomingCall = com.rotd.havenpad.domain.call.IncomingCall

typealias LiveKitConnectionDetails = com.rotd.havenpad.LiveKitConnectionDetails

data class Feedback(
    val id: String = "",
    val content: String = "",
    val category: String = "",
    val screenshotUrl: String? = null,
    val userId: String = "",
    val createdAt: Long = 0L,
    val synced: Boolean = false,
)