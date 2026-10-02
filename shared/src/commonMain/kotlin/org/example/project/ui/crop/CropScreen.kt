package org.example.project.ui.crop

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import org.example.project.ui.common.navSharedElement
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_flip_horizontally
import photocollagemaker.shared.generated.resources.ic_flip_vertically
import photocollagemaker.shared.generated.resources.ic_rotate_right

/** Gap between the photo and the edges of the grey stage, so the corner handles stay grabbable. */
internal val CropStageInset = 24.dp

/**
 * The full-screen crop UI (the background remover's first step): top bar, stage, ratio strip. The
 * photo editor's Crop tool is [CropTool], built from the same [CropState] and [CropStage].
 *
 * @param showTransformTools adds a rotate / flip-horizontal / flip-vertical row under the top bar,
 * see [CropState.transform].
 */
@Composable
internal fun CropContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onCropConfirmed: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
    title: String = tr("Crop"),
    showTransformTools: Boolean = false,
    // When set, the area inside the crop rectangle is a shared element under this key, so it can
    // morph into the next screen's photo (the background remover's eraser).
    cropSharedKey: Any? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val state = remember(sourceImage) { CropState(sourceImage) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = title,
            onClose = onBack,
            doneEnabled = state.canCrop,
            onDone = { state.crop()?.let(onCropConfirmed) },
        )

        if (showTransformTools) {
            CropTransformRow(
                enabled = state.image != null,
                onRotate = { state.transform(1, flipHorizontal = false, flipVertical = false) },
                onFlipHorizontal = { state.transform(0, flipHorizontal = true, flipVertical = false) },
                onFlipVertical = { state.transform(0, flipHorizontal = false, flipVertical = true) },
            )
        }

        CropStage(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f)),
            cropSharedKey = cropSharedKey,
        )

        AspectRatioStrip(
            options = AspectRatioOptions,
            selected = state.selectedOption,
            onSelected = state::selectOption,
        )
    }
}

