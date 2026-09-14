package org.example.project.gallery

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
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
