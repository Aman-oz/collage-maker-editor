package org.example.project.data.ads

import com.ozi.nextgen.compose.ads_kmp.OziAds
import com.ozi.nextgen.compose.ads_manager.remote_config.model.AdPlacementKeys
import org.example.project.data.AppSettings
import org.example.project.navigation.Destination

/**
 * The `internal_app_interstitial` shown when leaving a screen with back (its back / close button,
 * or the system back). Pacing is the SDK's time-and-counter rule (`interstitial_count`,
 * `interstitial_delay_min_sec` / `_max_sec`), so most back presses show nothing.
 *
 * The loading dialog and how long it stays up come from Remote Config, per platform:
 * - `show_inter_ad_dialog_android` / `show_inter_ad_dialog_ios` — default `true`
 * - `inter_ad_dialog_timeout_android` / `inter_ad_dialog_timeout_ios` — seconds, default `2`
 *
 * The screen is popped once the SDK reports back, which it always does (ad closed, paced out,
 * failed, disabled or blocked at the screen's point), so back can't get stuck behind the ad.
 */
class BackInterstitial(private val settings: AppSettings) {

    private var inFlight = false

    /** Leaves [from] (the top of the back stack) through [pop], after the ad when one is due. */
    fun goBack(from: Destination?, pop: () -> Unit) {
        val point = from?.let(::interstitialBackPoint)
        if (point == null || settings.isPremium.value) {
            pop()
            return
        }
        // A second back press while the ad loads or shows is ignored.
        if (inFlight) return
        inFlight = true

        val suffix = if (OziAds.platformName == "ios") "ios" else "android"
        val showDialog = OziAds.remoteBoolean("show_inter_ad_dialog_$suffix", default = true)
        val dialogSeconds = OziAds.remoteLong("inter_ad_dialog_timeout_$suffix", default = 2L).coerceIn(0L, 10L)

        OziAds.interstitial.showWithTimeAndCounterForPlacement(
            placementKey = AdPlacementKeys.INTERNAL_APP_INTERSTITIAL,
            point = point,
            showDialog = showDialog,
            dialogTimeoutMillis = dialogSeconds * 1_000L,
            onComplete = {
                inFlight = false
                pop()
            },
        )
    }
}

/**
 * The `point` for [AdPoints]' `<placement>_<screen>` naming, one per screen; add it to
 * `internal_app_interstitial`'s `points` in Remote Config to stop the ad on that screen's back
 * only. `null` for screens that never show it: the launch flow, Home (leaving it exits the app)
 * and the paywalls.
 */
internal fun interstitialBackPoint(destination: Destination): String? {
    val screen = when (destination) {
        Destination.Splash, Destination.Onboarding, Destination.Home -> return null
        is Destination.Premium, is Destination.ProOffer -> return null
        is Destination.Language -> "language"
        Destination.Settings -> "settings"
        is Destination.Gallery -> "gallery"
        is Destination.Editor -> "editor"
        is Destination.CollageEditor -> "collage_editor"
        Destination.Layouts -> "layouts"
        is Destination.LayoutEditor -> "layout_editor"
        is Destination.FreestyleEditor -> "freestyle_editor"
        Destination.ProEditor -> "pro_editor"
        is Destination.Templates -> "templates"
        is Destination.TemplatesEditor -> "templates_editor"
        Destination.Pip -> "pip"
        is Destination.PipEditor -> "pip_editor"
        is Destination.Frames -> "frames"
        is Destination.FramesEditor -> "frames_editor"
        Destination.SaveImage -> "save_image"
        is Destination.ShareImage -> "share_image"
        is Destination.Preview -> "preview"
        is Destination.BackgroundRemoverCrop -> "bg_remover_crop"
        Destination.BackgroundRemoverEditor -> "bg_remover_editor"
        Destination.SetBackground -> "set_background"
        is Destination.Shop -> "shop"
    }
    return "${AdPlacementKeys.INTERNAL_APP_INTERSTITIAL}_$screen"
}
