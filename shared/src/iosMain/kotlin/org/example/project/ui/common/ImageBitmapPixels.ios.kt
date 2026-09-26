package org.example.project.ui.common

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/**
 * An `0xAARRGGBB` int stored little-endian is the byte sequence B, G, R, A, which is exactly Skia's
 * BGRA_8888. The pixels come from `readPixels` (premultiplied), so they are declared premultiplied.
 */
internal actual fun imageBitmapFromArgb(pixels: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val p = pixels[i]
        val o = i * 4
        bytes[o] = p.toByte()
        bytes[o + 1] = (p shr 8).toByte()
        bytes[o + 2] = (p shr 16).toByte()
        bytes[o + 3] = (p ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}
