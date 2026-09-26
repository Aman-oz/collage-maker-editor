package org.example.project.ui.save

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toRect
import org.example.project.ui.common.copyBitmap

/**
 * Returns a copy of [source] with [watermark] stamped as a rounded square in the bottom-right
 * corner, at the [watermarkRect] the preview overlay also uses.
 */
internal fun bakeWatermark(source: ImageBitmap, watermark: ImageBitmap): ImageBitmap {
    val output = copyBitmap(source)
    // The resource-decoded logo is platform-decoded too, so copy it before the scaling draw.
    val logo = copyBitmap(watermark)
    val rect = watermarkRect(output.width, output.height)
    val corner = rect.width * WatermarkCornerFraction
    val canvas = Canvas(output)

    canvas.save()
    canvas.clipPath(Path().apply { addRoundRect(RoundRect(rect.toRect(), CornerRadius(corner))) })
    canvas.drawImageRect(
        image = logo,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(logo.width, logo.height),
        dstOffset = rect.topLeft,
        dstSize = rect.size,
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    canvas.restore()

    return output
}
