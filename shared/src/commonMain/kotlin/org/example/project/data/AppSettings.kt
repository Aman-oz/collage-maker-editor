package org.example.project.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which color scheme the app uses. [System] follows the device's dark mode setting. */
enum class ThemeMode { System, Light, Dark }

/**
 * App-wide user preferences, persisted in a [KeyValueStore]. A Koin singleton, so the theme set on
 * the settings screen reaches [org.example.project.App] immediately through [themeMode].
 */
class AppSettings(private val store: KeyValueStore) {

    private val _themeMode = MutableStateFlow(
        store.getString(KeyThemeMode)?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.System,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        store.putString(KeyThemeMode, mode.name)
        _themeMode.value = mode
    }

    /** Language code picked on the language screen, or `null` if the user never picked one. */
    var languageCode: String?
        get() = store.getString(KeyLanguageCode)
        set(value) {
            if (value != null) store.putString(KeyLanguageCode, value)
        }

    private companion object {
        const val KeyThemeMode = "theme_mode"
        const val KeyLanguageCode = "language_code"
    }
}
