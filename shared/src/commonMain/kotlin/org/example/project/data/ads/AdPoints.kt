package org.example.project.data.ads

/**
 * Call-site tokens passed as `point` to ads-kmp. Adding one to a placement's
 * `points` array in Remote Config (`ad_configuration_android` /
 * `ad_configuration_ios`) blocks that placement at that spot only, while it
 * stays enabled everywhere else. Named `<placement>_<screen>`.
 */
object AdPoints {
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

    /** `internal_app_banner` at the bottom of the Layout editor. */
    const val INTERNAL_APP_BANNER_LAYOUT_EDITOR = "internal_app_banner_layout_editor"

    /** `internal_app_native` at the bottom of the Save Image screen. */
    const val INTERNAL_APP_NATIVE_SAVE = "internal_app_native_save"

    /** `internal_app_native` at the bottom of the Share Image screen. */
    const val INTERNAL_APP_NATIVE_SHARE = "internal_app_native_share"
}
