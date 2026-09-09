package org.example.project.ui.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

private data class AspectRatioOption(val label: String, val ratio: Float?)

private val AspectRatioOptions = listOf(
    AspectRatioOption("Free", null),
    AspectRatioOption("1:1", 1f / 1f),
    AspectRatioOption("4:3", 4f / 3f),
    AspectRatioOption("3:4", 3f / 4f),
    AspectRatioOption("5:4", 5f / 4f),
    AspectRatioOption("4:5", 4f / 5f),
    AspectRatioOption("3:2", 3f / 2f),
    AspectRatioOption("2:3", 2f / 3f),
    AspectRatioOption("9:16", 9f / 16f),
    AspectRatioOption("16:9", 16f / 9f),
)

@Composable
fun CropScreen(
    onBack: () -> Unit,
    onCropped: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CropViewModel = koinViewModel(),
) {
    CropContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onCropConfirmed = { cropped ->
            viewModel.applyCrop(cropped)
            onCropped()
        },
        modifier = modifier,
    )
}

@Composable
private fun CropContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onCropConfirmed: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedOption by remember { mutableStateOf(AspectRatioOptions.first()) }
    var cropRect by remember(sourceImage) {
        mutableStateOf(sourceImage?.let { fullImageRect(it) })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        CropTopBar(
            onBack = onBack,
            cropEnabled = sourceImage != null && cropRect != null,
            onCrop = {
                val image = sourceImage
                val rect = cropRect
                if (image != null && rect != null) {
                    onCropConfirmed(cropImageBitmap(image, rect.toIntRectClamped(image)))
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        ) {
            if (sourceImage != null) {
                CropCanvas(
                    image = sourceImage,
                    ratio = selectedOption.ratio,
                    cropRect = cropRect ?: fullImageRect(sourceImage),
                    onCropRectChange = { cropRect = it },
                )
            } else {
                Text(
                    text = "No image to crop",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        AspectRatioGrid(
            options = AspectRatioOptions,
            selected = selectedOption,
            onSelected = { option ->
                selectedOption = option
                val image = sourceImage
                val ratio = option.ratio
                if (image != null && ratio != null) {
                    cropRect = centeredRectForRatio(image, ratio)
                }
            },
        )
    }
}

@Composable
private fun CropTopBar(onBack: () -> Unit, onCrop: () -> Unit, cropEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Crop",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        AccentPillButton(text = "Crop", onClick = onCrop, enabled = cropEnabled)
    }
}

@Composable
private fun CropCanvas(
    image: ImageBitmap,
    ratio: Float?,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val boxHeightPx = with(density) { maxHeight.toPx() }
        val bitmapWidth = image.width.toFloat()
        val bitmapHeight = image.height.toFloat()
        val scale = min(boxWidthPx / bitmapWidth, boxHeightPx / bitmapHeight)
        val displayWidth = bitmapWidth * scale
        val displayHeight = bitmapHeight * scale
        val imageOffset = Offset((boxWidthPx - displayWidth) / 2f, (boxHeightPx - displayHeight) / 2f)
        val bounds = remember(bitmapWidth, bitmapHeight) { Rect(0f, 0f, bitmapWidth, bitmapHeight) }
        val handleTouchPx = with(density) { 28.dp.toPx() }
        val minSizePx = with(density) { 40.dp.toPx() } / scale

        Image(
            bitmap = image,
            contentDescription = "Photo to crop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        var activeHandle by remember { mutableStateOf<CropHandle?>(null) }
        // pointerInput's suspend block only restarts when its keys change (box/bitmap size), so a
        // gesture that spans several onDrag calls would otherwise keep seeing the `cropRect` and
        // `ratio` values captured back when the block last (re)started. rememberUpdatedState keeps
        // a live reference so each new gesture — and each ratio change mid-session — reads fresh.
        val latestCropRect = rememberUpdatedState(cropRect)
        val latestRatio = rememberUpdatedState(ratio)

        fun displayRectFor(rect: Rect) = Rect(
            offset = imageOffset + Offset(rect.left * scale, rect.top * scale),
            size = Size(rect.width * scale, rect.height * scale),
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmapWidth, bitmapHeight, boxWidthPx, boxHeightPx) {
                    // Accumulated within one continuous gesture; reseeded from the latest state at
                    // the start of every new gesture.
                    var gestureRect = latestCropRect.value
                    detectDragGestures(
                        onDragStart = { start ->
                            gestureRect = latestCropRect.value
                            activeHandle = hitTest(start, displayRectFor(gestureRect), handleTouchPx)
                        },
                        onDragEnd = { activeHandle = null },
                        onDragCancel = { activeHandle = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaBitmap = Offset(dragAmount.x / scale, dragAmount.y / scale)
                            gestureRect = when (val handle = activeHandle) {
                                is CropHandle.Corner ->
                                    resizeCropRect(gestureRect, handle.corner, deltaBitmap, latestRatio.value, bounds, minSizePx)
                                CropHandle.Move -> translateCropRect(gestureRect, deltaBitmap, bounds)
                                null -> gestureRect
                            }
                            onCropRectChange(gestureRect)
                        },
                    )
                },
        ) {
            drawCropOverlay(displayRectFor(cropRect), size)
        }
    }
}

private fun DrawScope.drawCropOverlay(displayRect: Rect, canvasSize: Size) {
    val scrimColor = Color.Black.copy(alpha = 0.55f)
    drawRect(scrimColor, topLeft = Offset(0f, 0f), size = Size(canvasSize.width, displayRect.top.coerceAtLeast(0f)))
    drawRect(
        scrimColor,
        topLeft = Offset(0f, displayRect.bottom),
        size = Size(canvasSize.width, (canvasSize.height - displayRect.bottom).coerceAtLeast(0f)),
    )
    drawRect(
        scrimColor,
        topLeft = Offset(0f, displayRect.top),
        size = Size(displayRect.left.coerceAtLeast(0f), displayRect.height),
    )
    drawRect(
        scrimColor,
        topLeft = Offset(displayRect.right, displayRect.top),
        size = Size((canvasSize.width - displayRect.right).coerceAtLeast(0f), displayRect.height),
    )

    drawRect(Color.White, topLeft = displayRect.topLeft, size = displayRect.size, style = Stroke(width = 2.dp.toPx()))

    val gridColor = Color.White.copy(alpha = 0.6f)
    val gridStroke = 1.dp.toPx()
    for (i in 1..2) {
        val x = displayRect.left + displayRect.width * i / 3f
        drawLine(gridColor, Offset(x, displayRect.top), Offset(x, displayRect.bottom), strokeWidth = gridStroke)
        val y = displayRect.top + displayRect.height * i / 3f
        drawLine(gridColor, Offset(displayRect.left, y), Offset(displayRect.right, y), strokeWidth = gridStroke)
    }

    val handleLength = 18.dp.toPx()
    val handleStroke = 3.dp.toPx()
    val corners = listOf(
        displayRect.topLeft to (Offset(handleLength, 0f) to Offset(0f, handleLength)),
        Offset(displayRect.right, displayRect.top) to (Offset(-handleLength, 0f) to Offset(0f, handleLength)),
        Offset(displayRect.left, displayRect.bottom) to (Offset(handleLength, 0f) to Offset(0f, -handleLength)),
        displayRect.bottomRight to (Offset(-handleLength, 0f) to Offset(0f, -handleLength)),
    )
    for ((corner, arms) in corners) {
        drawLine(Color.White, corner, corner + arms.first, strokeWidth = handleStroke, cap = StrokeCap.Round)
        drawLine(Color.White, corner, corner + arms.second, strokeWidth = handleStroke, cap = StrokeCap.Round)
    }
}

internal sealed interface CropHandle {
    data class Corner(val corner: RectCorner) : CropHandle
    data object Move : CropHandle
}

internal enum class RectCorner { TopLeft, TopRight, BottomLeft, BottomRight }

internal fun hitTest(pos: Offset, displayRect: Rect, handleRadius: Float): CropHandle? {
    val corners = mapOf(
        RectCorner.TopLeft to displayRect.topLeft,
        RectCorner.TopRight to Offset(displayRect.right, displayRect.top),
        RectCorner.BottomLeft to Offset(displayRect.left, displayRect.bottom),
        RectCorner.BottomRight to displayRect.bottomRight,
    )
    corners.forEach { (corner, point) ->
        if ((pos - point).getDistance() <= handleRadius) return CropHandle.Corner(corner)
    }
    return if (displayRect.contains(pos)) CropHandle.Move else null
}

internal fun translateCropRect(rect: Rect, delta: Offset, bounds: Rect): Rect {
    val newLeft = (rect.left + delta.x).coerceIn(bounds.left, (bounds.right - rect.width).coerceAtLeast(bounds.left))
    val newTop = (rect.top + delta.y).coerceIn(bounds.top, (bounds.bottom - rect.height).coerceAtLeast(bounds.top))
    return Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height)
}

internal fun resizeCropRect(
    rect: Rect,
    corner: RectCorner,
    delta: Offset,
    ratio: Float?,
    bounds: Rect,
    minSize: Float,
): Rect {
    val anchor = when (corner) {
        RectCorner.TopLeft -> rect.bottomRight
        RectCorner.TopRight -> Offset(rect.left, rect.bottom)
        RectCorner.BottomLeft -> Offset(rect.right, rect.top)
        RectCorner.BottomRight -> rect.topLeft
    }
    val currentFree = when (corner) {
        RectCorner.TopLeft -> rect.topLeft
        RectCorner.TopRight -> Offset(rect.right, rect.top)
        RectCorner.BottomLeft -> Offset(rect.left, rect.bottom)
        RectCorner.BottomRight -> rect.bottomRight
    }

    val freeX = (currentFree.x + delta.x).coerceIn(bounds.left, bounds.right)
    val freeY = (currentFree.y + delta.y).coerceIn(bounds.top, bounds.bottom)

    var width = abs(freeX - anchor.x)
    var height = abs(freeY - anchor.y)

    if (ratio != null && ratio > 0f) {
        val safeWidth = width.coerceAtLeast(0.0001f)
        val safeHeight = height.coerceAtLeast(0.0001f)
        if (safeWidth / safeHeight > ratio) width = safeHeight * ratio else height = safeWidth / ratio
    }

    width = width.coerceAtLeast(minSize)
    height = height.coerceAtLeast(minSize)

    val signX = if (freeX >= anchor.x) 1f else -1f
    val signY = if (freeY >= anchor.y) 1f else -1f
    val clampedFreeX = (anchor.x + width * signX).coerceIn(bounds.left, bounds.right)
    val clampedFreeY = (anchor.y + height * signY).coerceIn(bounds.top, bounds.bottom)

    val left = min(anchor.x, clampedFreeX)
    val top = min(anchor.y, clampedFreeY)
    val right = max(anchor.x, clampedFreeX)
    val bottom = max(anchor.y, clampedFreeY)
    return Rect(left, top, right, bottom)
}


@Composable
private fun AspectRatioGrid(
    options: List<AspectRatioOption>,
    selected: AspectRatioOption,
    onSelected: (AspectRatioOption) -> Unit,
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.height(164.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(options) { option ->
            AspectRatioChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun AspectRatioChip(option: AspectRatioOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) EditorAccent else EditorControlBackground)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val ratio = option.ratio ?: 1f
        val glyphWidth = if (ratio >= 1f) 22.dp else 22.dp * ratio
        val glyphHeight = if (ratio >= 1f) 22.dp / ratio else 22.dp
        Box(
            modifier = Modifier
                .size(width = glyphWidth, height = glyphHeight)
                .border(width = 1.5.dp, color = if (selected) EditorOnAccent else EditorIconTint, RoundedCornerShape(2.dp)),
        )
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = option.label,
            fontSize = 10.sp,
            color = if (selected) EditorOnAccent else EditorLabelTint,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun CropScreenPreview() {
    ThemePreviews {
        CropContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onCropConfirmed = {})
    }
}
