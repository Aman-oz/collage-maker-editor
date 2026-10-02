package org.example.project.ui.text

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
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
