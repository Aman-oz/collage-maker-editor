package org.example.project.ui.pip

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.roundToInt

/** Blur level (see `computeBlurredBitmap`) of the backdrop photo; strong, like the LAS app's. */
internal const val PipBackdropBlurLevel = 8

/** Longest side the backdrop is blurred at. It is blurred anyway, so more pixels add nothing. */
internal const val PipBackdropMaxSide = 1080

/** Width of the baked image; its height follows the frame's aspect ratio. */
internal const val PipOutputWidth = 1080

/**
 * One slot's window as `0f..1f` fractions of the whole frame, so it lays out the same at preview
 * size and at bake resolution.
 */
internal data class PipSlotRect(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun contains(point: Offset): Boolean = point.x in left..right && point.y in top..bottom

    /** [point] (a fraction of the frame) as a fraction of this rect. */
    fun toLocal(point: Offset): Offset = Offset(
        x = if (width > 0f) (point.x - left) / width else 0f,
        y = if (height > 0f) (point.y - top) / height else 0f,
    )
}

/**
 * This slot's window in a [frameWidth]x[frameHeight] frame. As in the LAS app's `PhotoLayout`, the
 * mask is placed at its natural pixel size with its top-left corner at ([PipSlot.x], [PipSlot.y]).
 */
internal fun PipSlot.slotRect(maskWidth: Int, maskHeight: Int, frameWidth: Int, frameHeight: Int): PipSlotRect {
    val fw = frameWidth.coerceAtLeast(1).toFloat()
    val fh = frameHeight.coerceAtLeast(1).toFloat()
    return PipSlotRect(left = x / fw, top = y / fh, width = maskWidth / fw, height = maskHeight / fh)
}

/**
 * The slot under [point] (a fraction of the frame), or null. Slots are drawn in list order, so the
 * last one wins where they overlap. A slot's rect is only its mask's bounding box, so a slot whose
 * mask is transparent at the point ([isMaskOpaque], given the point as a fraction of that rect)
 * lets it through to the ones below.
 */
internal fun slotIndexAt(
    point: Offset,
    rects: List<PipSlotRect>,
    isMaskOpaque: (slotIndex: Int, local: Offset) -> Boolean,
): Int? {
    for (index in rects.indices.reversed()) {
        val rect = rects[index]
        if (rect.contains(point) && isMaskOpaque(index, rect.toLocal(point))) return index
    }
    return null
}

/**
 * The size a [width]x[height] photo is downscaled to before blurring it into the backdrop: at most
 * [PipBackdropMaxSide] on its longest side, never upscaled.
 */
internal fun backdropWorkingSize(width: Int, height: Int): Pair<Int, Int> {
    val longSide = max(width, height)
    if (longSide <= PipBackdropMaxSide) return width to height
    val scale = PipBackdropMaxSide.toFloat() / longSide
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

/** The baked image's size for a [frameWidth]x[frameHeight] frame: [PipOutputWidth] wide, same aspect. */
internal fun pipOutputSize(frameWidth: Int, frameHeight: Int): Pair<Int, Int> {
    val aspect = frameWidth.coerceAtLeast(1).toFloat() / frameHeight.coerceAtLeast(1)
    return PipOutputWidth to (PipOutputWidth / aspect).roundToInt().coerceAtLeast(1)
}
