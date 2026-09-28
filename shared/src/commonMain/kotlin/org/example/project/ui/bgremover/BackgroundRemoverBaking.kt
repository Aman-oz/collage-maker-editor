package org.example.project.ui.bgremover

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.example.project.ui.common.MaskStroke
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.imageBitmapFromArgb
import org.example.project.ui.common.strokeToPath

/** One undoable step of the background remover, replayed in order over the original photo. */
internal sealed interface EraseOp {

    /** A brush stroke that either cuts the photo away or, with [restore], paints the original back. */
    data class Brush(val stroke: MaskStroke, val restore: Boolean) : EraseOp

    /**
     * An Auto (magic) tap: the colour region in [mask] (alpha only, any size — it is stretched over
     * the photo) is cut away or, with [restore], painted back from the original.
     */
    data class MagicRegion(val mask: ImageBitmap, val restore: Boolean) : EraseOp
}

/**
 * Draws [original] with every op in [ops] applied, into `Offset.Zero`/[imageSize]. The erase ops use
 * `BlendMode.Clear`/`DstOut`, which wipe whatever is beneath too, so the caller must draw this into
 * its own layer (an offscreen `graphicsLayer` on screen, a fresh bitmap in [bakeCutOut]).
 * [original] must be a bitmap this app created (see `copyBitmap`).
 */
internal fun DrawScope.drawErasedImage(original: ImageBitmap, ops: List<EraseOp>, imageSize: Size) {
    val dstSize = IntSize(imageSize.width.roundToInt().coerceAtLeast(1), imageSize.height.roundToInt().coerceAtLeast(1))
    drawImageScaled(original, IntOffset.Zero, dstSize)
    for (op in ops) {
        when (op) {
            is EraseOp.Brush -> {
                val path = strokeToPath(op.stroke, Offset.Zero, imageSize)
                if (op.restore) {
                    clipPath(path) { drawImageScaled(original, IntOffset.Zero, dstSize) }
                } else {
                    drawPath(path, Color.Black, blendMode = BlendMode.Clear)
                }
            }
            is EraseOp.MagicRegion -> if (op.restore) {
                // Mask the original in its own layer first, so DstIn trims only the restored copy
                // and not the cut-out already drawn beneath it.
                drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, imageSize), Paint()) }
                drawImageScaled(original, IntOffset.Zero, dstSize)
                drawImageScaled(op.mask, IntOffset.Zero, dstSize, BlendMode.DstIn)
                drawIntoCanvas { it.restore() }
            } else {
                drawImageScaled(op.mask, IntOffset.Zero, dstSize, BlendMode.DstOut)
            }
        }
    }
}

/**
 * Bakes the cut-out at full resolution, leaving the erased areas transparent. The next step
 * (`SetBackgroundScreen`) puts a backdrop behind it and flattens it, since export is JPEG.
 */
internal fun bakeCutOut(original: ImageBitmap, ops: List<EraseOp>): ImageBitmap {
    val size = Size(original.width.toFloat(), original.height.toFloat())
    val cutOut = ImageBitmap(original.width, original.height)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(cutOut), size) {
        drawErasedImage(original, ops, size)
    }
    return cutOut
}

/**
 * A downscaled copy of the photo's pixels for the Auto tool's colour-region taps, read once so each
 * tap only pays for the flood fill. CPU-heavy to build and query; use off the main thread.
 */
internal class MagicSampler(original: ImageBitmap) {
    private val width: Int
    private val height: Int
    private val pixels: IntArray

    init {
        val (w, h) = autoMaskWorkingSize(original.width, original.height)
        width = w
        height = h
        val small = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(small), Size(w.toFloat(), h.toFloat())) {
            drawImageScaled(original, IntOffset.Zero, IntSize(w, h))
        }
        pixels = IntArray(w * h)
        small.readPixels(pixels)
    }

    /**
     * Mask of the region around [point] (fractions of the photo size) within [tolerance], for
     * [EraseOp.MagicRegion]. Its bilinear upscale when drawn softens the cut edge.
     */
    /** The opaque colour of the pixel at [point] (fractions of the photo size). */
    fun colorAt(point: Offset): Color {
        val x = (point.x * width).toInt().coerceIn(0, width - 1)
        val y = (point.y * height).toInt().coerceIn(0, height - 1)
        return Color(pixels[y * width + x]).copy(alpha = 1f)
    }

    fun regionMask(point: Offset, tolerance: Int): ImageBitmap {
        val x = (point.x * width).toInt()
        val y = (point.y * height).toInt()
        val inside = colorRegionMask(pixels, width, height, x, y, tolerance)
        // Copied so the mask is a canvas-backed bitmap like every other drawImageScaled source.
        return copyBitmap(imageBitmapFromArgb(maskToArgb(inside), width, height))
    }
}
