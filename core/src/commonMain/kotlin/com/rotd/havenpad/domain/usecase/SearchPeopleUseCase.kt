package com.rotd.keja_safe.domain.usecase

import com.rotd.keja_safe.domain.model.PeopleSearchFilters
import com.rotd.keja_safe.domain.model.PeopleSearchSort
import com.rotd.keja_safe.domain.model.User

/**
 * Pure people-directory search: query matching + advanced filters + sort.
 */
class SearchPeopleUseCase {
    operator fun invoke(
        candidates: List<User>,
        query: String,
        filters: PeopleSearchFilters,
    ): List<User> {
        val needle = query.trim().lowercase()
        val matched = candidates.filter { user ->
            if (filters.onlineOnly && !user.isActive) return@filter false
            if (needle.isEmpty()) return@filter true
            matchesQuery(user, needle, filters)
        }
        return sort(matched, filters.sortBy)
    }

    private fun matchesQuery(
        user: User,
        needle: String,
        filters: PeopleSearchFilters,
    ): Boolean {
        if (!filters.matchUsername && !filters.matchEmail && !filters.matchPhone) {
            // Fallback: username when every field is disabled so search never silently dies
            return user.username.lowercase().contains(needle)
        }
        if (filters.matchUsername && user.username.lowercase().contains(needle)) return true
        if (filters.matchEmail && user.email.orEmpty().lowercase().contains(needle)) return true
        if (filters.matchPhone) {
            val phone = user.phoneNumber.filter { it.isDigit() }
            val digits = needle.filter { it.isDigit() }
            if (digits.isNotEmpty() && phone.contains(digits)) return true
            if (user.phoneNumber.lowercase().contains(needle)) return true
        }
        // Public profile copy — keep email/phone out of display but allow name + about matching
        if (filters.matchUsername) {
            if (user.professionalBackground.lowercase().contains(needle)) return true
            if (user.ambitions.lowercase().contains(needle)) return true
            if (user.interests.any { it.lowercase().contains(needle) }) return true
        }
        return false
    }

    private fun sort(users: List<User>, sortBy: PeopleSearchSort): List<User> {
        return when (sortBy) {
            PeopleSearchSort.NAME_ASC -> users.sortedBy { it.username.lowercase() }
            PeopleSearchSort.NAME_DESC -> users.sortedByDescending { it.username.lowercase() }
            PeopleSearchSort.RECENTLY_ACTIVE -> users.sortedByDescending { it.lastSeen }
            PeopleSearchSort.ONLINE_FIRST -> users.sortedWith(
                compareByDescending<User> { it.isActive }
                    .thenByDescending { it.lastSeen }
                    .thenBy { it.username.lowercase() }
            )
        }
    }
}
