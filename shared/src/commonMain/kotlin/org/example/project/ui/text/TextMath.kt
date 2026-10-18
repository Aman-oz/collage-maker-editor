package org.example.project.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** How close (in degrees) a rotation must get to a quarter turn before it snaps onto it. */
internal const val TextRotationSnapDegrees = 4f

/**
 * Rotates [vector] clockwise by [degrees] on screen (y points down), the same direction as
 * `graphicsLayer { rotationZ }`. Used to turn gesture deltas reported in the rotated text box's
 * own coordinates back into photo coordinates.
 */
internal fun rotateVector(vector: Offset, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    val c = cos(radians)
    val s = sin(radians)
    return Offset(vector.x * c - vector.y * s, vector.x * s + vector.y * c)
}

/** Screen angle of [vector] in degrees, clockwise from the +x axis (y points down). */
internal fun vectorAngleDegrees(vector: Offset): Float =
    atan2(vector.y, vector.x) * 180f / PI.toFloat()

/**
 * Snaps [degrees] onto the nearest multiple of 90° when within [TextRotationSnapDegrees] of it, so
 * text is easy to set back exactly straight (or exactly sideways); otherwise returns it unchanged.
 */
internal fun snapTextRotation(degrees: Float): Float {
    val nearestQuarter = (degrees / 90f).roundToInt() * 90f
    return if (abs(degrees - nearestQuarter) <= TextRotationSnapDegrees) nearestQuarter else degrees
}

/**
 * A label's background plate, sized from the label's measured text height so it keeps its
 * proportions however the label is scaled: how far it reaches past the text sideways and
 * vertically, and its corner radius. See [drawTextPlate].
 */
internal const val TextBackgroundPadXFraction = 0.22f
internal const val TextBackgroundPadYFraction = 0.04f
internal const val TextBackgroundCornerFraction = 0.2f

/** Where the Color panel's Corner slider (0–100) starts: about [TextBackgroundCornerFraction]. */
internal const val DefaultTextPlateCorner = 40f

/** The Corner slider's 0–100 as the radius of a plate [plateHeight] tall: 100 rounds its ends fully. */
internal fun textPlateCornerRadius(corner: Float, plateHeight: Float): Float =
    corner.coerceIn(0f, 100f) / 100f * plateHeight / 2f

/**
 * Where the Padding sliders start: about the plate the freestyle labels still get from
 * [TextBackgroundPadXFraction] / [TextBackgroundPadYFraction] of a line's height.
 */
internal const val DefaultTextPaddingX = 13f
internal const val DefaultTextPaddingY = 3f

/** The padding (em of the font size) at a Padding slider's end. */
internal const val MaxTextPaddingEm = 2f

/** A Padding slider's 0–100 in em of the font size. */
internal fun textPaddingEm(slider: Float): Float = slider.coerceIn(0f, 100f) / 100f * MaxTextPaddingEm

/**
 * The plate around words laid out [wordsSize] big at the origin, in their pixels: reaching past them
 * on each side by that side's padding, where [emPx] is the font size in those pixels.
 */
internal fun textPlateRect(paddingLeft: Float, paddingTop: Float, paddingRight: Float, paddingBottom: Float, wordsSize: Size, emPx: Float): Rect =
    Rect(
        left = -textPaddingEm(paddingLeft) * emPx,
        top = -textPaddingEm(paddingTop) * emPx,
        right = wordsSize.width + textPaddingEm(paddingRight) * emPx,
        bottom = wordsSize.height + textPaddingEm(paddingBottom) * emPx,
    )

/** A text outline's width as a fraction of its font size. Half of it falls inside the letters. */
internal const val TextStrokeWidthFraction = 0.12f

/** The Curve slider's middle, where the words run straight. Below it they arch (∩), above it they sag (∪). */
internal const val TextCurveStraight = 50f

/** How much of a circle the widest line wraps at either end of the Curve slider. */
internal const val MaxTextCurveRadians = 1.8f * PI.toFloat()

