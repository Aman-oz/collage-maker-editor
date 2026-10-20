package org.example.project.ui.stickers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.example.project.ui.text.rotateVector

private val CropHandleTouch = 36.dp
private val CropCornerArm = 14.dp

/**
 * The Crop panel's frame over the uncut sticker [full] on a [photoSize] photo: turned with it, the
 * area outside [crop] (display fractions, see [sourceToDisplayCrop]) dimmed, corners that resize the
 * crop (keeping [ratio], width / height as seen, when set) and the inside that slides it. A drag
 * inside the turned box arrives in the box's own frame, so it maps straight onto the crop.
 */
@Composable
internal fun BoxScope.StickerCropFrame(
    full: PlacedSticker,
    crop: Rect,
    photoSize: IntSize,
    ratio: Float?,
    onCropChange: (Rect) -> Unit,
) {
    val density = LocalDensity.current
    val width = full.widthFraction * photoSize.width
    val height = width / full.aspectRatio
    if (width <= 0f || height <= 0f) return
    val currentCrop by rememberUpdatedState(crop)
    val currentOnChange by rememberUpdatedState(onCropChange)
    val currentSize by rememberUpdatedState(Size(width, height))
    val currentRatio by rememberUpdatedState(ratio)
    val fullAspect = full.aspectRatio

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    (full.centerX * photoSize.width - width / 2f).roundToInt(),
                    (full.centerY * photoSize.height - height / 2f).roundToInt(),
                )
            }
            .size(with(density) { width.toDp() }, with(density) { height.toDp() })
            .graphicsLayer { rotationZ = full.rotationDegrees },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = Rect(crop.left * size.width, crop.top * size.height, crop.right * size.width, crop.bottom * size.height)
            val scrim = Color.Black.copy(alpha = 0.5f)
            drawRect(scrim, Offset.Zero, Size(size.width, r.top))
            drawRect(scrim, Offset(0f, r.bottom), Size(size.width, size.height - r.bottom))
            drawRect(scrim, Offset(0f, r.top), Size(r.left, r.height))
            drawRect(scrim, Offset(r.right, r.top), Size(size.width - r.right, r.height))
            // Thirds, then the frame and a heavier L at each corner.
            val thin = 1.dp.toPx()
            for (i in 1..2) {
                val x = r.left + r.width * i / 3f
                val y = r.top + r.height * i / 3f
                drawLine(Color.White.copy(alpha = 0.5f), Offset(x, r.top), Offset(x, r.bottom), thin)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(r.left, y), Offset(r.right, y), thin)
            }
            drawRect(Color.White, r.topLeft, r.size, style = Stroke(1.5.dp.toPx()))
            val arm = CropCornerArm.toPx()
            val heavy = 3.dp.toPx()
            for ((corner, dir) in listOf(r.topLeft to Offset(1f, 1f), r.topRight to Offset(-1f, 1f), r.bottomRight to Offset(-1f, -1f), r.bottomLeft to Offset(1f, -1f))) {
                drawLine(Color.White, corner, corner + Offset(arm * dir.x, 0f), heavy, StrokeCap.Round)
                drawLine(Color.White, corner, corner + Offset(0f, arm * dir.y), heavy, StrokeCap.Round)
            }
        }

        // The inside slides the crop.
        Box(
            modifier = Modifier
                .offset { IntOffset((crop.left * width).roundToInt(), (crop.top * height).roundToInt()) }
                .size(with(density) { (crop.width * width).toDp() }, with(density) { (crop.height * height).toDp() })
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        currentOnChange(moveCrop(currentCrop, drag.x / currentSize.width, drag.y / currentSize.height))
                    }
                },
        )

        val corners = listOf(crop.topLeft, crop.topRight, crop.bottomRight, crop.bottomLeft)
        corners.forEachIndexed { index, point ->
            Box(
                modifier = Modifier
                    .offset {
                        val half = CropHandleTouch.toPx() / 2f
                        IntOffset((point.x * width - half).roundToInt(), (point.y * height - half).roundToInt())
                    }
                    .size(CropHandleTouch)
                    .pointerInput(index) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentOnChange(
                                dragCropCorner(
                                    currentCrop,
                                    index,
                                    drag.x / currentSize.width,
                                    drag.y / currentSize.height,
                                    currentRatio,
                                    fullAspect,
                                ),
                            )
                        }
                    },
            )
        }
    }
}

