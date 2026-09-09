package org.example.project.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Renders [source] with [preset] composited on top at [intensity] (0f..1f), using the exact same
 * [OverlayPreset.draw] lambda the live preview uses — so the baked result matches what was shown.
 */
internal fun bakeOverlay(source: ImageBitmap, preset: OverlayPreset, intensity: Float): ImageBitmap {
    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())

    val size = Size(source.width.toFloat(), source.height.toFloat())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        preset.draw(this, intensity, preset.naturalBlendMode)
    }

    return output
}