/** Slider values this close to [TextCurveStraight] stay straight, so a near-zero curvature's huge radius never reaches the maths. */
private const val TextCurveDeadZone = 0.5f

/**
 * The Curve slider's 0–100 as a signed curvature (1 / radius, in the layout's pixels) for words
 * whose widest line is [widestLinePx] wide: that line wraps [MaxTextCurveRadians] of a circle at the
 * slider's ends. Positive arches the words, negative sags them, 0 is straight.
 */
internal fun textCurvature(curve: Float, widestLinePx: Float): Float {
    val bend = (TextCurveStraight - curve.coerceIn(0f, 100f)) / TextCurveStraight
    if (widestLinePx <= 0f || abs(curve - TextCurveStraight) < TextCurveDeadZone) return 0f
    return bend * MaxTextCurveRadians / widestLinePx
}

/**
 * Where [point] of a straight text lands once the text is bent onto concentric arcs with [curvature]
 * around [center]: the horizontal line through [center] becomes an arc of radius 1 / [curvature]
 * that still passes through [center], its length kept, and every other line an arc around the same
 * circle's centre, farther out the farther it was above. So distances along the middle line don't
 * change and the lines stay as far apart as they were.
 */
internal fun bendTextPoint(point: Offset, center: Offset, curvature: Float): Offset {
    if (curvature == 0f) return point
    val angle = (point.x - center.x) * curvature
    val radius = 1f / curvature - (point.y - center.y)
    val pivotY = center.y + 1f / curvature
    return Offset(center.x + radius * sin(angle), pivotY - radius * cos(angle))
}

/** How far (degrees, clockwise) a glyph at [x] turns on the arc of [bendTextPoint]. */
internal fun bendTextAngleDegrees(x: Float, centerX: Float, curvature: Float): Float =
    (x - centerX) * curvature * 180f / PI.toFloat()

/**
 * [text] in [case]. Cased a character at a time, so the result is exactly as long as [text] and the
 * text field can show it while typing with an identity offset mapping. Capitalize words starts each
 * run of letters and digits (an apostrophe doesn't end one: "don't" → "Don't") and lowers the rest.
 */
internal fun applyTextCase(text: String, case: TextCase): String = when (case) {
    TextCase.None -> text
    TextCase.Upper -> CharArray(text.length) { text[it].uppercaseChar() }.concatToString()
    TextCase.Lower -> CharArray(text.length) { text[it].lowercaseChar() }.concatToString()
    TextCase.Title -> buildString(text.length) {
        var wordStart = true
        for (char in text) {
            append(if (wordStart) char.titlecaseChar() else char.lowercaseChar())
            wordStart = !char.isLetterOrDigit() && char != '\''
        }
    }
}

/** The Spacing sliders' middle: the font's own letter and line spacing. */
internal const val TextSpacingNormal = 50f

/** The letter spacing (em) at the Letter spacing slider's ends: a little tighter, a lot looser. */
internal const val MinLetterSpacingEm = -0.1f
internal const val MaxLetterSpacingEm = 0.6f

/** The line height (em) at the Line spacing slider's ends and, for the slider's middle, about a font's own. */
internal const val MinLineHeightEm = 0.8f
internal const val NormalLineHeightEm = 1.2f
internal const val MaxLineHeightEm = 2.6f

/**
 * The Letter spacing slider's 0–100 as em added between letters: 0 at [TextSpacingNormal], down to
 * [MinLetterSpacingEm] below it and up to [MaxLetterSpacingEm] above it. The two halves are mapped
 * apart because letters run into each other long before they run far apart.
 */
internal fun letterSpacingEm(slider: Float): Float = spacingAround(slider, MinLetterSpacingEm, 0f, MaxLetterSpacingEm)

/**
 * The Line spacing slider's 0–100 as a line height in em, or null at [TextSpacingNormal] for the
 * font's own line height (fonts differ, so no one em value would leave every font as it was). Either
 * side runs from about a font's own, [NormalLineHeightEm], to [MinLineHeightEm] / [MaxLineHeightEm].
 */
