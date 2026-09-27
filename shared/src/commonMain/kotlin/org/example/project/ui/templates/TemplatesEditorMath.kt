package org.example.project.ui.templates

/** Most a slot photo can be zoomed in, relative to its center-cropped fit. */
internal const val MaxSlotZoom = 5f

/**
 * The user's pinch/pan on one slot photo, on top of its center-cropped fit. [offsetX]/[offsetY] are
 * fractions of the slot's own width/height (in the slot's rotated frame), so the same transform
 * applies at preview size and at bake resolution.
 *
 * Applied as "scale about the slot center, then translate" — the order a `graphicsLayer` with the
 * default center `transformOrigin` uses, which baking mirrors.
 */
data class SlotTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

/**
 * Applies one transform-gesture step: [zoom] multiplies the scale (clamped to `1..MaxSlotZoom`), and
 * [panX]/[panY] (px) move the photo within a [slotWidth]x[slotHeight] px slot. The offset is clamped
 * so the zoomed photo always covers the slot — never revealing the background behind the frame.
 */
internal fun SlotTransform.applyGesture(
    panX: Float,
    panY: Float,
    zoom: Float,
    slotWidth: Float,
    slotHeight: Float,
): SlotTransform {
    val newScale = (scale * zoom).coerceIn(1f, MaxSlotZoom)
    // A photo scaled by s about the center overhangs the slot by (s - 1) / 2 of its size per side.
    val limit = (newScale - 1f) / 2f
    return SlotTransform(
        scale = newScale,
        offsetX = (offsetX + panX / slotWidth.coerceAtLeast(1f)).coerceIn(-limit, limit),
        offsetY = (offsetY + panY / slotHeight.coerceAtLeast(1f)).coerceIn(-limit, limit),
    )
}

