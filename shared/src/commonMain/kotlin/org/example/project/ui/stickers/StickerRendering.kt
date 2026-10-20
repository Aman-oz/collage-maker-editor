package org.example.project.ui.stickers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.blur.gaussianBlurArgb
import org.example.project.ui.common.imageBitmapFromArgb
import org.koin.compose.koinInject

// One renderer for a sticker's every edit, shared by the Stickers tool's stage, the editor's live
// layers and the bake, so all three agree. A sticker is drawn in two steps:
//
// 1. Its *body* ([renderStickerBody]): the source picture cut by the crop, drawn to its box's
//    shape, coloured (effect, adjustments, fill) and masked by the eraser — into a bitmap of its own,
//    which only changes when those do. Its *shadow* ([renderStickerShadow]) is the body's
//    silhouette, blurred.
// 2. [drawSticker] places them: turned, reshaped onto its corners, tilted, flipped, with the shadow under it, the outline round
//    it and its opacity over the lot. Moving, pinching and turning only redo this step.

/** The longer side of the body a sticker is previewed from; the bake draws its own at full size. */
internal const val PreviewStickerBodySide = 640

/** The longer side of the source picture the preview draws bodies from. */
internal const val PreviewStickerSourceSide = 768

/** The longer side the shadow is blurred at: a soft shadow needs no detail, and a blur this small is quick. */
private const val ShadowWorkingSide = 160

/** How many copies of the body, round each of two rings, make up its outline. */
private const val OutlineSteps = 20

/** A sticker's drawn parts: its [body], and its blurred silhouette when it has a shadow. */
internal class StickerArt(val body: ImageBitmap, val shadow: StickerShadowArt?)

/** A blurred silhouette, larger than the body by [padX] / [padY] (fractions of its width / height) a side. */
internal class StickerShadowArt(val image: ImageBitmap, val padX: Float, val padY: Float)

/**
 * The sticker's body drawn from [source] with its longer side [maxSide]: cut by its crop and drawn
 * into its box's shape, through its colour matrix, then masked by its
 * eraser strokes in an offscreen layer (erase clears the mask, restore paints it back).
 */
internal fun renderStickerBody(source: ImageBitmap, sticker: PlacedSticker, maxSide: Int): ImageBitmap {
    val aspect = sticker.aspectRatio
    val width = (if (aspect >= 1f) maxSide.toFloat() else maxSide * aspect).roundToInt().coerceAtLeast(1)
    val height = (if (aspect >= 1f) maxSide / aspect else maxSide.toFloat()).roundToInt().coerceAtLeast(1)
    val output = ImageBitmap(width, height)
    val canvas = Canvas(output)

    // Where the whole source lands so that its crop fills the bitmap.
    val fullWidth = width / (sticker.cropRight - sticker.cropLeft)
    val fullHeight = height / (sticker.cropBottom - sticker.cropTop)
    val originX = -sticker.cropLeft * fullWidth
    val originY = -sticker.cropTop * fullHeight
    canvas.drawImageRect(
        image = source,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(source.width, source.height),
        dstOffset = IntOffset(originX.roundToInt(), originY.roundToInt()),
        dstSize = IntSize(fullWidth.roundToInt().coerceAtLeast(1), fullHeight.roundToInt().coerceAtLeast(1)),
        paint = Paint().apply {
            filterQuality = FilterQuality.High
            colorFilter = stickerColorMatrix(sticker)?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
        },
    )

    if (sticker.eraseStrokes.isNotEmpty()) {
        val bounds = Rect(0f, 0f, width.toFloat(), height.toFloat())
        // The layer is the mask: opaque where the sticker shows. It lands on the picture with DstIn.
        canvas.saveLayer(bounds, Paint().apply { blendMode = BlendMode.DstIn })
        canvas.drawRect(bounds, Paint().apply { color = Color.Black })
        for (stroke in sticker.eraseStrokes) {
            val paint = Paint().apply {
                color = Color.Black
                blendMode = if (stroke.restore) BlendMode.SrcOver else BlendMode.Clear
            }
            val radius = stroke.radius * fullWidth
            val points = stroke.points.chunked(2).map { (x, y) -> Offset(originX + x * fullWidth, originY + y * fullHeight) }
            if (points.size == 1) {
                canvas.drawCircle(points[0], radius, paint)
            } else if (points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (point in points.drop(1)) lineTo(point.x, point.y)
                }
                canvas.drawPath(
                    path,
                    paint.apply {
                        style = PaintingStyle.Stroke
                        strokeWidth = radius * 2f
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                    },
                )
            }
        }
        canvas.restore()
    }
    return output
}

