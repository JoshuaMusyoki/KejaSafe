package com.rotd.keja_safe.domain.repository

import com.rotd.keja_safe.domain.model.SavedPeopleSearch
import kotlinx.coroutines.flow.Flow

interface PeopleSearchRepository {
    fun observeSavedSearches(ownerUid: String): Flow<List<SavedPeopleSearch>>
    suspend fun saveSearch(search: SavedPeopleSearch)
    suspend fun removeSearch(ownerUid: String, searchId: String)
    suspend fun clearAllForUser(ownerUid: String)
}