internal fun lineHeightEm(slider: Float): Float? =
    if (slider == TextSpacingNormal) null else spacingAround(slider, MinLineHeightEm, NormalLineHeightEm, MaxLineHeightEm)

private fun spacingAround(slider: Float, min: Float, normal: Float, max: Float): Float {
    val value = slider.coerceIn(0f, 100f)
    return if (value <= TextSpacingNormal) {
        min + (normal - min) * value / TextSpacingNormal
    } else {
        normal + (max - normal) * (value - TextSpacingNormal) / (100f - TextSpacingNormal)
    }
}

/** Black, a text shadow's first colour. */
internal const val DefaultShadowArgb: Int = -0x1000000

/** The opacity a text's shadow starts at when the Shadow panel opens on a text without one. */
internal const val DefaultShadowOpacity = 0.5f

/** A shadow's first offset on each axis (down and to the right), in em. */
internal const val DefaultShadowOffsetEm = 0.06f

/** How far one tap of a Distance arrow moves the shadow, and the farthest it goes on either axis, in em. */
internal const val ShadowNudgeEm = 0.02f
internal const val MaxShadowOffsetEm = 0.5f

/** The Blur slider's first value, and the blur radius (em) at its end. */
internal const val DefaultShadowBlur = 30f
internal const val MaxShadowBlurEm = 0.4f

/** The Blur slider's 0–100 as a blur radius in em. */
internal fun shadowBlurEm(slider: Float): Float = slider.coerceIn(0f, 100f) / 100f * MaxShadowBlurEm

/** The shadow offset ([dx], [dy]) moved by [step], kept within [MaxShadowOffsetEm] on each axis. */
internal fun nudgeShadowOffset(dx: Float, dy: Float, step: Offset): Offset = Offset(
    (dx + step.x).coerceIn(-MaxShadowOffsetEm, MaxShadowOffsetEm),
    (dy + step.y).coerceIn(-MaxShadowOffsetEm, MaxShadowOffsetEm),
)

/**
 * The Angle slider runs 0–100 over 0°–[MaxShadowAngleDegrees], clockwise from pointing right (y down),
 * stopping short of 360°: that would read back as 0° and send the thumb to the slider's start.
 */
internal const val MaxShadowAngleDegrees = 359f

internal fun shadowAngleDegrees(slider: Float): Float = slider.coerceIn(0f, 100f) / 100f * MaxShadowAngleDegrees

/** The Angle slider's value for a shadow offset ([dx], [dy]); the angle a Distance arrow left it at. */
internal fun shadowAngleSlider(dx: Float, dy: Float): Float {
    if (dx == 0f && dy == 0f) return 0f
    val degrees = (vectorAngleDegrees(Offset(dx, dy)) + 360f) % 360f
    return (degrees / MaxShadowAngleDegrees * 100f).coerceIn(0f, 100f)
}

/**
 * The shadow offset ([dx], [dy]) turned to the Angle slider's [slider], its distance kept (or the
 * first offset's, if the shadow sits right under the words and has no direction to turn).
 */
internal fun shadowOffsetAtAngle(dx: Float, dy: Float, slider: Float): Offset {
    val distance = hypot(dx, dy).takeIf { it > 0f } ?: hypot(DefaultShadowOffsetEm, DefaultShadowOffsetEm)
    val radians = shadowAngleDegrees(slider) * PI.toFloat() / 180f
    return Offset(distance * cos(radians), distance * sin(radians))
}

/** Half the size of the upright box around a [width] × [height] box turned by [degrees]. */
internal fun rotatedHalfSize(width: Float, height: Float, degrees: Float): Size {
    val radians = degrees * PI.toFloat() / 180f
    val c = abs(cos(radians))
    val sn = abs(sin(radians))
    return Size(width / 2f * c + height / 2f * sn, width / 2f * sn + height / 2f * c)
}
