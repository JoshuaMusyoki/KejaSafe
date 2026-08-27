package com.rotd.kejasafe.domain.chat

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class PeopleSearchScope {
    CHATS,
    DISCOVER,
}

@Keep
@Serializable
enum class PeopleSearchSort {
    NAME_ASC,
    NAME_DESC,
    RECENTLY_ACTIVE,
    ONLINE_FIRST,
}

@Keep
@Serializable
data class PeopleSearchFilters(
    val onlineOnly: Boolean = false,
    val matchUsername: Boolean = true,
    val matchEmail: Boolean = true,
    val matchPhone: Boolean = true,
    val sortBy: PeopleSearchSort = PeopleSearchSort.ONLINE_FIRST,
)

/**
 * Snapshot of a people search the user chose to keep.
 * Stores both criteria and the result user ids so the set can be reopened later.
 */
@Keep
@Serializable
data class SavedPeopleSearch(
    val id: String,
    val ownerUid: String,
    val query: String,
    val scope: PeopleSearchScope,
    val filters: PeopleSearchFilters = PeopleSearchFilters(),
    val resultUserIds: List<String> = emptyList(),
    val resultUsernames: List<String> = emptyList(),
    val resultCount: Int = 0,
    val savedAt: Long = System.currentTimeMillis(),
    val label: String = "",
) {
    val displayLabel: String
        get() = label.ifBlank {
            query.trim().ifBlank { "Saved search · $resultCount" }
        }
}