/** The photo with its draggable crop rect over it, inset from the edges by [CropStageInset]. */
@Composable
internal fun CropStage(state: CropState, modifier: Modifier = Modifier, cropSharedKey: Any? = null) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        val image = state.image
        if (image != null) {
            CropCanvas(
                image = image,
                ratio = state.selectedOption.ratio,
                cropRect = state.cropRect ?: fullImageRect(image),
                onCropRectChange = state::updateCropRect,
                accent = scheme.primary,
                cropSharedKey = cropSharedKey,
            )
        } else {
            Text(
                text = tr("No image to crop"),
                color = scheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** The photo editor's Crop tool, opened inside the editor (see [ToolScaffold]). */
@Composable
internal fun CropTool(
    sourceImage: ImageBitmap,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val state = remember(sourceImage) { CropState(sourceImage) }

    ToolScaffold(modifier = modifier, photoInset = CropStageInset) {
        CropStage(
            state = state,
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f)),
        )

        ToolPanel(
            title = tr("Crop"),
            onClose = onClose,
            onDone = { state.crop()?.let(onApply) },
            doneEnabled = state.canCrop,
        ) {
            AspectRatioStrip(
                options = AspectRatioOptions,
                selected = state.selectedOption,
                onSelected = state::selectOption,
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

/** Rotate 90° clockwise, flip horizontally and flip vertically, as outlined square buttons. */
@Composable
private fun CropTransformRow(
    enabled: Boolean,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
    ) {
        CropTransformButton(vectorResource(Res.drawable.ic_rotate_right), tr("Rotate"), enabled, onRotate)
        CropTransformButton(vectorResource(Res.drawable.ic_flip_horizontally), tr("Flip horizontally"), enabled, onFlipHorizontal)
        CropTransformButton(vectorResource(Res.drawable.ic_flip_vertically), tr("Flip vertically"), enabled, onFlipVertical)
    }
}

@Composable
private fun CropTransformButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(shape)
            .border(width = 1.dp, color = onSurface.copy(alpha = if (enabled) 0.7f else 0.25f), shape = shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) onSurface else onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CropCanvas(
    image: ImageBitmap,
    ratio: Float?,
    cropRect: Rect,
    onCropRectChange: (Rect) -> Unit,
    accent: Color,
    cropSharedKey: Any?,
) {
    // The canvas spans the whole stage while the photo is inset by StageInset, so handles sitting
    // on the photo's edge still receive touches on their outer half.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val boxWidthPx = with(density) { maxWidth.toPx() }
        val boxHeightPx = with(density) { maxHeight.toPx() }
        val insetPx = with(density) { CropStageInset.toPx() }
        val bitmapWidth = image.width.toFloat()
        val bitmapHeight = image.height.toFloat()
        val scale = min((boxWidthPx - 2 * insetPx) / bitmapWidth, (boxHeightPx - 2 * insetPx) / bitmapHeight)
        val displayWidth = bitmapWidth * scale
        val displayHeight = bitmapHeight * scale
        val imageOffset = Offset((boxWidthPx - displayWidth) / 2f, (boxHeightPx - displayHeight) / 2f)
        val imageDisplayRect = Rect(imageOffset, Size(displayWidth, displayHeight))
        val bounds = remember(bitmapWidth, bitmapHeight) { Rect(0f, 0f, bitmapWidth, bitmapHeight) }
        val handleTouchPx = with(density) { 28.dp.toPx() }
        val minSizePx = with(density) { 40.dp.toPx() } / scale

        // Padding + Fit centers the photo exactly where imageOffset/scale above put it.
        Image(
            bitmap = image,
            contentDescription = tr("Photo to crop"),
            modifier = Modifier.fillMaxSize().padding(CropStageInset),
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

        if (cropSharedKey != null) {
            // The shared element: a box exactly over the crop rectangle, showing the same pixels
            // the photo underneath shows there (the whole photo again, shifted so the cropped part
            // lands in the box, which clips the rest). At rest it is indistinguishable from the
            // photo below it; in a transition it is the cropped picture that flies to the next
            // screen, or that the next screen's photo shrinks back into. It sits under the handle
            // canvas, so the crop frame still draws over it.
            val cropDisplayRect = displayRectFor(cropRect)
            Box(
                modifier = Modifier
                    .offset { IntOffset(cropDisplayRect.left.roundToInt(), cropDisplayRect.top.roundToInt()) }
                    .size(with(density) { cropDisplayRect.width.toDp() }, with(density) { cropDisplayRect.height.toDp() })
                    .navSharedElement(cropSharedKey)
                    .clipToBounds(),
            ) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        .offset {
                            IntOffset(
                                (imageOffset.x - cropDisplayRect.left).roundToInt(),
                                (imageOffset.y - cropDisplayRect.top).roundToInt(),
                            )
                        }
                        .size(with(density) { displayWidth.toDp() }, with(density) { displayHeight.toDp() }),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }

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
                                is CropHandle.Edge ->
                                    resizeCropEdge(gestureRect, handle.edge, deltaBitmap, latestRatio.value, bounds, minSizePx)
                                CropHandle.Move -> translateCropRect(gestureRect, deltaBitmap, bounds)
                                null -> gestureRect
                            }
                            onCropRectChange(gestureRect)
                        },
                    )
                },
        ) {
            drawCropOverlay(displayRectFor(cropRect), imageDisplayRect, accent)
        }
    }
}

/**
 * Dims the part of the photo outside [cropRect] (the grey stage around the photo is left alone),
 * then draws the accent border, rule-of-thirds grid, inset corner brackets and white edge bars.
 */
