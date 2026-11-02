package com.rotd.stawi.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.rotd.stawi.domain.model.DefaultLearningStacks
import com.rotd.stawi.domain.model.StackUnlockProgress
import com.rotd.stawi.domain.model.User

class SyncPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val IS_SYNC_ENABLED = booleanPreferencesKey("is_sync_enabled")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val IS_AUTO_SIGN_OUT_ENABLED = booleanPreferencesKey("is_auto_sign_out_enabled")
        val APP_PIN = stringPreferencesKey("app_pin")
        val LOCKED_FOR_UID = stringPreferencesKey("locked_for_uid")
        val LAST_UNLOCKED_UID = stringPreferencesKey("last_unlocked_uid")
        val KNOWN_ACCOUNTS = stringPreferencesKey("known_accounts")
        val LAST_READ_DAY_INDEX = intPreferencesKey("last_read_day_index")
        val LAST_READ_TIMESTAMP = longPreferencesKey("last_read_timestamp")
        val STACK_UNLOCK_PROGRESS = stringPreferencesKey("stack_unlock_progress")
        val SELECTED_LEARNING_STACK_ID = stringPreferencesKey("selected_learning_stack_id")
        val HAS_SHOWN_DOCS_REFERRAL = booleanPreferencesKey("has_shown_docs_referral")
        val SPONSORED_PROMO_DAY = stringPreferencesKey("sponsored_promo_day")
        val SPONSORED_PROMO_COUNT = intPreferencesKey("sponsored_promo_count")
        val SPONSORED_PROMO_LAST_SHOWN_AT = longPreferencesKey("sponsored_promo_last_shown_at")
        val CLAIMED_LINK_REWARDS = stringPreferencesKey("claimed_link_rewards")
        val CHAT_ACCESS_EXPIRES_AT = longPreferencesKey("chat_access_expires_at")
        val CHAT_ACCESS_GRANTED_POINTS = intPreferencesKey("chat_access_granted_points")
        val PENDING_CHAT_ACCESS_PROMPT = booleanPreferencesKey("pending_chat_access_prompt")
        val VISIBLE_NAV_ITEMS = stringPreferencesKey("visible_nav_items")
        /** Bumped when default dock features change (e.g. Videos floating button on by default). */
        val VISIBLE_NAV_DEFAULTS_VERSION = intPreferencesKey("visible_nav_defaults_version")
    }

    companion object {
        const val CHAT_ACCESS_DURATION_MS = 3 * 60 * 60 * 1000L // 3 hours
        /** Finance / Learn tracks / Chat / Learning Groups + Videos floating action. */
        val DEFAULT_VISIBLE_NAV_ITEMS: Set<String> =
            setOf("Chat", "Learning Groups", "Finance", "Learn", "Videos")
        private const val CURRENT_NAV_DEFAULTS_VERSION = 3
    }

    val isSyncEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_SYNC_ENABLED] ?: true
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE] ?: false
    }

    val themeMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: false
    }

    val isAutoSignOutEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_AUTO_SIGN_OUT_ENABLED] ?: true
    }

    val appPin: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_PIN]
    }

    val lockedForUid: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LOCKED_FOR_UID]
    }

    val lastUnlockedUid: Flow<String?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_UNLOCKED_UID]
    }

    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0L
    }

    val lastReadDayIndex: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_READ_DAY_INDEX] ?: -1
    }

    val lastReadTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_READ_TIMESTAMP] ?: 0L
    }

    val selectedLearningStackId: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_LEARNING_STACK_ID]
            ?: DefaultLearningStacks.FINANCIAL_LITERACY
    }

    val stackUnlockProgress: Flow<Map<String, StackUnlockProgress>> = dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.STACK_UNLOCK_PROGRESS]
        val parsed = if (raw.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                Json.decodeFromString<Map<String, StackUnlockProgress>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        }
        // Migrate legacy single-track progress into Data Annotation once
        val legacyIndex = preferences[PreferencesKeys.LAST_READ_DAY_INDEX]
        val legacyTs = preferences[PreferencesKeys.LAST_READ_TIMESTAMP]
        if (legacyIndex != null && DefaultLearningStacks.DATA_ANNOTATION !in parsed) {
            parsed + (DefaultLearningStacks.DATA_ANNOTATION to StackUnlockProgress(
                dayIndex = legacyIndex,
                timestamp = legacyTs ?: 0L
            ))
        } else {
            parsed
        }
    }

    val hasShownDocsReferral: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SHOWN_DOCS_REFERRAL] ?: false
    }

    val knownAccounts: Flow<List<User>> = dataStore.data.map { preferences ->
        val json = preferences[PreferencesKeys.KNOWN_ACCOUNTS] ?: "[]"
        try {
            Json.decodeFromString<List<User>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateSyncEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SYNC_ENABLED] = enabled
        }
    }

    suspend fun updateDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = enabled
        }
    }

    suspend fun updateThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun updateAutoSignOutEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AUTO_SIGN_OUT_ENABLED] = enabled
        }
    }

    suspend fun updateAppPin(pin: String?) {
        dataStore.edit { preferences ->
            if (pin == null) {
                preferences.remove(PreferencesKeys.APP_PIN)
            } else {
                preferences[PreferencesKeys.APP_PIN] = pin
            }
        }
    }

    suspend fun updateLockedForUid(uid: String?) {
        dataStore.edit { preferences ->
            if (uid == null) {
                preferences.remove(PreferencesKeys.LOCKED_FOR_UID)
            } else {
                preferences[PreferencesKeys.LOCKED_FOR_UID] = uid
            }
        }
    }

    suspend fun updateLastUnlockedUid(uid: String?) {
        dataStore.edit { preferences ->
            if (uid == null) {
                preferences.remove(PreferencesKeys.LAST_UNLOCKED_UID)
            } else {
                preferences[PreferencesKeys.LAST_UNLOCKED_UID] = uid
            }
        }
    }

    suspend fun clearSessionLockState() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.APP_PIN)
            preferences.remove(PreferencesKeys.LOCKED_FOR_UID)
            preferences.remove(PreferencesKeys.LAST_UNLOCKED_UID)
        }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    suspend fun addKnownAccount(user: User) {
        dataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.KNOWN_ACCOUNTS] ?: "[]"
            val current = try {
                Json.decodeFromString<List<User>>(json).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Update if exists, else add
            val index = current.indexOfFirst { it.uid == user.uid }
            if (index != -1) {
                current[index] = user
            } else {
                current.add(user)
            }
            
            preferences[PreferencesKeys.KNOWN_ACCOUNTS] = Json.encodeToString(current)
        }
    }

    suspend fun removeKnownAccount(uid: String) {
        dataStore.edit { preferences ->
            val json = preferences[PreferencesKeys.KNOWN_ACCOUNTS] ?: "[]"
            val current = try {
                Json.decodeFromString<List<User>>(json).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            current.removeAll { it.uid == uid }
            preferences[PreferencesKeys.KNOWN_ACCOUNTS] = Json.encodeToString(current)
        }
    }

    suspend fun updateLastReadMaterial(index: Int) {
        updateStackUnlockProgress(DefaultLearningStacks.DATA_ANNOTATION, index)
    }

    suspend fun updateSelectedLearningStackId(stackId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LEARNING_STACK_ID] = stackId
        }
    }

    suspend fun updateStackUnlockProgress(stackId: String, index: Int) {
        dataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.STACK_UNLOCK_PROGRESS]
            val currentMap = if (raw.isNullOrBlank()) {
                mutableMapOf()
            } else {
                try {
                    Json.decodeFromString<Map<String, StackUnlockProgress>>(raw).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            }
            val existing = currentMap[stackId] ?: StackUnlockProgress()
            if (index > existing.dayIndex) {
                currentMap[stackId] = StackUnlockProgress(
                    dayIndex = index,
                    timestamp = System.currentTimeMillis()
                )
            } else if (index == existing.dayIndex && existing.timestamp == 0L) {
                currentMap[stackId] = existing.copy(timestamp = System.currentTimeMillis())
            }
            preferences[PreferencesKeys.STACK_UNLOCK_PROGRESS] = Json.encodeToString(currentMap)

            // Keep legacy keys in sync for Data Annotation during transition
            if (stackId == DefaultLearningStacks.DATA_ANNOTATION) {
                val progress = currentMap[stackId] ?: return@edit
                preferences[PreferencesKeys.LAST_READ_DAY_INDEX] = progress.dayIndex
                preferences[PreferencesKeys.LAST_READ_TIMESTAMP] = progress.timestamp
            }
        }
    }

    suspend fun updateHasShownDocsReferral(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_DOCS_REFERRAL] = shown
        }
    }


    val claimedLinkRewards: Flow<Set<String>> = dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.CLAIMED_LINK_REWARDS].orEmpty()
        if (raw.isBlank()) emptySet() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val chatAccessExpiresAt: Flow<Long> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CHAT_ACCESS_EXPIRES_AT] ?: 0L
    }

    val chatAccessGrantedPoints: Flow<Int> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CHAT_ACCESS_GRANTED_POINTS] ?: 0
    }

    val visibleNavItems: Flow<Set<String>> = dataStore.data.map { preferences ->
        parseVisibleNavItems(preferences)
    }

    /**
     * Applies nav default migrations once (e.g. re-enable Videos after it moved to the
     * floating search button). Safe to call on every app start.
     */
    suspend fun ensureVisibleNavDefaults() {
        dataStore.edit { preferences ->
            val version = preferences[PreferencesKeys.VISIBLE_NAV_DEFAULTS_VERSION] ?: 0
            if (version >= CURRENT_NAV_DEFAULTS_VERSION) return@edit

            val items = parseVisibleNavItems(preferences).toMutableSet()
            // v2: Videos is no longer a dock tab but should stay available by default
            // via the floating search control.
            items.add("Videos")
            // v3: Community tab renamed to Learning Groups
            if (items.remove("Community")) {
                items.add("Learning Groups")
            }
            preferences[PreferencesKeys.VISIBLE_NAV_ITEMS] = items.joinToString(",")
            preferences[PreferencesKeys.VISIBLE_NAV_DEFAULTS_VERSION] = CURRENT_NAV_DEFAULTS_VERSION
        }
    }

    private fun parseVisibleNavItems(preferences: Preferences): Set<String> {
        val raw = preferences[PreferencesKeys.VISIBLE_NAV_ITEMS]
        if (raw == null) {
            return DEFAULT_VISIBLE_NAV_ITEMS
        }
        val items = raw.split(",").filter { it.isNotBlank() }.toMutableSet()
        // Migration for merged Finance tab and mandatory Learn default
        if (items.contains("Invest") || items.contains("Budget") || items.contains("Tax")) {
            items.remove("Invest")
            items.remove("Budget")
            items.remove("Tax")
            items.add("Finance")
        }
        // Migration: Community tab renamed
        if (items.remove("Community")) {
            items.add("Learning Groups")
        }
        // Ensure Learn is present by default for everyone (can still be hidden in settings later)
        if (!items.contains("Learn") && raw.split(",").size < 4) {
            items.add("Learn")
        }
        // Until migration persists, surface Videos so the default matches product intent.
        val version = preferences[PreferencesKeys.VISIBLE_NAV_DEFAULTS_VERSION] ?: 0
        if (version < CURRENT_NAV_DEFAULTS_VERSION) {
            items.add("Videos")
        }
        return items
    }

    suspend fun updateVisibleNavItems(items: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.VISIBLE_NAV_ITEMS] = items.joinToString(",")
        }
    }

    val pendingChatAccessPrompt: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PENDING_CHAT_ACCESS_PROMPT] ?: false
    }

    suspend fun markLinkRewardClaimed(linkId: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.CLAIMED_LINK_REWARDS].orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toMutableSet()
            current.add(linkId)
            preferences[PreferencesKeys.CLAIMED_LINK_REWARDS] = current.joinToString(",")
        }
    }

    suspend fun startChatAccess(points: Int, durationMs: Long = CHAT_ACCESS_DURATION_MS) {
        dataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            val existingExpiresAt = preferences[PreferencesKeys.CHAT_ACCESS_EXPIRES_AT] ?: 0L
            val existingPoints = preferences[PreferencesKeys.CHAT_ACCESS_GRANTED_POINTS] ?: 0
            val stillActive = existingExpiresAt > now
            preferences[PreferencesKeys.CHAT_ACCESS_EXPIRES_AT] = now + durationMs
            preferences[PreferencesKeys.CHAT_ACCESS_GRANTED_POINTS] =
                if (stillActive) existingPoints + points else points
            preferences[PreferencesKeys.PENDING_CHAT_ACCESS_PROMPT] = false
        }
    }

    suspend fun clearChatAccess(promptRenewal: Boolean) {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CHAT_ACCESS_EXPIRES_AT)
            preferences.remove(PreferencesKeys.CHAT_ACCESS_GRANTED_POINTS)
            preferences.remove(PreferencesKeys.CLAIMED_LINK_REWARDS)
            preferences[PreferencesKeys.PENDING_CHAT_ACCESS_PROMPT] = promptRenewal
        }
    }

    suspend fun clearPendingChatAccessPrompt() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PENDING_CHAT_ACCESS_PROMPT] = false
        }
    }

    data class SponsoredPromoState(
        val dayKey: String = "",
        val showCount: Int = 0,
        val lastShownAt: Long = 0L
    )

    val sponsoredPromoState: Flow<SponsoredPromoState> = dataStore.data.map { preferences ->
        SponsoredPromoState(
            dayKey = preferences[PreferencesKeys.SPONSORED_PROMO_DAY].orEmpty(),
            showCount = preferences[PreferencesKeys.SPONSORED_PROMO_COUNT] ?: 0,
            lastShownAt = preferences[PreferencesKeys.SPONSORED_PROMO_LAST_SHOWN_AT] ?: 0L
        )
    }

    suspend fun recordSponsoredPromoShown(dayKey: String, shownAt: Long) {
        dataStore.edit { preferences ->
            val currentDay = preferences[PreferencesKeys.SPONSORED_PROMO_DAY].orEmpty()
            val nextCount = if (currentDay == dayKey) {
                (preferences[PreferencesKeys.SPONSORED_PROMO_COUNT] ?: 0) + 1
            } else {
                1
            }
            preferences[PreferencesKeys.SPONSORED_PROMO_DAY] = dayKey
            preferences[PreferencesKeys.SPONSORED_PROMO_COUNT] = nextCount
            preferences[PreferencesKeys.SPONSORED_PROMO_LAST_SHOWN_AT] = shownAt
        }
    }
}



