package org.example.project.ui.pip

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import org.example.project.ui.blur.computeBlurredBitmap
import org.example.project.ui.common.drawImageCropped
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.drawFreestyleLayer
import org.example.project.ui.templates.SlotTransform

/** Shown behind the frame until the blurred backdrop is ready. */
private val BackdropPlaceholder = Color(0xFF9E9E9E)

/** Dims an empty slot's window, so it reads as "tap to add a photo". */
private val EmptySlotShade = Color.Black.copy(alpha = 0.28f)

/**
 * Everything the Pip editor edits, as one undo step: the photo in each slot (the picked ones, or a
 * replacement), each slot photo's pinch/pan, and the text/sticker [layers] over the whole picture
 * (freestyle layers, positioned as fractions of it).
 */
data class PipEdit(
    val photos: Map<Int, ImageBitmap> = emptyMap(),
    val transforms: Map<Int, SlotTransform> = emptyMap(),
    val layers: List<FreestyleLayer> = emptyList(),
)

/**
 * The photo the blurred backdrop is made from: the first slot's, as in the LAS app, or the first
 * filled slot's when that one is empty.
 */
internal val PipEdit.backdropSource: ImageBitmap?
    get() = photos[0] ?: photos.entries.minByOrNull { it.key }?.value

/** [photo] downscaled (see [backdropWorkingSize]) and heavily blurred, to fill behind the frame. */
internal fun blurredBackdrop(photo: ImageBitmap): ImageBitmap {
    val (width, height) = backdropWorkingSize(photo.width, photo.height)
    val small = ImageBitmap(width, height)
    Canvas(small).drawImageRect(
        image = photo,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(photo.width, photo.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return computeBlurredBitmap(small, PipBackdropBlurLevel)
}

/**
 * Draws the PIP picture into this scope, which has the frame's aspect ratio: the blurred [backdrop]
 * center-cropped to fill, each slot's photo center-cropped into its window with its pinch/pan and
 * cut to shape by the slot's mask, then the transparent frame over it all. Shared by the live
 * preview and [bakePip] so they match.
 *
 * Each slot is drawn in its own layer, so the mask's `DstIn` keeps only that slot's photo (where
 * the mask is opaque) instead of also cutting away the backdrop — the LAS app's `saveLayer` +
 * `PorterDuff.Mode.DST_IN`.
 */
internal fun DrawScope.drawPip(
    template: PipTemplate,
    assets: PipFrameAssets,
    backdrop: ImageBitmap?,
    edit: PipEdit,
    shadeEmptySlots: Boolean,
) {
    val canvasSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
    if (backdrop != null) {
        drawImageCropped(backdrop, IntOffset.Zero, canvasSize)
    } else {
        drawRect(BackdropPlaceholder)
    }

    template.slots.forEachIndexed { index, slot ->
        val mask = assets.masks[index]
        val rect = slot.slotRect(mask.width, mask.height, assets.frame.width, assets.frame.height)
        val left = rect.left * size.width
        val top = rect.top * size.height
        val width = rect.width * size.width
        val height = rect.height * size.height
        val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
        val dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1))
        val photo = edit.photos[index]
        if (photo == null && !shadeEmptySlots) return@forEachIndexed

        drawIntoCanvas { it.saveLayer(Rect(left, top, left + width, top + height), Paint()) }
        if (photo != null) {
            val transform = edit.transforms[index] ?: SlotTransform()
            val center = Offset(left + width / 2f, top + height / 2f)
            clipRect(left = left, top = top, right = left + width, bottom = top + height) {
                // Mirrors the templates editor: scale about the slot center, then translate.
                translate(left = transform.offsetX * width, top = transform.offsetY * height) {
                    scale(scale = transform.scale, pivot = center) {
                        drawImageCropped(photo, dstOffset, dstSize)
                    }
                }
            }
        } else {
            drawRect(EmptySlotShade, topLeft = Offset(left, top), size = Size(width, height))
        }
        drawImageScaled(mask, dstOffset, dstSize, blendMode = BlendMode.DstIn)
        drawIntoCanvas { it.restore() }
    }

    drawImageScaled(assets.frame, IntOffset.Zero, canvasSize)
}

/**
 * Renders the finished PIP at [pipOutputSize], then its text/sticker layers on top. Empty slots are
 * left showing the backdrop, as the LAS app exports them.
 *
 * The layers were sized against the on-screen picture ([previewWidthPx] wide at [previewDensity]),
 * so their dp/sp are converted to output pixels by the same ratio, as `bakeBackground` does.
 */
internal fun bakePip(
    template: PipTemplate,
    assets: PipFrameAssets,
    backdrop: ImageBitmap?,
    edit: PipEdit,
    textMeasurer: TextMeasurer,
    previewWidthPx: Float,
    previewDensity: Float,
): ImageBitmap {
    val (width, height) = pipOutputSize(assets.frame.width, assets.frame.height)
    val output = ImageBitmap(width, height)
    val size = Size(width.toFloat(), height.toFloat())
    val dpToOutputPx = previewDensity * (if (previewWidthPx > 0f) width / previewWidthPx else 1f)
    val textDensity = Density(dpToOutputPx)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(output), size) {
        drawPip(template, assets, backdrop, edit, shadeEmptySlots = false)
        for (layer in edit.layers) {
            drawFreestyleLayer(layer, textMeasurer, textDensity, size, dpToOutputPx)
        }
    }
    return output
}
