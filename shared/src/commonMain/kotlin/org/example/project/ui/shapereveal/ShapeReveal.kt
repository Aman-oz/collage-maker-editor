package org.example.project.ui.shapereveal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import org.example.project.ui.blur.computeBlurredBitmap
import org.example.project.ui.common.RevealShape
import org.example.project.ui.common.buildShapePath
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.grayscaleBitmap
import kotlin.math.roundToInt

/** Which whole-image effect the shape reveals the original through. */
internal enum class RevealEffect { Blur, Grayscale }

/** The current shape placement, all normalized to the image (so preview and bake agree). */
internal data class ShapePlacement(
    val shape: RevealShape,
    val centerXFraction: Float = 0.5f,
    val centerYFraction: Float = 0.5f,
    val sizeFraction: Float = 0.55f,
    val rotationDegrees: Float = 0f,
)

internal fun buildEffectBitmap(source: ImageBitmap, effect: RevealEffect, blurLevel: Int): ImageBitmap = when (effect) {
    RevealEffect.Blur -> computeBlurredBitmap(source, blurLevel)
    RevealEffect.Grayscale -> grayscaleBitmap(source)
}

/**
 * Cap height as a fraction of the font size for the platform sans-serif (Roboto ≈ 0.711,
 * SF Pro ≈ 0.705). Glyph shapes are sized so their cap height matches the placement size.
 */
private const val CapHeightRatio = 0.71f

private val GlyphMask = Color.Black

/**
 * Extra stroke (as a fraction of the placement size) drawn around glyphs to embolden them past the
 * font's Black weight. Round joins keep it an exact offset of the outline, so strokes around the
 * font's internal overlap contours (see [drawPlacedShape]) never poke outside the glyph.
 */
private const val GlyphBoldRatio = 0.07f

/**
 * Draws [placement]'s shape — rotated about its center — filled with [color], or, when
 * [outlineWidthPx] is set, as an outline of that width around the shape.
 *
 * Path shapes come from [buildShapePath]. Glyph shapes are laid out as black-weight text sized so
 * the glyph spans the placement size vertically and is centered on it, then emboldened with a
 * [GlyphBoldRatio] stroke. Heavy fonts build glyphs from overlapping contours, so stroking the
 * text directly draws those internal seams (lines across the A's crossbar, an X inside the 2).
 * Instead the outline is a wider stroke with the filled glyph erased from it in a layer, which
 * keeps only the outer ring.
 */
private fun DrawScope.drawPlacedShape(
    placement: ShapePlacement,
    imageOffset: Offset,
    imageSize: Size,
    textMeasurer: TextMeasurer,
    color: Color,
    outlineWidthPx: Float? = null,
) {
    val sizePx = placement.sizeFraction * imageSize.width
    val centerPx = imageOffset + Offset(placement.centerXFraction * imageSize.width, placement.centerYFraction * imageSize.height)
    val glyph = placement.shape.glyph
    if (glyph == null) {
        rotate(degrees = placement.rotationDegrees, pivot = centerPx) {
            val path = buildShapePath(placement.shape, sizePx)
            path.translate(centerPx - Offset(sizePx / 2f, sizePx / 2f))
            drawPath(path = path, color = color, style = outlineWidthPx?.let { Stroke(width = it) } ?: Fill)
        }
        return
    }

    // Density(1f) makes sp == px, so the glyph size is independent of the scope's density
    // (the preview draws at screen density, the bake at 1:1 pixels).
    val layout = textMeasurer.measure(
        text = glyph,
        style = TextStyle(fontSize = (sizePx / CapHeightRatio).sp, fontWeight = FontWeight.Black),
        density = Density(1f),
    )
    // The cap-height box sits between (baseline - sizePx) and baseline; center that box.
    val topLeft = Offset(
        x = centerPx.x - layout.size.width / 2f,
        y = centerPx.y + sizePx / 2f - layout.firstBaseline,
    )
    val boldPx = sizePx * GlyphBoldRatio
    fun drawGlyph(color: Color, strokePx: Float, blendMode: BlendMode = DrawScope.DefaultBlendMode, withFill: Boolean = true) {
        // Fill must be passed explicitly: on Android the layout's paint keeps the last draw style
        // when none is given, so an unstyled drawText after a stroke would stroke again.
        if (withFill) drawText(textLayoutResult = layout, color = color, topLeft = topLeft, drawStyle = Fill, blendMode = blendMode)
        drawText(
            textLayoutResult = layout,
            color = color,
            topLeft = topLeft,
            drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round),
            blendMode = blendMode,
        )
    }

    if (outlineWidthPx == null) {
        rotate(degrees = placement.rotationDegrees, pivot = centerPx) { drawGlyph(color, boldPx) }
    } else {
        // The layer is opened before rotating so its (unrotated) bounds cover the whole scope.
        drawContext.canvas.saveLayer(Rect(Offset.Zero, size), Paint())
        rotate(degrees = placement.rotationDegrees, pivot = centerPx) {
            drawGlyph(color, boldPx + 2f * outlineWidthPx, withFill = false)
            drawGlyph(GlyphMask, boldPx, blendMode = BlendMode.DstOut)
        }
        drawContext.canvas.restore()
    }
}

