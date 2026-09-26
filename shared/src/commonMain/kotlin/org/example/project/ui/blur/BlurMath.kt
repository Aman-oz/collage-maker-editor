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
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.imageBitmapFromArgb

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

/** Gaussian sigma per blur level, as a fraction of the image's longer side (level 15 ≈ 3.75%). */
private const val SigmaFractionPerLevel = 0.0025f

/** Longest side the blur itself runs at; the result is upscaled, which a blurred image survives. */
private const val MaxWorkingSide = 1024

/**
 * Sigma (in working pixels) worth keeping resolution for. Beyond this the image is downscaled
 * further, since a wider blur carries no detail that the extra pixels would preserve.
 */
private const val TargetWorkingSigma = 6f

/**
 * A real Gaussian blur of [source] whose strength is relative to the image size, so the preview
 * and the full-resolution bake look the same. The blur runs on a downscaled copy (see
 * [MaxWorkingSide] / [TargetWorkingSigma]) and is upscaled with bilinear filtering; that keeps it
 * fast without the blocky look a plain downscale/upscale "blur" has.
 */
internal fun computeBlurredBitmap(source: ImageBitmap, blurLevel: Int): ImageBitmap {
    if (blurLevel <= 0) return copyBitmap(source)

    val longSide = max(source.width, source.height).toFloat()
    val sigma = blurLevel * SigmaFractionPerLevel * longSide
    val scale = minOf(1f, MaxWorkingSide / longSide, TargetWorkingSigma / sigma)
    val smallWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
    val smallHeight = (source.height * scale).roundToInt().coerceAtLeast(1)

    val small = ImageBitmap(smallWidth, smallHeight)
    Canvas(small).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(smallWidth, smallHeight),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    val pixels = IntArray(smallWidth * smallHeight)
    small.readPixels(pixels)
    val blurredSmall = imageBitmapFromArgb(gaussianBlurArgb(pixels, smallWidth, smallHeight, sigma * scale), smallWidth, smallHeight)

    val blurred = ImageBitmap(source.width, source.height)
    Canvas(blurred).drawImageRect(
        image = blurredSmall,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(smallWidth, smallHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return blurred
}

/**
 * Gaussian blur of packed ARGB [pixels], approximated by three successive box blurs (each pass
 * horizontal then vertical). Box blurs are O(1) per pixel regardless of radius thanks to a running
 * sum, and three of them are visually indistinguishable from a true Gaussian. Edges clamp, so the
 * border doesn't darken or bleed in transparent black.
 */
internal fun gaussianBlurArgb(pixels: IntArray, width: Int, height: Int, sigma: Float): IntArray {
    val out = pixels.copyOf()
    if (sigma < 0.5f || width <= 0 || height <= 0) return out
    val tmp = IntArray(out.size)
    for (radius in boxBlurRadiiForGaussian(sigma, passes = 3)) {
        if (radius <= 0) continue
        boxBlurPass(src = out, dst = tmp, count = height, length = width, lineStride = width, step = 1, radius = radius)
        boxBlurPass(src = tmp, dst = out, count = width, length = height, lineStride = 1, step = width, radius = radius)
    }
    return out
}

/**
 * Box radii whose successive application approximates a Gaussian of [sigma] (the standard
 * "ideal averaging filter width" derivation: mix two odd widths so the variances add up to σ²).
 */
internal fun boxBlurRadiiForGaussian(sigma: Float, passes: Int): IntArray {
    val ideal = sqrt(12f * sigma * sigma / passes + 1f)
    var lower = floor(ideal).toInt()
    if (lower % 2 == 0) lower--
    val upper = lower + 2
    val lowerCount = ((12f * sigma * sigma - passes * lower * lower - 4f * passes * lower - 3f * passes) / (-4f * lower - 4f)).roundToInt()
    return IntArray(passes) { i -> ((if (i < lowerCount) lower else upper) - 1) / 2 }
}

/**
 * One box-blur pass over [count] lines of [length] pixels. Line `n` starts at `n * lineStride` and
 * successive pixels are [step] apart, so the same code does rows (stride = width, step = 1) and
 * columns (stride = 1, step = width).
 */
private fun boxBlurPass(src: IntArray, dst: IntArray, count: Int, length: Int, lineStride: Int, step: Int, radius: Int) {
    val window = 2 * radius + 1
    val half = window / 2
    val last = length - 1
    for (line in 0 until count) {
        val base = line * lineStride
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val p = src[base + i.coerceIn(0, last) * step]
            a += p ushr 24
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        for (i in 0 until length) {
            dst[base + i * step] =
                (((a + half) / window) shl 24) or (((r + half) / window) shl 16) or (((g + half) / window) shl 8) or ((b + half) / window)
            val add = src[base + min(i + radius + 1, last) * step]
            val remove = src[base + max(i - radius, 0) * step]
            a += (add ushr 24) - (remove ushr 24)
            r += ((add shr 16) and 0xFF) - ((remove shr 16) and 0xFF)
            g += ((add shr 8) and 0xFF) - ((remove shr 8) and 0xFF)
            b += (add and 0xFF) - (remove and 0xFF)
        }
    }
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
