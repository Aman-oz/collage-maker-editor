package org.example.project.ui.ratio

import kotlin.math.min

/**
 * Padding is expressed as a percent of the reframed photo's shorter side, so the on-screen preview
 * and the full-resolution bake pad by the same proportion no matter how much the preview is scaled
 * down, and every side gets the same border width.
 */
internal val PaddingPercentRange = 0f..20f

/** Padding in pixels on each side of a [width]×[height] image padded by [percent] of its shorter side. */
internal fun paddingPx(width: Int, height: Int, percent: Float): Float =
    min(width, height) * percent.coerceIn(PaddingPercentRange) / 100f
