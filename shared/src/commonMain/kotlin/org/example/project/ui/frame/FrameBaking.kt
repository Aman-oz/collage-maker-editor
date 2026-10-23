package org.example.project.ui.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Renders [source] at full resolution with a [color] frame drawn around its edge, and [frameArt]
 * (a server frame) stretched over it. [width] and
 * [cornerRadius] are in the on-screen preview's own units, scaled up by
 * `source.width / previewCanvasWidthPx` so the baked frame matches the preview's proportions.
 */
internal fun bakeFrame(
    source: ImageBitmap,
    color: Color,
    width: Float,
    cornerRadius: Float,
    previewCanvasWidthPx: Float,
    frameArt: ImageBitmap? = null,
): ImageBitmap {
    if ((width <= 0f || color.alpha <= 0f || previewCanvasWidthPx <= 0f) && frameArt == null) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val scaleFactor = if (previewCanvasWidthPx > 0f) source.width / previewCanvasWidthPx else 0f
    val size = Size(source.width.toFloat(), source.height.toFloat())

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawFrame(
            imageOffset = Offset.Zero,
            imageSize = size,
            color = color,
            widthPx = width * scaleFactor,
            cornerRadiusPx = cornerRadius * scaleFactor,
        )
    }
    // The decorative frame (already trimmed of its margin, see trimFrameArt) stretched over the whole
    // photo, as the preview draws it.
    if (frameArt != null) {
        canvas.drawImageRect(
            image = frameArt,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(frameArt.width, frameArt.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(source.width, source.height),
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
    }

    return output
}
