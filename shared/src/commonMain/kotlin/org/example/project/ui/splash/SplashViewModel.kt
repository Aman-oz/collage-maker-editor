package org.example.project.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings

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
 * Any real start-up work (remote config, assets) belongs here, alongside the fixed delay below.
 */
class SplashViewModel(private val settings: AppSettings) : ViewModel() {

    private val _next = MutableStateFlow<SplashNext?>(null)
    val next: StateFlow<SplashNext?> = _next.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MS)
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
