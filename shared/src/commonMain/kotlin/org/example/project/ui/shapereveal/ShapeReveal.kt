package org.example.project.ui.shapereveal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
 * Builds the shape's clip path in the target coordinate space (preview or bitmap): a [placement]-sized
 * shape centered at its fractional position. Rotation is applied separately at draw time about [centerPx].
 */
private fun shapePathFor(placement: ShapePlacement, imageOffset: Offset, imageSize: Size): Pair<Path, Offset> {
    val sizePx = placement.sizeFraction * imageSize.width
    val centerPx = imageOffset + Offset(placement.centerXFraction * imageSize.width, placement.centerYFraction * imageSize.height)
    val path = buildShapePath(placement.shape, sizePx)
    path.translate(centerPx - Offset(sizePx / 2f, sizePx / 2f))
    return path to centerPx
}

/**
 * Draws [base] (the whole-image effect) then reveals [reveal] (the original) through the [placement]
 * shape. Shared by the live preview and the bake, so what the user positions is exactly what's saved.
 */
internal fun DrawScope.drawShapeReveal(
    base: ImageBitmap,
    reveal: ImageBitmap,
    placement: ShapePlacement,
    imageOffset: Offset,
    imageSize: Size,
) {
    val dstOffset = IntOffset(imageOffset.x.roundToInt(), imageOffset.y.roundToInt())
    val dstSize = IntSize(imageSize.width.roundToInt().coerceAtLeast(1), imageSize.height.roundToInt().coerceAtLeast(1))
    drawImageScaled(base, dstOffset, dstSize)

    val (path, centerPx) = shapePathFor(placement, imageOffset, imageSize)
    rotate(degrees = placement.rotationDegrees, pivot = centerPx) {
        clipPath(path) { drawImageScaled(reveal, dstOffset, dstSize) }
    }
}

/** Strokes the [placement] shape's outline so the user can see where the reveal region is. */
internal fun DrawScope.drawShapeOutline(
    placement: ShapePlacement,
    imageOffset: Offset,
    imageSize: Size,
    color: Color,
    strokeWidthPx: Float,
) {
    val (path, centerPx) = shapePathFor(placement, imageOffset, imageSize)
    rotate(degrees = placement.rotationDegrees, pivot = centerPx) {
        drawPath(path = path, color = color, style = Stroke(width = strokeWidthPx))
    }
}

/** Bakes the shape reveal at full resolution: [base]/[reveal] are the source's own size. */
internal fun bakeShapeReveal(base: ImageBitmap, reveal: ImageBitmap, placement: ShapePlacement): ImageBitmap {
    val output = ImageBitmap(base.width, base.height)
    val canvas = Canvas(output)
    val size = Size(base.width.toFloat(), base.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawShapeReveal(base = base, reveal = reveal, placement = placement, imageOffset = Offset.Zero, imageSize = size)
    }
    return output
}
