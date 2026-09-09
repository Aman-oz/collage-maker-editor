package org.example.project.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * A slider whose filled segment runs from [referenceValue] (not necessarily the start of [range])
 * to the thumb — e.g. a bipolar -100..100 adjustment fills from its 0 midpoint, while a plain
 * 0..100 intensity fills from its start, using this same widget with `referenceValue = 0f`.
 */
@Composable
internal fun CenterFillSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    referenceValue: Float = 0f,
    onDraggingChange: (Boolean) -> Unit = {},
) {
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

        fun xToValue(x: Float): Float =
            (((x - thumbRadiusPx) / usableWidth) * (range.endInclusive - range.start) + range.start)
                .coerceIn(range.start, range.endInclusive)

        val thumbX = valueToX(value)
        val referenceX = valueToX(referenceValue)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInputDragAndTap(
                    onDrag = { x -> onValueChange(xToValue(x)) },
                    onTap = { x -> onValueChange(xToValue(x)) },
                    onDraggingChange = onDraggingChange,
                ),
        ) {
            val trackY = size.height / 2f
            val trackStroke = 4.dp.toPx()
            drawLine(
                color = EditorControlBackground,
                start = Offset(thumbRadiusPx, trackY),
                end = Offset(widthPx - thumbRadiusPx, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = EditorAccent,
                start = Offset(referenceX, trackY),
                end = Offset(thumbX, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round,
            )
            drawCircle(color = Color.White, radius = thumbRadiusPx, center = Offset(thumbX, trackY))
        }
    }
}

private fun Modifier.pointerInputDragAndTap(
    onDrag: (Float) -> Unit,
    onTap: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit = {},
): Modifier = this
    .pointerInput(onDrag) {
        detectDragGestures(
            onDragStart = { pos ->
                onDraggingChange(true)
                onDrag(pos.x)
            },
            onDragEnd = { onDraggingChange(false) },
            onDragCancel = { onDraggingChange(false) },
            onDrag = { change, _ ->
                change.consume()
                onDrag(change.position.x)
            },
        )
    }
    .pointerInput(onTap) {
        detectTapGestures(onTap = { pos -> onTap(pos.x) })
    }
