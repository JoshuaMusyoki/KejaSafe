package com.rotd.stawi.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rotd.stawi.domain.model.SavedPeopleSearch
import com.rotd.stawi.domain.repository.PeopleSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PeopleSearchRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : PeopleSearchRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private object Keys {
        val SAVED_PEOPLE_SEARCHES = stringPreferencesKey("saved_people_searches")
    }

    override fun observeSavedSearches(ownerUid: String): Flow<List<SavedPeopleSearch>> {
        return dataStore.data.map { prefs ->
            readAll(prefs)
                .filter { it.ownerUid == ownerUid }
                .sortedByDescending { it.savedAt }
        }
    }

    override suspend fun saveSearch(search: SavedPeopleSearch) {
        if (search.ownerUid.isBlank()) return
        dataStore.edit { prefs ->
            val current = readAll(prefs).toMutableList()
            // Replace same query+scope for this owner, otherwise prepend
            current.removeAll {
                it.ownerUid == search.ownerUid &&
                    it.id == search.id
            }
            // Cap history per account
            val others = current.filter { it.ownerUid != search.ownerUid }
            val mine = (listOf(search) + current.filter { it.ownerUid == search.ownerUid })
                .distinctBy { it.id }
                .take(MAX_PER_USER)
            prefs[Keys.SAVED_PEOPLE_SEARCHES] = json.encodeToString(others + mine)
        }
    }

    override suspend fun removeSearch(ownerUid: String, searchId: String) {
        dataStore.edit { prefs ->
            val next = readAll(prefs).filterNot {
                it.ownerUid == ownerUid && it.id == searchId
            }
            prefs[Keys.SAVED_PEOPLE_SEARCHES] = json.encodeToString(next)
        }
    }

    override suspend fun clearAllForUser(ownerUid: String) {
        dataStore.edit { prefs ->
            val next = readAll(prefs).filterNot { it.ownerUid == ownerUid }
            prefs[Keys.SAVED_PEOPLE_SEARCHES] = json.encodeToString(next)
        }
    }

    private fun readAll(prefs: Preferences): List<SavedPeopleSearch> {
        val raw = prefs[Keys.SAVED_PEOPLE_SEARCHES] ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MAX_PER_USER = 30
    }
}
