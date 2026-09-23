package org.example.project.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * The shared "cover with an effect, brush to reveal the original underneath" mechanic behind the
 * Splash / Blur family (ported from the LAS `QueShotSplashView`, which erases a full-screen effect
 * layer with a `PorterDuff.DST_OUT` brush). Here the same result is produced the other way round —
 * draw the [base] effect image, then draw the [reveal] original back inside the brush paths — so the
 * exact same call backs both the live preview and the final bake.
 */

internal val MaskBrushSizeRange = 0f..10f
internal const val MaskBrushSizeDefault = 3f

/** Max brush radius, as a fraction of the displayed image width, at [MaskBrushSizeRange]'s top end. */
private const val MaxMaskBrushRadiusFraction = 0.22f

/** Converts a [MaskBrushSizeRange] value into a brush radius, as a fraction of the image width. */
internal fun maskBrushRadiusFraction(brushSize: Float): Float =
    (brushSize / MaskBrushSizeRange.endInclusive) * MaxMaskBrushRadiusFraction

/**
 * One completed brush stroke: the dragged [points] (each a `0f..1f` fraction of the image bounds)
 * plus the brush [radiusFraction] (a fraction of the image width) that was in effect while it was
 * drawn — captured per-stroke so a later brush-size change doesn't retroactively resize old strokes.
 */
internal data class MaskStroke(val points: List<Offset>, val radiusFraction: Float)

/** Builds the reveal path for one committed [MaskStroke], using its own recorded brush size. */
internal fun strokeToPath(stroke: MaskStroke, imageOffset: Offset, imageSize: Size): Path =
    buildStrokePath(stroke.points, imageOffset, imageSize, stroke.radiusFraction * imageSize.width)

/**
 * Draws [base] as the bottom layer, then draws [reveal] back within each path in [revealPaths] (one
 * per brush stroke). Used identically by the on-screen preview and the bake.
 */
internal fun DrawScope.drawMaskReveal(
    base: ImageBitmap,
    reveal: ImageBitmap,
    imageOffset: Offset,
    imageSize: Size,
    revealPaths: List<Path>,
) {
    val dstOffset = IntOffset(imageOffset.x.roundToInt(), imageOffset.y.roundToInt())
    val dstSize = IntSize(imageSize.width.roundToInt().coerceAtLeast(1), imageSize.height.roundToInt().coerceAtLeast(1))
    drawImageScaled(base, dstOffset, dstSize)
    for (path in revealPaths) {
        clipPath(path) { drawImageScaled(reveal, dstOffset, dstSize) }
    }
}

/**
 * Bakes the finished brush effect at full resolution: the [base] effect image with the [reveal]
 * original brought back through every stroke. [base]/[reveal] must be the source's own size.
 */
internal fun bakeMaskReveal(base: ImageBitmap, reveal: ImageBitmap, strokes: List<MaskStroke>): ImageBitmap {
    if (strokes.isEmpty()) return base
    val output = ImageBitmap(base.width, base.height)
    val canvas = Canvas(output)
    val size = Size(base.width.toFloat(), base.height.toFloat())
    val paths = strokes.map { strokeToPath(it, Offset.Zero, size) }
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawMaskReveal(base = base, reveal = reveal, imageOffset = Offset.Zero, imageSize = size, revealPaths = paths)
    }
    return output
}
