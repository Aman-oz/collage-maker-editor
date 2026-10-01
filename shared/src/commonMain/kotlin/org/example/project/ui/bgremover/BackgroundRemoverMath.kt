package org.example.project.ui.bgremover

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Neighbouring pixels closer than this (max channel difference) belong to the same region. */
internal const val AutoStepTolerance = 18

/** Pixels further than this from the border colour a region grew from stop the fill. */
internal const val AutoSeedTolerance = 60

/** Pixels at or below this alpha are already transparent, so they count as background. */
private const val TransparentAlpha = 8

/** The auto mask is computed on a copy whose longer side is at most this many pixels. */
internal const val AutoMaskMaxSide = 512

/**
 * Size to downscale a `width × height` image to before running [autoBackgroundMask], so the flood
 * fill stays fast on full-resolution photos. Never upscales.
 */
internal fun autoMaskWorkingSize(width: Int, height: Int, maxSide: Int = AutoMaskMaxSide): Pair<Int, Int> {
    val longer = max(width, height)
    if (longer <= maxSide) return width to height
    val scale = maxSide.toFloat() / longer
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

/** Largest per-channel RGB difference between two packed `0xAARRGGBB` colours. */
internal fun colorDistance(a: Int, b: Int): Int {
    val dr = abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF))
    val dg = abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF))
    val db = abs((a and 0xFF) - (b and 0xFF))
    return max(dr, max(dg, db))
}

/**
 * Heuristic "Auto" background detection — no ML: flood-fills inward from every border pixel,
 * treating the photo's edges as background. A neighbour joins a region when it is close both to
 * the pixel it is reached from ([stepTolerance], so smooth gradients such as a sky are followed)
 * and to the border colour that region started from ([seedTolerance], so the fill cannot creep
 * across a soft subject edge one small step at a time). Already-transparent pixels always join.
 *
 * Works well on plain or softly graded backdrops; busy scenes need the eraser.
 *
 * @param pixels packed `0xAARRGGBB`, row-major, `width * height` long.
 * @return `true` for pixels to keep (foreground), `false` for background.
 */
internal fun autoBackgroundMask(
    pixels: IntArray,
    width: Int,
    height: Int,
    stepTolerance: Int = AutoStepTolerance,
    seedTolerance: Int = AutoSeedTolerance,
): BooleanArray {
    val count = width * height
    require(pixels.size >= count) { "Expected $count pixels, got ${pixels.size}" }
    if (count == 0) return BooleanArray(0)
    val border = buildList {
        for (x in 0 until width) {
            add(x)
            add((height - 1) * width + x)
        }
        for (y in 0 until height) {
            add(y * width)
            add(y * width + width - 1)
        }
    }
    val background = floodFill(pixels, width, height, border, stepTolerance, seedTolerance)
    return BooleanArray(count) { !background[it] }
}

/**
 * The magic eraser's "wand": the connected region around pixel ([seedX], [seedY]) whose colours are
 * within [tolerance] of the tapped pixel. Only the seed tolerance applies, so the region is exactly
 * "this colour, touching here" and cannot drift along a gradient into the subject.
 *
 * @return `true` for pixels inside the region.
 */
internal fun colorRegionMask(pixels: IntArray, width: Int, height: Int, seedX: Int, seedY: Int, tolerance: Int): BooleanArray {
    val count = width * height
    require(pixels.size >= count) { "Expected $count pixels, got ${pixels.size}" }
    if (count == 0) return BooleanArray(0)
    val seed = seedY.coerceIn(0, height - 1) * width + seedX.coerceIn(0, width - 1)
    return floodFill(pixels, width, height, listOf(seed), stepTolerance = 255, seedTolerance = tolerance)
}

/**
 * Breadth-first fill from [seeds]; each filled pixel remembers the seed colour its region started
 * from so [seedTolerance] is measured against that, not against the neighbour it was reached from.
 */
