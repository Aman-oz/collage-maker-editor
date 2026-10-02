package org.example.project.ui.collage

import org.example.project.ui.templates.MaxSlotZoom
import org.example.project.ui.templates.SlotTransform
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The largest `width x height` with [aspect] (width / height) that fits inside [maxW] x [maxH] —
 * how the preview sizes the canvas for the selected ratio inside the space the editor gives it.
 */
internal fun fitAspect(maxW: Float, maxH: Float, aspect: Float): Pair<Float, Float> {
    if (maxW <= 0f || maxH <= 0f || aspect <= 0f) return 0f to 0f
    return if (maxW / maxH > aspect) (maxH * aspect) to maxH else maxW to (maxW / aspect)
}

/**
 * The baked bitmap's pixel size for [aspect]: the longer side is [longSide] so a 9:16 story and a
 * 1:1 square cost about the same memory, and the shorter side follows the ratio.
 */
internal fun collageOutputSize(aspect: Float, longSide: Int = 1080): Pair<Int, Int> =
    if (aspect >= 1f) {
        longSide to (longSide / aspect).roundToInt().coerceAtLeast(1)
    } else {
        (longSide * aspect).roundToInt().coerceAtLeast(1) to longSide
    }

/**
 * The drawn size of an [imageW]x[imageH] photo in a [slotW]x[slotH] slot: the cover fit
 * (`ContentScale.Crop`) times the user's [scale]. Unlike the Templates editor, the photo is not
 * pre-cropped to the slot, so the part the cover fit hides along one axis stays reachable by panning
 * even at 1x.
 */
internal fun slotPhotoSize(imageW: Float, imageH: Float, slotW: Float, slotH: Float, scale: Float): Pair<Float, Float> {
    if (imageW <= 0f || imageH <= 0f) return 0f to 0f
    val cover = max(slotW / imageW, slotH / imageH) * scale
    return imageW * cover to imageH * cover
}

/**
 * Clamps a slot's [SlotTransform] so the photo always covers its [slotW]x[slotH] slot. Applied on
 * every draw as well as on every gesture, because the slot's size changes under a stored transform
 * (ratio, border width, a new layout) and a stale offset must never reveal the background.
 */
internal fun SlotTransform.clampedToSlot(imageW: Float, imageH: Float, slotW: Float, slotH: Float): SlotTransform {
    val s = scale.coerceIn(1f, MaxSlotZoom)
    if (slotW <= 0f || slotH <= 0f) return SlotTransform(scale = s)
    val (drawnW, drawnH) = slotPhotoSize(imageW, imageH, slotW, slotH, s)
    // Offsets are fractions of the slot size; the photo can shift by half its overhang per side.
    val limitX = ((drawnW - slotW) / 2f / slotW).coerceAtLeast(0f)
    val limitY = ((drawnH - slotH) / 2f / slotH).coerceAtLeast(0f)
    return SlotTransform(
        scale = s,
        offsetX = offsetX.coerceIn(-limitX, limitX),
        offsetY = offsetY.coerceIn(-limitY, limitY),
    )
}

/** Applies one pinch/drag step ([panX]/[panY] px, [zoom] factor) to a collage slot photo, then clamps it. */
internal fun SlotTransform.applyCollageGesture(
    panX: Float,
    panY: Float,
    zoom: Float,
    imageW: Float,
    imageH: Float,
    slotW: Float,
    slotH: Float,
): SlotTransform = SlotTransform(
    scale = scale * zoom,
    offsetX = offsetX + panX / slotW.coerceAtLeast(1f),
    offsetY = offsetY + panY / slotH.coerceAtLeast(1f),
).clampedToSlot(imageW, imageH, slotW, slotH)

/** The border widths that make a collage on a free layout a premium one (see [isPremiumCollageEdit]). */
internal val PremiumCollageSpaceRange = 2f..5f

/** The canvas ratio that does the same. */
internal val PremiumCollageRatio = CollageRatio.Portrait

/**
 * Whether a collage on a *free* layout uses something premium: a border width ([space]) inside
 * [PremiumCollageSpaceRange], or the 4:5 canvas. The default border width lies outside that range,
 * so an untouched collage is free.
 */
internal fun isPremiumCollageEdit(space: Float, ratio: CollageRatio): Boolean =
    space in PremiumCollageSpaceRange || ratio == PremiumCollageRatio
