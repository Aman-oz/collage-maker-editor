package org.example.project.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt

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

internal const val ColorGridColumns = 12
internal const val ColorGridRows = 10

/** The grid's columns, left to right: blue round the wheel through red and yellow to green. */
private val ColorGridHues = listOf(200f, 218f, 245f, 275f, 312f, 355f, 10f, 25f, 36f, 50f, 60f, 90f)

/**
 * The Grid tab's colour at [row], [column]: the top row is greys from white to black, and each
 * column under it runs one hue from dark, through its full colour (the fifth of the nine), to pastel.
 */
internal fun colorGridColor(row: Int, column: Int): Color {
    if (row == 0) return Color.hsv(0f, 0f, 1f - column / (ColorGridColumns - 1f))
    val shade = row - 1
    val hue = ColorGridHues[column]
    return if (shade <= 4) {
        Color.hsv(hue, 1f, 0.3f + 0.7f * shade / 4f)
    } else {
        Color.hsv(hue, 1f - 0.21f * (shade - 4), 1f)
    }
}

/** The grid cell (row to column) showing [color], ignoring its alpha, or `null` when none does. */
internal fun colorGridCellOf(color: Color): Pair<Int, Int>? {
    for (row in 0 until ColorGridRows) {
        for (column in 0 until ColorGridColumns) {
            if (sameRgb(colorGridColor(row, column), color)) return row to column
        }
    }
    return null
}

/** Whether [a] and [b] show the same 8-bit RGB, whatever their alpha. */
internal fun sameRgb(a: Color, b: Color): Boolean =
    abs(a.red - b.red) < 0.5f / 255f + 1e-4f &&
        abs(a.green - b.green) < 0.5f / 255f + 1e-4f &&
        abs(a.blue - b.blue) < 0.5f / 255f + 1e-4f

/**
 * The Spectrum tab's colour under the fractions [x], [y] of its box: [x] is the hue, and from the
 * top the colour fills in from white to full at the middle, then darkens to black at the bottom.
 */
internal fun spectrumColor(x: Float, y: Float): Hsv {
    val hue = x.coerceIn(0f, 1f) * 360f
    val down = y.coerceIn(0f, 1f)
    return if (down <= 0.5f) Hsv(hue, down * 2f, 1f) else Hsv(hue, 1f, 1f - (down - 0.5f) * 2f)
}

/**
 * Where [spectrumColor] shows this colour, as fractions of the box. The spectrum holds only colours
 * that are full in saturation or brightness; any other lands on the nearer of its two halves, a tie
 * (black) on the darker.
 */
internal fun Hsv.spectrumPosition(): Offset {
    val x = hue / 360f
    return if (value >= 1f - 1e-3f || value > saturation) {
        Offset(x, saturation / 2f)
    } else {
        Offset(x, 0.5f + (1f - value) / 2f)
    }
}

/** The colour's RGB as six upper-case hex digits, without a `#`. */
internal fun Color.toHexRgb(): String =
    listOf(red, green, blue).joinToString("") { (it * 255f).roundToInt().toString(16).padStart(2, '0') }.uppercase()

/** Six hex digits (a leading `#` allowed) as an opaque colour, or `null` for anything else. */
internal fun parseHexRgb(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6) return null
    val rgb = digits.toLongOrNull(16) ?: return null
    return Color(0xFF000000 or rgb)
}

/** How many colours the picker keeps saved: two rows of five, less the ＋. */
internal const val MaxSavedColors = 9

/** [saved] with [argb] put first (moved there if it was already saved), keeping [MaxSavedColors]. */
internal fun addSavedColor(saved: List<Int>, argb: Int): List<Int> =
    (listOf(argb) + saved.filter { it != argb }).take(MaxSavedColors)
