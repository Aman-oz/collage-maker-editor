package org.example.project.ui.draw

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

private enum class DrawTab { Paint, Mosaic }

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
        onDone = { actions, canvasWidthPx ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyDrawing(bakeDrawing(source = image, actions = actions, previewCanvasWidthPx = canvasWidthPx))
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun DrawContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (actions: List<DrawAction>, canvasWidthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
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
    var currentPoints by remember { mutableStateOf(emptyList<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }
    var displayedImageWidthPx by remember { mutableFloatStateOf(0f) }

    fun buildAction(points: List<Offset>): DrawAction {
        val radiusFraction = brushRadiusFraction(brushSize)
        return when {
            isEraserActive -> EraseAction(points, radiusFraction)
            selectedTab == DrawTab.Paint -> PaintAction(points, radiusFraction, selectedColor.color)
            else -> MosaicAction(points, radiusFraction, selectedPattern)
        }
    }

    val sharpImage = remember(sourceImage) { sourceImage?.let { copyBitmap(it) } }
    val mosaicPreviewBitmaps = remember(sourceImage) {
        sourceImage?.let { image -> MosaicPatterns.associateWith { computeMosaicBitmap(image, it.cellFractionX, it.cellFractionY) } }
    } ?: emptyMap()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        DrawTopBar(
            onBack = onBack,
            onDone = { onDone(actions, displayedImageWidthPx) },
            doneEnabled = sourceImage != null,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && sharpImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val bitmapWidth = sourceImage.width.toFloat()
                val bitmapHeight = sourceImage.height.toFloat()
                val fitScale = min(boxWidthPx / bitmapWidth, boxHeightPx / bitmapHeight)
                val imageWidthPx = bitmapWidth * fitScale
                val imageHeightPx = bitmapHeight * fitScale
                val imageOffsetPx = Offset((boxWidthPx - imageWidthPx) / 2f, (boxHeightPx - imageHeightPx) / 2f)
                val imageSizePx = Size(imageWidthPx, imageHeightPx)
                val brushRadiusPx = brushRadiusFraction(brushSize) * imageWidthPx
                val imageCenterPx = imageOffsetPx + Offset(imageWidthPx / 2f, imageHeightPx / 2f)
                val dstOffset = IntOffset(imageOffsetPx.x.roundToInt(), imageOffsetPx.y.roundToInt())
                val dstSize = IntSize(imageWidthPx.roundToInt().coerceAtLeast(1), imageHeightPx.roundToInt().coerceAtLeast(1))
                LaunchedEffect(imageWidthPx) { displayedImageWidthPx = imageWidthPx }

                fun fractionFor(positionInBox: Offset): Offset = Offset(
                    ((positionInBox.x - imageOffsetPx.x) / imageWidthPx).coerceIn(0f, 1f),
                    ((positionInBox.y - imageOffsetPx.y) / imageHeightPx).coerceIn(0f, 1f),
                )

                val liveAction = if (currentPoints.isNotEmpty()) buildAction(currentPoints) else null

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageOffsetPx, imageWidthPx, imageHeightPx) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    isDragging = true
                                    cursorPositionPx = position
                                    currentPoints = listOf(fractionFor(position))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (currentPoints.isNotEmpty()) {
                                        commit(actions + buildAction(currentPoints))
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    currentPoints = emptyList()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    cursorPositionPx = change.position
                                    currentPoints = currentPoints + fractionFor(change.position)
                                },
                            )
                        },
                ) {
                    drawImageScaled(sharpImage, dstOffset, dstSize)
                    for (action in actions + listOfNotNull(liveAction)) {
                        val radiusPx = action.radiusFraction * imageWidthPx
                        val path = buildStrokePath(action.points, imageOffsetPx, imageSizePx, radiusPx)
                        when (action) {
                            is PaintAction -> drawPath(path, color = action.color)
                            is MosaicAction -> clipPath(path) {
                                drawImageScaled(mosaicPreviewBitmaps.getValue(action.pattern), dstOffset, dstSize)
                            }
                            is EraseAction -> clipPath(path) {
                                drawImageScaled(sharpImage, dstOffset, dstSize)
                            }
                        }
                    }
                    if (isDragging || isAdjustingBrush) {
                        drawCircle(
                            color = Color.White,
                            radius = brushRadiusPx,
                            center = if (isDragging) cursorPositionPx else imageCenterPx,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            } else {
                Text(
                    text = "No image to draw on",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        DrawTabRow(
            selected = selectedTab,
            onSelected = { tab ->
                selectedTab = tab
                isEraserActive = false
            },
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
        )

        Spacer(modifier = Modifier.height(8.dp))

        BrushSizeControlRow(value = brushSize)
        CenterFillSlider(
            value = brushSize,
            onValueChange = { brushSize = it },
            range = BrushSizeRange,
            referenceValue = BrushSizeRange.start,
            onDraggingChange = { isAdjustingBrush = it },
        )

        when (selectedTab) {
            DrawTab.Paint -> DrawColorRow(
                selected = selectedColor,
                onSelected = { selectedColor = it; isEraserActive = false },
            )
            DrawTab.Mosaic -> MosaicPatternRow(
                selected = selectedPattern,
                onSelected = { selectedPattern = it; isEraserActive = false },
            )
        }
    }
}

@Composable
private fun DrawTopBar(onBack: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Draw",
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

        AccentPillButton(text = "Done", onClick = onDone, enabled = doneEnabled)
    }
}

@Composable
private fun DrawTabRow(selected: DrawTab, onSelected: (DrawTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(50))
            .background(EditorControlBackground),
    ) {
        DrawTabItem(
            label = "Paint",
            selected = selected == DrawTab.Paint,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(DrawTab.Paint) },
        )
        DrawTabItem(
            label = "Mosaic",
            selected = selected == DrawTab.Mosaic,
            modifier = Modifier.weight(1f),
            onClick = { onSelected(DrawTab.Mosaic) },
        )
    }
}

