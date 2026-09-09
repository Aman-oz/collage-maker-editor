package org.example.project.ui.blur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/** The blur level and completed erase strokes together form one undo/redo checkpoint. */
private data class BlurEdit(val blurLevel: Int, val strokes: List<BlurStroke>)

@Composable
fun BlurScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlurViewModel = koinViewModel(),
) {
    BlurContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { blurLevel, strokes ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyBlur(bakeBlur(source = image, blurLevel = blurLevel, strokes = strokes))
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun BlurContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (blurLevel: Int, strokes: List<BlurStroke>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var edit by remember { mutableStateOf(BlurEdit(BlurLevelDefault, emptyList())) }
    var undoStack by remember { mutableStateOf(emptyList<BlurEdit>()) }
    var redoStack by remember { mutableStateOf(emptyList<BlurEdit>()) }

    fun commit(newEdit: BlurEdit) {
        undoStack = undoStack + edit
        redoStack = emptyList()
        edit = newEdit
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = redoStack + edit
        edit = previous
        undoStack = undoStack.dropLast(1)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = undoStack + edit
        edit = next
        redoStack = redoStack.dropLast(1)
    }

    var brushSize by remember { mutableFloatStateOf(BrushSizeDefault) }
    var currentStroke by remember { mutableStateOf(emptyList<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }

    val blurredImage = remember(sourceImage, edit.blurLevel) {
        sourceImage?.let { computeBlurredBitmap(it, edit.blurLevel) }
    }
    val sharpImage = remember(sourceImage) {
        sourceImage?.let { copyBitmap(it) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        BlurTopBar(
            onBack = onBack,
            onDone = { onDone(edit.blurLevel, edit.strokes) },
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
            if (sourceImage != null && blurredImage != null && sharpImage != null) {
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

                fun fractionFor(positionInBox: Offset): Offset = Offset(
                    ((positionInBox.x - imageOffsetPx.x) / imageWidthPx).coerceIn(0f, 1f),
                    ((positionInBox.y - imageOffsetPx.y) / imageHeightPx).coerceIn(0f, 1f),
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageOffsetPx, imageWidthPx, imageHeightPx) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    isDragging = true
                                    cursorPositionPx = position
                                    currentStroke = listOf(fractionFor(position))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (currentStroke.isNotEmpty()) {
                                        val stroke = BlurStroke(currentStroke, brushRadiusFraction(brushSize))
                                        commit(edit.copy(strokes = edit.strokes + stroke))
                                    }
                                    currentStroke = emptyList()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    currentStroke = emptyList()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    cursorPositionPx = change.position
                                    currentStroke = currentStroke + fractionFor(change.position)
                                },
                            )
                        },
                ) {
                    val committedPaths = edit.strokes.map { stroke -> buildStrokePath(stroke, imageOffsetPx, imageSizePx) }
                    val livePath = if (currentStroke.isNotEmpty()) {
                        listOf(buildStrokePath(currentStroke, imageOffsetPx, imageSizePx, brushRadiusPx))
                    } else {
                        emptyList()
                    }
                    drawBlurWithReveal(
                        sharpImage = sharpImage,
                        blurredImage = blurredImage,
                        imageOffset = imageOffsetPx,
                        imageSize = imageSizePx,
                        revealPaths = committedPaths + livePath,
                    )
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
                    text = "No image to blur",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        BlurLevelStepper(
            value = edit.blurLevel,
            onValueChange = { commit(edit.copy(blurLevel = it.coerceIn(BlurLevelMin, BlurLevelMax))) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        UndoRedoRow(
            undoEnabled = undoStack.isNotEmpty(),
            redoEnabled = redoStack.isNotEmpty(),
            onUndo = ::undo,
            onRedo = ::redo,
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

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun BlurTopBar(onBack: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Blur",
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
private fun BlurLevelStepper(value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            contentDescription = "Decrease blur",
            enabled = value > BlurLevelMin,
            onClick = { onValueChange(value - 1) },
        )
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        StepperButton(
            icon = Icons.Filled.Add,
            contentDescription = "Increase blur",
            enabled = value < BlurLevelMax,
            onClick = { onValueChange(value + 1) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(EditorControlBackground),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun UndoRedoRow(undoEnabled: Boolean, redoEnabled: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        EditorCircleIconButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            enabled = undoEnabled,
            onClick = onUndo,
        )
        Spacer(modifier = Modifier.width(16.dp))
        EditorCircleIconButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            enabled = redoEnabled,
            onClick = onRedo,
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

@Preview
@Composable
private fun BlurScreenPreview() {
    ThemePreviews {
        BlurContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _ -> })
    }
}
