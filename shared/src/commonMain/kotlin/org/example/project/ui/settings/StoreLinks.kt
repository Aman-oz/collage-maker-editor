package org.example.project.ui.settings

/**
 * Opens this app's rating page: the Play Store listing on Android, the App Store "write a review"
 * page on iOS (or, until [SettingsLinks.AppStoreId] is set, the in-app StoreKit review prompt).
 */
internal expect fun openStoreRating()

/** Public store URL of this app, for Share App; `null` when it isn't known yet (iOS without an id). */
internal expect fun appStoreUrl(): String?
