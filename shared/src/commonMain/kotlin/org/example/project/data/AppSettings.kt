package org.example.project.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.i18n.AppLocale
import org.example.project.ui.language.SupportedLanguages
import org.example.project.ui.language.deviceLanguageTag
import org.example.project.ui.language.resolveDefaultLanguageCode

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

    private val _isPremium = MutableStateFlow(store.getString(KeyPremium).toBoolean())

    /**
     * Whether the user has an active premium subscription. Nothing sets this until billing is
     * wired up; the purchase / restore flow should call [setPremium] with the store's answer.
     */
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun setPremium(premium: Boolean) {
        store.putString(KeyPremium, premium.toString())
        _isPremium.value = premium
    }

    /**
     * Language code picked on the language screen, or `null` if the user never picked one. Setting
     * it switches the whole app to that language at once, through [AppLocale].
     */
    var languageCode: String?
        get() = store.getString(KeyLanguageCode)
        set(value) {
            if (value != null) {
                store.putString(KeyLanguageCode, value)
                AppLocale.code = value
            }
        }

    init {
        // Before anything is shown: the saved language, or on a first run the device's own language
        // when the app has it (English otherwise), so the language screen itself is already in it.
        AppLocale.code = languageCode?.takeIf { code -> SupportedLanguages.any { it.code == code } }
            ?: resolveDefaultLanguageCode(deviceLanguageTag())
    }

    /**
     * Whether the first-run flow (language → onboarding) has been finished. Until it is, every
     * launch goes from Splash to the language picker again rather than to Home.
     */
    var hasCompletedOnboarding: Boolean
        get() = store.getString(KeyOnboardingCompleted).toBoolean()
        set(value) = store.putString(KeyOnboardingCompleted, value.toString())

    /**
     * When the launch-flow limited-time offer was first shown (epoch millis), or `null` if it never
     * was. Its countdown runs from here, so it keeps ticking across launches instead of restarting.
     */
    var launchOfferStartedAt: Long?
        get() = store.getString(KeyLaunchOfferStartedAt)?.toLongOrNull()
        set(value) {
            if (value != null) store.putString(KeyLaunchOfferStartedAt, value.toString())
        }

    private companion object {
        const val KeyThemeMode = "theme_mode"
        const val KeyLanguageCode = "language_code"
        const val KeyPremium = "is_premium"
        const val KeyOnboardingCompleted = "onboarding_completed"
        const val KeyLaunchOfferStartedAt = "launch_offer_started_at"
    }
}