/**
 * Takes every touch on the photo while the Eraser panel is open: a drag draws a stroke (a tap a dot)
 * that [onStroke] gets as it grows, in photo px, and [onStrokeEnd] once it lifts. The brush, of
 * [brushRadiusPx], follows the finger; with no finger down it shows at [previewAt] (the sticker's
 * centre while the size slider moves), faded in and out by [previewAlpha].
 */
@Composable
internal fun StickerEraserInput(
    brushRadiusPx: Float,
    onStroke: (points: List<Offset>) -> Unit,
    onStrokeEnd: () -> Unit,
    modifier: Modifier = Modifier,
    previewAt: Offset? = null,
    previewAlpha: () -> Float = { 0f },
) {
    val currentOnStroke by rememberUpdatedState(onStroke)
    val currentOnStrokeEnd by rememberUpdatedState(onStrokeEnd)
    var points by remember { mutableStateOf(emptyList<Offset>()) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { position ->
                        currentOnStroke(listOf(position))
                        currentOnStrokeEnd()
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        points = listOf(position)
                        finger = position
                        currentOnStroke(points)
                    },
                    onDragEnd = {
                        finger = null
                        currentOnStrokeEnd()
                    },
                    onDragCancel = {
                        finger = null
                        currentOnStrokeEnd()
                    },
                ) { change, _ ->
                    change.consume()
                    points = points + change.position
                    finger = change.position
                    currentOnStroke(points)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val touching = finger
            val at = touching ?: previewAt ?: return@Canvas
            // Read here, in the draw pass, so the fade redraws without recomposing.
            val alpha = if (touching != null) 1f else previewAlpha()
            if (alpha <= 0f) return@Canvas
            if (touching == null) drawCircle(Color.White.copy(alpha = 0.25f * alpha), brushRadiusPx, at)
            drawCircle(Color.Black.copy(alpha = 0.35f * alpha), brushRadiusPx, at, style = Stroke(3.dp.toPx()))
            drawCircle(Color.White.copy(alpha = alpha), brushRadiusPx, at, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** The purple of the Text tool's selection handles, which the reshape corners match. */
private val ReshapeHandleColor = Color(0xFF8B5CF6)
private val ReshapeHandleTouch = 34.dp
private val ReshapeDotSize = 10.dp

/**
 * Reshape's frame over [sticker] on a [photoSize] photo: the outline of its reshaped box and a dot
 * on each corner, which drags that corner alone ([onCornerDrag] gets its index and the move in the
 * sticker's turned frame). It lies over the whole photo, unturned, so a corner dragged far out is
 * still inside it and keeps taking touches.
 */
@Composable
internal fun StickerReshapeFrame(
    sticker: PlacedSticker,
    photoSize: IntSize,
    onCornerDrag: (index: Int, delta: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = sticker.widthFraction * photoSize.width
    val height = width / sticker.aspectRatio
    if (width <= 0f || height <= 0f) return
    val center = Offset(sticker.centerX * photoSize.width, sticker.centerY * photoSize.height)
    val corners = stickerCorners(sticker, width, height).map { center + rotateVector(it, sticker.rotationDegrees) }
    val currentRotation by rememberUpdatedState(sticker.rotationDegrees)
    val currentOnDrag by rememberUpdatedState(onCornerDrag)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(corners[0].x, corners[0].y)
                for (corner in corners.drop(1)) lineTo(corner.x, corner.y)
                close()
            }
            // A white hairline with a faint dark one under it, readable on any photo, as the Text tool's.
            drawPath(path, Color.Black.copy(alpha = 0.25f), style = Stroke(3.dp.toPx()))
            drawPath(path, Color.White.copy(alpha = 0.95f), style = Stroke(1.dp.toPx()))
        }
        corners.forEachIndexed { index, corner ->
            Box(
                modifier = Modifier
                    .offset {
                        val half = ReshapeHandleTouch.toPx() / 2f
                        IntOffset((corner.x - half).roundToInt(), (corner.y - half).roundToInt())
                    }
                    .size(ReshapeHandleTouch)
                    .pointerInput(index) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            currentOnDrag(index, rotateVector(drag, -currentRotation))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(ReshapeDotSize)) {
                    drawCircle(ReshapeHandleColor)
                    drawCircle(Color.White, style = Stroke(1.dp.toPx()))
                }
            }
        }
    }
}
