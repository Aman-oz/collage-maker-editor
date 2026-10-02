package org.example.project.ui.blur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/** The blur level and completed erase strokes together form one undo/redo checkpoint. */
private data class BlurEdit(val blurLevel: Int, val strokes: List<BlurStroke>)

@Composable
internal fun BlurTool(
    sourceImage: ImageBitmap,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    BlurContent(
        sourceImage = sourceImage,
        onBack = onClose,
        onDone = { blurLevel, strokes -> onApply(bakeBlur(source = sourceImage, blurLevel = blurLevel, strokes = strokes)) },
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

    val scheme = MaterialTheme.colorScheme

    ToolScaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && blurredImage != null && sharpImage != null) {
                // aspectRatio sizes the canvas to the photo itself, so the rounded clip follows the
                // photo's edges and stroke fractions map straight onto the canvas with no letterbox.
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    val density = LocalDensity.current
                    val imageWidthPx = with(density) { maxWidth.toPx() }
                    val imageHeightPx = with(density) { maxHeight.toPx() }
                    val imageSizePx = Size(imageWidthPx, imageHeightPx)
                    val brushRadiusPx = brushRadiusFraction(brushSize) * imageWidthPx
                    val imageCenterPx = Offset(imageWidthPx / 2f, imageHeightPx / 2f)

                    fun fractionFor(position: Offset): Offset = Offset(
                        (position.x / imageWidthPx).coerceIn(0f, 1f),
                        (position.y / imageHeightPx).coerceIn(0f, 1f),
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(imageWidthPx, imageHeightPx) {
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
                        val committedPaths = edit.strokes.map { stroke -> buildStrokePath(stroke, Offset.Zero, imageSizePx) }
                        val livePath = if (currentStroke.isNotEmpty()) {
                            listOf(buildStrokePath(currentStroke, Offset.Zero, imageSizePx, brushRadiusPx))
                        } else {
                            emptyList()
                        }
                        drawBlurWithReveal(
                            sharpImage = sharpImage,
                            blurredImage = blurredImage,
                            imageOffset = Offset.Zero,
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
                }
            } else {
                Text(
                    text = tr("No image to blur"),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ToolPanel(
            title = tr("Blur"),
            onClose = onBack,
            onDone = { onDone(edit.blurLevel, edit.strokes) },
            doneEnabled = sourceImage != null,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            BlurLevelStepper(
                value = edit.blurLevel,
                onValueChange = { commit(edit.copy(blurLevel = it.coerceIn(BlurLevelMin, BlurLevelMax))) },
            )

            UndoRedoRow(
                undoEnabled = undoStack.isNotEmpty(),
                redoEnabled = redoStack.isNotEmpty(),
                onUndo = ::undo,
                onRedo = ::redo,
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
                valueLabel = ::wholeNumberLabel,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** `−  N  +` stepper for the blur level; also used by the s-Blur shape-reveal tool. */
@Composable
internal fun BlurLevelStepper(value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(
            icon = Icons.Filled.Remove,
            contentDescription = tr("Decrease blur"),
            enabled = value > BlurLevelMin,
            onClick = { onValueChange(value - 1) },
        )
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$value",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        StepperButton(
            icon = Icons.Filled.Add,
            contentDescription = tr("Increase blur"),
            enabled = value < BlurLevelMax,
            onClick = { onValueChange(value + 1) },
        )
    }
}

private val StepperShape = RoundedCornerShape(10.dp)

/** Outlined rounded-square button; dims both outline and glyph when disabled. */
@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tint = if (enabled) onSurface else onSurface.copy(alpha = 0.3f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(StepperShape)
            .border(width = 1.dp, color = tint.copy(alpha = if (enabled) 0.7f else 0.3f), shape = StepperShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun UndoRedoRow(undoEnabled: Boolean, redoEnabled: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_undo),
            contentDescription = tr("Undo"),
            enabled = undoEnabled,
            onClick = onUndo,
        )
        Spacer(modifier = Modifier.width(4.dp))
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_redo),
            contentDescription = tr("Redo"),
            enabled = redoEnabled,
            onClick = onRedo,
        )
    }
}

@Composable
private fun BrushSizeLabelRow(value: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tr("Brush Size"),
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

@Preview
@Composable
private fun BlurScreenPreview() {
    ThemePreviews {
        BlurContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _ -> })
    }
}
