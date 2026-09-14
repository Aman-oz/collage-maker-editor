package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import kotlin.math.roundToInt

/**
 * Renders [state] into a square [outputSize]x[outputSize] bitmap: the background color fills the
 * canvas, every layer draws in order (later layers on top), and the border strokes the (optionally
 * rounded) canvas edge last. [previewSizePx] is the on-screen square canvas's own pixel size —
 * [FreestyleState.borderWidth]/[FreestyleState.cornerRadius] were chosen against that size, so
 * they're scaled up to [outputSize] the same way [org.example.project.ui.collage.bakeCollage] does.
 * [textMeasurer] renders [FreestyleContent.StickerContent]/[FreestyleContent.TextContent] layers,
 * matching how [org.example.project.ui.text.bakeText] measures at the live preview's own density.
 */
internal fun bakeFreestyle(
    state: FreestyleState,
    textMeasurer: TextMeasurer,
    previewSizePx: Float,
    outputSize: Int = 1080,
): ImageBitmap {
    val output = ImageBitmap(outputSize, outputSize)
    val canvas = Canvas(output)
    val size = Size(outputSize.toFloat(), outputSize.toFloat())
    val scaleFactor = if (previewSizePx > 0f) outputSize / previewSizePx else 1f
    val borderPx = state.borderWidth * scaleFactor
    val cornerPx = (state.cornerRadius * scaleFactor).coerceAtLeast(0f)
    val inset = borderPx / 2f

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        val canvasPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                ),
            )
        }

        clipPath(canvasPath) {
            drawRect(color = state.backgroundColor)
            for (layer in state.layers) {
                drawFreestyleLayer(layer, textMeasurer, size)
            }
        }

        if (borderPx > 0f) {
            drawPath(path = canvasPath, color = state.borderColor, style = Stroke(width = borderPx))
        }
    }
    return output
}

private fun DrawScope.drawFreestyleLayer(layer: FreestyleLayer, textMeasurer: TextMeasurer, canvasSize: Size) {
    val centerPx = Offset(layer.offsetFraction.x * canvasSize.width, layer.offsetFraction.y * canvasSize.height)

    rotate(degrees = layer.rotationDegrees, pivot = centerPx) {
        when (val content = layer.content) {
            is FreestyleContent.ImageContent -> {
                val image = copyBitmap(content.image)
                val width = canvasSize.width * FreestyleImageBaseWidthFraction * layer.scale
                val height = width * (image.height.toFloat() / image.width.toFloat())
                drawImageScaled(
                    image = image,
                    dstOffset = IntOffset(
                        (centerPx.x - width / 2f).roundToInt(),
                        (centerPx.y - height / 2f).roundToInt(),
                    ),
                    dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
                )
            }

            is FreestyleContent.StickerContent -> {
                val layout = textMeasurer.measure(content.emoji, TextStyle(fontSize = (FreestyleStickerBaseSizeSp * layer.scale).sp))
                drawText(
                    layout,
                    topLeft = Offset(centerPx.x - layout.size.width / 2f, centerPx.y - layout.size.height / 2f),
                )
            }

            is FreestyleContent.TextContent -> {
                val style = TextStyle(
                    fontFamily = content.font.fontFamily,
                    fontWeight = content.font.fontWeight,
                    fontStyle = content.font.fontStyle,
                    color = content.color,
                    fontSize = (FreestyleTextBaseSizeSp * layer.scale).sp,
                )
                val layout = textMeasurer.measure(content.text, style)
                drawText(
                    layout,
                    topLeft = Offset(centerPx.x - layout.size.width / 2f, centerPx.y - layout.size.height / 2f),
                )
            }
        }
    }
}
