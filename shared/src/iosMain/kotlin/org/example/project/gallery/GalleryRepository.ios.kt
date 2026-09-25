package org.example.project.gallery

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSortDescriptor
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSPredicate
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionSubtype
import platform.Photos.PHAssetCollectionSubtypeAlbumRegular
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumDepthEffect
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumFavorites
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumLivePhotos
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumPanoramas
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumRecentlyAdded
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumScreenshots
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumSelfPortraits
import platform.Photos.PHAssetCollectionTypeAlbum
import platform.Photos.PHAssetCollectionTypeSmartAlbum
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsDeliveryModeOpportunistic
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume

/** Assets fetched by [loadGalleryPhotos], keyed by `localIdentifier` — looked back up by the
 * later thumbnail/resolve calls, which only ever receive a [GalleryPhoto.id] string. */
private val assetCache = mutableMapOf<String, PHAsset>()

/** Collections found by [loadGalleryAlbums], keyed by `localIdentifier`, for [loadGalleryAlbumPhotos]. */
private val collectionCache = mutableMapOf<String, PHAssetCollection>()

/** Smart albums surfaced as Pinned, in display order. Recents is skipped: it *is* the Photos tab. */
private val PinnedSmartAlbums: List<PHAssetCollectionSubtype> = listOf(
    PHAssetCollectionSubtypeSmartAlbumFavorites,
    PHAssetCollectionSubtypeSmartAlbumRecentlyAdded,
    PHAssetCollectionSubtypeSmartAlbumScreenshots,
    PHAssetCollectionSubtypeSmartAlbumSelfPortraits,
    PHAssetCollectionSubtypeSmartAlbumDepthEffect,
    PHAssetCollectionSubtypeSmartAlbumLivePhotos,
    PHAssetCollectionSubtypeSmartAlbumPanoramas,
)

/** Images only, newest first. `1` is `PHAssetMediaTypeImage`; the varargs `%d` form of
 * `predicateWithFormat` isn't callable from Kotlin/Native, so the value is inlined. */
private fun imageFetchOptions() = PHFetchOptions().apply {
    predicate = NSPredicate.predicateWithFormat("mediaType == 1")
    sortDescriptors = listOf(NSSortDescriptor(key = "creationDate", ascending = false))
}

actual suspend fun loadGalleryPhotos(): List<GalleryPhoto> {
    val options = PHFetchOptions().apply {
        sortDescriptors = listOf(NSSortDescriptor(key = "creationDate", ascending = false))
    }
    val result = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)

    val photos = mutableListOf<GalleryPhoto>()
    for (index in 0 until result.count.toInt()) {
        val asset = result.objectAtIndex(index.toULong()) as PHAsset
        assetCache[asset.localIdentifier] = asset
        photos.add(GalleryPhoto(asset.localIdentifier))
    }
    return photos
}

actual suspend fun loadGalleryAlbums(): List<GalleryAlbum> {
    val pinned = PinnedSmartAlbums.flatMap { subtype ->
        PHAssetCollection.fetchAssetCollectionsWithType(PHAssetCollectionTypeSmartAlbum, subtype, null).collections()
    }.mapNotNull { it.toAlbum(GalleryAlbumSection.Pinned) }

    // User albums sorted by their newest photo, so recently used albums come first.
    val albums = PHAssetCollection
        .fetchAssetCollectionsWithType(PHAssetCollectionTypeAlbum, PHAssetCollectionSubtypeAlbumRegular, null)
        .collections()
        .mapNotNull { it.toAlbum(GalleryAlbumSection.Albums) }
        .sortedByDescending { assetCache[it.coverPhotoId]?.creationDate?.timeIntervalSince1970 ?: 0.0 }

    return pinned + albums
}

actual suspend fun loadGalleryAlbumPhotos(albumId: String): List<GalleryPhoto> {
    val collection = collectionCache[albumId] ?: return emptyList()
    val result = PHAsset.fetchAssetsInAssetCollection(collection, imageFetchOptions())
    return (0 until result.count.toInt()).map { index ->
        val asset = result.objectAtIndex(index.toULong()) as PHAsset
        assetCache[asset.localIdentifier] = asset
        GalleryPhoto(asset.localIdentifier)
    }
}

