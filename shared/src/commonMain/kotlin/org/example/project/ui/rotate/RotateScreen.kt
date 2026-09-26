package org.example.project.ui.rotate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Flip
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/** The full rotation state — every field participates in undo/redo as one checkpoint each. */
private data class RotateEdit(
    val rotationDegrees: Float = RotationDefault,
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
) {
    /** The buttons' turns plus the slider's whole turns — everything that swaps the frame. */
    val totalQuarterTurns: Int get() = normalizeQuarterTurns(quarterTurns + sliderQuarterTurns(rotationDegrees))

    /** The slider's sub-90° remainder — the only part drawn as a cover-scaled tilt. */
    val fineDegrees: Float get() = sliderFineDegrees(rotationDegrees)
}

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
                        quarterTurns = edit.totalQuarterTurns,
                        flipHorizontal = edit.flipHorizontal,
                        flipVertical = edit.flipVertical,
                        rotationDegrees = edit.fineDegrees,
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

    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Rotate",
            onClose = onBack,
            onDone = { onDone(edit) },
            doneEnabled = sourceImage != null,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val turns = edit.totalQuarterTurns
                val swapped = turns == 1 || turns == 3
                val bitmapWidth = sourceImage.width.toFloat()
                val bitmapHeight = sourceImage.height.toFloat()
                val footprintWidth = if (swapped) bitmapHeight else bitmapWidth
                val footprintHeight = if (swapped) bitmapWidth else bitmapHeight
                val fitScale = min(boxWidthPx / footprintWidth, boxHeightPx / footprintHeight)
                val imageWidthPx = bitmapWidth * fitScale
                val imageHeightPx = bitmapHeight * fitScale
                val coverScale = coverScaleForRotation(footprintWidth * fitScale, footprintHeight * fitScale, edit.fineDegrees)

                with(density) {
                    // The rounded clip belongs to the upright output frame, not the rotated photo:
                    // the photo is laid out at its own (possibly swapped) size and overflows the
                    // frame, so the corners stay rounded at every angle, just like the baked result.
                    Box(
                        modifier = Modifier
                            .size((footprintWidth * fitScale).toDp(), (footprintHeight * fitScale).toDp())
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = sourceImage,
                            contentDescription = "Photo preview",
                            modifier = Modifier
                                .requiredSize(imageWidthPx.toDp(), imageHeightPx.toDp())
                                .graphicsLayer(
                                    rotationZ = turns * 90f + edit.fineDegrees,
                                    scaleX = (if (edit.flipHorizontal) -1f else 1f) * coverScale,
                                    scaleY = (if (edit.flipVertical) -1f else 1f) * coverScale,
                                ),
                        )
                    }
                }
            } else {
                Text(text = "No image to rotate", color = scheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RotationLabelRow(value = edit.rotationDegrees)
        RotationPointsSlider(
            value = edit.rotationDegrees,
            onValueChange = { snapped ->
                // Every drag event reports a stop; only crossing into a new one is an undo step.
                if (snapped != edit.rotationDegrees) commit(edit.copy(rotationDegrees = snapped))
            },
        )

        RotateActionRow(
            onRotateLeft = { commit(edit.copy(quarterTurns = normalizeQuarterTurns(edit.quarterTurns - 1))) },
            onRotateRight = { commit(edit.copy(quarterTurns = normalizeQuarterTurns(edit.quarterTurns + 1))) },
            onFlipHorizontal = { commit(edit.copy(flipHorizontal = !edit.flipHorizontal)) },
            onFlipVertical = { commit(edit.copy(flipVertical = !edit.flipVertical)) },
        )

        UndoRedoRow(
            undoEnabled = undoStack.isNotEmpty(),
            redoEnabled = redoStack.isNotEmpty(),
            onUndo = ::undo,
            onRedo = ::redo,
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun RotationLabelRow(value: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Rotation",
            color = scheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.roundToInt().toString(),
            color = scheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * A slider that only ever rests at one of [RotationStops] — dragging or tapping snaps the thumb
 * to whichever stop is nearest, with a small dot marking each stop along the track. The dots are
 * evenly spaced even though the angles between them are not (45° steps near 0, 90° further out).
 */
@Composable
private fun RotationPointsSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val lastIndex = RotationStops.lastIndex.coerceAtLeast(1).toFloat()
    // The gesture detectors are keyed on Unit, so read the latest callback through State.
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 16.dp),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbHalfWidthPx = with(density) { 13.dp.toPx() }
        val usableWidth = (widthPx - thumbHalfWidthPx * 2).coerceAtLeast(1f)

        fun valueToX(v: Float): Float {
            val index = RotationStops.indexOf(nearestRotationStop(v))
            return thumbHalfWidthPx + (index / lastIndex) * usableWidth
        }

        fun xToValue(x: Float): Float =
            rotationStopAtPosition(((x - thumbHalfWidthPx) / usableWidth) * lastIndex)

        val thumbX = valueToX(value)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { position -> currentOnValueChange(xToValue(position.x)) },
                        onDrag = { change, _ ->
                            change.consume()
                            currentOnValueChange(xToValue(change.position.x))
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { position -> currentOnValueChange(xToValue(position.x)) })
                },
        ) {
            val trackY = size.height / 2f
            drawLine(
                color = scheme.onSurface.copy(alpha = 0.12f),
                start = Offset(thumbHalfWidthPx, trackY),
                end = Offset(widthPx - thumbHalfWidthPx, trackY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            for (stop in RotationStops) {
                drawCircle(
                    color = scheme.onSurface.copy(alpha = 0.22f),
                    radius = 4.dp.toPx(),
                    center = Offset(valueToX(stop), trackY),
                )
            }
            val thumbHalfHeightPx = 7.dp.toPx()
            drawRoundRect(
                color = scheme.primary,
                topLeft = Offset(thumbX - thumbHalfWidthPx, trackY - thumbHalfHeightPx),
                size = Size(thumbHalfWidthPx * 2f, thumbHalfHeightPx * 2f),
                cornerRadius = CornerRadius(thumbHalfHeightPx),
            )
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RotateActionButton(icon = Icons.AutoMirrored.Outlined.RotateLeft, label = "Rotate left", onClick = onRotateLeft)
        RotateActionButton(icon = Icons.AutoMirrored.Outlined.RotateRight, label = "Rotate right", onClick = onRotateRight)
        RotateActionButton(icon = Icons.Outlined.Flip, label = "Flip horizontally", onClick = onFlipHorizontal)
        RotateActionButton(icon = Icons.Outlined.Flip, label = "Flip vertically", onClick = onFlipVertical, rotateIcon90 = true)
    }
}

/** Outlined square icon button; [label] is only the accessibility description, the design has no caption. */
@Composable
private fun RotateActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    rotateIcon90: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .border(width = 1.dp, color = scheme.onSurface.copy(alpha = 0.7f), shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = scheme.onSurface,
            modifier = Modifier
                .size(20.dp)
                .then(if (rotateIcon90) Modifier.rotate(90f) else Modifier),
        )
    }
}

@Composable
private fun UndoRedoRow(undoEnabled: Boolean, redoEnabled: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        PlainIconButton(icon = vectorResource(Res.drawable.ic_undo), contentDescription = "Undo", enabled = undoEnabled, onClick = onUndo)
        Spacer(modifier = Modifier.width(4.dp))
        PlainIconButton(icon = vectorResource(Res.drawable.ic_redo), contentDescription = "Redo", enabled = redoEnabled, onClick = onRedo)
    }
}

@Composable
private fun PlainIconButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun RotateScreenPreview() {
    ThemePreviews {
        RotateContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = {})
    }
}
