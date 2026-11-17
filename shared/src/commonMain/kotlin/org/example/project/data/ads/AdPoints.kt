package org.example.project.data.ads

/**
 * Call-site tokens passed as `point` to ads-kmp. Adding one to a placement's
 * `points` array in Remote Config (`ad_configuration_android` /
 * `ad_configuration_ios`) blocks that placement at that spot only, while it
 * stays enabled everywhere else. Named `<placement>_<screen>`.
 *
 * The startup flow's own interstitials and app-open ads get their points from the SDK
 * (`StartupNavigator.adPoint`): `<placement>_<screen it leads to>`, e.g.
 * `splash_interstitial_language`, `premium_close_interstitial_main`, `app_open_main`, and
 * `<placement>_<screen>_close` when a screen opened from Settings closes, e.g.
 * `language_interstitial_language_close`.
 */
object AdPoints {
    /** `splash_banner` under "Get Started" on the splash (`get_started_variant` 1). */
    const val SPLASH_BANNER_SPLASH = "splash_banner_splash"

    /** `splash_native` under "Get Started" on the splash (`get_started_variant` 2). */
    const val SPLASH_NATIVE_SPLASH = "splash_native_splash"

    /** `language_native` at the bottom of the Language screen in the startup flow. */
    const val LANGUAGE_NATIVE_LANGUAGE = "language_native_language"

    /** `language_native` at the bottom of the Language screen opened from Settings. */
    const val LANGUAGE_NATIVE_SETTINGS = "language_native_settings"

    /** `internal_app_banner` above the photo on the photo editor screen. */
    const val INTERNAL_APP_BANNER_EDITOR = "internal_app_banner_editor"

    /** `on_action_rewarded_video` before "Save with watermark" on the Save Image screen. */
    const val ON_ACTION_REWARDED_VIDEO_SAVE = "on_action_rewarded_video_save"

    /**
     * `internal_app_interstitial` on "Save with watermark" when its rewarded ad couldn't show.
     * Not the back-press point (`internal_app_interstitial_save_image`), so either can be blocked alone.
     */
    const val INTERNAL_APP_INTERSTITIAL_SAVE = "internal_app_interstitial_save"

    /** `on_action_rewarded_video` behind "Unlock free" in the background remover's Ai Magic dialog. */
    const val ON_ACTION_REWARDED_VIDEO_AI_MAGIC = "on_action_rewarded_video_ai_magic"

    /**
     * `home_banner` under Home's bottom bar. Its type comes from Remote Config
     * `home_banner_type_android` / `home_banner_type_ios`: 0 adaptive (default), 1 collapsible, 2 large.
     */
    const val HOME_BANNER_HOME = "home_banner_home"

    /** `internal_app_banner` at the bottom of the Layouts screen (Home's Layouts View All). */
    const val INTERNAL_APP_BANNER_LAYOUTS = "internal_app_banner_layouts"

    /** `internal_app_banner` at the bottom of the Layout editor. */
    const val INTERNAL_APP_BANNER_LAYOUT_EDITOR = "internal_app_banner_layout_editor"

    /** `internal_app_native` at the bottom of the Save Image screen. */
    const val INTERNAL_APP_NATIVE_SAVE = "internal_app_native_save"

    /** `internal_app_native` at the bottom of the Share Image screen. */
    const val INTERNAL_APP_NATIVE_SHARE = "internal_app_native_share"
}