private fun platform.Photos.PHFetchResult.collections(): List<PHAssetCollection> =
    (0 until count.toInt()).map { objectAtIndex(it.toULong()) as PHAssetCollection }

/** Null for albums with no images (e.g. a video-only album), which Collections doesn't show. */
private fun PHAssetCollection.toAlbum(section: GalleryAlbumSection): GalleryAlbum? {
    val assets = PHAsset.fetchAssetsInAssetCollection(this, imageFetchOptions())
    val cover = assets.firstObject as? PHAsset ?: return null
    assetCache[cover.localIdentifier] = cover
    collectionCache[localIdentifier] = this
    return GalleryAlbum(
        id = localIdentifier,
        name = localizedTitle ?: "Album",
        section = section,
        coverPhotoId = cover.localIdentifier,
        photoCount = assets.count.toInt(),
    )
}

actual suspend fun loadGalleryThumbnail(photoId: String): ImageBitmap? {
    val asset = assetCache[photoId] ?: return null
    val image = requestThumbnailImage(asset) ?: return null
    return uiImageToImageBitmap(image)
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun requestThumbnailImage(asset: PHAsset): UIImage? = suspendCancellableCoroutine { continuation ->
    val options = PHImageRequestOptions().apply {
        deliveryMode = PHImageRequestOptionsDeliveryModeOpportunistic
        resizeMode = PHImageRequestOptionsResizeModeExact
        networkAccessAllowed = true
    }
    val requestId = PHImageManager.defaultManager().requestImageForAsset(
        asset,
        targetSize = CGSizeMake(300.0, 300.0),
        contentMode = PHImageContentModeAspectFill,
        options = options,
    ) { image, _ ->
        if (continuation.isActive) continuation.resume(image)
    }
    continuation.invokeOnCancellation {
        PHImageManager.defaultManager().cancelImageRequest(requestId)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun uiImageToImageBitmap(image: UIImage): ImageBitmap? {
    val data = UIImageJPEGRepresentation(image, 0.85) ?: return null
    val bytes = data.toByteArray()
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(length.toInt())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    }
    return bytes
}

actual suspend fun resolveGalleryImagePath(photoId: String): String? {
    val asset = assetCache[photoId] ?: return null
    val image = requestFullSizeImage(asset) ?: return null
    val data = uiImageToJpegData(image) ?: return null
    val fileName = "gallery_${NSUUID().UUIDString()}.jpg"
    val separator = if (NSTemporaryDirectory().endsWith("/")) "" else "/"
    val path = NSTemporaryDirectory() + separator + fileName
    NSFileManager.defaultManager.createFileAtPath(path, contents = data, attributes = null)
    return path
}

/**
 * Requests a decoded [UIImage], not the asset's raw original data — some library photos are
 * stored as HEIC, which Skia's [Image.makeFromEncoded] cannot decode. Re-encoding through
 * [uiImageToJpegData] guarantees a format the shared bitmap-loading code can always read.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun requestFullSizeImage(asset: PHAsset): UIImage? = suspendCancellableCoroutine { continuation ->
    val options = PHImageRequestOptions().apply {
        deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
        resizeMode = PHImageRequestOptionsResizeModeExact
        networkAccessAllowed = true
    }
    val requestId = PHImageManager.defaultManager().requestImageForAsset(
        asset,
        targetSize = CGSizeMake(2048.0, 2048.0),
        contentMode = PHImageContentModeAspectFit,
        options = options,
    ) { image, _ ->
        if (continuation.isActive) continuation.resume(image)
    }
    continuation.invokeOnCancellation {
        PHImageManager.defaultManager().cancelImageRequest(requestId)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun uiImageToJpegData(image: UIImage): NSData? = UIImageJPEGRepresentation(image, 0.92)
