package org.example.project.ui.auto

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

/** Longest side of the thumbnail the histogram is taken from. That is plenty for levels and avoids reading every pixel of a 12 MP photo. */
private const val SampleMaxSide = 256

/**
 * Samples a downscaled copy of [source] and returns its auto-correction matrix. The copy is also
 * drawn through a canvas first, which avoids the unreliable platform-decoded bitmap reads
 * described in `ImageBitmapDrawing.kt`.
 */
internal fun autoColorMatrix(source: ImageBitmap): FloatArray {
    val scale = minOf(1f, SampleMaxSide.toFloat() / max(source.width, source.height))
    val width = (source.width * scale).roundToInt().coerceAtLeast(1)
    val height = (source.height * scale).roundToInt().coerceAtLeast(1)
    val sample = ImageBitmap(width, height)
    Canvas(sample).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply { filterQuality = FilterQuality.Medium },
    )
    val pixels = IntArray(width * height)
    sample.readPixels(pixels)
    return autoColorMatrix(pixels)
}

/** Renders [source] through [matrix] into a new full-resolution bitmap. */
internal fun bakeAuto(source: ImageBitmap, matrix: FloatArray): ImageBitmap {
    val output = ImageBitmap(source.width, source.height)
    Canvas(output).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply {
            filterQuality = FilterQuality.High
            colorFilter = ColorFilter.colorMatrix(ColorMatrix(matrix))
        },
    )
    return output
}
