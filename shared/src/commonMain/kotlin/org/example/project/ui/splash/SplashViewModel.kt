package org.example.project.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ozi.nextgen.compose.ads_kmp.OziAds
import com.ozi.nextgen.compose.ads_manager.navigation.NavScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.example.project.data.AppSettings
import org.example.project.data.ads.AdsInitializer
import org.example.project.data.ads.startupState
import org.example.project.data.appicon.AppIcons
import kotlin.coroutines.resume

/**
 * Holds the splash screen state. [ready] stays `false` for the loading time, then the splash shows
 * its "Get Started" variant and [start] hands over to the startup flow (`OziAds.navigation`),
 * which picks the first screen (language, onboarding, premium, offer or Home) from Remote Config.
 *
 * The loading time is at least [SPLASH_DURATION_MS] and also covers ads start-up (consent if the
 * flow asks for it, then the ads SDK + Firebase Remote Config fetch, which ads-kmp caps at its own
 * fetch timeout) and the ad shown before the first screen (capped at [FIRST_AD_WAIT_MS]).
 */
class SplashViewModel(
    private val settings: AppSettings,
    private val ads: AdsInitializer,
) : ViewModel() {

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /** How splash continues: one of `GetStartedVariant` (0 = by itself, 1 banner, 2 native, 3 button). */
    val getStartedVariant: Int get() = OziAds.navigation.getStartedVariant(settings.startupState())

    init {
        viewModelScope.launch {
            val minimumTime = async { delay(SPLASH_DURATION_MS) }
            ads.start(isPremium = settings.isPremium.value)
            // Remote Config is in now, so this is where a console change of the icon lands.
            AppIcons.applyFromRemoteConfig()
            // Load the ad shown before the first screen, so it is ready when the user moves on.
            withTimeoutOrNull(FIRST_AD_WAIT_MS) {
                suspendCancellableCoroutine { cont ->
                    OziAds.navigation.preloadFirst(settings.startupState()) { if (cont.isActive) cont.resume(Unit) }
                }
            }
            minimumTime.await()
            _ready.value = true
        }
    }

    /** Leaves splash: shows the first step's ad (if any), then [onNavigate] with the first screen. */
    fun start(onNavigate: (NavScreen) -> Unit) {
        OziAds.navigation.first(settings.startupState(), onNavigate)
    }

    private companion object {
        const val SPLASH_DURATION_MS = 3_000L
        const val FIRST_AD_WAIT_MS = 5_000L
    }
}
