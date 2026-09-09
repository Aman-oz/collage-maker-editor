package org.example.project.ui.frame

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal val FrameWidthRange = 0f..30f
internal const val FrameWidthDefault = 12f

internal val FrameCornerRadiusRange = 0f..40f
internal const val FrameCornerRadiusDefault = 18f

/**
 * Draws a rounded-rect border hugging [imageSize] (offset by [imageOffset]), inset by half its own
 * [widthPx] so the stroke sits flush with the image edge — the same call draws the on-screen
 * preview (image bounds letterboxed within a larger canvas) and the full-resolution bake (image
 * bounds filling the whole canvas, so [imageOffset] is [Offset.Zero]).
 */
internal fun DrawScope.drawFrame(
    imageOffset: Offset,
    imageSize: Size,
    color: Color,
    widthPx: Float,
    cornerRadiusPx: Float,
) {
    if (widthPx <= 0f || color.alpha <= 0f) return
    val inset = widthPx / 2f
    drawRoundRect(
        color = color,
        topLeft = imageOffset + Offset(inset, inset),
        size = Size((imageSize.width - widthPx).coerceAtLeast(0f), (imageSize.height - widthPx).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(width = widthPx),
    )
}
