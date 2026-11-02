package com.rotd.keja_safe.domain.usecase

import com.rotd.keja_safe.domain.model.SavedPeopleSearch
import com.rotd.keja_safe.domain.repository.PeopleSearchRepository
import kotlinx.coroutines.flow.Flow

class GetSavedPeopleSearchesUseCase(
    private val repository: PeopleSearchRepository,
) {
    operator fun invoke(ownerUid: String): Flow<List<SavedPeopleSearch>> =
        repository.observeSavedSearches(ownerUid)
}

class SavePeopleSearchUseCase(
    private val repository: PeopleSearchRepository,
) {
    suspend operator fun invoke(search: SavedPeopleSearch) {
        repository.saveSearch(search)
    }
}

class RemoveSavedPeopleSearchUseCase(
    private val repository: PeopleSearchRepository,
) {
    suspend operator fun invoke(ownerUid: String, searchId: String) {
        repository.removeSearch(ownerUid, searchId)
    }
}

class ClearSavedPeopleSearchesUseCase(
    private val repository: PeopleSearchRepository,
) {
    suspend operator fun invoke(ownerUid: String) {
        repository.clearAllForUser(ownerUid)
    }
}
