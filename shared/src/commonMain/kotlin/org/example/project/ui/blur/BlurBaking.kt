package org.example.project.ui.blur

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.common.copyBitmap

/**
 * Renders the blurred image at full resolution, then reveals the sharp [source] back through
 * wherever the user erased. Each [BlurStroke]'s points and brush radius are expressed relative to
 * the on-screen preview (0f..1f of its width/height), matching how they were captured in
 * [BlurScreen] — scaling them by the full bitmap's own size reproduces the exact same reveal
 * shape the user saw, independent of preview vs. output resolution.
 */
internal fun bakeBlur(
    source: ImageBitmap,
    blurLevel: Int,
    strokes: List<BlurStroke>,
): ImageBitmap {
    if (blurLevel <= 0) return source

    val blurred = computeBlurredBitmap(source, blurLevel)
    if (strokes.isEmpty()) return blurred

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    val size = Size(source.width.toFloat(), source.height.toFloat())
    val paths = strokes.map { buildStrokePath(it, Offset.Zero, size) }
    val sharpCopy = copyBitmap(source)

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawBlurWithReveal(
            sharpImage = sharpCopy,
            blurredImage = blurred,
            imageOffset = Offset.Zero,
            imageSize = size,
            revealPaths = paths,
        )
    }
    return output
}
