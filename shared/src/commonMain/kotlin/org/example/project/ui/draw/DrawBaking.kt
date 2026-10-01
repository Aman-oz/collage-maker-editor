package org.example.project.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.scaledBitmap

/**
 * Replays [actions] at full resolution in the order they were drawn — paint strokes fill with
 * their color, mosaic strokes reveal their pattern (see [renderMosaic]), erase strokes reveal the plain
 * [source] again — so later strokes correctly paint over earlier ones exactly as the live preview
 * showed. Brush radii are fractions of the image width, so they scale to the source's real
 * resolution on their own.
 *
 * Each stroke is one polyline ([strokePolyline]) stroked with a brush: a solid colour, or an
 * [ImageShader] of a source-sized bitmap, which lines up 1:1 with the output — the same technique
 * as the live preview, so the bake matches it.
 */
internal fun bakeDrawing(source: ImageBitmap, actions: List<DrawAction>): ImageBitmap {
    if (actions.isEmpty()) return source

    // Copied so it draws reliably (a platform-decoded photo may not, see copyBitmap).
    val sharp = copyBitmap(source)
    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(sharp, Offset.Zero, Paint())

    val size = Size(source.width.toFloat(), source.height.toFloat())
    // Textures render smaller than the photo, so each is scaled up to it once for the shader.
    val patternBrushes = actions.filterIsInstance<MosaicAction>()
        .map { it.pattern }
        .distinct()
        .associateWith { pattern ->
            val mosaic = renderMosaic(sharp, pattern)
            val fitted = if (mosaic.width == source.width && mosaic.height == source.height) {
                mosaic
            } else {
                scaledBitmap(mosaic, source.width, source.height)
            }
            ShaderBrush(ImageShader(fitted))
        }
    val eraseBrush = ShaderBrush(ImageShader(sharp))

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        for (action in actions) {
            val path = strokePolyline(action.points, size)
            val stroke = brushStroke(action.radiusFraction * size.width)
            when (action) {
                is PaintAction -> drawPath(path, color = action.color, style = stroke)
                is MosaicAction -> drawPath(path, brush = patternBrushes.getValue(action.pattern), style = stroke)
                is EraseAction -> drawPath(path, brush = eraseBrush, style = stroke)
            }
        }
    }
    return output
}
