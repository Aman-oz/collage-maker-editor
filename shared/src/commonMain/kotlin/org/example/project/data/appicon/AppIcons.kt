package org.example.project.data.appicon

import com.ozi.nextgen.compose.ads_kmp.OziAds
import org.example.project.AppLog
import org.jetbrains.compose.resources.DrawableResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.app_icon_1
import photocollagemaker.shared.generated.resources.app_icon_10
import photocollagemaker.shared.generated.resources.app_icon_11
import photocollagemaker.shared.generated.resources.app_icon_12
import photocollagemaker.shared.generated.resources.app_icon_2
import photocollagemaker.shared.generated.resources.app_icon_3
import photocollagemaker.shared.generated.resources.app_icon_4
import photocollagemaker.shared.generated.resources.app_icon_5
import photocollagemaker.shared.generated.resources.app_icon_6
import photocollagemaker.shared.generated.resources.app_icon_7
import photocollagemaker.shared.generated.resources.app_icon_8
import photocollagemaker.shared.generated.resources.app_icon_9

/**
 * The launcher icons the app can switch between at runtime, chosen from Firebase Remote Config:
 * `app_icon_ios` / `app_icon_android` (a String) holds one of [ids], or [DEFAULT] (or
 * `app_icon_1`) for the default icon, which is `app_icon_1`. Changing it in the console changes the icon on the next launch, after the
 * splash's Remote Config fetch.
 *
 * - iOS: each id is an app icon set in `iosApp/iosApp/Assets.xcassets` (`app_icon_1.appiconset`,
 *   …), bundled as alternate icons by `ASSETCATALOG_COMPILER_INCLUDE_ALL_APPICON_ASSETS` in
 *   `Config.xcconfig`. iOS shows its own "You have changed the icon" alert on every switch.
 * - Android: one `<activity-alias android:name=".Launcher_<id>">` per icon in androidApp's
 *   manifest, with adaptive icons `mipmap-anydpi-v26/ic_launcher_<id>.xml`. The switch is made
 *   when the app next leaves the screen, so some launchers don't close it or lose its shortcut.
 *
 * Source artwork: `collage-maker-assets/AppIcon/<id>.png` (1024×1024).
 */
object AppIcons {
    /**
     * The default icon, `app_icon_1`: iOS `AppIcon`, Android `ic_launcher` (`.Launcher_default`).
     * Remote Config may name it either way, `default` or [DEFAULT_ARTWORK].
     */
    const val DEFAULT = "default"

    /** The artwork the default icon is made from, so `app_icon_1` in Remote Config means [DEFAULT]. */
    const val DEFAULT_ARTWORK = "app_icon_1"

    /** The alternate icons, besides the default. */
    val ids = listOf(
        "app_icon_2", "app_icon_3", "app_icon_4", "app_icon_5", "app_icon_6",
        "app_icon_7", "app_icon_8", "app_icon_9", "app_icon_10", "app_icon_11", "app_icon_12",
    )

    /**
     * Switches to the icon Remote Config asks for on this platform. Call after Remote Config is
     * applied (the splash's ads start-up), on the main thread. Nothing changes when the key is
     * missing (never fetched, or not added in the console), the value is unknown, or that icon is
     * already showing; [DEFAULT] goes back to the original icon.
     */
    fun applyFromRemoteConfig() {
        val key = "app_icon_${OziAds.platformName}"
        val wanted = OziAds.remoteValue(key)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { if (it == DEFAULT_ARTWORK) DEFAULT else it }
            ?: return
        if (wanted != DEFAULT && wanted !in ids) {
            AppLog.w(Tag, "$key = '$wanted' is not a known icon; keeping ${currentAppIcon()}")
            return
        }
        val current = currentAppIcon()
        if (wanted == current) return
        AppLog.i(Tag, "Switching the app icon: $current → $wanted ($key)")
        setAppIcon(wanted)
    }

    /**
     * The selected icon's artwork (`composeResources/drawable/<id>.png`, 512 px), for the app's own
     * uses of its icon: the splash and the saved photo's watermark. It is the icon the launcher
     * shows, or on Android the one about to replace it; a Remote Config change shows here from the
     * launch after it is applied, as on the home screen.
     */
    fun selectedArtwork(): DrawableResource = artworkOf(currentAppIcon())

    private fun artworkOf(id: String): DrawableResource = when (id) {
        "app_icon_2" -> Res.drawable.app_icon_2
        "app_icon_3" -> Res.drawable.app_icon_3
        "app_icon_4" -> Res.drawable.app_icon_4
        "app_icon_5" -> Res.drawable.app_icon_5
        "app_icon_6" -> Res.drawable.app_icon_6
        "app_icon_7" -> Res.drawable.app_icon_7
        "app_icon_8" -> Res.drawable.app_icon_8
        "app_icon_9" -> Res.drawable.app_icon_9
        "app_icon_10" -> Res.drawable.app_icon_10
        "app_icon_11" -> Res.drawable.app_icon_11
        "app_icon_12" -> Res.drawable.app_icon_12
        else -> Res.drawable.app_icon_1
    }

    private const val Tag = "AppIcons"
}

/** The icon showing now: one of [AppIcons.ids], or [AppIcons.DEFAULT]. */
internal expect fun currentAppIcon(): String

/** Shows icon [id] ([AppIcons.DEFAULT] for the original). Main thread only. */
internal expect fun setAppIcon(id: String)
