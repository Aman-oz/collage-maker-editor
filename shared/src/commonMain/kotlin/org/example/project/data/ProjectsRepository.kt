package org.example.project.data

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.example.project.ui.common.copyBitmap
import kotlin.math.max
import kotlin.math.roundToInt

/** Shortest side of a grid thumbnail, in px; enough for a third of a phone screen at 3x. */
private const val ThumbnailShortSide = 400

/**
 * The user's saved creations, shown on Home's Projects tab.
 *
 * Every export is also copied into the app's private files directory. The gallery copy has no
 * stable path on both platforms, and the cache copy the share screen uses can be evicted, so this
 * is the only copy the app can reliably list and reopen.
 */
class ProjectsRepository {

    private val directory: PlatformFile get() = FileKit.filesDir / "projects"

    private val _projects = MutableStateFlow<List<String>?>(null)

    /** Paths of saved projects, newest first; `null` until the first [refresh] finishes. */
    val projects: StateFlow<List<String>?> = _projects.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())

    /**
     * Downscaled bitmaps keyed by project path. Kept here, not in a screen, so the preview screen
     * can show the grid's thumbnail on its very first frame: the shared-element transition needs
     * the image in place immediately, before the full-resolution decode finishes.
     */
    val thumbnails: StateFlow<Map<String, ImageBitmap>> = _thumbnails.asStateFlow()

    suspend fun refresh() {
        _projects.value = withContext(Dispatchers.Default) {
            val dir = directory
            if (!dir.exists()) return@withContext emptyList()
            dir.list()
                .filter { it.isRegularFile() }
                // Files are named `collage_<epoch millis>.jpg`, so name order is creation order.
                .sortedByDescending { it.name }
                .map { it.path }
        }
    }

    /** Stores [bytes] as a new project named [filename] and returns its path. */
    suspend fun add(bytes: ByteArray, filename: String): String {
        val file = withContext(Dispatchers.Default) {
            directory.createDirectories()
            (directory / filename).also { it.write(bytes) }
        }
        refresh()
        return file.path
    }

    /** Removes the project at [path]. The gallery copy made at save time is left untouched. */
    suspend fun delete(path: String) {
        PlatformFile(path).delete(mustExist = false)
        _thumbnails.update { it - path }
        refresh()
    }

    /** Decodes and caches the thumbnail for [path], if it is not cached already. */
    suspend fun loadThumbnail(path: String) {
        if (path in _thumbnails.value) return
        val thumbnail = runCatching {
            withContext(Dispatchers.Default) { downscale(PlatformFile(path).toImageBitmap()) }
        }.getOrNull() ?: return
        _thumbnails.update { it + (path to thumbnail) }
    }
}

private fun downscale(source: ImageBitmap): ImageBitmap {
    val shortSide = minOf(source.width, source.height)
    if (shortSide <= ThumbnailShortSide) return copyBitmap(source)
    val scale = ThumbnailShortSide.toFloat() / shortSide
    val size = IntSize(
        max(1, (source.width * scale).roundToInt()),
        max(1, (source.height * scale).roundToInt()),
    )
    // Copy first: a platform-decoded bitmap does not reliably act as the source of a scaling draw.
    val copy = copyBitmap(source)
    val output = ImageBitmap(size.width, size.height)
    Canvas(output).drawImageRect(
        image = copy,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(copy.width, copy.height),
        dstOffset = IntOffset.Zero,
        dstSize = size,
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return output
}
