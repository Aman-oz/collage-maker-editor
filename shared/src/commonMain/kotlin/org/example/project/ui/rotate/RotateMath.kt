package org.example.project.ui.rotate

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Fixed set of angles the rotation seekbar rests on — dragging snaps the thumb (and the image) to
 * whichever of these is nearest, rather than allowing any continuous value in between.
 */
internal val RotationStops: List<Float> = listOf(-45f, -30f, -15f, 0f, 15f, 30f, 45f)
internal const val RotationDefault = 0f

/** Snaps an arbitrary angle to the nearest value in [RotationStops]. */
internal fun nearestRotationStop(angle: Float): Float =
    RotationStops.minByOrNull { abs(it - angle) } ?: angle

/** Reduces any quarter-turn count to the 0..3 range `Rotate Left`/`Rotate Right` actually cycle through. */
internal fun normalizeQuarterTurns(quarterTurns: Int): Int = ((quarterTurns % 4) + 4) % 4

/**
 * The uniform scale needed so a [width]x[height] rectangle, rotated by [angleDegrees] around its
 * own center, still fully covers its own original bounds — otherwise the corners of the frame
 * would show empty gaps wherever the tilted rectangle no longer reaches them.
 */
internal fun coverScaleForRotation(width: Float, height: Float, angleDegrees: Float): Float {
    if (angleDegrees == 0f || width <= 0f || height <= 0f) return 1f
    val radians = angleDegrees * PI.toFloat() / 180f
    val cosTheta = abs(cos(radians))
    val sinTheta = abs(sin(radians))
    val scaleForWidth = (width * cosTheta + height * sinTheta) / width
    val scaleForHeight = (height * cosTheta + width * sinTheta) / height
    return max(scaleForWidth, scaleForHeight)
}
