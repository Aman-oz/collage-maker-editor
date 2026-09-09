package org.example.project.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
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
 * Renders [content] on [source] at full image resolution, centered at [offsetFraction] — a
 * 0f..1f fraction of the image's own width/height, matching wherever the user dragged the text
 * to in the live preview.
 *
 * The text is measured with [textMeasurer] using its own remembered (real device) density —
 * exactly matching how the live preview rendered it on screen — rather than recomputing a font
 * size for a different density, which is error-prone (sp-to-px depends on both density and font
 * scale). The resulting layout is then scaled up geometrically by
 * `source.width / previewCanvasWidthPx` (the ratio between the full-resolution image and its
 * on-screen displayed width), so the baked text matches the preview's proportions exactly.
 */
internal fun bakeText(
    source: ImageBitmap,
    textMeasurer: TextMeasurer,
    content: String,
    fontStyleOption: TextFontStyleOption,
    color: Color,
    sizeSp: Float,
    previewCanvasWidthPx: Float,
    offsetFraction: Offset,
): ImageBitmap {
    if (content.isBlank() || previewCanvasWidthPx <= 0f) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val style = TextStyle(
        fontFamily = fontStyleOption.fontFamily,
        fontWeight = fontStyleOption.fontWeight,
        fontStyle = fontStyleOption.fontStyle,
        color = color,
        fontSize = sizeSp.sp,
    )
    val previewLayout = textMeasurer.measure(content, style)
    val scaleFactor = source.width / previewCanvasWidthPx
    val size = Size(source.width.toFloat(), source.height.toFloat())
    val centerPx = Offset(offsetFraction.x * size.width, offsetFraction.y * size.height)

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        val innerTopLeft = Offset(
            x = centerPx.x / scaleFactor - previewLayout.size.width / 2f,
            y = centerPx.y / scaleFactor - previewLayout.size.height / 2f,
        )
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            drawText(previewLayout, topLeft = innerTopLeft)
        }
    }

    return output
}