@Composable
private fun DrawTabItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EditorAccent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) EditorOnAccent else EditorLabelTint,
        )
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", enabled = undoEnabled, onClick = onUndo)
        Spacer(modifier = Modifier.width(16.dp))
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", enabled = redoEnabled, onClick = onRedo)
        Spacer(modifier = Modifier.width(16.dp))
        EditorCircleIconButton(icon = Icons.Filled.Delete, contentDescription = "Clear all", enabled = deleteEnabled, onClick = onDelete)
        Spacer(modifier = Modifier.width(16.dp))
        DrawToolToggleButton(icon = Icons.AutoMirrored.Filled.Backspace, contentDescription = "Eraser", selected = eraserActive, onClick = onToggleEraser)
    }
}

@Composable
private fun DrawToolToggleButton(icon: ImageVector, contentDescription: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (selected) EditorAccent else EditorControlBackground),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) EditorOnAccent else EditorIconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun BrushSizeControlRow(value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Brush Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorAccent,
        )
    }
}

@Composable
private fun DrawColorRow(selected: DrawColorOption, onSelected: (DrawColorOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(DrawColors) { option ->
            DrawColorSwatch(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun DrawColorSwatch(option: DrawColorOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 40.dp else 44.dp)
                .clip(CircleShape)
                .background(option.color)
                .border(width = 1.dp, color = EditorLabelTint.copy(alpha = 0.3f), shape = CircleShape),
        )
    }
}

@Composable
private fun MosaicPatternRow(selected: MosaicPattern, onSelected: (MosaicPattern) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(MosaicPatterns) { pattern ->
            MosaicPatternSwatch(pattern = pattern, selected = pattern == selected, onClick = { onSelected(pattern) })
        }
    }
}

@Composable
private fun MosaicPatternSwatch(pattern: MosaicPattern, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp)),
        ) {
            val columns = 4
            val rows = 4
            val cellWidth = size.width / columns
            val cellHeight = size.height / rows
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val color = pattern.previewColors[(row * columns + column) % pattern.previewColors.size]
                    drawRect(
                        color = color,
                        topLeft = Offset(column * cellWidth, row * cellHeight),
                        size = Size(cellWidth, cellHeight),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DrawScreenPreview() {
    ThemePreviews {
        DrawContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _ -> })
    }
}
