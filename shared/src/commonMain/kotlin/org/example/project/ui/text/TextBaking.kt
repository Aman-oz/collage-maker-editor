package org.example.project.ui.text

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

/** A text ready to draw: the item and the font it is shown in on the photo. */
internal class BakeableText(val item: TextItem, val font: TextFontStyleOption)

/**
 * The texts the Text tool leaves on the photo, kept live over it in the editor (so a tap can open
 * them again) until something needs them as pixels ([bakeTextLayers]). Each carries the font it was
 * shown in, already loaded, and they come with what their layout depends on in the tool: the
 * photo's width there ([canvasWidthPx]; their sizes are relative to it), the widest a line could get
 * before wrapping ([maxTextWidthPx]) and the screen [density] they were measured in. Laid out
 * against those, they draw the same at any size, scaled ([drawMeasuredTexts]).
 */
internal class TextLayers(
    val texts: List<BakeableText>,
    val canvasWidthPx: Float,
    val maxTextWidthPx: Int,
    val density: Density,
) {
    val items: List<TextItem> get() = texts.map { it.item }
}

/** A text of [TextLayers] laid out as the tool laid it out, and how much room its box takes there. */
internal class MeasuredText(val item: TextItem, val layout: TextLayoutResult, val boxSize: Size)

/**
 * Lays out every written text of [layers] as the Text tool did: in its [TextLayers.density], wrapping
 * at its text area's width ([TextItem.boxWidth]) when the side handles set one, else at
 * [TextLayers.maxTextWidthPx]. [MeasuredText.boxSize] is the selection box the tool drew round it
 * (padding, a plate reaching past it, a taller text area and a curve's extra height included), which
 * is what a tap hits.
 */
internal fun measureTextLayers(textMeasurer: TextMeasurer, layers: TextLayers): List<MeasuredText> =
    layers.texts.filter { it.item.content.isNotBlank() }.map { text ->
        val item = text.item
        val boxWidthPx = item.boxWidth?.let { (it * layers.canvasWidthPx).roundToInt().coerceAtLeast(1) }
        val layout = textMeasurer.measure(
            item.shownContent,
            item.textStyle(text.font, layers.density),
            constraints = if (boxWidthPx != null) {
                Constraints.fixedWidth(boxWidthPx)
            } else {
                Constraints(maxWidth = layers.maxTextWidthPx.coerceAtLeast(1))
            },
            density = layers.density,
        )
        val overflow = item.plateOverflow(
            emPx = with(layers.density) { item.sizeSp.sp.toPx() },
            boxPaddingXPx = with(layers.density) { TextLayerPaddingX.toPx() },
            boxPaddingYPx = with(layers.density) { TextLayerPaddingY.toPx() },
        )
        val padX = with(layers.density) { TextLayerPaddingX.toPx() } + overflow.width
        val padY = with(layers.density) { TextLayerPaddingY.toPx() } + overflow.height
        val wordsHeight = layout.size.height + (textBendOf(layout, item.curve)?.extraHeight ?: 0f)
        val areaHeight = maxOf(wordsHeight, (item.boxHeight ?: 0f) * layers.canvasWidthPx)
        MeasuredText(item, layout, Size(layout.size.width + padX * 2f, areaHeight + padY * 2f))
    }

/**
 * Draws [texts] over a photo that fills this scope, in order (later ones on top), each centred where
 * the user left it: a 0f..1f fraction of the photo's own width/height. Each layout, measured against a
 * photo [canvasWidthPx] wide, is scaled by `size.width / canvasWidthPx`, so the texts keep their
 * proportions to the photo at any size: on the editor's screen or at full resolution.
 *
 * A text turns clockwise around its own centre, matching the tool's `graphicsLayer { rotationZ }`
 * (whose default pivot is the text box's centre). A taller text area only centres the words in more
 * room, so it doesn't move them here. A curved text is bent from that same layout ([drawTextItem]).
 */
internal fun DrawScope.drawMeasuredTexts(texts: List<MeasuredText>, canvasWidthPx: Float) {
    if (canvasWidthPx <= 0f) return
    val scaleFactor = size.width / canvasWidthPx
    for (text in texts) {
        val item = text.item
        val centerPx = Offset(item.centerX * size.width, item.centerY * size.height)
        val innerTopLeft = Offset(
            x = centerPx.x / scaleFactor - text.layout.size.width / 2f,
            y = centerPx.y / scaleFactor - text.layout.size.height / 2f,
        )
        withOpacityLayer(item.opacity) {
            rotate(item.rotationDegrees, pivot = centerPx) {
                scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
                    drawTextItem(item, text.layout, innerTopLeft)
                }
            }
        }
    }
}

/**
 * [source] with [layers] drawn on it at full resolution. Measuring in the tool's own density, rather
 * than recomputing a font size for the image's, keeps it exact (sp-to-px depends on both density and
 * font scale); the layouts are then scaled up geometrically, so the baked texts match the preview.
 */
