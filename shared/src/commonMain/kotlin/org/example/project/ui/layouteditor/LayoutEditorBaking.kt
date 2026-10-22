package org.example.project.ui.layouteditor

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.collage.collageOutputSize
import org.example.project.ui.freestyle.drawFreestyleLayer

/** The baked picture's longer side. */
internal const val LayoutBakeLongSide = 1440

/**
 * Renders [edit] with its stickers into a bitmap shaped like its canvas (see [collageOutputSize]).
 * Empty slots are left to the background.
 *
 * Everything but the stickers is measured in layout units, which scale with the canvas by
 * themselves. The stickers are sized in dp against the on-screen canvas ([previewWidthPx] wide at
 * [previewDensity]), so their dp turn into output pixels by the same ratio, as `bakeFreestyle` does.
 *
 * [art] holds the background and frame pictures (see [LayoutArtImages]); the caller loads them,
 * since this doesn't suspend.
 */
internal fun bakeLayout(
    edit: LayoutEdit,
    textMeasurer: TextMeasurer,
    previewWidthPx: Float,
    previewDensity: Float,
    longSide: Int = LayoutBakeLongSide,
    art: LayoutArtImages = LayoutArtImages(),
): ImageBitmap {
    val (outW, outH) = collageOutputSize(edit.canvasAspect(), longSide)
    val output = ImageBitmap(outW, outH)
    val size = Size(outW.toFloat(), outH.toFloat())
    val dpToOutputPx = previewDensity * (if (previewWidthPx > 0f) outW / previewWidthPx else 1f)
    val slots = edit.slots(size.width, size.height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output), size) {
        drawLayout(edit, slots, emptySlot = Color.Transparent, art = art)
        for (layer in edit.layers) {
            drawFreestyleLayer(layer, textMeasurer, Density(dpToOutputPx), size, dpToOutputPx)
        }
    }
    return output
}
