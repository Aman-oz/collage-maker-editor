package org.example.project.ui.common

import androidx.compose.ui.graphics.Color

/** A colour as the custom colour picker edits it: [hue] in degrees (0–360), the rest 0–1. */
internal data class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    fun toColor(): Color = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
}

/**
 * The inverse of [Color.hsv], so the picker can open on the colour already in use. A grey has no
 * hue of its own and comes back as 0 (red), which is where its saturation slider then starts from.
 */
internal fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * ((green - blue) / delta)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return Hsv(
        hue = if (hue < 0f) hue + 360f else hue,
        saturation = if (max == 0f) 0f else delta / max,
        value = max,
    )
}
