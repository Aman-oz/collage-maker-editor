package org.example.project.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.data.ads.AdsInitializer

/** Where the splash hands off to once its loading time is over. */
enum class SplashNext {
    /** First install (onboarding not finished yet): the language → onboarding → paywall flow. */
    FirstRun,

    /** Returning, non-premium user: the paywall, then Home. */
    Premium,

    /** Returning premium user: nothing to sell, straight to Home. */
    Home,
}

/**
 * Holds the splash screen state. [next] stays `null` for the loading time, then says where to go.
 * The loading time is at least [SPLASH_DURATION_MS] and also covers ads start-up (consent, then
 * the ads SDK + Firebase Remote Config fetch, which ads-kmp caps at its own fetch timeout).
 */
class SplashViewModel(
    private val settings: AppSettings,
    private val ads: AdsInitializer,
) : ViewModel() {

    private val _next = MutableStateFlow<SplashNext?>(null)
    val next: StateFlow<SplashNext?> = _next.asStateFlow()

    init {
        viewModelScope.launch {
            val adsReady = async { ads.start(isPremium = settings.isPremium.value) }
            delay(SPLASH_DURATION_MS)
            adsReady.await()
            _next.value = when {
                !settings.hasCompletedOnboarding -> SplashNext.FirstRun
                settings.isPremium.value -> SplashNext.Home
                else -> SplashNext.Premium
            }
        }
    }

    private companion object {
        const val SPLASH_DURATION_MS = 3_000L
    }
}
