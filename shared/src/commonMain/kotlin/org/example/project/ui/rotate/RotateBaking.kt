package org.example.project.ui.rotate

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

/**
 * Renders [source] rotated by whole 90° turns and/or mirrored — exact, gap-free operations, so the
 * output is simply [source]'s pixels relocated onto a canvas whose width/height are swapped
 * whenever [quarterTurns] is odd (a rotated portrait photo becomes landscape, and vice versa).
 */
internal fun bakeQuarterTurnsAndFlip(
    source: ImageBitmap,
    quarterTurns: Int,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
): ImageBitmap {
    val turns = normalizeQuarterTurns(quarterTurns)
    if (turns == 0 && !flipHorizontal && !flipVertical) return source

    val swapped = turns == 1 || turns == 3
    val outputWidth = if (swapped) source.height else source.width
    val outputHeight = if (swapped) source.width else source.height
    val output = ImageBitmap(outputWidth, outputHeight)
    val canvas = Canvas(output)

    canvas.save()
    canvas.translate(outputWidth / 2f, outputHeight / 2f)
    canvas.rotate(turns * 90f)
    canvas.scale(if (flipHorizontal) -1f else 1f, if (flipVertical) -1f else 1f)
    canvas.translate(-source.width / 2f, -source.height / 2f)
    canvas.drawImage(source, Offset.Zero, Paint())
    canvas.restore()

    return output
}

/**
 * Renders [source] tilted by [degrees] around its center, scaled up by [coverScaleForRotation] so
 * the tilted content still fills the original [source]-sized frame with no empty corners — the
 * same "straighten" look the live preview shows via a matching `graphicsLayer` transform.
 */
internal fun bakeFineRotation(source: ImageBitmap, degrees: Float): ImageBitmap {
    if (degrees == 0f) return source

    val width = source.width.toFloat()
    val height = source.height.toFloat()
    val scale = coverScaleForRotation(width, height, degrees)
    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)

    canvas.save()
    canvas.translate(width / 2f, height / 2f)
    canvas.rotate(degrees)
    canvas.scale(scale, scale)
    canvas.translate(-width / 2f, -height / 2f)
    canvas.drawImage(source, Offset.Zero, Paint())
    canvas.restore()

    return output
}

/** Applies the exact quarter-turn/flip first, then the fine straighten angle on top. */
internal fun bakeRotate(
    source: ImageBitmap,
    quarterTurns: Int,
    flipHorizontal: Boolean,
    flipVertical: Boolean,
    rotationDegrees: Float,
): ImageBitmap {
    val turned = bakeQuarterTurnsAndFlip(source, quarterTurns, flipHorizontal, flipVertical)
    return bakeFineRotation(turned, rotationDegrees)
}
