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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.LiquidSliderThumb
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.pointerInputPressed
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_flip_horizontally
import photocollagemaker.shared.generated.resources.ic_flip_vertically
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_rotate_left
import photocollagemaker.shared.generated.resources.ic_rotate_right
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
internal fun RotateTool(
    sourceImage: ImageBitmap,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    RotateContent(
        sourceImage = sourceImage,
        onBack = onClose,
        onDone = { edit ->
            onApply(
                bakeRotate(
                    source = sourceImage,
                    quarterTurns = edit.totalQuarterTurns,
                    flipHorizontal = edit.flipHorizontal,
                    flipVertical = edit.flipVertical,
                    rotationDegrees = edit.fineDegrees,
                ),
            )
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

    ToolScaffold(modifier = modifier, photoInset = 24.dp) {
        BoxWithConstraints(
            modifier = Modifier
                .toolStage()
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
                            contentDescription = tr("Photo preview"),
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
                Text(text = tr("No image to rotate"), color = scheme.onSurface)
            }
        }

        ToolPanel(
            title = tr("Rotate"),
            onClose = onBack,
            onDone = { onDone(edit) },
            doneEnabled = sourceImage != null,
        ) {
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
            text = tr("Rotation"),
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
 * evenly spaced even though the angles between them are not (45° steps near 0, 90° further out),
 * and each has its angle printed above it, in the style of the other sliders' range labels.
 */
@Composable
private fun RotationPointsSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val lastIndex = RotationStops.lastIndex.coerceAtLeast(1).toFloat()
    // The gesture detectors are keyed on Unit, so read the latest callback through State.
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val selectedStop = nearestRotationStop(value)
    Column(modifier = modifier.fillMaxWidth()) {
        // One label per stop, each centred over its dot: the row is measured as wide as the track
        // box below, and a label's centre is the dot's x in that box (see valueToX there).
        Layout(
            content = {
                for (stop in RotationStops) {
                    Text(
                        text = wholeNumberLabel(stop),
                        color = if (stop == selectedStop) scheme.primary else scheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (stop == selectedStop) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val thumbHalfWidth = RotationThumbWidth.toPx() / 2f
            val usableWidth = (constraints.maxWidth - thumbHalfWidth * 2).coerceAtLeast(1f)
            layout(constraints.maxWidth, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { index, placeable ->
                    val centerX = thumbHalfWidth + index / lastIndex * usableWidth
                    placeable.place((centerX - placeable.width / 2f).roundToInt(), 0)
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val thumbHalfWidthPx = with(density) { RotationThumbWidth.toPx() } / 2f
            val usableWidth = (widthPx - thumbHalfWidthPx * 2).coerceAtLeast(1f)

            fun valueToX(v: Float): Float {
                val index = RotationStops.indexOf(nearestRotationStop(v))
                return thumbHalfWidthPx + (index / lastIndex) * usableWidth
            }

            fun xToValue(x: Float): Float =
                rotationStopAtPosition(((x - thumbHalfWidthPx) / usableWidth) * lastIndex)

            val thumbX = valueToX(value)
            val liquidState = rememberLiquidState()
            var pressed by remember { mutableStateOf(false) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(liquidState)
                    .pointerInputPressed { pressed = it }
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
            }

            // A sibling of the liquefiable track, so the drop can refract the track and its stop dots.
            LiquidSliderThumb(
                liquidState = liquidState,
                pressed = pressed,
                centerX = thumbX,
                width = RotationThumbWidth,
                height = RotationThumbHeight,
                accentColor = scheme.primary,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

private val RotationThumbWidth = 32.dp
private val RotationThumbHeight = 18.dp

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
        RotateActionButton(icon = vectorResource(Res.drawable.ic_rotate_left), label = tr("Rotate left"), onClick = onRotateLeft)
        RotateActionButton(icon = vectorResource(Res.drawable.ic_rotate_right), label = tr("Rotate right"), onClick = onRotateRight)
        RotateActionButton(icon = vectorResource(Res.drawable.ic_flip_horizontally), label = tr("Flip horizontally"), onClick = onFlipHorizontal)
        RotateActionButton(icon = vectorResource(Res.drawable.ic_flip_vertically), label = tr("Flip vertically"), onClick = onFlipVertical)
    }
}

/** Outlined square icon button; [label] is only the accessibility description, the design has no caption. */
@Composable
private fun RotateActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .border(width = 1.dp, color = scheme.onSurface.copy(alpha = 0.7f), shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = scheme.onSurface,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun UndoRedoRow(undoEnabled: Boolean, redoEnabled: Boolean, onUndo: () -> Unit, onRedo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        UndoRedoButton(icon = vectorResource(Res.drawable.ic_undo), contentDescription = tr("Undo"), enabled = undoEnabled, onClick = onUndo)
        Spacer(modifier = Modifier.width(4.dp))
        UndoRedoButton(icon = vectorResource(Res.drawable.ic_redo), contentDescription = tr("Redo"), enabled = redoEnabled, onClick = onRedo)
    }
}

@Preview
@Composable
private fun RotateScreenPreview() {
    ThemePreviews {
        RotateContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = {})
    }
}
