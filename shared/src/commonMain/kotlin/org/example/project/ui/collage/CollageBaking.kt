package org.example.project.ui.collage

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Bakes [state] into a bitmap shaped by its [CollageState.ratio] (see [collageOutputSize]).
 * [previewWidthPx] is the on-screen canvas's pixel width and [spacePxPreview]/[cornerPxPreview] are
 * the border/corner gaps in that preview's pixels; both are scaled up to the output resolution the
 * same way every other tool scales a preview-relative measure up to the source's full resolution.
 */
internal fun bakeCollage(
    state: CollageState,
    previewWidthPx: Float,
    spacePxPreview: Float,
    cornerPxPreview: Float,
    longSide: Int = 1080,
): ImageBitmap {
    val (outW, outH) = collageOutputSize(state.ratio.aspect, longSide)
    val output = ImageBitmap(outW, outH)
    val canvas = Canvas(output)
    val size = Size(outW.toFloat(), outH.toFloat())
    val scale = if (previewWidthPx > 0f) outW / previewWidthPx else 1f

    val geometries = computeSlotGeometries(
        template = state.template,
        canvasW = size.width,
        canvasH = size.height,
        spacePx = spacePxPreview * scale,
        cornerPx = cornerPxPreview * scale,
    )

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawCollage(
            geometries = geometries,
            images = state.images,
            background = state.backgroundColor,
            canvasW = size.width,
            canvasH = size.height,
            emptySlotColor = Color.White,
        )
    }
    return output
}
