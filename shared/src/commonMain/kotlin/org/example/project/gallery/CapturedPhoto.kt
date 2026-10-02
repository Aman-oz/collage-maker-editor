package org.example.project.gallery

import androidx.compose.ui.graphics.ImageBitmap
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.scaledBitmap

/** Short side of a captured photo's grid preview, in pixels; the same scale as library thumbnails. */
private const val CapturedThumbnailShortSide = 300

/**
 * A grid-cell preview of a photo the gallery's camera cell just took. Such a photo is a file in the
 * app's cache, not a library asset, so [loadGalleryThumbnail] (a MediaStore URI on Android, a
 * `PHAsset` on iOS) can't look it up; it is decoded from [path] the way the editors decode it, which
 * also applies the EXIF rotation a camera JPEG carries, and scaled down so the grid never holds a
 * full-resolution bitmap.
 */
suspend fun loadCapturedPhotoThumbnail(path: String): ImageBitmap? = withContext(Dispatchers.Default) {
    runCatching {
        val full = PlatformFile(path).toImageBitmap()
        val scale = CapturedThumbnailShortSide.toFloat() / minOf(full.width, full.height)
        if (scale >= 1f) {
            full
        } else {
            // Copy first: a platform-decoded bitmap does not reliably act as the source of a scaling draw.
            scaledBitmap(copyBitmap(full), (full.width * scale).roundToInt(), (full.height * scale).roundToInt())
        }
    }.getOrNull()
}
