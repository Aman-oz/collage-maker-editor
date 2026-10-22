package org.example.project.ui.layouteditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import org.example.project.ui.filter.PhotoFilters
import org.example.project.ui.filter.toColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.example.project.ui.collage.SlotGeometry
import org.example.project.ui.collage.computeSlotGeometries
import org.example.project.ui.collage.geom.PointF
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.drawImageCropped
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.frame.drawFrame
import kotlin.math.roundToInt

/**
 * The slots of [template] on a [canvasW] x [canvasH] canvas, [marginPx] in from its edges and
 * [spacePx] (each side) apart, with [cornerPx] corners. Every geometry is in canvas pixels, so the
 * collage editor's [org.example.project.ui.collage.hitTestSlot] works on them as they are.
 *
 * The collage editor's slot maths (the LAS port) insets a slot's outer edges by twice the space, so
 * there the space is the outer margin too. Here the margin is its own: the slots are laid out over
 * the margin's inner area grown by that double space on each side, which puts the outer edges
 * exactly on the margin and leaves the gaps between slots as they were.
 */
internal fun computeLayoutSlots(
    template: TemplateItem,
    canvasW: Float,
    canvasH: Float,
    marginPx: Float,
    spacePx: Float,
    cornerPx: Float,
): List<SlotGeometry> {
    val grow = 2f * spacePx
    val areaW = (canvasW - 2f * marginPx).coerceAtLeast(1f) + 2f * grow
    val areaH = (canvasH - 2f * marginPx).coerceAtLeast(1f) + 2f * grow
    val shift = marginPx - grow
    return computeSlotGeometries(template, areaW, areaH, spacePx, cornerPx).map { g ->
        SlotGeometry(
            item = g.item,
            leftPx = g.leftPx + shift,
            topPx = g.topPx + shift,
            wPx = g.wPx,
            hPx = g.hPx,
            clipPath = g.clipPath,
            touchPolygon = g.touchPolygon.map { PointF(it.x + shift, it.y + shift) },
        )
    }
}

/** [computeLayoutSlots] for [edit] on a [canvasW] x [canvasH] canvas, its measures turned into pixels. */
internal fun LayoutEdit.slots(canvasW: Float, canvasH: Float): List<SlotGeometry> {
    val unit = layoutUnitPx(canvasW, canvasH)
    return computeLayoutSlots(template, canvasW, canvasH, margin * unit, space * unit, corner * unit)
}

/**
 * The server pictures [LayoutEdit.backgroundArt] and [LayoutEdit.frameArt] point at, loaded by
 * whoever draws (the preview as they come in, the ViewModel before a bake). Each must be a bitmap
 * this app made (`copyBitmap`), since it is redrawn through scaling draws. A picture not in yet is
 * simply left out.
 */
internal data class LayoutArtImages(
    val background: ImageBitmap? = null,
    val frame: ImageBitmap? = null,
    val transition: LayoutArtTransition = LayoutArtTransition(),
)

/**
 * How far the preview is through putting a new background or frame picture on: the one before is
 * drawn under it (null for the colour / no frame) and the new one over it at [backgroundProgress] /
 * [frameProgress], the frame also settling from a little zoomed in. The bake is never mid-way.
 */
internal data class LayoutArtTransition(
    val previousBackground: ImageBitmap? = null,
    val backgroundProgress: Float = 1f,
    val previousFrame: ImageBitmap? = null,
    val frameProgress: Float = 1f,
)

/** How much larger a frame starts as it is put on, settling to the canvas's size. */
internal const val FrameSettleZoom = 0.06f

/**
 * Draws [edit] (without its stickers and texts, which sit above it) on this scope, whose size is the
 * canvas: the background (colour, or [art]'s background picture cropped to the canvas, at its
 * opacity), the slots' shadows, the photos clipped to their slots, the slots' outlines, the frame
 * and [art]'s frame picture stretched over it all. Shared by the preview and the bake, so they match.
 *
 * [emptySlot] fills a slot with no photo; the bake passes transparent, leaving the background.
 */
