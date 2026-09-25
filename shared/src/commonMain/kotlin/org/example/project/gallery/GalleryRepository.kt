package org.example.project.gallery

import androidx.compose.ui.graphics.ImageBitmap

/** One photo in the device's library. [id] is an opaque platform handle — a `content://` URI
 * string on Android, a `PHAsset.localIdentifier` on iOS — never shown to the user directly. */
data class GalleryPhoto(val id: String)

/**
 * Which Collections section an album is listed under. Neither platform exposes a public API for
 * face-grouped "People" albums, so that part of the design has no data source and is left out.
 */
enum class GalleryAlbumSection { Pinned, Albums }

/**
 * One album (an iOS `PHAssetCollection`, or an Android MediaStore bucket). [coverPhotoId] is its
 * most recent photo and goes through [loadGalleryThumbnail] like any grid cell.
 */
data class GalleryAlbum(
    val id: String,
    val name: String,
    val section: GalleryAlbumSection,
    val coverPhotoId: String,
    val photoCount: Int,
)

/** Lists the device's photos, most recent first. Requires [GalleryAccessStatus.Granted] or
 * [GalleryAccessStatus.Limited] — the caller is responsible for checking access first. */
expect suspend fun loadGalleryPhotos(): List<GalleryPhoto>

/**
 * Lists the device's non-empty photo albums: system albums (Favourites, Screenshots, …) under
 * [GalleryAlbumSection.Pinned], everything else under [GalleryAlbumSection.Albums], each most
 * recently updated first. Same access requirement as [loadGalleryPhotos].
 */
expect suspend fun loadGalleryAlbums(): List<GalleryAlbum>

/** The photos in [albumId] (a [GalleryAlbum.id]), most recent first. */
expect suspend fun loadGalleryAlbumPhotos(albumId: String): List<GalleryPhoto>

/** A small, decoded preview of [photoId] for a grid cell. */
expect suspend fun loadGalleryThumbnail(photoId: String): ImageBitmap?

/**
 * Resolves [photoId] to a path `PlatformFile(path)` can decode at full resolution — the same
 * shape [io.github.vinceglb.filekit.PlatformFile] already expects elsewhere in this app: a bare
 * `content://` URI on Android, an absolute filesystem path on iOS.
 */
expect suspend fun resolveGalleryImagePath(photoId: String): String?
