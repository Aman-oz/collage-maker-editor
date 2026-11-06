package org.example.project.data.ads

import com.ozi.nextgen.compose.ads_kmp.OziAds
import com.ozi.nextgen.compose.ads_kmp.OziAdsConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.project.Platform
import photocollagemaker.shared.generated.resources.Res
import kotlin.coroutines.resume

/**
 * Starts the ads SDK (ads-kmp) once per launch: UMP consent first, then
 * [OziAds.initialize], which applies the bundled defaults, initializes Google
 * Mobile Ads and fetches Firebase Remote Config.
 *
 * Each platform reads only its own Remote Config parameter, both in the
 * `collage-maker-photoeditor` Firebase project:
 * - Android: `ad_configuration_android`, defaults in `files/ads_defaults_android.json`
 * - iOS: `ad_configuration_ios`, defaults in `files/ads_defaults_ios.json`
 *
 * The iOS AdMob app ID is `GADApplicationIdentifier` in `iosApp/iosApp/Info.plist`.
 */
class AdsInitializer(private val platform: Platform) {

    private var started = false

    /**
     * Gathers consent and initializes ads. Returns whether Remote Config was
     * fetched (`false` means the bundled defaults are in effect). Main thread only.
     */
    suspend fun start(isPremium: Boolean): Boolean {
        OziAds.isPremium = isPremium
        if (started) return true
        started = true

        val androidDefaults = Res.readBytes("files/ads_defaults_android.json").decodeToString()
        val iosDefaults = Res.readBytes("files/ads_defaults_ios.json").decodeToString()

        suspendCancellableCoroutine { cont ->
            OziAds.consent.gather { error ->
                if (error != null) println("Ads: consent error: $error")
                if (cont.isActive) cont.resume(Unit)
            }
        }
        return suspendCancellableCoroutine { cont ->
            OziAds.initialize(
                OziAdsConfig(
                    androidAppId = ANDROID_ADMOB_APP_ID,
                    isDebug = platform.isDebugBuild,
                    androidDefaultsJson = androidDefaults,
                    iosDefaultsJson = iosDefaults,
                ),
            ) { fetched ->
                println("Ads: initialized on ${OziAds.platformName} (${OziAds.activeConfigKey}), Remote Config fetched=$fetched")
                if (cont.isActive) cont.resume(fetched)
            }
        }
    }

    private companion object {
        // TODO: Google's Android *test* app ID. Replace with this app's AdMob Android
        //  app ID (ca-app-pub-XXXX~YYYY) before release.
        const val ANDROID_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    }
}
