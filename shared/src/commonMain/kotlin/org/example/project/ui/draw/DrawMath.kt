package org.example.project.ui.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal val BrushSizeRange = 1f..60f
internal const val BrushSizeDefault = 20f

/** Max brush radius, as a fraction of the displayed image width, at [BrushSizeRange]'s top end. */
private const val MaxBrushRadiusFraction = 0.15f

/** Converts a [BrushSizeRange] value into a brush radius, as a fraction of the image width. */
internal fun brushRadiusFraction(brushSize: Float): Float =
    (brushSize / BrushSizeRange.endInclusive) * MaxBrushRadiusFraction

/**
 * One completed brush stroke. [points] are `0f..1f` fractions of the image bounds and
 * [radiusFraction] is a fraction of the image width — both captured at the moment the stroke was
 * drawn, so later brush-size changes don't retroactively resize strokes already on the canvas.
 */
internal sealed interface DrawAction {
    val points: List<Offset>
    val radiusFraction: Float
}

/** A solid-color freehand stroke. */
internal data class PaintAction(
    override val points: List<Offset>,
    override val radiusFraction: Float,
    val color: Color,
) : DrawAction

/** A stroke that reveals a pixelated (see [MosaicPattern]) version of the photo. */
internal data class MosaicAction(
    override val points: List<Offset>,
    override val radiusFraction: Float,
    val pattern: MosaicPattern,
) : DrawAction

/** A stroke that reveals the original, unedited photo — undoing paint/mosaic strokes under it. */
internal data class EraseAction(
    override val points: List<Offset>,
    override val radiusFraction: Float,
) : DrawAction

/**
 * A hard-edged "pixelate" mosaic: downscale with smoothing (each small pixel averages its cell,
 * so it stays colorful/photographic) then upscale with [FilterQuality.None] so the blocks land
 * with crisp edges instead of blurring back together. [cellFractionX]/[cellFractionY] size each
 * cell as a fraction of the source's own width — independent X/Y fractions let the same technique
 * produce square blocks or, when one axis is much coarser, a striped/brick look.
 */
internal fun computeMosaicBitmap(source: ImageBitmap, cellFractionX: Float, cellFractionY: Float): ImageBitmap {
    val cellPxX = (cellFractionX * source.width).roundToInt().coerceAtLeast(1)
    val cellPxY = (cellFractionY * source.width).roundToInt().coerceAtLeast(1)
    val smallWidth = (source.width / cellPxX).coerceAtLeast(1)
    val smallHeight = (source.height / cellPxY).coerceAtLeast(1)

    val small = ImageBitmap(smallWidth, smallHeight)
    Canvas(small).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(smallWidth, smallHeight),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )

    val mosaic = ImageBitmap(source.width, source.height)
    Canvas(mosaic).drawImageRect(
        image = small,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(smallWidth, smallHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply { filterQuality = FilterQuality.None },
    )
    return mosaic
}
