package org.example.project.ui.settings

import org.example.project.i18n.tr

/**
 * External links opened from the settings screen. Blank until the real URLs are provided; a blank
 * link shows a "coming soon" message instead of opening anything.
 */
internal object SettingsLinks {
    /**
     * Numeric Apple id of the app (the `id…` in its App Store URL). Optional: when blank, iOS looks
     * it up by bundle id once the app is live (`StoreLinks.ios.kt`). Android needs nothing here:
     * its store links are built from the package name.
     */
    const val AppStoreId = "6831080271"
    const val PrivacyPolicyUrl = "https://gamemaxstudio.com/privacy-policy/"

    /**
     * Terms of Use. iOS: Apple's standard EULA, which App Review accepts as the terms for
     * subscriptions (guideline 3.1.2). Android: the privacy policy, which covers both there.
     */
    val TermsOfUseUrl: String get() = platformTermsOfUseUrl(PrivacyPolicyUrl)

    /** There is no separate about page either: About Us opens the privacy policy too. */
    const val AboutUsUrl = PrivacyPolicyUrl

    /** The developer's App Store page, opened by More Apps on both platforms. */
    const val MoreAppsUrl = "https://apps.apple.com/us/developer/ozi-gamemax-entertainment/id1781183024"
}

/** Ratings at or above this go to the store rating page; lower ones just get a thank-you. */
internal const val StoreRatingThreshold = 4

/** Share App text; the store link is appended on its own line so it stays tappable everywhere. */
internal fun shareAppMessage(storeUrl: String): String =
    tr(
        "I've been making gorgeous photo collages with Collage Maker: Pic Editor 📸✨ Tons of layouts, templates, filters and stickers — and it's free. Try it out:\n{0}",
        storeUrl,
    )

/** The Terms of Use link for this platform; [fallback] is the privacy policy. */
internal expect fun platformTermsOfUseUrl(fallback: String): String