internal fun DrawScope.drawLayout(edit: LayoutEdit, slots: List<SlotGeometry>, emptySlot: Color, art: LayoutArtImages = LayoutArtImages()) {
    val unit = layoutUnitPx(size.width, size.height)
    val backgroundAlpha = edit.backgroundOpacity / LayoutBackgroundOpacityRange.endInclusive
    val canvasSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    // The picture if there is one (the bake only has one when the edit does), else the colour.
    fun drawBackground(picture: ImageBitmap?, alpha: Float) {
        if (picture != null) drawImageCropped(picture, IntOffset.Zero, canvasSize, alpha = alpha) else drawRect(edit.background, alpha = alpha)
    }
    val backgroundProgress = art.transition.backgroundProgress
    if (backgroundProgress < 1f) drawBackground(art.transition.previousBackground, backgroundAlpha)
    drawBackground(art.background, backgroundAlpha * backgroundProgress)

    if (edit.shadow > 0f) {
        for (g in slots) drawSlotShadow(g, edit.shadow / LayoutShadowRange.endInclusive, unit)
    }

    val filter = edit.photoColorFilter()

    for (g in slots) {
        translate(g.leftPx, g.topPx) {
            clipPath(g.clipPath) {
                val photo = edit.photos[g.item.index]
                if (photo != null) {
                    drawLayoutPhoto(photo, g.wPx, g.hPx, filter)
                } else if (emptySlot.alpha > 0f) {
                    drawRect(emptySlot, size = Size(g.wPx, g.hPx))
                }
            }
        }
    }

    val borderPx = edit.borderWidth * unit
    if (borderPx > 0f && edit.borderOpacity > 0f) {
        val color = edit.borderColor.copy(alpha = edit.borderColor.alpha * edit.borderOpacity / LayoutBorderOpacityRange.endInclusive)
        for (g in slots) {
            translate(g.leftPx, g.topPx) {
                // Clipped, a stroke twice as wide leaves exactly its width inside the slot.
                clipPath(g.clipPath) { drawPath(g.clipPath, color, style = Stroke(width = borderPx * 2f)) }
            }
        }
    }

    drawFrame(
        imageOffset = Offset.Zero,
        imageSize = size,
        color = edit.frameColor,
        widthPx = edit.frameWidth * unit,
        cornerRadiusPx = edit.frameCorner * unit,
    )

    val frameProgress = art.transition.frameProgress
    if (frameProgress < 1f) art.transition.previousFrame?.let { drawImageScaled(it, IntOffset.Zero, canvasSize, alpha = 1f - frameProgress) }
    art.frame?.let { frame ->
        scale(1f + FrameSettleZoom * (1f - frameProgress)) { drawImageScaled(frame, IntOffset.Zero, canvasSize, alpha = frameProgress) }
    }
}

/**
 * A soft shadow under a slot, [strength] 0..1. There is no blur that draws the same on Android and
 * iOS, so the softness is stacked translucent outlines of the slot's shape, widest first: their
 * alpha adds up toward the slot's edge and fades out away from it. The photo drawn over the slot
 * hides the part under it.
 */
private fun DrawScope.drawSlotShadow(g: SlotGeometry, strength: Float, unit: Float) {
    val spread = ShadowMaxSpreadUnits * unit * strength
    translate(g.leftPx, g.topPx + ShadowMaxDropUnits * unit * strength) {
        for (step in ShadowSteps downTo 1) {
            drawPath(
                path = g.clipPath,
                color = Color.Black.copy(alpha = ShadowStepAlpha * strength),
                style = Stroke(width = spread * 2f * step / ShadowSteps, join = StrokeJoin.Round),
            )
        }
        drawPath(g.clipPath, Color.Black.copy(alpha = ShadowStepAlpha * strength))
    }
}

