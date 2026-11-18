package org.example.project.data.ads

import com.ozi.nextgen.compose.ads_kmp.OziAds
import com.ozi.nextgen.compose.ads_kmp.OziAdsConfig
import com.ozi.nextgen.compose.ads_kmp.prepare
import com.ozi.nextgen.compose.ads_manager.remote_config.model.AdPlacementKeys
import kotlinx.coroutines.suspendCancellableCoroutine
import org.example.project.Platform
import photocollagemaker.shared.generated.resources.Res
import kotlin.coroutines.resume

/**
 * Starts the ads SDK (ads-kmp) once per launch: UMP consent first, then the iOS tracking prompt
 * ([requestAdTrackingAuthorization]), then [OziAds.initialize], which applies the bundled defaults, initializes Google
 * Mobile Ads and fetches Firebase Remote Config.
 *
 * Each platform reads only its own Remote Config parameter, both in the
 * `collage-maker-photoeditor` Firebase project:
 * - Android: `ad_configuration_android`, defaults in `files/ads_defaults_android.json`
 * - iOS: `ad_configuration_ios`, defaults in `files/ads_defaults_ios.json`
 *
 * The AdMob app ID is per build: Google's test ID in debug builds, the app's own in release
 * ([Platform.adMobAppId]). Android sets it in `androidApp/build.gradle.kts` (`resValue` per build
 * type); iOS in `iosApp/Configuration/Config.xcconfig` (`ADMOB_APP_ID[config=…]`, read by Info.plist).
 *
 * The startup flow (splash → language / onboarding / premium / offer → Home, and the ads between
 * them) is driven the same way by `OziAds.navigation`: `app_navigation_configuration_android` /
 * `app_navigation_configuration_ios`, falling back to `files/app_navigation_configuration_<platform>.json`
 * whenever the parameter can't be used (not fetched yet, fetch failed, missing, or unreadable).
 * Its `splash.show_consent` decides whether the UMP form runs here.
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

        val config = OziAdsConfig(
            androidAppId = platform.adMobAppId,
            isDebug = platform.isDebugBuild,
            androidDefaultsJson = Res.readBytes("files/ads_defaults_android.json").decodeToString(),
            iosDefaultsJson = Res.readBytes("files/ads_defaults_ios.json").decodeToString(),
            androidNavigationDefaultsJson = Res.readBytes("files/app_navigation_configuration_android.json").decodeToString(),
            iosNavigationDefaultsJson = Res.readBytes("files/app_navigation_configuration_ios.json").decodeToString(),
        )

        // Lets show_consent be read before initialize (Remote Config's last fetch, else the bundled file).
        OziAds.navigation.prepare(config)
        if (OziAds.navigation.showConsentOnSplash) {
            suspendCancellableCoroutine { cont ->
                OziAds.consent.gather { error ->
                    if (error != null) println("Ads: consent error: $error")
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        } else {
            println("Ads: show_consent is off, skipping the consent form")
        }
        // iOS: App Tracking Transparency after consent, before ads load (Google's recommended order).
        requestAdTrackingAuthorization()
        return suspendCancellableCoroutine { cont ->
            OziAds.initialize(config) { fetched ->
                val block = if (platform.isDebugBuild) "debug_ad_configuration" else "release_ad_configuration"
                println("Ads: initialized on ${OziAds.platformName} (${OziAds.activeConfigKey} → $block, app ID ${platform.adMobAppId}), Remote Config fetched=$fetched")
                val flow = OziAds.navigation.config
                println(
                    "Ads: startup flow from ${OziAds.navigation.source} (${OziAds.navigation.activeKey}): " +
                        "first=${flow.firstLaunch.map { it.screen.key }} second=${flow.secondLaunch.map { it.screen.key }}",
                )
                // What the active block resolved to, to check a console change landed (relaunch after publishing).
                val banner = AdPlacementKeys.INTERNAL_APP_BANNER
                println("Ads: $banner enabled=${OziAds.isEnabled(banner, AdPoints.INTERNAL_APP_BANNER_EDITOR)} adId=${OziAds.adIdFor(banner).ifEmpty { "(empty)" }}")
                // Warm the back-press interstitial, so the first back doesn't wait on a load.
                if (!isPremium) OziAds.interstitial.loadForPlacement(AdPlacementKeys.INTERNAL_APP_INTERSTITIAL)
                if (cont.isActive) cont.resume(fetched)
            }
        }
    }
}