/**
 * The [body]'s silhouette, blurred by the sticker's shadow blur, or null without a shadow. It is
 * blurred small ([ShadowWorkingSide]) and drawn back up, which a soft shadow can't tell apart.
 */
internal fun renderStickerShadow(body: ImageBitmap, sticker: PlacedSticker): StickerShadowArt? {
    if (sticker.shadowOpacity <= 0f) return null
    val scale = ShadowWorkingSide.toFloat() / max(body.width, body.height)
    val bodyWidth = (body.width * scale).roundToInt().coerceAtLeast(1)
    val bodyHeight = (body.height * scale).roundToInt().coerceAtLeast(1)
    val sigma = sticker.shadowBlur.coerceIn(0f, 100f) / 100f * MaxStickerShadowBlur * ShadowWorkingSide
    // Room for the blur to spread past the body's edge.
    val pad = ceil(sigma * 3f).toInt()
    val width = bodyWidth + pad * 2
    val height = bodyHeight + pad * 2
    val silhouette = ImageBitmap(width, height)
    Canvas(silhouette).drawImageRect(
        image = body,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(body.width, body.height),
        dstOffset = IntOffset(pad, pad),
        dstSize = IntSize(bodyWidth, bodyHeight),
        paint = Paint().apply {
            filterQuality = FilterQuality.High
            colorFilter = ColorFilter.tint(Color.Black, BlendMode.SrcIn)
        },
    )
    val image = if (sigma > 0.3f) {
        val pixels = IntArray(width * height)
        silhouette.readPixels(pixels)
        imageBitmapFromArgb(gaussianBlurArgb(pixels, width, height, sigma), width, height)
    } else {
        silhouette
    }
    return StickerShadowArt(image, pad.toFloat() / bodyWidth, pad.toFloat() / bodyHeight)
}

/** [sticker]'s body and shadow from [source] at a body side of [maxSide]. */
internal fun renderStickerArt(source: ImageBitmap, sticker: PlacedSticker, maxSide: Int): StickerArt {
    val body = renderStickerBody(source, sticker, maxSide)
    return StickerArt(body, renderStickerShadow(body, sticker))
}

/**
 * The Transform tilt as a perspective matrix about the sticker's centre (its box [width] ×
 * [height]): a positive horizontal tilt shrinks its right side as if leaning away, and the left
 * grows to match; the centre stays put. Null when flat.
 */
internal fun stickerTiltMatrix(sticker: PlacedSticker, width: Float, height: Float): Matrix? {
    val tiltX = tiltAmount(sticker.tiltX)
    val tiltY = tiltAmount(sticker.tiltY)
    if (tiltX == 0f && tiltY == 0f) return null
    return Matrix().apply {
        // The 4×4 matrix's perspective terms for x and y (Matrix.Perspective0 / Perspective1): w = 1 + px·x + py·y.
        values[3] = tiltX / (width / 2f)
        values[7] = tiltY / (height / 2f)
    }
}

/** A row-major 3×3 projective map as a Compose [Matrix] (whose 4×4 values are column-major). */
internal fun projectiveMatrix(map: FloatArray): Matrix = Matrix().apply {
    values[0] = map[0]
    values[4] = map[1]
    values[12] = map[2]
    values[1] = map[3]
    values[5] = map[4]
    values[13] = map[5]
    values[3] = map[6]
    values[7] = map[7]
    values[15] = map[8]
}

/**
 * Draws [sticker] from its [art] onto a photo of [photoSize] (this scope's own size for the
 * preview, the full picture's for the bake): its shadow, then its outline, then its body, as one
 * layer at its opacity so the three don't show through each other.
 */
