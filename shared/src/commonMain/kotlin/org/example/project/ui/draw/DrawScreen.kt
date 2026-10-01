package org.example.project.ui.draw

import androidx.compose.foundation.Canvas
import org.example.project.ui.common.scaledBitmap
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_eraser
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_trash
import photocollagemaker.shared.generated.resources.ic_undo

private enum class DrawTab { Paint, Mosaic }

/** Share of the screen width taken by the Paint/Mosaic toggle and the tool icons under it. */
private const val ToolClusterWidthFraction = 0.64f

@Composable
fun DrawScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrawViewModel = koinViewModel(),
) {
    DrawContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { actions ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyDrawing(bakeDrawing(source = image, actions = actions))
            }
            onApplied()
        },
        modifier = modifier,
    )
}

/**
 * The bitmap a [pattern] stroke reveals in the preview, rendered at [base]'s (the canvas's) own
 * size so it can be an [ImageShader] lined up 1:1 with the canvas. Same patterns as [renderMosaic]:
 * pixelate cells are fractions of the width and textures use normalized coordinates, so the
 * preview-sized render looks just like the full-resolution bake.
 */
private fun renderPreviewMosaic(base: ImageBitmap, pattern: MosaicPattern): ImageBitmap = when (pattern) {
    is PixelateMosaic -> computeMosaicBitmap(base, pattern.cellFractionX, pattern.cellFractionY)
    is TextureMosaic -> renderMosaicTexture(pattern.texture, base.width, base.height)
}

