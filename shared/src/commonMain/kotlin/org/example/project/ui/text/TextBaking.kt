package org.example.project.ui.text

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/** A text ready to bake: the item and the font it is shown in on the photo. */
internal class BakeableText(val item: TextItem, val font: TextFontStyleOption)

/**
 * Renders [texts] on [source] at full image resolution, in order (later ones on top), each centred
 * where the user left it: a 0f..1f fraction of the image's own width/height.
 *
 * Each text is measured with [textMeasurer] using its own remembered (real device) density —
 * exactly matching how the live preview rendered it on screen — rather than recomputing a font
 * size for a different density, which is error-prone (sp-to-px depends on both density and font
 * scale). The resulting layout is then scaled up geometrically by
 * `source.width / previewCanvasWidthPx` (the ratio between the full-resolution image and its
 * on-screen displayed width), so the baked text matches the preview's proportions exactly.
 *
 * A text turns clockwise around its own centre, matching the preview's `graphicsLayer { rotationZ }`
 * (whose default pivot is the text box's centre). Its lines break where the preview's did: at its
 * text area's width ([TextItem.boxWidth]) when the side handles set one, else at
 * [previewMaxTextWidthPx], the widest the preview let words get before wrapping them. A taller area
 * ([TextItem.boxHeight]) only centres the words in more room, so it doesn't move them here.
 */
internal fun bakeTextItems(
    source: ImageBitmap,
    textMeasurer: TextMeasurer,
    texts: List<BakeableText>,
    previewCanvasWidthPx: Float,
    previewMaxTextWidthPx: Int,
): ImageBitmap {
    val shown = texts.filter { it.item.content.isNotBlank() }
    if (shown.isEmpty() || previewCanvasWidthPx <= 0f) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val scaleFactor = source.width / previewCanvasWidthPx
    val size = Size(source.width.toFloat(), source.height.toFloat())

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        for (text in shown) {
            val item = text.item
            val style = TextStyle(
                fontFamily = text.font.fontFamily,
                fontWeight = text.font.fontWeight,
                fontStyle = text.font.fontStyle,
                color = item.textColor,
                fontSize = item.sizeSp.sp,
                textAlign = item.alignment.textAlign,
            )
            val boxWidthPx = item.boxWidth?.let { (it * previewCanvasWidthPx).roundToInt().coerceAtLeast(1) }
            val previewLayout = textMeasurer.measure(
                item.content,
                style,
                constraints = if (boxWidthPx != null) {
                    Constraints.fixedWidth(boxWidthPx)
                } else {
                    Constraints(maxWidth = previewMaxTextWidthPx.coerceAtLeast(1))
                },
            )
            val centerPx = Offset(item.centerX * size.width, item.centerY * size.height)
            val innerTopLeft = Offset(
                x = centerPx.x / scaleFactor - previewLayout.size.width / 2f,
                y = centerPx.y / scaleFactor - previewLayout.size.height / 2f,
            )
            rotate(item.rotationDegrees, pivot = centerPx) {
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    drawTextUnderlay(item, previewLayout, innerTopLeft)
                    drawText(previewLayout, topLeft = innerTopLeft)
                }
            }
        }
    }

    return output
}

/**
 * The plate behind a text label whose text box is [textSize] at [textTopLeft]: a rounded rectangle
 * reaching a little past the words. Shared by every text preview and bake (this tool's and the
 * freestyle layers') so they all match.
 */
internal fun DrawScope.drawTextPlate(
    brush: Brush,
    textTopLeft: Offset,
    textSize: Size,
    cornerFraction: Float = TextBackgroundCornerFraction,
) {
    val padX = textSize.height * TextBackgroundPadXFraction
    val padY = textSize.height * TextBackgroundPadYFraction
    val corner = textSize.height * cornerFraction
    // Translated rather than offset through topLeft: a gradient brush resolves against the scope it
    // is drawn in, so this makes it span the plate itself, wherever on the canvas the label sits.
    translate(textTopLeft.x - padX, textTopLeft.y - padY) {
        drawRoundRect(
            brush = brush,
            size = Size(textSize.width + padX * 2f, textSize.height + padY * 2f),
            cornerRadius = CornerRadius(corner, corner),
        )
    }
}

/** The words' colour with their opacity, as the preview and the bake both draw them. */
internal val TextItem.textColor: Color get() = Color(colorArgb).copy(alpha = textOpacity)

/**
 * What goes under a text's words, in the preview and the bake alike: its background plate, then its
 * outline. The outline is the words' own [layout] stroked, so it follows every glyph exactly; drawn
 * under the fill, only its outer half shows. Its width comes from the font size in the layout's own
 * density, which is the preview's in both places, so it scales with the rest of the bake.
 */
internal fun DrawScope.drawTextUnderlay(item: TextItem, layout: TextLayoutResult, topLeft: Offset) {
    item.backgroundArgb?.let { argb ->
        drawTextPlate(
            brush = SolidColor(Color(argb).copy(alpha = item.backgroundOpacity)),
            textTopLeft = topLeft,
            textSize = layout.size.toSize(),
            cornerFraction = textPlateCornerFraction(item.backgroundCorner),
        )
    }
    item.strokeArgb?.let { argb ->
        val widthPx = with(layout.layoutInput.density) { item.sizeSp.sp.toPx() } * TextStrokeWidthFraction
        drawText(
            textLayoutResult = layout,
            color = Color(argb).copy(alpha = item.strokeOpacity),
            topLeft = topLeft,
            drawStyle = Stroke(width = widthPx, join = StrokeJoin.Round),
        )
    }
}
