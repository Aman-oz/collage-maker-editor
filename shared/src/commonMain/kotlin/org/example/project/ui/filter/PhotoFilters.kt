package org.example.project.ui.filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

/**
 * A selectable filter. [matrix] is `null` for "no filter" (both [isNoneOption] and the plain
 * "Original" baseline use this — they render identically, [isNoneOption] only changes whether the
 * filmstrip shows an icon or the actual photo thumbnail for it).
 */
internal data class PhotoFilter(
    val label: String,
    val matrix: FloatArray?,
    val isNoneOption: Boolean = false,
)

private fun saturationMatrix(saturation: Float): FloatArray {
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    val invSat = 1f - saturation
    val r = lumR * invSat
    val g = lumG * invSat
    val b = lumB * invSat
    return floatArrayOf(
        r + saturation, g, b, 0f, 0f,
        r, g + saturation, b, 0f, 0f,
        r, g, b + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun tintMatrix(redMul: Float, greenMul: Float, blueMul: Float): FloatArray = floatArrayOf(
    redMul, 0f, 0f, 0f, 0f,
    0f, greenMul, 0f, 0f, 0f,
    0f, 0f, blueMul, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private val SepiaMatrix = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** Washed-out look: desaturate a little and lift the shadows. */
private fun fadeMatrix(): FloatArray = saturationMatrix(0.75f).also { m ->
    m[4] += 18f
    m[9] += 18f
    m[14] += 18f
}

internal val PhotoFilters: List<PhotoFilter> = listOf(
    PhotoFilter("None", matrix = null, isNoneOption = true),
    PhotoFilter("Original", matrix = null),
    PhotoFilter("Vivid", saturationMatrix(1.45f)),
    PhotoFilter("Warm", tintMatrix(1.18f, 1.02f, 0.82f)),
    PhotoFilter("Cool", tintMatrix(0.85f, 1.0f, 1.2f)),
    PhotoFilter("B&W", saturationMatrix(0f)),
    PhotoFilter("Sepia", SepiaMatrix),
    PhotoFilter("Fade", fadeMatrix()),
)

internal fun PhotoFilter.toColorFilter(): ColorFilter? = matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }

/** Renders [source] through [filter] into a new bitmap, so the effect survives past this screen. */
internal fun bakeFilter(source: ImageBitmap, filter: PhotoFilter): ImageBitmap {
    val colorFilter = filter.toColorFilter() ?: return source
    val output = ImageBitmap(source.width, source.height)
    Canvas(output).drawImage(source, Offset.Zero, Paint().apply { this.colorFilter = colorFilter })
    return output
}
