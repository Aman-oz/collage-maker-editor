package org.example.project.ui.layouteditor

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.ui.common.copyBitmap

/**
 * A picture from one of the server's layout libraries: a decorative frame (the "borders" category: as
 * large as the canvas, transparent in the middle, drawn stretched over the whole layout, whatever the
 * canvas's shape, once [trimFrameArt] has cut its transparent margin) or a background (a pattern
 * filling the canvas, cropped to its shape). [aspectRatio] is its width / height.
 */
data class LayoutArt(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val imageUrl: String,
    val aspectRatio: Float,
    val isPremium: Boolean,
)

/** A sub-category of a library (a tile under its grid, showing its first picture). */
internal data class LayoutArtSubcategory(val id: String, val name: String, val items: List<LayoutArt>)

/** A library category (a pill at the foot of its tab) and its sub-categories that have pictures. */
internal data class LayoutArtCategory(val id: String, val name: String, val subcategories: List<LayoutArtSubcategory>)

/**
 * The layout editor's picture libraries on the central-data server (frames and backgrounds), each
 * three levels deep like the stickers: `categories?superCategoryId=<library>` → each one's
 * `subcategories?categoryId=` → each of those' `assets?categoryId=&subCategoryId=` (the server wants
 * both). There are few, so a tab reads its whole library at once; a sub-category with no picture,
 * and a category with no sub-category left, are dropped. Each library is kept for the process; a
 * failure is not.
 */
class LayoutArtLibrary(private val api: CentralDataApi) {

    private val mutex = Mutex()
    private val libraries = mutableMapOf<String, List<LayoutArtCategory>>()

    /** The library under [superCategoryId], e.g. `CentralDataConfig.BORDERS_SUPER_CATEGORY_ID`. */
    internal suspend fun getCategories(superCategoryId: String): List<LayoutArtCategory> =
        mutex.withLock { libraries[superCategoryId] }
            ?: load(superCategoryId).also { mutex.withLock { libraries[superCategoryId] = it } }

    private suspend fun load(superCategoryId: String): List<LayoutArtCategory> = coroutineScope {
        api.categories(superCategoryId)
            .filter { it.isEnabled && it.id.isNotBlank() }
            .sortedBy { it.sequence }
            .map { category ->
                async {
                    val subcategories = api.subcategories(category.id)
                        .filter { it.isEnabled && it.id.isNotBlank() }
                        .sortedBy { it.sequence }
                        .map { sub ->
                            async {
                                val items = api.assets(category.id, sub.id).toLayoutArts(api::absoluteUrl)
                                LayoutArtSubcategory(sub.id, sub.name, items)
                            }
                        }
                        .awaitAll()
                        .filter { it.items.isNotEmpty() }
                    LayoutArtCategory(category.id, category.name, subcategories)
                }
            }
            .awaitAll()
            .filter { it.subcategories.isNotEmpty() }
    }
}

/** Enabled pictures, in server order. [absoluteUrl] resolves the server's relative paths. */
internal fun List<CentralAssetDto>.toLayoutArts(absoluteUrl: (String) -> String): List<LayoutArt> =
    filter { it.isEnabled }
        .sortedBy { it.sequence }
        .mapNotNull { asset ->
            val file = asset.moreFields.image.firstOrNull { it.url.isNotBlank() } ?: return@mapNotNull null
            LayoutArt(
                id = asset.id,
                name = asset.name,
                thumbnailUrl = absoluteUrl(asset.thumbnailUrl?.takeIf { it.isNotBlank() } ?: file.url),
                imageUrl = absoluteUrl(file.url),
                aspectRatio = if (file.width > 0f && file.height > 0f) file.width / file.height else 1f,
                isPremium = asset.isPremium,
            )
        }

/**
 * The box (left, top, right, bottom; right and bottom exclusive) of the [width] x [height] ARGB
 * [pixels] whose alpha is above [alphaThreshold], or null when every pixel is clearer than that.
 * The server's frames are drawn with a margin of transparent canvas around them (1-6% per side);
 * trimmed to this box, the frame meets the layout's edges.
 */
internal fun opaqueBounds(pixels: IntArray, width: Int, height: Int, alphaThreshold: Int = FrameAlphaThreshold): IntArray? {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if ((pixels[row + x] ushr 24) > alphaThreshold) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                bottom = y
            }
        }
    }
    return if (right < 0) null else intArrayOf(left, top, right + 1, bottom + 1)
}

/** Alpha (0-255) under which a frame's edge pixels count as margin: the faint end of its glow. */
internal const val FrameAlphaThreshold = 8

/**
 * [frame] (a bitmap this app made, see [copyBitmap]) cut to its [opaqueBounds], so stretching it
 * over the canvas puts the frame on the canvas's edges. Returned as is when there is nothing to trim.
 */
internal fun trimFrameArt(frame: ImageBitmap): ImageBitmap {
    val pixels = IntArray(frame.width * frame.height)
    frame.readPixels(pixels)
    val (left, top, right, bottom) = opaqueBounds(pixels, frame.width, frame.height) ?: return frame
    if (left == 0 && top == 0 && right == frame.width && bottom == frame.height) return frame
    val trimmed = ImageBitmap(right - left, bottom - top)
    Canvas(trimmed).drawImageRect(
        image = frame,
        srcOffset = IntOffset(left, top),
        srcSize = IntSize(right - left, bottom - top),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(right - left, bottom - top),
        paint = Paint(),
    )
    return trimmed
}
