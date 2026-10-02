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
import androidx.compose.ui.unit.toSize
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.text.drawTextPlate
import kotlin.math.roundToInt

/**
 * Renders [state] into an [outputWidth]-wide bitmap with the on-screen canvas's aspect ratio
 * ([previewWidthPx] x [previewHeightPx]): the background fills it and every layer draws in
 * order (later layers on top). An image layer's [FreestyleLayer.borderWidth]/
 * [FreestyleLayer.cornerRadius] are dp chosen against that preview, so [previewDensity] converts
 * them to preview pixels before they're scaled up to the output the same way
 * [org.example.project.ui.collage.bakeCollage] does.
 * [textMeasurer] renders [FreestyleContent.StickerContent]/[FreestyleContent.TextContent] layers at
 * that same dp-to-output density, so a label keeps its size relative to the canvas.
 */
internal fun bakeFreestyle(
    state: FreestyleState,
    textMeasurer: TextMeasurer,
    previewWidthPx: Float,
    previewHeightPx: Float,
    previewDensity: Float,
    outputWidth: Int = 1080,
): ImageBitmap {
    val aspect = if (previewWidthPx > 0f && previewHeightPx > 0f) previewHeightPx / previewWidthPx else 1f
    val outputHeight = (outputWidth * aspect).roundToInt().coerceAtLeast(1)
    val output = ImageBitmap(outputWidth, outputHeight)
    val canvas = Canvas(output)
    val size = Size(outputWidth.toFloat(), outputHeight.toFloat())
    val dpToOutputPx = previewDensity * (if (previewWidthPx > 0f) outputWidth / previewWidthPx else 1f)
    val textDensity = Density(dpToOutputPx)

    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        drawRect(brush = state.background.brush)
        for (layer in state.layers) {
            drawFreestyleLayer(layer, textMeasurer, textDensity, size, dpToOutputPx)
        }
    }
    return output
}

internal fun DrawScope.drawFreestyleLayer(
    layer: FreestyleLayer,
    textMeasurer: TextMeasurer,
    textDensity: Density,
    canvasSize: Size,
    dpToOutputPx: Float,
) {
    val centerPx = Offset(layer.offsetFraction.x * canvasSize.width, layer.offsetFraction.y * canvasSize.height)

    rotate(degrees = layer.rotationDegrees, pivot = centerPx) {
        when (val content = layer.content) {
            is FreestyleContent.ImageContent -> {
                val image = copyBitmap(content.image)
                val width = canvasSize.width * FreestyleImageBaseWidthFraction * layer.scale
                val height = width * (image.height.toFloat() / image.width.toFloat())
                val left = centerPx.x - width / 2f
                val top = centerPx.y - height / 2f
                val cornerPx = (layer.cornerRadius * dpToOutputPx).coerceAtMost(minOf(width, height) / 2f)
                val photoPath = Path().apply {
                    addRoundRect(RoundRect(left, top, left + width, top + height, CornerRadius(cornerPx, cornerPx)))
                }
                clipPath(photoPath) {
                    drawImageScaled(
                        image = image,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
                    )
                    // Stroking the clip path itself leaves only the inner half of the stroke visible,
                    // so doubling the width yields a frame drawn inside the photo's edge — the same
                    // as the preview's Modifier.border.
                    val borderPx = layer.borderWidth * dpToOutputPx
                    if (borderPx > 0f) {
                        drawPath(path = photoPath, color = FreestyleImageBorderColor, style = Stroke(width = borderPx * 2f))
                    }
                }
            }

            is FreestyleContent.StickerContent -> {
                val layout = textMeasurer.measure(
                    content.emoji,
                    TextStyle(fontSize = (FreestyleStickerBaseSizeSp * layer.scale).sp),
                    density = textDensity,
                )
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
                    brush = content.fill.brush,
                    fontSize = (FreestyleTextBaseSizeSp * layer.scale).sp,
                )
                val layout = textMeasurer.measure(content.text, style, density = textDensity)
                val topLeft = Offset(centerPx.x - layout.size.width / 2f, centerPx.y - layout.size.height / 2f)
                content.background?.let { drawTextPlate(it.brush, topLeft, layout.size.toSize()) }
                drawText(layout, topLeft = topLeft)
            }
        }
    }
}
