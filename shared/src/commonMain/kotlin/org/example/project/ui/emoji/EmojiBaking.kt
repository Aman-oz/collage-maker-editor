package org.example.project.ui.emoji

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

/**
 * Renders every placed emoji onto [source] at full image resolution, each centered at its own
 * `offsetFraction` (0f..1f of the image bounds) and sized by `EmojiBaseSizeSp * scale` — measured
 * with [textMeasurer] at its own remembered (real device) density, then scaled up geometrically
 * by `source.width / previewCanvasWidthPx`, exactly like [org.example.project.ui.text.bakeText].
 */
internal fun bakeEmojis(
    source: ImageBitmap,
    textMeasurer: TextMeasurer,
    placedEmojis: List<PlacedEmoji>,
    previewCanvasWidthPx: Float,
): ImageBitmap {
    if (placedEmojis.isEmpty() || previewCanvasWidthPx <= 0f) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val scaleFactor = source.width / previewCanvasWidthPx
    val size = Size(source.width.toFloat(), source.height.toFloat())

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        for (placed in placedEmojis) {
            val style = TextStyle(fontSize = (EmojiBaseSizeSp * placed.scale).sp)
            val layout = textMeasurer.measure(placed.emoji, style)
            val centerPx = Offset(placed.offsetFraction.x * size.width, placed.offsetFraction.y * size.height)
            val innerTopLeft = Offset(
                x = centerPx.x / scaleFactor - layout.size.width / 2f,
                y = centerPx.y / scaleFactor - layout.size.height / 2f,
            )
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                drawText(layout, topLeft = innerTopLeft)
            }
        }
    }

    return output
}
