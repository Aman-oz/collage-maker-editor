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

/** The shapes Free may take: a panorama or a screenshot as the first photo would leave the slots slivers. */
internal val OriginalCanvasAspectRange = 0.5f..2f

/**
 * The canvas's width / height for a [ratio] picked in the Canvas panel. Free (`null`) follows the
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

/** A Transform slider (0–100) as the lean's perspective across the slot, -[MaxLayoutTilt]..[MaxLayoutTilt]. */
internal fun layoutTiltAmount(slider: Float): Float = (slider.coerceIn(0f, 100f) - LayoutTiltFlat) / LayoutTiltFlat * MaxLayoutTilt

/**
 * How much bigger than the slot the photo has to be before it is leant, so the lean still leaves no
 * corner of the slot bare.
 *
 * The lean is drawn about the slot's centre as `q = p / (1 + a·p)`, with `a` the perspective per px
 * (each tilt over the slot's half side). A slot corner `q` therefore shows the photo's point
 * `p = q / (1 - a·q)`, and at the worst corner `a·q` is the two tilts' sizes added up.
 */
internal fun layoutTiltCover(photo: LayoutPhotoShape): Float =
    1f / (1f - abs(layoutTiltAmount(photo.tiltX)) - abs(layoutTiltAmount(photo.tiltY)))

/**
 * The box the photo, in its own turned frame, must cover: the slot grown by [layoutTiltCover], then
 * [rotatedSlotBounds] for its turn.
 */
internal fun layoutCoverBounds(slotW: Float, slotH: Float, photo: LayoutPhotoShape): Pair<Float, Float> {
    val grow = layoutTiltCover(photo)
    return rotatedSlotBounds(slotW * grow, slotH * grow, photo.rotationDegrees)
}

/**
 * The drawn size of [photo] (an [imageW] x [imageH] picture) in a [slotW] x [slotH] slot at zoom
 * [scale]: cover-fitted to the slot's [layoutCoverBounds], so a turned or leant photo still fills
 * its slot at 1x.
 */
internal fun layoutPhotoSize(
    imageW: Float,
    imageH: Float,
    slotW: Float,
    slotH: Float,
    photo: LayoutPhotoShape,
    scale: Float,
): Pair<Float, Float> {
    if (imageW <= 0f || imageH <= 0f) return 0f to 0f
    val (boundsW, boundsH) = layoutCoverBounds(slotW, slotH, photo)
    val cover = max(boundsW / imageW, boundsH / imageH) * scale
    return imageW * cover to imageH * cover
}

/** What [layoutPhotoSize] needs of a [LayoutPhoto]; split out so the maths is testable without a bitmap. */
internal data class LayoutPhotoShape(
    val rotationDegrees: Float = 0f,
    val tiltX: Float = LayoutTiltFlat,
    val tiltY: Float = LayoutTiltFlat,
)

internal val LayoutPhoto.shape: LayoutPhotoShape get() = LayoutPhotoShape(rotationDegrees, tiltX, tiltY)

/**
 * Clamps [transform] so the photo still covers its slot: the zoom to 1..[MaxSlotZoom], and the
 * offset (fractions of the slot's width and height) to the photo's overhang past the slot's
 * [layoutCoverBounds]. The offset is clamped in the turned photo's own frame, where that overhang is
 * an upright box, and turned back.
 *
 * Applied on every draw as well as every gesture, because the slot or the photo's turn and lean
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
    val (boundsW, boundsH) = layoutCoverBounds(slotW, slotH, photo)
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
