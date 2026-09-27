package org.example.project.ui.templates

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageCropped
import org.example.project.ui.common.drawImageScaled
import kotlin.math.roundToInt

/**
 * One template slot resolved to `0f..1f` fractions of the whole frame. The server gives slot centers
 * in the frame image's pixel space, so the center is converted to a top-left rectangle here.
 * [rotation] is degrees clockwise about the slot's center.
 */
internal data class NormalizedSlot(
    val index: Int,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
)

/**
 * Normalizes a template's pixel-space slots against the real frame [frameWidth]x[frameHeight], sorted
 * by `elevation` (paint order) — the Compose equivalent of the LAS `NewFrameEditor` coordinate loop.
 */
internal fun TemplateFrame.normalizedSlots(frameWidth: Float, frameHeight: Float): List<NormalizedSlot> {
    val fw = frameWidth.coerceAtLeast(1f)
    val fh = frameHeight.coerceAtLeast(1f)
    return slots.sortedBy { it.elevation }.map { slot ->
        NormalizedSlot(
            index = slot.index,
            left = (slot.x - slot.width / 2f) / fw,
            top = (slot.y - slot.height / 2f) / fh,
            width = slot.width / fw,
            height = slot.height / fh,
            rotation = slot.rotation,
        )
    }
}

/**
 * Bakes the finished template into a bitmap: each slot's photo is center-cropped into its (possibly
 * rotated) rectangle with the user's [transforms] applied, then the decorative [frameImage] is drawn
 * over everything so its transparent windows reveal the photos — the same layering the editor
 * preview shows. Output width is fixed; height follows the frame's aspect ratio.
 */
internal fun bakeTemplate(
    frame: TemplateFrame,
    frameImage: ImageBitmap,
    images: Map<Int, ImageBitmap>,
    transforms: Map<Int, SlotTransform> = emptyMap(),
    outputWidth: Int = 1080,
): ImageBitmap {
    val aspect = frameImage.width.toFloat() / frameImage.height.toFloat()
    val outputHeight = (outputWidth / aspect).roundToInt().coerceAtLeast(1)
    val output = ImageBitmap(outputWidth, outputHeight)
    val canvas = Canvas(output)
    val size = Size(outputWidth.toFloat(), outputHeight.toFloat())
    val slots = frame.normalizedSlots(frameImage.width.toFloat(), frameImage.height.toFloat())

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        for (slot in slots) {
            val image = images[slot.index] ?: continue
            val left = slot.left * outputWidth
            val top = slot.top * outputHeight
            val w = (slot.width * outputWidth).coerceAtLeast(1f)
            val h = (slot.height * outputHeight).coerceAtLeast(1f)
            val transform = transforms[slot.index] ?: SlotTransform()
            val center = Offset(left + w / 2f, top + h / 2f)
            rotate(degrees = slot.rotation, pivot = center) {
                clipRect(left = left, top = top, right = left + w, bottom = top + h) {
                    // Mirrors the preview's graphicsLayer: scale about the slot center, then translate.
                    translate(left = transform.offsetX * w, top = transform.offsetY * h) {
                        scale(scale = transform.scale, pivot = center) {
                            drawImageCropped(
                                image = image,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                            )
                        }
                    }
                }
            }
        }
        drawImageScaled(
            image = copyBitmap(frameImage),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(outputWidth, outputHeight),
        )
    }
    return output
}
