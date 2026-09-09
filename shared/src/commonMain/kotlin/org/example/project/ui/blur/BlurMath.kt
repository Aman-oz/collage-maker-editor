package org.example.project.ui.blur

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled

internal const val BlurLevelMin = 0
internal const val BlurLevelMax = 15
internal const val BlurLevelDefault = 3

internal val BrushSizeRange = 0f..10f
internal const val BrushSizeDefault = 3f

/** Max erase-brush radius, as a fraction of the displayed image width, at [BrushSizeRange]'s top end. */
private const val MaxBrushRadiusFraction = 0.2f

/** Converts a [BrushSizeRange] value into an erase-brush radius, as a fraction of the image width. */
internal fun brushRadiusFraction(brushSize: Float): Float =
    (brushSize / BrushSizeRange.endInclusive) * MaxBrushRadiusFraction

/**
 * A cheap, dependency-free blur: downscale then upscale with high-quality filtering. The
 * resampling itself produces the soft/blurred look, using only the same [Canvas.drawImageRect]
 * API [org.example.project.ui.crop.cropImageBitmap] already relies on — no platform-specific
 * blur API needed, so the exact same function can back both the live preview and the final bake.
 */
internal fun computeBlurredBitmap(source: ImageBitmap, blurLevel: Int): ImageBitmap {
    if (blurLevel <= 0) return copyBitmap(source)

    val scale = (1f / (1f + blurLevel * 0.5f)).coerceIn(0.02f, 1f)
    val smallWidth = (source.width * scale).roundToInt().coerceAtLeast(2)
    val smallHeight = (source.height * scale).roundToInt().coerceAtLeast(2)

    val small = ImageBitmap(smallWidth, smallHeight)
    Canvas(small).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(smallWidth, smallHeight),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )

    val blurred = ImageBitmap(source.width, source.height)
    Canvas(blurred).drawImageRect(
        image = small,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(smallWidth, smallHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return blurred
}

/**
 * One completed erase stroke: the dragged [points] (each a `0f..1f` fraction of the image bounds)
 * plus the brush [radiusFraction] (also a fraction of the image width) that was in effect while it
 * was drawn — captured per-stroke so a later brush-size change doesn't retroactively resize
 * strokes that were already committed.
 */
internal data class BlurStroke(val points: List<Offset>, val radiusFraction: Float)

/** Builds the reveal path for one [BlurStroke], using its own recorded brush size. */
internal fun buildStrokePath(stroke: BlurStroke, imageOffset: Offset, imageSize: Size): Path =
    buildStrokePath(stroke.points, imageOffset, imageSize, stroke.radiusFraction * imageSize.width)

/**
 * Draws [blurredImage] as the base, then reveals [sharpImage] underneath within each path in
 * [revealPaths] (one per erase stroke) — the exact same call works for the live preview's
 * [androidx.compose.foundation.Canvas] and for baking via `CanvasDrawScope`, so what the user sees
 * is guaranteed to match what gets saved.
 */
internal fun DrawScope.drawBlurWithReveal(
    sharpImage: ImageBitmap,
    blurredImage: ImageBitmap,
    imageOffset: Offset,
    imageSize: Size,
    revealPaths: List<Path>,
) {
    val dstOffset = IntOffset(imageOffset.x.roundToInt(), imageOffset.y.roundToInt())
    val dstSize = IntSize(imageSize.width.roundToInt().coerceAtLeast(1), imageSize.height.roundToInt().coerceAtLeast(1))

    drawImageScaled(blurredImage, dstOffset, dstSize)
    for (path in revealPaths) {
        clipPath(path) {
            drawImageScaled(sharpImage, dstOffset, dstSize)
        }
    }
}
