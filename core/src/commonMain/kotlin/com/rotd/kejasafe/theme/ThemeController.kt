package com.rotd.kejasafe.theme

import com.rotd.kejasafe.storage.LocalSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * User-facing appearance preference. Survives activity recreation (system theme / process death).
 */
object ThemeController {
    private const val KEY_MODE = "theme_mode"

    private val _mode = MutableStateFlow(loadMode())
    val mode = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        LocalSettings.putString(KEY_MODE, mode.name)
    }

    fun restore() {
        _mode.value = loadMode()
    }

    private fun loadMode(): ThemeMode {
        val raw = LocalSettings.getString(KEY_MODE) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }
}
