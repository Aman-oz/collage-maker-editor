package org.example.project.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled

/**
 * Replays [actions] at full resolution in the order they were drawn — paint strokes fill with
 * their color, mosaic strokes reveal a pixelated copy of [source], erase strokes reveal the plain
 * [source] again — so later strokes correctly paint over earlier ones exactly as the live preview
 * showed. [previewCanvasWidthPx] is the on-screen preview's width the strokes were captured
 * against, used to scale their brush radius up to the source's real resolution.
 */
internal fun bakeDrawing(
    source: ImageBitmap,
    actions: List<DrawAction>,
    previewCanvasWidthPx: Float,
): ImageBitmap {
    if (actions.isEmpty() || previewCanvasWidthPx <= 0f) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val size = Size(source.width.toFloat(), source.height.toFloat())
    val dstOffset = IntOffset.Zero
    val dstSize = IntSize(source.width, source.height)
    val sharpCopy = if (actions.any { it is EraseAction }) copyBitmap(source) else null
    val mosaicBitmaps = actions.filterIsInstance<MosaicAction>()
        .map { it.pattern }
        .distinct()
        .associateWith { computeMosaicBitmap(source, it.cellFractionX, it.cellFractionY) }

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        for (action in actions) {
            val radiusPx = action.radiusFraction * size.width
            val path = buildStrokePath(action.points, Offset.Zero, size, radiusPx)
            when (action) {
                is PaintAction -> drawPath(path, color = action.color)
                is MosaicAction -> clipPath(path) {
                    drawImageScaled(mosaicBitmaps.getValue(action.pattern), dstOffset, dstSize)
                }
                is EraseAction -> clipPath(path) {
                    drawImageScaled(sharpCopy ?: source, dstOffset, dstSize)
                }
            }
        }
    }
    return output
}
