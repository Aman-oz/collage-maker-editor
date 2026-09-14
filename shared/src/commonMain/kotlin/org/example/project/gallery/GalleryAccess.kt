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
}

/** Reads the platform's current photo-library permission and exposes a way to request it. */
@Composable
expect fun rememberGalleryAccessState(): GalleryAccessState
