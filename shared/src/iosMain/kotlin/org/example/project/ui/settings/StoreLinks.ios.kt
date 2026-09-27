package org.example.project.ui.settings

import platform.Foundation.NSURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication

internal actual fun openStoreRating() {
    val id = SettingsLinks.AppStoreId
    if (id.isBlank()) {
        // No App Store id before the app is published, so fall back to Apple's in-app prompt.
        @Suppress("DEPRECATION")
        val scene = UIApplication.sharedApplication.keyWindow?.windowScene ?: return
        SKStoreReviewController.requestReviewInScene(scene)
        return
    }
    val url = NSURL.URLWithString("itms-apps://apps.apple.com/app/id$id?action=write-review") ?: return
    UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
}

internal actual fun appStoreUrl(): String? =
    SettingsLinks.AppStoreId.takeIf { it.isNotBlank() }?.let { "https://apps.apple.com/app/id$it" }
