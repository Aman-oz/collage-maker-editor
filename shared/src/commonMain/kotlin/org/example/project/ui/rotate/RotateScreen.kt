package org.example.project.ui.rotate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/** The full rotation state — every field participates in undo/redo as one checkpoint each. */
private data class RotateEdit(
    val rotationDegrees: Float = RotationDefault,
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
)

@Composable
fun RotateScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RotateViewModel = koinViewModel(),
) {
    RotateContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { edit ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyRotation(
                    bakeRotate(
                        source = image,
                        quarterTurns = edit.quarterTurns,
                        flipHorizontal = edit.flipHorizontal,
                        flipVertical = edit.flipVertical,
                        rotationDegrees = edit.rotationDegrees,
                    ),
                )
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun RotateContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (RotateEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var edit by remember { mutableStateOf(RotateEdit()) }
    var undoStack by remember { mutableStateOf(emptyList<RotateEdit>()) }
    var redoStack by remember { mutableStateOf(emptyList<RotateEdit>()) }

    fun commit(newEdit: RotateEdit) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        RotateTopBar(onBack = onBack, onDone = { onDone(edit) }, doneEnabled = sourceImage != null)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val turns = normalizeQuarterTurns(edit.quarterTurns)
                val swapped = turns == 1 || turns == 3
                val bitmapWidth = sourceImage.width.toFloat()
                val bitmapHeight = sourceImage.height.toFloat()
                val footprintWidth = if (swapped) bitmapHeight else bitmapWidth
                val footprintHeight = if (swapped) bitmapWidth else bitmapHeight
                val fitScale = min(boxWidthPx / footprintWidth, boxHeightPx / footprintHeight)
                val imageWidthPx = bitmapWidth * fitScale
                val imageHeightPx = bitmapHeight * fitScale
                val coverScale = coverScaleForRotation(footprintWidth * fitScale, footprintHeight * fitScale, edit.rotationDegrees)

                with(density) {
                    Image(
                        bitmap = sourceImage,
                        contentDescription = "Photo preview",
                        modifier = Modifier
                            .size(imageWidthPx.toDp(), imageHeightPx.toDp())
                            .graphicsLayer(
                                rotationZ = turns * 90f + edit.rotationDegrees,
                                scaleX = (if (edit.flipHorizontal) -1f else 1f) * coverScale,
                                scaleY = (if (edit.flipVertical) -1f else 1f) * coverScale,
                            ),
                    )
                }
            } else {
                Text(
                    text = "No image to rotate",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        RotationControlRow(value = edit.rotationDegrees)
        RotationPointsSlider(
            value = edit.rotationDegrees,
            onValueChange = { commit(edit.copy(rotationDegrees = nearestRotationStop(it))) },
        )

        RotateActionRow(
            onRotateLeft = { commit(edit.copy(quarterTurns = normalizeQuarterTurns(edit.quarterTurns - 1))) },
            onRotateRight = { commit(edit.copy(quarterTurns = normalizeQuarterTurns(edit.quarterTurns + 1))) },
            onFlipHorizontal = { commit(edit.copy(flipHorizontal = !edit.flipHorizontal)) },
            onFlipVertical = { commit(edit.copy(flipVertical = !edit.flipVertical)) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        UndoRedoRow(
            undoEnabled = undoStack.isNotEmpty(),
            redoEnabled = redoStack.isNotEmpty(),
            onUndo = ::undo,
            onRedo = ::redo,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RotateTopBar(onBack: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Rotate",
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
private fun RotationControlRow(value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Rotation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorLabelTint,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}°",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorAccent,
        )
    }
}

/**
 * A slider that only ever rests at one of [RotationStops] — dragging or tapping snaps the thumb
 * to whichever stop is nearest, with a small dot marking each stop along the track.
 */
@Composable
private fun RotationPointsSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val range = RotationStops.first()..RotationStops.last()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 20.dp),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbRadiusPx = with(density) { 12.dp.toPx() }
        val usableWidth = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        fun valueToX(v: Float): Float =
            thumbRadiusPx + ((v - range.start) / (range.endInclusive - range.start)) * usableWidth

        fun xToValue(x: Float): Float {
            val raw = ((x - thumbRadiusPx) / usableWidth) * (range.endInclusive - range.start) + range.start
            return nearestRotationStop(raw.coerceIn(range.start, range.endInclusive))
        }

        val thumbX = valueToX(value)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position -> onValueChange(xToValue(position.x)) },
                        onDrag = { change, _ ->
                            change.consume()
                            onValueChange(xToValue(change.position.x))
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { position -> onValueChange(xToValue(position.x)) })
                },
        ) {
            val trackY = size.height / 2f
            drawLine(
                color = EditorControlBackground,
                start = Offset(thumbRadiusPx, trackY),
                end = Offset(widthPx - thumbRadiusPx, trackY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            for (stop in RotationStops) {
                val stopX = valueToX(stop)
                drawCircle(
                    color = if (stop == value) EditorAccent else EditorLabelTint.copy(alpha = 0.5f),
                    radius = 3.dp.toPx(),
                    center = Offset(stopX, trackY),
                )
            }
            drawCircle(color = Color.White, radius = thumbRadiusPx, center = Offset(thumbX, trackY))
        }
    }
}

@Composable
private fun RotateActionRow(
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RotateActionButton(icon = Icons.AutoMirrored.Filled.RotateLeft, label = "Rotate Left", onClick = onRotateLeft)
        RotateActionButton(icon = Icons.AutoMirrored.Filled.RotateRight, label = "Rotate Right", onClick = onRotateRight)
        RotateActionButton(icon = Icons.Filled.Flip, label = "Flip H", onClick = onFlipHorizontal)
        RotateActionButton(icon = Icons.Filled.Flip, label = "Flip V", onClick = onFlipVertical, rotateIcon90 = true)
    }
}

@Composable
private fun RotateActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    rotateIcon90: Boolean = false,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EditorControlBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = EditorIconTint,
                modifier = Modifier
                    .size(22.dp)
                    .then(if (rotateIcon90) Modifier.rotate(90f) else Modifier),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = EditorLabelTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UndoRedoRow(undoEnabled: Boolean, redoEnabled: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", enabled = undoEnabled, onClick = onUndo)
        Spacer(modifier = Modifier.width(16.dp))
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", enabled = redoEnabled, onClick = onRedo)
    }
}

@Preview
@Composable
private fun RotateScreenPreview() {
    ThemePreviews {
        RotateContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = {})
    }
}
