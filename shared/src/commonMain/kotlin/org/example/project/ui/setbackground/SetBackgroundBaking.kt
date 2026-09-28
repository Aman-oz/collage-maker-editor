package org.example.project.ui.setbackground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.example.project.ui.common.drawImageCropped
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.drawFreestyleLayer

/** What fills the transparent areas behind the cut-out. */
sealed interface Backdrop {
    data class Solid(val color: Color) : Backdrop

    data class Gradient(val colors: List<Color>) : Backdrop

    /** A photo the user picked, center-cropped to fill. Must be a bitmap this app created (`copyBitmap`). */
    data class Photo(val image: ImageBitmap) : Backdrop
}

/**
 * Everything Set Background edits, as one undo step: the [backdrop] and the text/sticker [layers]
 * over the cut-out (freestyle layers, positioned as fractions of the photo).
 */
data class SetBackgroundEdit(
    val backdrop: Backdrop = Backdrop.Solid(Color.White),
    val layers: List<FreestyleLayer> = emptyList(),
)

/** Fills this scope with [backdrop]. Shared by the live preview and [bakeBackground] so they match. */
internal fun DrawScope.drawBackdrop(backdrop: Backdrop) {
    when (backdrop) {
        is Backdrop.Solid -> drawRect(backdrop.color)
        is Backdrop.Gradient -> drawRect(Brush.linearGradient(backdrop.colors, start = Offset.Zero, end = Offset(size.width, size.height)))
        is Backdrop.Photo -> drawImageCropped(backdrop.image, IntOffset.Zero, size.toIntSize())
    }
}

/** [backdrop] with [cutOut] over it. The preview draws the layers above this as composables. */
internal fun DrawScope.drawComposite(cutOut: ImageBitmap, backdrop: Backdrop) {
    drawBackdrop(backdrop)
    drawImageScaled(cutOut, IntOffset.Zero, size.toIntSize())
}

/**
 * Flattens [cutOut] onto [edit]'s backdrop at full resolution, then draws its layers on top. The
 * export path encodes JPEG, which has no alpha, so this is where the transparency finally goes.
 *
 * The layers were sized against the on-screen photo ([previewWidthPx] wide at [previewDensity]),
 * so their dp/sp are converted to output pixels by the same ratio, as `bakeFreestyle` does: a label
 * keeps its size relative to the photo.
 */
internal fun bakeBackground(
    cutOut: ImageBitmap,
    edit: SetBackgroundEdit,
    textMeasurer: TextMeasurer,
    previewWidthPx: Float,
    previewDensity: Float,
): ImageBitmap {
    val output = ImageBitmap(cutOut.width, cutOut.height)
    val size = Size(cutOut.width.toFloat(), cutOut.height.toFloat())
    val dpToOutputPx = previewDensity * (if (previewWidthPx > 0f) cutOut.width / previewWidthPx else 1f)
    val textDensity = Density(dpToOutputPx)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output), size) {
        drawComposite(cutOut, edit.backdrop)
        for (layer in edit.layers) {
            drawFreestyleLayer(layer, textMeasurer, textDensity, size, dpToOutputPx)
        }
    }
    return output
}

private fun Size.toIntSize() = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