internal fun bakeTextLayers(source: ImageBitmap, textMeasurer: TextMeasurer, layers: TextLayers): ImageBitmap {
    val measured = measureTextLayers(textMeasurer, layers)
    if (measured.isEmpty() || layers.canvasWidthPx <= 0f) return source

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())
    val size = Size(source.width.toFloat(), source.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawMeasuredTexts(measured, layers.canvasWidthPx)
    }
    return output
}

/**
 * Draws [block] into a layer composited at [opacity], as the preview's `graphicsLayer { alpha }`
 * does: the text fades as one picture, so where its outline, plate and words overlap they don't
 * show through each other. The layer spans the whole canvas, set up before any turn or scale.
 */
private inline fun DrawScope.withOpacityLayer(opacity: Float, block: DrawScope.() -> Unit) {
    if (opacity >= 1f) return block()
    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = opacity.coerceIn(0f, 1f) }) }
    block()
    drawIntoCanvas { it.restore() }
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

/**
 * How [this] text's words are set in [font], in the preview and the bake alike: its size, colour and
 * alignment, the Spacing panel's letter and line spacing, the Format panel's bold, italic and lines,
 * and the Shadow panel's shadow, whose pixels come from [density] (the preview's). Bold and italic stand in for the font's
 * own weight and slant; a family with one file gets them synthesized.
 */
internal fun TextItem.textStyle(font: TextFontStyleOption, density: Density): TextStyle = TextStyle(
    fontFamily = font.fontFamily,
    fontWeight = if (bold) FontWeight.Bold else font.fontWeight,
    fontStyle = if (italic) FontStyle.Italic else font.fontStyle,
    fontSize = sizeSp.sp,
    color = textColor,
    textAlign = alignment.textAlign,
    letterSpacing = letterSpacingEm(letterSpacing).em,
    lineHeight = lineHeightEm(lineSpacing)?.em ?: TextUnit.Unspecified,
    textDecoration = TextDecoration.combine(
        listOfNotNull(TextDecoration.Underline.takeIf { underline }, TextDecoration.LineThrough.takeIf { strikethrough }),
    ),
    shadow = textShadow(density),
)

/**
 * The Shadow panel's shadow, in pixels of [density] — the preview's in the bake too, which measures in
 * it and scales the result up, so the shadow scales with the words. Null without one.
 */
private fun TextItem.textShadow(density: Density): Shadow? {
    if (shadowOpacity <= 0f) return null
    val emPx = with(density) { sizeSp.sp.toPx() }
    return Shadow(
        color = Color(shadowArgb).copy(alpha = shadowOpacity),
        offset = Offset(shadowDx * emPx, shadowDy * emPx),
        blurRadius = shadowBlurEm(shadowBlur) * emPx,
    )
}

/**
 * This text's plate around its words laid out as [layout] (at the origin, in its pixels): the
 * Padding panel's padding on each side, in em of the font size in the layout's density.
 */
internal fun TextItem.plateRect(layout: TextLayoutResult): Rect {
    val emPx = with(layout.layoutInput.density) { sizeSp.sp.toPx() }
    return textPlateRect(paddingLeft, paddingTop, paddingRight, paddingBottom, layout.size.toSize(), emPx)
}

/**
 * How far this text's plate reaches past the padding its box already has round the words
 * ([boxPaddingXPx] / [boxPaddingYPx]), on the wider side of each axis: what the box has to grow by on
 * every side to take the plate in, keeping its centre. Nothing without a plate. [emPx] is the font
 * size in the same pixels.
 */
internal fun TextItem.plateOverflow(emPx: Float, boxPaddingXPx: Float, boxPaddingYPx: Float): Size {
    if (backgroundArgb == null) return Size.Zero
    val x = maxOf(textPaddingEm(paddingLeft), textPaddingEm(paddingRight)) * emPx - boxPaddingXPx
    val y = maxOf(textPaddingEm(paddingTop), textPaddingEm(paddingBottom)) * emPx - boxPaddingYPx
    return Size(x.coerceAtLeast(0f), y.coerceAtLeast(0f))
}

/** The words' colour with their opacity, as the preview and the bake both draw them. */
internal val TextItem.textColor: Color get() = Color(colorArgb).copy(alpha = textOpacity)

/**
 * What goes under a text's words, in the preview and the bake alike: its background plate (as far
 * past the words as its padding says, [plateRect]), then its outline. The outline is the words' own [layout] stroked, so it follows every glyph exactly; drawn
 * under the fill, only its outer half shows. Its width comes from the font size in the layout's own
 * density, which is the preview's in both places, so it scales with the rest of the bake.
 */
internal fun DrawScope.drawTextUnderlay(item: TextItem, layout: TextLayoutResult, topLeft: Offset) {
    item.backgroundArgb?.let { argb ->
        val plate = item.plateRect(layout)
        val corner = textPlateCornerRadius(item.backgroundCorner, plate.height)
        drawRoundRect(
            color = Color(argb).copy(alpha = item.backgroundOpacity),
            topLeft = topLeft + plate.topLeft,
            size = plate.size,
            cornerRadius = CornerRadius(corner, corner),
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