/**
 * Draws [base] (the whole-image effect) then reveals [reveal] (the original) through the [placement]
 * shape. Shared by the live preview and the bake, so what the user positions is exactly what's saved.
 *
 * The reveal is masked in a layer rather than clipped, because glyph shapes have no path: the shape
 * is drawn first, then [reveal] with `SrcIn` so it survives only where the shape is. The order
 * matters: a blend mode only affects pixels the draw covers, so masking with the shape (`DstIn`)
 * would leave the sharp photo untouched outside it. Only the mask is rotated, so the revealed
 * photo stays aligned with the base.
 */
internal fun DrawScope.drawShapeReveal(
    base: ImageBitmap,
    reveal: ImageBitmap,
    placement: ShapePlacement,
    imageOffset: Offset,
    imageSize: Size,
    textMeasurer: TextMeasurer,
) {
    val dstOffset = IntOffset(imageOffset.x.roundToInt(), imageOffset.y.roundToInt())
    val dstSize = IntSize(imageSize.width.roundToInt().coerceAtLeast(1), imageSize.height.roundToInt().coerceAtLeast(1))
    drawImageScaled(base, dstOffset, dstSize)

    drawContext.canvas.saveLayer(Rect(imageOffset, imageSize), Paint())
    drawPlacedShape(placement, imageOffset, imageSize, textMeasurer, color = GlyphMask)
    drawImageScaled(reveal, dstOffset, dstSize, blendMode = BlendMode.SrcIn)
    drawContext.canvas.restore()
}

/** Strokes the [placement] shape's outline so the user can see where the reveal region is. */
internal fun DrawScope.drawShapeOutline(
    placement: ShapePlacement,
    imageOffset: Offset,
    imageSize: Size,
    textMeasurer: TextMeasurer,
    color: Color,
    strokeWidthPx: Float,
) {
    drawPlacedShape(placement, imageOffset, imageSize, textMeasurer, color = color, outlineWidthPx = strokeWidthPx)
}

/** Bakes the shape reveal at full resolution: [base]/[reveal] are the source's own size. */
internal fun bakeShapeReveal(base: ImageBitmap, reveal: ImageBitmap, placement: ShapePlacement, textMeasurer: TextMeasurer): ImageBitmap {
    val output = ImageBitmap(base.width, base.height)
    val canvas = Canvas(output)
    val size = Size(base.width.toFloat(), base.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawShapeReveal(base = base, reveal = reveal, placement = placement, imageOffset = Offset.Zero, imageSize = size, textMeasurer = textMeasurer)
    }
    return output
}
