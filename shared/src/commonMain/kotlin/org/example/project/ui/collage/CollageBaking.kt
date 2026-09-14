package org.example.project.ui.collage

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageCropped
import kotlin.math.roundToInt

/**
 * Renders [state] into a square [outputSize]x[outputSize] bitmap: the border color fills the
 * background, then each slot's image is center-cropped into its rounded-rect area. [previewSizePx]
 * is the on-screen square canvas's own pixel size — [CollageState.borderWidth]/[CollageState.cornerRadius]
 * were chosen against that size, so they're scaled up to [outputSize] the same way the other tools
 * scale a preview-relative brush/frame size up to the source's real resolution.
 */
internal fun bakeCollage(state: CollageState, previewSizePx: Float, outputSize: Int = 1080): ImageBitmap {
    val output = ImageBitmap(outputSize, outputSize)
    val canvas = Canvas(output)
    val size = Size(outputSize.toFloat(), outputSize.toFloat())
    val scaleFactor = if (previewSizePx > 0f) outputSize / previewSizePx else 1f
    val borderPx = state.borderWidth * scaleFactor
    val cornerPx = (state.cornerRadius * scaleFactor / 2f).coerceAtLeast(0f)
    val halfBorder = borderPx / 2f

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawRect(color = state.borderColor)

        for (slot in state.template.slots) {
            val slotImage = state.images.find { it.slotIndex == slot.index } ?: continue
            val left = size.width * slot.left + halfBorder
            val top = size.height * slot.top + halfBorder
            val width = (size.width * slot.width - borderPx).coerceAtLeast(1f)
            val height = (size.height * slot.height - borderPx).coerceAtLeast(1f)

            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = left + width,
                        bottom = top + height,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    ),
                )
            }
            clipPath(path) {
                drawImageCropped(
                    image = copyBitmap(slotImage.image),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(width.roundToInt(), height.roundToInt()),
                )
            }
        }
    }
    return output
}
