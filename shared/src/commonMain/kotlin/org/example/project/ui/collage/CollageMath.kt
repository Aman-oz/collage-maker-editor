package org.example.project.ui.collage

import kotlin.math.roundToInt

/**
 * The largest `width x height` with [aspect] (width / height) that fits inside [maxW] x [maxH] —
 * how the preview sizes the canvas for the selected ratio inside the space the editor gives it.
 */
internal fun fitAspect(maxW: Float, maxH: Float, aspect: Float): Pair<Float, Float> {
    if (maxW <= 0f || maxH <= 0f || aspect <= 0f) return 0f to 0f
    return if (maxW / maxH > aspect) (maxH * aspect) to maxH else maxW to (maxW / aspect)
}

/**
 * The baked bitmap's pixel size for [aspect]: the longer side is [longSide] so a 9:16 story and a
 * 1:1 square cost about the same memory, and the shorter side follows the ratio.
 */
internal fun collageOutputSize(aspect: Float, longSide: Int = 1080): Pair<Int, Int> =
    if (aspect >= 1f) {
        longSide to (longSide / aspect).roundToInt().coerceAtLeast(1)
    } else {
        (longSide * aspect).roundToInt().coerceAtLeast(1) to longSide
    }
