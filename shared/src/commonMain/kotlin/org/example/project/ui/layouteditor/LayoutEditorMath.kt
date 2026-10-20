package org.example.project.ui.layouteditor

import org.example.project.ui.templates.MaxSlotZoom
import org.example.project.ui.templates.SlotTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The shapes Original may take: a panorama or a screenshot as the first photo would leave the slots slivers. */
internal val OriginalCanvasAspectRange = 0.5f..2f

/**
 * The canvas's width / height for a [ratio] picked in the Canvas panel. Original (`null`) follows the
 * first photo's shape ([firstPhotoWidth] x [firstPhotoHeight]), kept to [OriginalCanvasAspectRange],
 * and is square while there is no photo yet.
 */
internal fun layoutCanvasAspect(ratio: Float?, firstPhotoWidth: Int?, firstPhotoHeight: Int?): Float {
    if (ratio != null) return ratio
    if (firstPhotoWidth == null || firstPhotoHeight == null || firstPhotoWidth <= 0 || firstPhotoHeight <= 0) return 1f
    return (firstPhotoWidth.toFloat() / firstPhotoHeight).coerceIn(OriginalCanvasAspectRange)
}

internal fun LayoutEdit.canvasAspect(): Float {
    val first = photos.minByOrNull { it.key }?.value?.image
    return layoutCanvasAspect(canvas.ratio, first?.width, first?.height)
}

/** One layout unit in pixels for a [canvasW] x [canvasH] canvas: see [LayoutEdit]. */
internal fun layoutUnitPx(canvasW: Float, canvasH: Float): Float = min(canvasW, canvasH) / LayoutUnitsPerSide

/**
 * The width and height of the box a [slotW] x [slotH] slot covers once turned by [degrees] — the
 * smallest upright box in the turned photo's own frame that holds the whole slot. A photo at least
 * this big, moved no further than its overhang past it, leaves no corner of the slot bare.
 */
internal fun rotatedSlotBounds(slotW: Float, slotH: Float, degrees: Float): Pair<Float, Float> {
    val radians = degrees * PI.toFloat() / 180f
    val c = abs(cos(radians))
    val s = abs(sin(radians))
    return (slotW * c + slotH * s) to (slotW * s + slotH * c)
}

/**
 * The drawn size of [photo] (an [imageW] x [imageH] picture) in a [slotW] x [slotH] slot at zoom
 * [scale]: stretched by its width / height, then cover-fitted to the slot's [rotatedSlotBounds], so
 * a turned or narrowed photo still fills its slot at 1x.
 */
internal fun layoutPhotoSize(
    imageW: Float,
    imageH: Float,
    slotW: Float,
    slotH: Float,
    photo: LayoutPhotoShape,
    scale: Float,
): Pair<Float, Float> {
    val w = imageW * photo.stretchX
    val h = imageH * photo.stretchY
    if (w <= 0f || h <= 0f) return 0f to 0f
    val (boundsW, boundsH) = rotatedSlotBounds(slotW, slotH, photo.rotationDegrees)
    val cover = max(boundsW / w, boundsH / h) * scale
    return w * cover to h * cover
}

/** What [layoutPhotoSize] needs of a [LayoutPhoto]; split out so the maths is testable without a bitmap. */
internal data class LayoutPhotoShape(
    val rotationDegrees: Float = 0f,
    val stretchX: Float = 1f,
    val stretchY: Float = 1f,
)

internal val LayoutPhoto.shape: LayoutPhotoShape get() = LayoutPhotoShape(rotationDegrees, stretchX, stretchY)

/**
 * Clamps [transform] so the photo still covers its slot: the zoom to 1..[MaxSlotZoom], and the
 * offset (fractions of the slot's width and height) to the photo's overhang past the slot's
 * [rotatedSlotBounds]. The offset is clamped in the turned photo's own frame, where that overhang is
 * an upright box, and turned back.
 *
 * Applied on every draw as well as every gesture, because the slot or the photo's turn and stretch
 * change under a stored transform, and a stale offset must never bare the background.
 */
internal fun clampLayoutTransform(
    transform: SlotTransform,
    imageW: Float,
    imageH: Float,
    slotW: Float,
    slotH: Float,
    photo: LayoutPhotoShape,
): SlotTransform {
    val s = transform.scale.coerceIn(1f, MaxSlotZoom)
    if (slotW <= 0f || slotH <= 0f) return SlotTransform(scale = s)
    val (drawnW, drawnH) = layoutPhotoSize(imageW, imageH, slotW, slotH, photo, s)
    val (boundsW, boundsH) = rotatedSlotBounds(slotW, slotH, photo.rotationDegrees)
    val limitX = ((drawnW - boundsW) / 2f).coerceAtLeast(0f)
    val limitY = ((drawnH - boundsH) / 2f).coerceAtLeast(0f)

    val radians = photo.rotationDegrees * PI.toFloat() / 180f
    val c = cos(radians)
    val sn = sin(radians)
    val ox = transform.offsetX * slotW
    val oy = transform.offsetY * slotH
    // Into the photo's frame, clamp, and back out.
    val px = (ox * c + oy * sn).coerceIn(-limitX, limitX)
    val py = (-ox * sn + oy * c).coerceIn(-limitY, limitY)
    return SlotTransform(
        scale = s,
        offsetX = (px * c - py * sn) / slotW,
        offsetY = (px * sn + py * c) / slotH,
    )
}

/** Applies one pinch/drag step ([panX]/[panY] px, [zoom] factor) to a slot's photo, then clamps it. */
internal fun SlotTransform.applyLayoutGesture(
    panX: Float,
    panY: Float,
    zoom: Float,
    imageW: Float,
    imageH: Float,
    slotW: Float,
    slotH: Float,
    photo: LayoutPhotoShape,
): SlotTransform = clampLayoutTransform(
    SlotTransform(
        scale = scale * zoom,
        offsetX = offsetX + panX / slotW.coerceAtLeast(1f),
        offsetY = offsetY + panY / slotH.coerceAtLeast(1f),
    ),
    imageW, imageH, slotW, slotH, photo,
)

/** A picked photo's longest side once loaded: enough for the bake, light enough to filter. */
internal const val LayoutPhotoMaxSide = 2048

/** The size to load an [width] x [height] photo at so its longer side is at most [maxSide]. */
internal fun layoutPhotoLoadSize(width: Int, height: Int, maxSide: Int = LayoutPhotoMaxSide): Pair<Int, Int> {
    val longer = max(width, height)
    if (longer <= maxSide || longer <= 0) return width to height
    val scale = maxSide.toFloat() / longer
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