private const val ShadowSteps = 6
private const val ShadowStepAlpha = 0.07f
private const val ShadowMaxSpreadUnits = 14f
private const val ShadowMaxDropUnits = 4f

/** The layout's filter over every photo, or null with none (or None). */
internal fun LayoutEdit.photoColorFilter(): ColorFilter? {
    val label = filterLabel ?: return null
    return PhotoFilters.firstOrNull { it.label == label }?.toColorFilter(filterIntensity)
}

/**
 * Draws [photo] in a slot-local [slotW] x [slotH] box: leant about the slot's centre (Transform),
 * then about its own (re-clamped) centre turned, mirrored and sized by [layoutPhotoSize], through
 * [colorFilter]. The caller clips to the slot.
 */
internal fun DrawScope.drawLayoutPhoto(photo: LayoutPhoto, slotW: Float, slotH: Float, colorFilter: ColorFilter? = null) {
    val image = photo.image
    val imageW = image.width.toFloat()
    val imageH = image.height.toFloat()
    val t = clampLayoutTransform(photo.transform, imageW, imageH, slotW, slotH, photo.shape)
    val (drawnW, drawnH) = layoutPhotoSize(imageW, imageH, slotW, slotH, photo.shape, t.scale)
    val center = Offset(slotW / 2f + t.offsetX * slotW, slotH / 2f + t.offsetY * slotH)
    val tilt = layoutTiltMatrix(photo, slotW, slotH)
    withTransform({
        if (tilt != null) {
            translate(slotW / 2f, slotH / 2f)
            transform(tilt)
            translate(-slotW / 2f, -slotH / 2f)
        }
        rotate(photo.rotationDegrees, pivot = center)
        scale(scaleX = if (photo.flipX) -1f else 1f, scaleY = if (photo.flipY) -1f else 1f, pivot = center)
    }) {
        drawImageScaled(
            image = image,
            dstOffset = IntOffset((center.x - drawnW / 2f).roundToInt(), (center.y - drawnH / 2f).roundToInt()),
            dstSize = IntSize(drawnW.roundToInt().coerceAtLeast(1), drawnH.roundToInt().coerceAtLeast(1)),
            colorFilter = colorFilter,
        )
    }
}

/**
 * The Transform lean as a perspective matrix about a [slotW] x [slotH] slot's centre, as the Stickers
 * tool's (`stickerTiltMatrix`): a positive horizontal lean shrinks the right side as if leaning away,
 * the left grows to match. Null when flat.
 */
internal fun layoutTiltMatrix(photo: LayoutPhoto, slotW: Float, slotH: Float): Matrix? {
    val tiltX = layoutTiltAmount(photo.tiltX)
    val tiltY = layoutTiltAmount(photo.tiltY)
    if ((tiltX == 0f && tiltY == 0f) || slotW <= 0f || slotH <= 0f) return null
    return Matrix().apply {
        // The 4×4 matrix's perspective terms for x and y: w = 1 + px·x + py·y.
        values[3] = tiltX / (slotW / 2f)
        values[7] = tiltY / (slotH / 2f)
    }
}

/**
 * The preview's guides over [drawLayout]: a dashed [color] outline round every empty slot, as a
 * place to tap, and a solid one round the [selectedIndex] slot.
 */
internal fun DrawScope.drawLayoutGuides(
    slots: List<SlotGeometry>,
    filledSlots: Set<Int>,
    selectedIndex: Int?,
    color: Color,
) {
    val dashed = Stroke(
        width = 1.5f * density,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * density, 4f * density)),
    )
    for (g in slots) {
        translate(g.leftPx, g.topPx) {
            when {
                g.item.index == selectedIndex -> drawPath(g.clipPath, color, style = Stroke(width = 3f * density))
                g.item.index !in filledSlots -> drawPath(g.clipPath, color.copy(alpha = 0.8f), style = dashed)
            }
        }
    }
}