@Composable
private fun DrawContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (actions: List<DrawAction>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var actions by remember { mutableStateOf(emptyList<DrawAction>()) }
    var undoStack by remember { mutableStateOf(emptyList<List<DrawAction>>()) }
    var redoStack by remember { mutableStateOf(emptyList<List<DrawAction>>()) }

    fun commit(newActions: List<DrawAction>) {
        undoStack = undoStack + listOf(actions)
        redoStack = emptyList()
        actions = newActions
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = redoStack + listOf(actions)
        actions = previous
        undoStack = undoStack.dropLast(1)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = undoStack + listOf(actions)
        actions = next
        redoStack = redoStack.dropLast(1)
    }

    var selectedTab by remember { mutableStateOf(DrawTab.Paint) }
    var selectedColor by remember { mutableStateOf(DrawColorDefault) }
    var selectedPattern by remember { mutableStateOf(MosaicPatternDefault) }
    var isEraserActive by remember { mutableStateOf(false) }
    var brushSize by remember { mutableFloatStateOf(BrushSizeDefault) }
    // The stroke in progress. Only the canvas's draw phase reads these, so a finger move redraws
    // the canvas without recomposing the screen.
    val livePoints = remember { mutableStateListOf<Offset>() }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    fun buildAction(points: List<Offset>): DrawAction {
        val radiusFraction = brushRadiusFraction(brushSize)
        return when {
            isEraserActive -> EraseAction(points, radiusFraction)
            selectedTab == DrawTab.Paint -> PaintAction(points, radiusFraction, selectedColor.color)
            else -> MosaicAction(points, radiusFraction, selectedPattern)
        }
    }

    // Every bitmap below is rendered off the main thread: a full-size photo copy or a per-pixel
    // texture takes long enough on the main thread to freeze touch handling.
    val sharpImage by produceState<ImageBitmap?>(null, sourceImage) {
        value = sourceImage?.let { withContext(Dispatchers.Default) { copyBitmap(it) } }
    }
    // The photo at the canvas's pixel size: drawn 1:1 each frame, and the erase brush's shader.
    val previewBase by produceState<ImageBitmap?>(null, sharpImage, canvasSize) {
        val image = sharpImage
        value = if (image == null || canvasSize.width <= 0 || canvasSize.height <= 0) {
            null
        } else {
            withContext(Dispatchers.Default) { scaledBitmap(image, canvasSize.width, canvasSize.height) }
        }
    }
    val eraseBrush = remember(previewBase) { previewBase?.let { ShaderBrush(ImageShader(it)) } }
    // Mosaic brushes are rendered lazily, only for patterns picked or already on the canvas, since
    // rendering every pattern up front would hold a dozen canvas-sized bitmaps for nothing.
    val mosaicBrushes = remember(previewBase) { mutableStateMapOf<MosaicPattern, ShaderBrush>() }
    LaunchedEffect(previewBase, selectedPattern, selectedTab, actions) {
        val base = previewBase ?: return@LaunchedEffect
        val needed = actions.filterIsInstance<MosaicAction>().map { it.pattern }.toMutableSet()
        // Warms the picked pattern on selection, so the first stroke has it ready.
        if (selectedTab == DrawTab.Mosaic) needed += selectedPattern
        for (pattern in needed - mosaicBrushes.keys) {
            val bitmap = withContext(Dispatchers.Default) { renderPreviewMosaic(base, pattern) }
            mosaicBrushes[pattern] = ShaderBrush(ImageShader(bitmap))
        }
    }
    val mosaicSwatches by produceState(emptyMap<MosaicPattern, ImageBitmap>(), sharpImage) {
        val image = sharpImage ?: return@produceState
        value = withContext(Dispatchers.Default) { MosaicPatterns.associateWith { renderMosaicSwatch(image, it) } }
    }
    // Committed strokes only change on commit/undo/redo, so their paths are built once, not per frame.
    val committedPaths = remember(actions, canvasSize) {
        val size = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        actions.map { strokePolyline(it.points, size) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Draw",
            onClose = onBack,
            onDone = { onDone(actions) },
            doneEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            val sharp = sharpImage
            if (sourceImage != null && sharp != null) {
                // aspectRatio sizes the canvas to the photo itself, so the canvas bounds are the
                // image bounds (no letterbox offset) and the rounded clip follows the photo's edges.
                Canvas(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            fun fractionFor(position: Offset): Offset = Offset(
                                (position.x / size.width).coerceIn(0f, 1f),
                                (position.y / size.height).coerceIn(0f, 1f),
                            )
                            var lastKeptPx = Offset.Zero
                            detectDragGestures(
                                onDragStart = { position ->
                                    isDragging = true
                                    cursorPositionPx = position
                                    lastKeptPx = position
                                    livePoints.clear()
                                    livePoints += fractionFor(position)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (livePoints.isNotEmpty()) {
                                        commit(actions + buildAction(livePoints.toList()))
                                    }
                                    livePoints.clear()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    livePoints.clear()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    cursorPositionPx = change.position
                                    // Spacing points by a quarter radius keeps the stroke smooth
                                    // while a long stroke stays a few hundred points, not thousands.
                                    val radiusPx = brushRadiusFraction(brushSize) * size.width
                                    if (isFarEnough(lastKeptPx, change.position, maxOf(2f, radiusPx * 0.25f))) {
                                        lastKeptPx = change.position
                                        livePoints += fractionFor(change.position)
                                    }
                                },
                            )
                        },
                ) {
                    val dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
                    val base = previewBase
                    drawImageScaled(base ?: sharp, IntOffset.Zero, dstSize)

                    fun drawAction(action: DrawAction, path: Path) {
                        val stroke = brushStroke(action.radiusFraction * size.width)
                        when (action) {
                            is PaintAction -> drawPath(path, color = action.color, style = stroke)
                            // Not ready yet (rendering in the background): the stroke appears once it is.
                            is MosaicAction -> mosaicBrushes[action.pattern]?.let { drawPath(path, brush = it, style = stroke) }
                            is EraseAction -> eraseBrush?.let { drawPath(path, brush = it, style = stroke) }
                        }
                    }

                    if (committedPaths.size == actions.size) {
                        actions.forEachIndexed { index, action -> drawAction(action, committedPaths[index]) }
                    }
                    if (livePoints.isNotEmpty()) {
                        val points = livePoints.toList()
                        drawAction(buildAction(points), strokePolyline(points, size))
                    }
                    if (isDragging || isAdjustingBrush) {
                        drawCircle(
                            color = Color.White,
                            radius = brushRadiusFraction(brushSize) * size.width,
                            center = if (isDragging) cursorPositionPx else center,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            } else {
                Text(
                    text = "No image to draw on",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        DrawTabToggle(
            selected = selectedTab,
            onSelected = { tab ->
                selectedTab = tab
                isEraserActive = false
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp),
        )

        DrawToolRow(
            undoEnabled = undoStack.isNotEmpty(),
            redoEnabled = redoStack.isNotEmpty(),
            deleteEnabled = actions.isNotEmpty(),
            eraserActive = isEraserActive,
            onUndo = ::undo,
            onRedo = ::redo,
            onDelete = { if (actions.isNotEmpty()) commit(emptyList()) },
            onToggleEraser = { isEraserActive = !isEraserActive },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp),
        )

        BrushSizeLabelRow(value = brushSize)
        CenterFillSlider(
            value = brushSize,
            onValueChange = { brushSize = it },
            range = BrushSizeRange,
            referenceValue = BrushSizeRange.start,
            onDraggingChange = { isAdjustingBrush = it },
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        )

        when (selectedTab) {
            DrawTab.Paint -> DrawColorRow(
                selected = selectedColor,
                onSelected = { selectedColor = it; isEraserActive = false },
            )
            DrawTab.Mosaic -> MosaicPatternRow(
                swatches = mosaicSwatches,
                selected = selectedPattern,
                onSelected = { selectedPattern = it; isEraserActive = false },
            )
        }
    }
}

/** Pill-shaped Paint/Mosaic segmented toggle; the selected half gets a soft accent pill. */
@Composable
private fun DrawTabToggle(selected: DrawTab, onSelected: (DrawTab) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth(ToolClusterWidthFraction)
            .clip(RoundedCornerShape(50))
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp),
    ) {
        for (tab in DrawTab.entries) {
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) scheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(onClick = { onSelected(tab) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DrawToolRow(
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    deleteEnabled: Boolean,
    eraserActive: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDelete: () -> Unit,
    onToggleEraser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(ToolClusterWidthFraction),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UndoRedoButton(icon = vectorResource(Res.drawable.ic_undo), contentDescription = "Undo", enabled = undoEnabled, onClick = onUndo)
        UndoRedoButton(icon = vectorResource(Res.drawable.ic_redo), contentDescription = "Redo", enabled = redoEnabled, onClick = onRedo)
        DrawToolIcon(icon = vectorResource(Res.drawable.ic_trash), contentDescription = "Clear all", enabled = deleteEnabled, onClick = onDelete)
        DrawToolIcon(icon = vectorResource(Res.drawable.ic_eraser), contentDescription = "Eraser", selected = eraserActive, onClick = onToggleEraser)
    }
}

/** Bare outline tool icon; [selected] (the eraser toggle) tints it and adds a soft accent circle. */
@Composable
private fun DrawToolIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        selected -> scheme.primary
        enabled -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.3f)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) scheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BrushSizeLabelRow(value: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Brush Size",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.primary,
        )
    }
}

@Composable
private fun DrawColorRow(selected: DrawColorOption, onSelected: (DrawColorOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(DrawColors) { option ->
            SelectableSwatch(selected = option == selected, size = 48.dp, onClick = { onSelected(option) }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(option.color)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(SwatchInnerCorner)),
                )
            }
        }
    }
}

@Composable
private fun MosaicPatternRow(
    swatches: Map<MosaicPattern, ImageBitmap>,
    selected: MosaicPattern,
    onSelected: (MosaicPattern) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(MosaicPatterns) { pattern ->
            SelectableSwatch(selected = pattern == selected, size = 58.dp, onClick = { onSelected(pattern) }) {
                val swatch = swatches[pattern]
                if (swatch != null) {
                    Image(
                        bitmap = swatch,
                        contentDescription = pattern.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                }
            }
        }
    }
}

@Preview
@Composable
private fun DrawScreenPreview() {
    ThemePreviews {
        DrawContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = {})
    }
}
