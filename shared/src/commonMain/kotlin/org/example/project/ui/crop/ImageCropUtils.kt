package org.example.project.ui.crop

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Renders the [rect] region of [source] into a new, correctly-sized [ImageBitmap]. */
internal fun cropImageBitmap(source: ImageBitmap, rect: IntRect): ImageBitmap {
    val width = rect.width.coerceAtLeast(1)
    val height = rect.height.coerceAtLeast(1)
    val cropped = ImageBitmap(width, height)
    Canvas(cropped).drawImageRect(
        image = source,
        srcOffset = IntOffset(rect.left, rect.top),
        srcSize = IntSize(width, height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint(),
    )
    return cropped
}

internal fun fullImageRect(bitmap: ImageBitmap): Rect =
    Rect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())

/** The largest [ratio] (width/height) rect that fits centered within [bitmap]. */
internal fun centeredRectForRatio(bitmap: ImageBitmap, ratio: Float): Rect {
    val bw = bitmap.width.toFloat()
    val bh = bitmap.height.toFloat()
    var w = bw
    var h = bw / ratio
    if (h > bh) {
        h = bh
        w = bh * ratio
    }
    val left = (bw - w) / 2f
    val top = (bh - h) / 2f
    return Rect(left, top, left + w, top + h)
}

internal fun Rect.toIntRectClamped(bitmap: ImageBitmap): IntRect {
    val l = left.roundToInt().coerceIn(0, bitmap.width)
    val t = top.roundToInt().coerceIn(0, bitmap.height)
    val r = right.roundToInt().coerceIn(0, bitmap.width)
    val b = bottom.roundToInt().coerceIn(0, bitmap.height)
    return IntRect(l, t, r.coerceAtLeast(l + 1), b.coerceAtLeast(t + 1))
}