internal fun DrawScope.drawSticker(art: StickerArt, sticker: PlacedSticker, photoSize: Size) {
    val width = sticker.widthFraction * photoSize.width
    val height = width / sticker.aspectRatio
    if (width <= 0f || height <= 0f) return
    val long = max(width, height)
    val flipX = if (sticker.flippedHorizontally) -1f else 1f
    val flipY = if (sticker.flippedVertically) -1f else 1f
    val box = Rect(-width / 2f, -height / 2f, width / 2f, height / 2f)

    withTransform({
        translate(sticker.centerX * photoSize.width, sticker.centerY * photoSize.height)
        rotate(sticker.rotationDegrees, pivot = Offset.Zero)
        // Reshape: the box drawn onto its moved corners, so everything in it (shadow and outline too) follows.
        stickerReshapeMap(sticker, width, height)?.let { transform(projectiveMatrix(it)) }
        stickerTiltMatrix(sticker, width, height)?.let { transform(it) }
    }) {
        val layered = sticker.opacity < 1f
        if (layered) {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(box.inflate(long * 2f), Paint().apply { alpha = sticker.opacity.coerceIn(0f, 1f) })
            }
        }

        art.shadow?.let { shadow ->
            // The offset stays in the sticker's turned frame (as a text's shadow turns with it), but
            // isn't mirrored by a flip; the silhouette itself is.
            val unit = long * StickerShadowUnit
            withTransform({
                translate(sticker.shadowDx * unit, sticker.shadowDy * unit)
                scale(flipX, flipY, pivot = Offset.Zero)
            }) {
                drawImageInto(
                    image = shadow.image,
                    rect = Rect(
                        box.left - width * shadow.padX,
                        box.top - height * shadow.padY,
                        box.right + width * shadow.padX,
                        box.bottom + height * shadow.padY,
                    ),
                    colorFilter = ColorFilter.tint(Color(sticker.shadowArgb).copy(alpha = sticker.shadowOpacity.coerceIn(0f, 1f)), BlendMode.SrcIn),
                )
            }
        }

        withTransform({ scale(flipX, flipY, pivot = Offset.Zero) }) {
            if (sticker.outlineWidth > 0f) {
                // The body's silhouette, stamped round two rings: its edge grown by the outline's width.
                val radius = sticker.outlineWidth.coerceIn(0f, 100f) / 100f * MaxStickerOutline * long
                val tint = ColorFilter.tint(Color(sticker.outlineArgb), BlendMode.SrcIn)
                for (ring in listOf(radius, radius * 0.5f)) {
                    for (step in 0 until OutlineSteps) {
                        val angle = step * 2.0 * PI / OutlineSteps
                        val offset = Offset((cos(angle) * ring).toFloat(), (sin(angle) * ring).toFloat())
                        drawImageInto(art.body, box.translate(offset), tint)
                    }
                }
            }
            drawImageInto(art.body, box, null)
        }

        if (layered) drawIntoCanvas { it.restore() }
    }
}

private fun DrawScope.drawImageInto(image: ImageBitmap, rect: Rect, colorFilter: ColorFilter?) {
    drawIntoCanvas { canvas ->
        canvas.drawImageRect(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
            dstSize = IntSize(rect.width.roundToInt().coerceAtLeast(1), rect.height.roundToInt().coerceAtLeast(1)),
            paint = Paint().apply {
                filterQuality = FilterQuality.High
                this.colorFilter = colorFilter
            },
        )
    }
}

/** The largest side a sticker's body is drawn at for the bake; past this an SVG costs memory, not detail. */
private const val MaxBakedStickerSidePx = 2048

/**
 * Draws [stickers] over [source] at full resolution, bottom to top, exactly as [drawSticker] shows
 * them. Each SVG is drawn afresh ([renderSvg], the loader's uncached render) at the size its crop
 * takes on the photo, so a sticker stays sharp on a large photo. A sticker that can't be drawn is
 * left out.
 */
internal suspend fun bakeStickers(
    source: ImageBitmap,
    stickers: List<PlacedSticker>,
    renderSvg: suspend (url: String, sidePx: Int) -> ImageBitmap?,
): ImageBitmap {
    if (stickers.isEmpty()) return source
    val size = Size(source.width.toFloat(), source.height.toFloat())
    val arts = stickers.map { sticker ->
        val bodySide = stickerLongSidePx(sticker, size.width).roundToInt().coerceIn(1, MaxBakedStickerSidePx)
        // The whole picture at the size that gives its crop [bodySide] pixels.
        val crop = min(sticker.cropRight - sticker.cropLeft, sticker.cropBottom - sticker.cropTop)
        val sourceSide = (bodySide / crop).roundToInt().coerceIn(1, MaxBakedStickerSidePx * 2)
        renderSvg(sticker.imageUrl, sourceSide)?.let { renderStickerArt(it, sticker, bodySide) }
    }

    val output = ImageBitmap(source.width, source.height)
    val canvas = Canvas(output)
    canvas.drawImage(source, Offset.Zero, Paint())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, size) {
        stickers.forEachIndexed { index, sticker -> arts[index]?.let { drawSticker(it, sticker, size) } }
    }
    return output
}