private fun floodFill(
    pixels: IntArray,
    width: Int,
    height: Int,
    seeds: List<Int>,
    stepTolerance: Int,
    seedTolerance: Int,
): BooleanArray {
    val count = width * height
    val filled = BooleanArray(count)
    val seed = IntArray(count)
    val queue = IntArray(count)
    var head = 0
    var tail = 0

    fun enqueue(index: Int, seedColor: Int) {
        filled[index] = true
        seed[index] = seedColor
        queue[tail++] = index
    }

    for (i in seeds) if (!filled[i]) enqueue(i, pixels[i])

    while (head < tail) {
        val i = queue[head++]
        val x = i % width
        val y = i / width
        val color = pixels[i]
        val seedColor = seed[i]
        fun visit(n: Int) {
            if (filled[n]) return
            val candidate = pixels[n]
            val transparent = (candidate ushr 24) <= TransparentAlpha
            if (transparent ||
                (colorDistance(candidate, color) <= stepTolerance && colorDistance(candidate, seedColor) <= seedTolerance)
            ) {
                enqueue(n, seedColor)
            }
        }
        if (x > 0) visit(i - 1)
        if (x < width - 1) visit(i + 1)
        if (y > 0) visit(i - width)
        if (y < height - 1) visit(i + width)
    }
    return filled
}

/** Magic eraser tolerance at the Auto slider's two ends (max channel difference). */
internal const val MinMagicTolerance = 6
internal const val MaxMagicTolerance = 120

/** Auto slider's starting position: a moderate tolerance that suits most plain backdrops. */
internal const val MagicStrengthDefault = 0.3f

/** Maps the Auto slider's `0..1` [strength] to a colour tolerance for [colorRegionMask]. */
internal fun magicTolerance(strength: Float): Int {
    val t = strength.coerceIn(0f, 1f)
    return (MinMagicTolerance + (MaxMagicTolerance - MinMagicTolerance) * t).roundToInt()
}

/** Packs [inside] into an alpha-only mask image's pixels: opaque inside, fully transparent outside. */
internal fun maskToArgb(inside: BooleanArray): IntArray =
    IntArray(inside.size) { if (inside[it]) 0xFF000000.toInt() else 0 }

/** Zoom at which the photo fits the stage; the zoom slider's centre. */
internal const val FitZoom = 1f

/** Below [FitZoom], so the zoom slider's left half has room to zoom out. */
internal const val MinZoom = 0.5f
internal const val MaxZoom = 5f

/** Keeps a zoom factor between [MinZoom] and [MaxZoom]. */
internal fun clampZoom(zoom: Float): Float = zoom.coerceIn(MinZoom, MaxZoom)

/** The zoom slider's value range: centre is [FitZoom], left zooms out, right zooms in. */
internal val ZoomSliderRange = -1f..1f

/**
 * Maps a zoom slider [value] in [ZoomSliderRange] to a zoom factor. Each half is linear on its own
 * side of [FitZoom] (which is not the midpoint of [MinZoom]..[MaxZoom]), so the centre is always fit.
 */
internal fun sliderToZoom(value: Float): Float {
    val v = value.coerceIn(ZoomSliderRange)
    return if (v >= 0f) FitZoom + v * (MaxZoom - FitZoom) else FitZoom + v * (FitZoom - MinZoom)
}

/** Inverse of [sliderToZoom], so a pinch keeps the slider's thumb in step. */
internal fun zoomToSlider(zoom: Float): Float {
    val z = clampZoom(zoom)
    return if (z >= FitZoom) (z - FitZoom) / (MaxZoom - FitZoom) else (z - FitZoom) / (FitZoom - MinZoom)
}

/** Smallest brush, so the slider's zero end still erases something. */
internal const val MinBrushRadiusFraction = 0.004f

/**
 * Brush radius as a fraction of the photo width for a slider-derived [baseFraction] at [zoom].
 * Dividing by the zoom keeps the brush the same size on screen, so zooming in gives finer control.
 */
internal fun brushRadiusFraction(baseFraction: Float, zoom: Float): Float =
    baseFraction.coerceAtLeast(MinBrushRadiusFraction) / clampZoom(zoom)
