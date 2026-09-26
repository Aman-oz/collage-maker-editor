package org.example.project.ui.ratio

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.crop.centeredRectForRatio
import org.example.project.ui.crop.cropImageBitmap
import org.example.project.ui.crop.toIntRectClamped

/** Fill behind the photo in the padded border. */
internal val RatioPaddingColor = Color.White

/** Center-crops [source] to [ratio] (width / height), or leaves it whole when [ratio] is null. */
internal fun reframeImage(source: ImageBitmap, ratio: Float?): ImageBitmap =
    if (ratio == null) source else cropImageBitmap(source, centeredRectForRatio(source, ratio).toIntRectClamped(source))

/**
 * Adds a [RatioPaddingColor] border of [paddingPercent] around [image] without changing its size:
 * the photo is center-cropped to fill the inner area (like `ContentScale.Crop` in the preview), so
 * the chosen ratio is kept exactly. Returns [image] itself when there is no padding.
 */
internal fun padImage(image: ImageBitmap, paddingPercent: Float): ImageBitmap {
    val pad = paddingPx(image.width, image.height, paddingPercent).roundToInt()
    if (pad <= 0) return image
    val innerSize = IntSize((image.width - 2 * pad).coerceAtLeast(1), (image.height - 2 * pad).coerceAtLeast(1))
    // "Free" hands us the raw platform-decoded photo, which doesn't redraw reliably as the source
    // of a scaling draw — copy it through an in-memory canvas first (see ImageBitmapDrawing.kt).
    val source = copyBitmap(image)
    val src = centeredRectForRatio(source, innerSize.width.toFloat() / innerSize.height).toIntRectClamped(source)

    val output = ImageBitmap(image.width, image.height)
    Canvas(output).apply {
        drawRect(0f, 0f, image.width.toFloat(), image.height.toFloat(), Paint().apply { color = RatioPaddingColor })
        drawImageRect(
            image = source,
            srcOffset = IntOffset(src.left, src.top),
            srcSize = IntSize(src.width, src.height),
            dstOffset = IntOffset(pad, pad),
            dstSize = innerSize,
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
    }
    return output
}
