package org.example.project.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Returns a fully desaturated (grayscale) copy of [source], drawn through a saturation-0
 * [ColorMatrix] color filter — no per-pixel loop, so it works identically on Android and iOS and
 * backs both the color-splash preview and its bake.
 */
internal fun grayscaleBitmap(source: ImageBitmap): ImageBitmap {
    val output = ImageBitmap(source.width, source.height)
    Canvas(output).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply {
            filterQuality = FilterQuality.High
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        },
    )
    return output
}

/**
 * Returns a fresh raster copy of [source]. Some platform-decoded [ImageBitmap]s (e.g. a photo
 * loaded straight from disk) don't redraw correctly when used as the source of a raw
 * `Canvas.drawImageRect`/`DrawScope.drawImage` call — copying once through an in-memory canvas up
 * front sidesteps that and gives [drawImageScaled] a bitmap it can reliably redraw many times.
 */
internal fun copyBitmap(source: ImageBitmap): ImageBitmap {
    val copy = ImageBitmap(source.width, source.height)
    Canvas(copy).drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(source.width, source.height),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return copy
}

/**
 * Draws [image] scaled into [dstOffset]/[dstSize] via `Canvas.drawImageRect` (through
 * [drawIntoCanvas]) rather than the `DrawScope.drawImage` extension — [image] must be a bitmap
 * this app created itself (e.g. via [copyBitmap], or drawn into a fresh `ImageBitmap`), not a raw
 * platform-decoded one, which doesn't redraw reliably through either scaling call.
 */
internal fun DrawScope.drawImageScaled(image: ImageBitmap, dstOffset: IntOffset, dstSize: IntSize) {
    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = dstOffset,
            dstSize = dstSize,
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
    }
}

/**
 * Draws [image] center-cropped to fill [dstSize] exactly (the `ContentScale.Crop` a `DrawScope`
 * gets for free from the `Image` composable, but not from a raw `Canvas.drawImageRect` call) —
 * same [image]-must-be-a-bitmap-this-app-created caveat as [drawImageScaled].
 */
internal fun DrawScope.drawImageCropped(image: ImageBitmap, dstOffset: IntOffset, dstSize: IntSize) {
    val sourceWidth = image.width.toFloat()
    val sourceHeight = image.height.toFloat()
    val destAspect = dstSize.width.toFloat() / dstSize.height.toFloat()
    val sourceAspect = sourceWidth / sourceHeight
    val cropWidth: Float
    val cropHeight: Float
    if (sourceAspect > destAspect) {
        cropHeight = sourceHeight
        cropWidth = sourceHeight * destAspect
    } else {
        cropWidth = sourceWidth
        cropHeight = sourceWidth / destAspect
    }
    val cropX = ((sourceWidth - cropWidth) / 2f).roundToInt()
    val cropY = ((sourceHeight - cropHeight) / 2f).roundToInt()

    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset(cropX, cropY),
            srcSize = IntSize(cropWidth.roundToInt().coerceAtLeast(1), cropHeight.roundToInt().coerceAtLeast(1)),
            dstOffset = dstOffset,
            dstSize = dstSize,
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
    }
}

/**
 * Builds a path unioning a circle (radius [radiusPx]) at every point of one dragged brush stroke.
 * Points are `0f..1f` fractions of [imageSize], offset by [imageOffset] — so the same stroke data
 * draws correctly whether [imageOffset]/[imageSize] describe a letterboxed on-screen preview or a
 * full-resolution bake canvas (where offset is [Offset.Zero]).
 */
internal fun buildStrokePath(points: List<Offset>, imageOffset: Offset, imageSize: Size, radiusPx: Float): Path {
    val path = Path()
    for (fraction in points) {
        val center = imageOffset + Offset(fraction.x * imageSize.width, fraction.y * imageSize.height)
        path.addOval(Rect(center = center, radius = radiusPx))
    }
    return path
}
