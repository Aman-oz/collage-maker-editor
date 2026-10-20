package org.example.project.data.network

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.caverock.androidsvg.SVG
import kotlin.math.max
import kotlin.math.roundToInt

actual fun rasterizeSvg(bytes: ByteArray, maxSidePx: Int): ImageBitmap {
    val svg = SVG.getFromInputStream(bytes.inputStream())
    val viewBox = svg.documentViewBox
    val width = viewBox?.width() ?: svg.documentWidth
    val height = viewBox?.height() ?: svg.documentHeight
    require(width > 0f && height > 0f) { "SVG has no size" }
    // Without a viewBox the document would draw at its own size whatever the viewport; with one,
    // renderToCanvas scales it to fill the bitmap.
    if (viewBox == null) svg.setDocumentViewBox(0f, 0f, width, height)
    val scale = maxSidePx / max(width, height)
    val bitmap = Bitmap.createBitmap(
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    svg.renderToCanvas(Canvas(bitmap), RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()))
    return bitmap.asImageBitmap()
}
