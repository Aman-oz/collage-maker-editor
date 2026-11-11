package org.example.project.ui.settings

/**
 * Opens this app's rating page: the Play Store listing on Android, the App Store "write a review"
 * page on iOS (or, while the app has no App Store id, the in-app StoreKit review prompt).
 *
 * Suspends because iOS may first have to look the App Store id up (see `StoreLinks.ios.kt`).
 */
internal expect suspend fun openStoreRating()

/**
 * Public store URL to share for Share App. On iOS, before the app is on the App Store, this is the
 * developer's App Store page ([SettingsLinks.MoreAppsUrl]) so sharing still has somewhere to send people.
 */
internal expect suspend fun appStoreUrl(): String
