package org.example.project.ui.save

import androidx.compose.ui.unit.IntRect
import kotlin.math.min
import kotlin.math.roundToInt

/** Watermark side length as a fraction of the image's shorter side. */
internal const val WatermarkSizeFraction = 0.12f

/** Gap between the watermark and the image's bottom/right edges, as a fraction of the shorter side. */
internal const val WatermarkMarginFraction = 0.035f

/** Watermark corner radius as a fraction of its own side length. */
internal const val WatermarkCornerFraction = 0.22f

/**
 * Where the watermark sits on a [width] x [height] image: a square in the bottom-right corner.
 * Everything scales off the shorter side, so the live preview (in dp) and the full-resolution bake
 * (in px) place it identically regardless of the photo's aspect ratio.
 */
internal fun watermarkRect(width: Int, height: Int): IntRect {
    val shortSide = min(width, height)
    val side = (shortSide * WatermarkSizeFraction).roundToInt().coerceAtLeast(1)
    val margin = (shortSide * WatermarkMarginFraction).roundToInt()
    val right = width - margin
    val bottom = height - margin
    return IntRect(left = right - side, top = bottom - side, right = right, bottom = bottom)
}