private fun DrawScope.drawCropOverlay(cropRect: Rect, imageRect: Rect, accent: Color) {
    val scrim = Color.Black.copy(alpha = 0.5f)
    drawRect(scrim, imageRect.topLeft, Size(imageRect.width, (cropRect.top - imageRect.top).coerceAtLeast(0f)))
    drawRect(
        scrim,
        Offset(imageRect.left, cropRect.bottom),
        Size(imageRect.width, (imageRect.bottom - cropRect.bottom).coerceAtLeast(0f)),
    )
    drawRect(scrim, Offset(imageRect.left, cropRect.top), Size((cropRect.left - imageRect.left).coerceAtLeast(0f), cropRect.height))
    drawRect(
        scrim,
        Offset(cropRect.right, cropRect.top),
        Size((imageRect.right - cropRect.right).coerceAtLeast(0f), cropRect.height),
    )

    val gridStroke = 1.dp.toPx()
    for (i in 1..2) {
        val x = cropRect.left + cropRect.width * i / 3f
        drawLine(accent, Offset(x, cropRect.top), Offset(x, cropRect.bottom), strokeWidth = gridStroke)
        val y = cropRect.top + cropRect.height * i / 3f
        drawLine(accent, Offset(cropRect.left, y), Offset(cropRect.right, y), strokeWidth = gridStroke)
    }
    drawRect(accent, topLeft = cropRect.topLeft, size = cropRect.size, style = Stroke(width = 3.dp.toPx()))

    val inset = 8.dp.toPx()
    val arm = 14.dp.toPx()
    val bracketStroke = 3.dp.toPx()
    val inner = Rect(cropRect.left + inset, cropRect.top + inset, cropRect.right - inset, cropRect.bottom - inset)
    val brackets = listOf(
        inner.topLeft to Offset(1f, 1f),
        inner.topRight to Offset(-1f, 1f),
        inner.bottomLeft to Offset(1f, -1f),
        inner.bottomRight to Offset(-1f, -1f),
    )
    for ((corner, dir) in brackets) {
        drawLine(accent, corner, corner + Offset(arm * dir.x, 0f), strokeWidth = bracketStroke, cap = StrokeCap.Square)
        drawLine(accent, corner, corner + Offset(0f, arm * dir.y), strokeWidth = bracketStroke, cap = StrokeCap.Square)
    }

    val bar = 11.dp.toPx()
    val barStroke = 6.dp.toPx()
    val center = cropRect.center
    listOf(Offset(center.x, cropRect.top), Offset(center.x, cropRect.bottom)).forEach { mid ->
        drawLine(Color.White, mid - Offset(bar, 0f), mid + Offset(bar, 0f), strokeWidth = barStroke, cap = StrokeCap.Round)
    }
    listOf(Offset(cropRect.left, center.y), Offset(cropRect.right, center.y)).forEach { mid ->
        drawLine(Color.White, mid - Offset(0f, bar), mid + Offset(0f, bar), strokeWidth = barStroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun AspectRatioStrip(
    options: List<AspectRatioOption>,
    selected: AspectRatioOption,
    onSelected: (AspectRatioOption) -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(options) { option ->
            AspectRatioChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

/**
 * A rounded frame shaped like the ratio it stands for, with the platform glyph inside (or "Free"
 * for free-form) and the ratio label underneath.
 */
@Composable
private fun AspectRatioChip(option: AspectRatioOption, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.primary else scheme.onSurface
    val frame = ratioFrameSize(option.ratio)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Fixed-height slot so frames of different heights stay vertically centered on one line
        // and their labels share a baseline.
        Box(modifier = Modifier.height(48.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(frame)
                    .border(width = 1.5.dp, color = tint, shape = RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    option.ratio == null -> Text(text = tr(option.label), color = tint, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    option.platform != null -> PlatformGlyph(platform = option.platform, color = tint)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            // Free already shows its label inside the frame; an empty line keeps the row aligned.
            text = if (option.ratio == null) "" else option.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * Chip frame size for [ratio]: wide ratios grow to 42dp wide, tall ones to 46dp high, from a 34dp
 * square baseline. Free-form uses a slightly larger square to fit its label.
 */
private fun ratioFrameSize(ratio: Float?): DpSize {
    if (ratio == null) return DpSize(36.dp, 36.dp)
    return if (ratio >= 1f) {
        val width = minOf(34.dp * ratio, 42.dp)
        DpSize(width, width / ratio)
    } else {
        val height = minOf(34.dp / ratio, 46.dp)
        DpSize(height * ratio, height)
    }
}

/** Simple outline marks for the platform a ratio is meant for, drawn rather than shipped as assets. */
@Composable
private fun PlatformGlyph(platform: RatioPlatform, color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val stroke = Stroke(width = 1.3.dp.toPx())
        when (platform) {
            RatioPlatform.Instagram -> {
                drawRoundRect(color, cornerRadius = CornerRadius(4.dp.toPx()), style = stroke)
                drawCircle(color, radius = size.minDimension * 0.22f, style = stroke)
                drawCircle(color, radius = 0.9.dp.toPx(), center = Offset(size.width * 0.76f, size.height * 0.24f))
            }
            RatioPlatform.YouTube -> {
                val height = size.height * 0.72f
                val top = (size.height - height) / 2f
                drawRoundRect(color, topLeft = Offset(0f, top), size = Size(size.width, height), cornerRadius = CornerRadius(3.dp.toPx()), style = stroke)
                val play = Path().apply {
                    moveTo(size.width * 0.42f, size.height * 0.36f)
                    lineTo(size.width * 0.66f, size.height * 0.5f)
                    lineTo(size.width * 0.42f, size.height * 0.64f)
                    close()
                }
                drawPath(play, color)
            }
        }
    }
}

@Preview
@Composable
private fun CropScreenPreview() {
    ThemePreviews {
        CropContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onCropConfirmed = {})
    }
}
