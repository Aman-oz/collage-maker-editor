package org.example.project.ui.settings

import org.example.project.i18n.tr

/**
 * External links opened from the settings screen. Blank until the real URLs are provided; a blank
 * link shows a "coming soon" message instead of opening anything.
 */
internal object SettingsLinks {
    /**
     * Numeric Apple id of the app (the `id…` in its App Store URL), known once it is published.
     * Android needs nothing here: its store links are built from the package name.
     */
    const val AppStoreId = ""
    const val AboutUsUrl = ""
    const val PrivacyPolicyUrl = ""
    const val TermsOfUseUrl = ""
}

/** Ratings at or above this go to the store rating page; lower ones just get a thank-you. */
internal const val StoreRatingThreshold = 4

/** Share App text; the store link is appended on its own line so it stays tappable everywhere. */
internal fun shareAppMessage(storeUrl: String): String =
    tr(
        "I've been making gorgeous photo collages with Pic Collage Maker 📸✨ Tons of layouts, templates, filters and stickers — and it's free. Try it out:\n{0}",
        storeUrl,
    )
