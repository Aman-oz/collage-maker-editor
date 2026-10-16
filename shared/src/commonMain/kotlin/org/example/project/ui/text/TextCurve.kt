package org.example.project.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * How a text's words are bent by its Curve slider, worked out from their layout: the arcs'
 * [curvature] around the layout's [center] (see [bendTextPoint]), and how far down ([shiftY]) the
 * bent words are moved so they stay centred where the straight ones were. [extraHeight] is how much
 * taller than the straight words they then are, which the preview pads its text box by so the
 * selection still goes round them.
 */
internal class TextBend(val curvature: Float, val center: Offset, val shiftY: Float, val extraHeight: Float) {
    fun map(point: Offset): Offset = bendTextPoint(point, center, curvature) + Offset(0f, shiftY)
}

/** The bend of words laid out as [layout] with the Curve slider at [curve]; null while they run straight. */
internal fun textBendOf(layout: TextLayoutResult, curve: Float): TextBend? {
    val widest = (0 until layout.lineCount).maxOfOrNull { layout.getLineRight(it) - layout.getLineLeft(it) } ?: 0f
    val curvature = textCurvature(curve, widest)
    if (curvature == 0f) return null
    val size = layout.size.toSize()
    val center = Offset(size.width / 2f, size.height / 2f)
    val bent = roundedRectOutline(Rect(Offset.Zero, size), 0f).map { bendTextPoint(it, center, curvature) }
    val top = bent.minOf { it.y }
    val bottom = bent.maxOf { it.y }
    return TextBend(
        curvature = curvature,
        center = center,
        shiftY = center.y - (top + bottom) / 2f,
        extraHeight = (bottom - top - size.height).coerceAtLeast(0f),
    )
}

/**
 * A text's plate, outline and words, as the bake draws them and as the preview draws a text that
 * isn't being typed in: straight, or bent along [bend]'s arcs.
 */
internal fun DrawScope.drawTextItem(item: TextItem, layout: TextLayoutResult, topLeft: Offset, bend: TextBend? = textBendOf(layout, item.curve)) {
    if (bend == null) {
        drawTextUnderlay(item, layout, topLeft)
        drawText(layout, topLeft = topLeft)
        return
    }
    item.backgroundArgb?.let { argb -> drawBentPlate(Color(argb).copy(alpha = item.backgroundOpacity), item, layout, topLeft, bend) }
    val glyphs = bentGlyphs(layout)
    item.strokeArgb?.let { argb ->
        val stroke = Stroke(
            width = with(layout.layoutInput.density) { item.sizeSp.sp.toPx() } * TextStrokeWidthFraction,
            join = StrokeJoin.Round,
        )
        val color = Color(argb).copy(alpha = item.strokeOpacity)
        drawBentGlyphs(glyphs, bend, topLeft) { drawText(layout, color = color, drawStyle = stroke) }
    }
    drawBentGlyphs(glyphs, bend, topLeft) { drawText(layout) }
}

/**
 * One glyph of a bent text: the slice of the layout ([clip], in the layout's own coordinates) that
 * is drawn turned and moved onto the arc, and the point of it ([anchor], its middle on its line's
 * middle) that lands on the arc.
 */
private class BentGlyph(val clip: Rect, val anchor: Offset)

/**
 * The glyph slices of [layout]. Next to each other they cover their line without gaps, and the
 * slices at a line's ends and the top and bottom lines' outer edges reach a line's height past it,
 * so an outline or a swash hanging past the letters' boxes isn't cut off there.
 */
private fun bentGlyphs(layout: TextLayoutResult): List<BentGlyph> {
    val glyphs = mutableListOf<BentGlyph>()
    for (line in 0 until layout.lineCount) {
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        val reach = bottom - top
        val clipTop = if (line == 0) top - reach else top
        val clipBottom = if (line == layout.lineCount - 1) bottom + reach else bottom
        // A character made of two code units (an emoji) reports the same box for both.
        val boxes = (layout.getLineStart(line) until layout.getLineEnd(line, visibleEnd = true))
            .map { layout.getBoundingBox(it) }
            .filter { it.width > 0f }
            .distinct()
        if (boxes.isEmpty()) continue
        val left = boxes.minOf { it.left }
        val right = boxes.maxOf { it.right }
        for (box in boxes) {
            glyphs += BentGlyph(
                clip = Rect(
                    left = if (box.left == left) box.left - reach else box.left,
                    top = clipTop,
                    right = if (box.right == right) box.right + reach else box.right,
                    bottom = clipBottom,
                ),
                anchor = Offset(box.center.x, (top + bottom) / 2f),
            )
        }
    }
    return glyphs
}

/**
 * Runs [draw] (which draws the whole layout at the origin) once per glyph, clipped to that glyph's
 * slice and moved so its anchor sits on the arc, turned to follow it.
 */
private inline fun DrawScope.drawBentGlyphs(glyphs: List<BentGlyph>, bend: TextBend, topLeft: Offset, draw: DrawScope.() -> Unit) {
    for (glyph in glyphs) {
        val at = bend.map(glyph.anchor)
        withTransform({
            translate(topLeft.x + at.x, topLeft.y + at.y)
            rotate(bendTextAngleDegrees(glyph.anchor.x, bend.center.x, bend.curvature), pivot = Offset.Zero)
            translate(-glyph.anchor.x, -glyph.anchor.y)
            clipRect(glyph.clip.left, glyph.clip.top, glyph.clip.right, glyph.clip.bottom)
        }) {
            draw()
        }
    }
}

/** The plate of [drawTextUnderlay], bent with the words: its outline sampled and every point bent. */
private fun DrawScope.drawBentPlate(color: Color, item: TextItem, layout: TextLayoutResult, topLeft: Offset, bend: TextBend) {
    val plate = item.plateRect(layout)
    val outline = roundedRectOutline(plate, textPlateCornerRadius(item.backgroundCorner, plate.height))
    val path = Path()
    outline.forEachIndexed { index, point ->
        val bent = bend.map(point)
        if (index == 0) path.moveTo(bent.x, bent.y) else path.lineTo(bent.x, bent.y)
    }
    path.close()
    translate(topLeft.x, topLeft.y) { drawPath(path, color) }
}

/**
 * Points round [rect] with corners of [radius], clockwise from the top of its left edge. The top and
 * bottom edges are sampled finely since bending turns them into arcs; the sides stay straight under
 * [bendTextPoint] (they only move along a radius), so they need no points between the corners.
 */
private fun roundedRectOutline(rect: Rect, radius: Float, edgeSteps: Int = 48, cornerSteps: Int = 8): List<Offset> {
    val r = radius.coerceIn(0f, rect.minDimension / 2f)
    val points = mutableListOf<Offset>()
    fun corner(centerX: Float, centerY: Float, startDegrees: Float) {
        for (i in 0..cornerSteps) {
            val radians = (startDegrees + 90f * i / cornerSteps) * PI.toFloat() / 180f
            points += Offset(centerX + r * cos(radians), centerY + r * sin(radians))
        }
    }
    fun edge(from: Offset, to: Offset) {
        for (i in 1 until edgeSteps) points += from + (to - from) * (i / edgeSteps.toFloat())
    }
    corner(rect.left + r, rect.top + r, 180f)
    edge(Offset(rect.left + r, rect.top), Offset(rect.right - r, rect.top))
    corner(rect.right - r, rect.top + r, 270f)
    corner(rect.right - r, rect.bottom - r, 0f)
    edge(Offset(rect.right - r, rect.bottom), Offset(rect.left + r, rect.bottom))
    corner(rect.left + r, rect.bottom - r, 90f)
    return points
}
