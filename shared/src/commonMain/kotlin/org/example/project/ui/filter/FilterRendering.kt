package org.example.project.ui.filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.project.data.network.decodeImageBitmap
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.imageBitmapFromArgb
import org.example.project.ui.common.toReadable
import photocollagemaker.shared.generated.resources.Res

/** Longest side the Filter and Overlay tools preview at; Done renders the photo at its own size. */
internal const val FilterPreviewLongSide = 1280

/** Longest side of the chips' thumbnails, and of the overlay textures they are drawn with. */
internal const val FilterThumbnailLongSide = 160

/**
 * The LUT pictures and overlay textures under `composeResources/files/`, decoded to pixels once and
 * kept, least recently used first out past [MaxCachedPixels]: the overlay textures are up to 960 px
 * a side, and the Overlay tool's thumbnails go through every one.
 */
private object FilterAssets {
    private const val MaxCachedPixels = 12_000_000

    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, PixelImage>()

    /** [paths] loaded, textures (never LUTs, which must keep their exact size) cut to [textureMaxSide]. */
    suspend fun lookup(paths: Set<String>, textureMaxSide: Int?): FilterAssetLookup {
        val loaded = paths.associateWith { path ->
            val maxSide = textureMaxSide.takeUnless { path.startsWith("filters/") }
            val key = "$path@${maxSide ?: 0}"
            mutex.withLock { cache.remove(key)?.also { cache[key] = it } } ?: load(path, maxSide).also { image ->
                mutex.withLock {
                    cache[key] = image
                    var total = cache.values.sumOf { it.pixels.size }
                    val keys = cache.keys.iterator()
                    while (total > MaxCachedPixels && cache.size > 1 && keys.hasNext()) {
                        val oldest = keys.next()
                        if (oldest == key) continue
                        total -= cache.getValue(oldest).pixels.size
                        keys.remove()
                    }
                }
            }
        }
        return FilterAssetLookup { loaded[it] }
    }

    private suspend fun load(path: String, maxSide: Int?): PixelImage {
        val decoded = decodeImageBitmap(Res.readBytes("files/$path"))
        val image = if (maxSide != null && max(decoded.width, decoded.height) > maxSide) scaledToFit(decoded, maxSide) else decoded.toReadable()
        return image.toPixelImage()
    }
}

private fun ImageBitmap.toPixelImage(): PixelImage {
    val pixels = IntArray(width * height)
    readPixels(pixels)
    return PixelImage(pixels, width, height)
}

/**
 * [source] scaled down to fit [maxSide] (or itself when it already does), for the tools' previews
 * and thumbnails: a filter runs on every pixel, so the preview runs on fewer of them.
 */
internal fun scaledToFit(source: ImageBitmap, maxSide: Int): ImageBitmap {
    val longSide = max(source.width, source.height)
    if (longSide <= maxSide) return source
    val scale = maxSide.toFloat() / longSide
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    val output = ImageBitmap(width, height)
    Canvas(output).drawImageRect(
        // Copied first: a platform-decoded bitmap doesn't redraw reliably as the source of a scaling draw.
        image = copyBitmap(source),
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return output
}

/**
 * [source] through [steps], as a new bitmap of its size, worked out off the main thread. Overlay
 * textures are cut to [textureMaxSide] first, which a thumbnail can't tell from the full one.
 */
internal suspend fun renderFilterSteps(source: ImageBitmap, steps: List<FilterStep>, textureMaxSide: Int? = null): ImageBitmap =
    withContext(Dispatchers.Default) {
        val assets = FilterAssets.lookup(filterAssetPaths(steps), textureMaxSide)
        val image = source.toReadable().toPixelImage()
        runFilterSteps(image, steps, assets)
        imageBitmapFromArgb(image.pixels, image.width, image.height)
    }

/** [source] through [filter] at full strength; [source] itself for "None". */
internal suspend fun renderFilter(source: ImageBitmap, filter: PhotoFilter, textureMaxSide: Int? = null): ImageBitmap =
    if (filter.isNoneOption) source else renderFilterSteps(source, parseFilterRecipe(filter.recipe), textureMaxSide)

/**
 * [filtered] over [original] at [intensity] (0..1), as a new bitmap: the tools' strength slider.
 * Every CGE filter and blend ends by mixing its result with the photo, so this is the same as
 * running it at that strength.
 */
internal fun mixFiltered(original: ImageBitmap, filtered: ImageBitmap, intensity: Float): ImageBitmap {
    if (intensity >= 1f || filtered === original) return filtered
    if (intensity <= 0f) return original
    val output = ImageBitmap(original.width, original.height)
    val canvas = Canvas(output)
    canvas.drawImage(original, Offset.Zero, Paint())
    canvas.drawImage(filtered, Offset.Zero, Paint().apply { alpha = intensity })
    return output
}

/** [source] through [filter] at [intensity], at its own size: what the Filter tool's Done applies. */
internal suspend fun bakeFilter(source: ImageBitmap, filter: PhotoFilter, intensity: Float): ImageBitmap {
    if (filter.isNoneOption || intensity <= 0f) return source
    return mixFiltered(source, renderFilter(source, filter), intensity)
}
