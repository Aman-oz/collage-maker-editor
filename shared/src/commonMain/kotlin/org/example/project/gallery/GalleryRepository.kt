package org.example.project.gallery

import androidx.compose.ui.graphics.ImageBitmap

/** One photo in the device's library. [id] is an opaque platform handle — a `content://` URI
 * string on Android, a `PHAsset.localIdentifier` on iOS — never shown to the user directly. */
data class GalleryPhoto(val id: String)

/** Lists the device's photos, most recent first. Requires [GalleryAccessStatus.Granted] or
 * [GalleryAccessStatus.Limited] — the caller is responsible for checking access first. */
expect suspend fun loadGalleryPhotos(): List<GalleryPhoto>

/** A small, decoded preview of [photoId] for a grid cell. */
expect suspend fun loadGalleryThumbnail(photoId: String): ImageBitmap?

/**
 * Resolves [photoId] to a path `PlatformFile(path)` can decode at full resolution — the same
 * shape [io.github.vinceglb.filekit.PlatformFile] already expects elsewhere in this app: a bare
 * `content://` URI on Android, an absolute filesystem path on iOS.
 */
expect suspend fun resolveGalleryImagePath(photoId: String): String?
