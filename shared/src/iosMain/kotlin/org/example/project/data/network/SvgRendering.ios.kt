package org.example.project.data.network

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import kotlin.math.max
import kotlin.math.roundToInt
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.Rect
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthContext
import org.jetbrains.skia.svg.SVGLengthUnit

actual fun rasterizeSvg(bytes: ByteArray, maxSidePx: Int): ImageBitmap {
    // Skia ignores <style> sheets, so their class rules are copied onto the shapes first.
    val source = inlineSvgClassStyles(bytes.decodeToString()).encodeToByteArray()
    val dom = SVGDOM(Data.makeFromBytes(source))
    val root = checkNotNull(dom.root) { "Not an SVG document" }
    val viewBox = root.viewBox
    val width: Float
    val height: Float
    if (viewBox != null && viewBox.width > 0f && viewBox.height > 0f) {
        width = viewBox.width
        height = viewBox.height
    } else {
        val intrinsic = root.getIntrinsicSize(SVGLengthContext(width = 100f, height = 100f))
        width = intrinsic.x
        height = intrinsic.y
        root.viewBox = Rect.makeWH(width, height)
    }
    require(width > 0f && height > 0f) { "SVG has no size" }
    val scale = maxSidePx / max(width, height)
    val outWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val outHeight = (height * scale).roundToInt().coerceAtLeast(1)
    // The root fills the container, and its viewBox scales the drawing to it, whatever width and
    // height the file itself declares.
    root.width = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
    root.height = SVGLength(100f, SVGLengthUnit.PERCENTAGE)
    dom.setContainerSize(outWidth.toFloat(), outHeight.toFloat())

    val bitmap = Bitmap()
    bitmap.allocN32Pixels(outWidth, outHeight)
    val canvas = Canvas(bitmap)
    dom.render(canvas)
    canvas.close()
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}
