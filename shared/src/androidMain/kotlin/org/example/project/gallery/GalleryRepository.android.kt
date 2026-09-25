package org.example.project.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform

private fun appContext(): Context = KoinPlatform.getKoin().get()

/** Target thumbnail edge, in pixels — grid cells are small, so there's no need to decode full-res. */
private const val ThumbnailSizePx = 300

actual suspend fun loadGalleryPhotos(): List<GalleryPhoto> = withContext(Dispatchers.IO) {
    val photos = mutableListOf<GalleryPhoto>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    appContext().contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder,
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            photos.add(GalleryPhoto(uri.toString()))
        }
    }
    photos
}

/** Virtual album id for photos starred in the system gallery (`IS_FAVORITE`, Android 11+). */
private const val FavouritesAlbumId = "favourites"

/** Buckets surfaced as Pinned, in display order. Matched case-insensitively on the bucket name. */
private val PinnedBucketNames = listOf("Camera", "Screenshots")

/**
 * One pass over MediaStore, grouped by bucket. The cursor is sorted newest first, so each
 * bucket's first row is its cover, and buckets naturally come out ordered by their latest photo.
 */
actual suspend fun loadGalleryAlbums(): List<GalleryAlbum> = withContext(Dispatchers.IO) {
    class Bucket(val id: String, val name: String, val coverId: String, var count: Int = 1)

    val buckets = LinkedHashMap<String, Bucket>()
    var favouriteCover: String? = null
    var favouriteCount = 0
    val supportsFavourites = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val projection = buildList {
        add(MediaStore.Images.Media._ID)
        add(MediaStore.Images.Media.BUCKET_ID)
        add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        if (supportsFavourites) add(MediaStore.MediaColumns.IS_FAVORITE)
    }.toTypedArray()

    appContext().contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val favouriteColumn = if (supportsFavourites) cursor.getColumnIndex(MediaStore.MediaColumns.IS_FAVORITE) else -1

        while (cursor.moveToNext()) {
            val photoId = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                cursor.getLong(idColumn),
            ).toString()

            if (favouriteColumn >= 0 && cursor.getInt(favouriteColumn) == 1) {
                if (favouriteCover == null) favouriteCover = photoId
                favouriteCount++
            }

            val bucketId = cursor.getString(bucketIdColumn) ?: continue
            val existing = buckets[bucketId]
            if (existing != null) {
                existing.count++
            } else {
                val name = cursor.getString(bucketNameColumn) ?: "Other"
                buckets[bucketId] = Bucket(bucketId, name, photoId)
            }
        }
    }

    val pinned = buildList {
        favouriteCover?.let { add(GalleryAlbum(FavouritesAlbumId, "Favourites", GalleryAlbumSection.Pinned, it, favouriteCount)) }
        PinnedBucketNames.forEach { pinnedName ->
            buckets.values.firstOrNull { it.name.equals(pinnedName, ignoreCase = true) }?.let {
                add(GalleryAlbum(it.id, it.name, GalleryAlbumSection.Pinned, it.coverId, it.count))
            }
        }
    }
    val pinnedIds = pinned.map { it.id }.toSet()
    val albums = buckets.values
        .filter { it.id !in pinnedIds }
        .map { GalleryAlbum(it.id, it.name, GalleryAlbumSection.Albums, it.coverId, it.count) }

    pinned + albums
}

actual suspend fun loadGalleryAlbumPhotos(albumId: String): List<GalleryPhoto> = withContext(Dispatchers.IO) {
    if (albumId == FavouritesAlbumId && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()
    val (selection, args) = if (albumId == FavouritesAlbumId) {
        "${MediaStore.MediaColumns.IS_FAVORITE} = 1" to null
    } else {
        "${MediaStore.Images.Media.BUCKET_ID} = ?" to arrayOf(albumId)
    }

    val photos = mutableListOf<GalleryPhoto>()
    appContext().contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID),
        selection,
        args,
        "${MediaStore.Images.Media.DATE_ADDED} DESC",
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn))
            photos.add(GalleryPhoto(uri.toString()))
        }
    }
    photos
}

actual suspend fun loadGalleryThumbnail(photoId: String): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(photoId)
        val resolver = appContext().contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= ThumbnailSizePx &&
            bounds.outHeight / (sampleSize * 2) >= ThumbnailSizePx
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }?.asImageBitmap()
    }.getOrNull()
}

/** Already a `content://` URI string — the same one `PlatformFile(path)` elsewhere in this app decodes. */
actual suspend fun resolveGalleryImagePath(photoId: String): String? = photoId
