package org.example.project.ui.collage

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Bakes [state] into a square [outputSize]x[outputSize] bitmap. [previewSizePx] is the on-screen
 * canvas's pixel size and [spacePxPreview]/[cornerPxPreview] are the border/corner gaps in that
 * preview's pixels; both are scaled up to the output resolution the same way every other tool scales
 * a preview-relative measure up to the source's full resolution.
 */
internal fun bakeCollage(
    state: CollageState,
    previewSizePx: Float,
    spacePxPreview: Float,
    cornerPxPreview: Float,
    outputSize: Int = 1080,
): ImageBitmap {
    val output = ImageBitmap(outputSize, outputSize)
    val canvas = Canvas(output)
    val size = Size(outputSize.toFloat(), outputSize.toFloat())
    val scale = if (previewSizePx > 0f) outputSize / previewSizePx else 1f

    val geometries = computeSlotGeometries(
        template = state.template,
        canvasW = outputSize.toFloat(),
        canvasH = outputSize.toFloat(),
        spacePx = spacePxPreview * scale,
        cornerPx = cornerPxPreview * scale,
    )

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawCollage(
            geometries = geometries,
            images = state.images,
            background = state.backgroundColor,
            canvasW = outputSize.toFloat(),
            canvasH = outputSize.toFloat(),
            emptySlotColor = Color.White,
        )
    }
    return output
}
