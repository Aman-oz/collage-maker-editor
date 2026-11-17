package org.example.project

interface Platform {
    val name: String

    /** True for debug builds; ads read the `debug_ad_configuration` Remote Config block then. */
    val isDebugBuild: Boolean

    /**
     * This build's AdMob app ID: Google's test ID in debug builds, the app's own in release.
     * Android: the `admob_app_id` string from `androidApp/build.gradle.kts` (`resValue` per build type).
     * iOS: `GADApplicationIdentifier` in Info.plist, set per configuration in `Config.xcconfig`.
     */
    val adMobAppId: String
}

expect fun getPlatform(): Platform