/**
 * The parts of [sticker] that its body depends on, so a body is only drawn again when one of them
 * changes: moving, sizing, turning, tilting, outline, shadow colour and opacity all leave it be.
 */
private fun bodyKey(sticker: PlacedSticker): PlacedSticker = sticker.copy(
    id = 0L,
    centerX = 0f,
    centerY = 0f,
    widthFraction = 0f,
    rotationDegrees = 0f,
    flippedHorizontally = false,
    flippedVertically = false,
    outlineArgb = 0,
    outlineWidth = 0f,
    opacity = 1f,
    shadowArgb = 0,
    shadowOpacity = 0f,
    shadowDx = 0f,
    shadowDy = 0f,
    shadowBlur = 0f,
    tiltX = StickerTiltFlat,
    tiltY = StickerTiltFlat,
    cornerOffsets = emptyList(),
    isPremium = false,
)

/**
 * [sticker]'s art for the preview, or null until its picture is in: the grid's thumbnail at first,
 * then its SVG at [PreviewStickerSourceSide]. Its body is drawn again only when [bodyKey] changes,
 * its shadow when that or the blur does.
 */
@Composable
internal fun rememberStickerArt(sticker: PlacedSticker): StickerArt? {
    val loader: NetworkImageLoader = koinInject()
    var source by remember(sticker.imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sticker.imageUrl) {
        if (sticker.thumbnailUrl.isNotEmpty()) loader.load(sticker.thumbnailUrl)?.let { if (source == null) source = it }
        loader.load(sticker.imageUrl, PreviewStickerSourceSide)?.let { source = it }
    }
    val image = source ?: return null
    val key = bodyKey(sticker)
    val body = remember(image, key) { renderStickerBody(image, sticker, PreviewStickerBodySide) }
    val hasShadow = sticker.shadowOpacity > 0f
    val shadow = remember(body, hasShadow, sticker.shadowBlur) { if (hasShadow) renderStickerShadow(body, sticker) else null }
    return remember(body, shadow) { StickerArt(body, shadow) }
}

/** One live sticker drawn over a photo that this fills. */
@Composable
internal fun StickerLayer(sticker: PlacedSticker, modifier: Modifier = Modifier) {
    val art = rememberStickerArt(sticker) ?: return
    Canvas(modifier = modifier.fillMaxSize()) { drawSticker(art, sticker, size) }
}

/**
 * Live [stickers] drawn over a picture that this fills exactly, clipped to it as the bake crops
 * them. With [onStickerTapped], a tap on one reports its id: each sticker has an invisible box,
 * turned like it, rather than the whole overlay taking every touch (as [org.example.project.ui.text.TextLayersOverlay]).
 */
@Composable
internal fun StickerLayersOverlay(
    stickers: List<PlacedSticker>,
    modifier: Modifier = Modifier,
    onStickerTapped: ((Long) -> Unit)? = null,
) {
    val currentOnTapped by rememberUpdatedState(onStickerTapped)
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        for (sticker in stickers) {
            key(sticker.id) { StickerLayer(sticker) }
        }
        if (onStickerTapped == null) return@BoxWithConstraints
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        for (sticker in stickers) {
            key(sticker.id) {
                val boxWidth = sticker.widthFraction * widthPx
                val boxHeight = boxWidth / sticker.aspectRatio
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (sticker.centerX * widthPx - boxWidth / 2f).roundToInt(),
                                (sticker.centerY * heightPx - boxHeight / 2f).roundToInt(),
                            )
                        }
                        .size(with(density) { boxWidth.toDp() }, with(density) { boxHeight.toDp() })
                        .graphicsLayer { rotationZ = sticker.rotationDegrees }
                        .pointerInput(sticker.id) { detectTapGestures { currentOnTapped?.invoke(sticker.id) } },
                )
            }
        }
    }
}
