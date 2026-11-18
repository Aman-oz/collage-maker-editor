package org.example.project.data.ads

/**
 * Asks for App Tracking Transparency permission on iOS, so ads may use the advertising ID; returns
 * once the user has answered, or at once when they already have (or on Android, which has no such
 * prompt). Called after the consent form and before the ads SDK starts, while the splash is on
 * screen: iOS only shows the prompt to an active app.
 */
internal expect suspend fun requestAdTrackingAuthorization()
