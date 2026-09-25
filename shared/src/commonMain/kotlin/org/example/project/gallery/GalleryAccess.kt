package org.example.project.gallery

import androidx.compose.runtime.Composable

/**
 * [Granted] and [Limited] both mean "proceed to load the gallery" — [Limited] is the
 * partial-access case (Android's `READ_MEDIA_VISUAL_USER_SELECTED`, iOS's `PHAuthorizationStatus`
 * `Limited`) where the query naturally only returns whatever subset the user allowed.
 */
enum class GalleryAccessStatus { NotDetermined, Granted, Limited, Denied }

interface GalleryAccessState {
    val status: GalleryAccessStatus
    fun requestAccess()

    /**
     * Opens this app's page in the system Settings app. Once the user has denied access, neither
     * platform shows the system prompt again, so Settings is the only way left to grant it.
     */
    fun openSettings()
}

/**
 * Reads the platform's current photo-library permission and exposes a way to request it.
 *
 * The status is re-read every time the screen resumes, so a permission granted in Settings (via
 * [GalleryAccessState.openSettings]) is picked up as soon as the user comes back to the app.
 */
@Composable
expect fun rememberGalleryAccessState(): GalleryAccessState
