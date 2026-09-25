package org.example.project.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt

/**
 * A slider whose filled segment runs from [referenceValue] (not necessarily the start of [range])
 * to the thumb — e.g. a bipolar -100..100 adjustment fills from its 0 midpoint, while a plain
 * 0..100 intensity fills from its start, using this same widget with `referenceValue = 0f`.
 *
 * The defaults are the dark editor chrome with a round white thumb; pass colors and a wider
 * [thumbWidth] for a pill thumb on light surfaces (the collage editor).
 *
 * With [glassThumb], the thumb is drawn as its own node instead of on the track canvas: at rest it
 * is the solid [thumbColor] pill, and while pressed it grows by [GlassThumbScale] and turns into
 * Liquid Glass that refracts the track (fill included) underneath it. The track canvas is the
 * `liquefiable` source, so it must stay a sibling of the thumb — a liquid node can't sample a
 * layer it is drawn inside.
 */
@Composable
internal fun CenterFillSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    referenceValue: Float = 0f,
    onDraggingChange: (Boolean) -> Unit = {},
    trackColor: Color = EditorControlBackground,
    fillColor: Color = EditorAccent,
    thumbColor: Color = Color.White,
    thumbWidth: Dp = 24.dp,
    thumbHeight: Dp = 24.dp,
    horizontalPadding: Dp = 20.dp,
    glassThumb: Boolean = false,
    glassTint: Color = Color.White.copy(alpha = 0.3f),
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = horizontalPadding),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbRadiusPx = with(density) { thumbWidth.toPx() } / 2f
        val thumbHalfHeightPx = with(density) { thumbHeight.toPx() } / 2f
        val usableWidth = (widthPx - thumbRadiusPx * 2).coerceAtLeast(1f)

        fun valueToX(v: Float): Float =
            thumbRadiusPx + ((v - range.start) / (range.endInclusive - range.start)) * usableWidth

        fun xToValue(x: Float): Float =
            (((x - thumbRadiusPx) / usableWidth) * (range.endInclusive - range.start) + range.start)
                .coerceIn(range.start, range.endInclusive)

        val thumbX = valueToX(value)
        val referenceX = valueToX(referenceValue)
        val liquidState = rememberLiquidState()
        var pressed by remember { mutableStateOf(false) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(if (glassThumb) Modifier.liquefiable(liquidState) else Modifier)
                .pointerInputDragAndTap(
                    onDrag = { x -> onValueChange(xToValue(x)) },
                    onTap = { x -> onValueChange(xToValue(x)) },
                    onDraggingChange = onDraggingChange,
                )
                .then(if (glassThumb) Modifier.pointerInputPressed { pressed = it } else Modifier),
        ) {
            val trackY = size.height / 2f
            val trackStroke = 4.dp.toPx()
            drawLine(
                color = trackColor,
                start = Offset(thumbRadiusPx, trackY),
                end = Offset(widthPx - thumbRadiusPx, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = fillColor,
                start = Offset(referenceX, trackY),
                end = Offset(thumbX, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round,
            )
            if (!glassThumb) {
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(thumbX - thumbRadiusPx, trackY - thumbHalfHeightPx),
                    size = Size(thumbRadiusPx * 2f, thumbHalfHeightPx * 2f),
                    cornerRadius = CornerRadius(thumbHalfHeightPx),
                )
            }
        }

        if (glassThumb) {
            GlassSliderThumb(
                liquidState = liquidState,
                pressed = pressed,
                centerX = { thumbX },
                width = thumbWidth,
                height = thumbHeight,
                solidColor = thumbColor,
                glassTint = glassTint,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/** How much the glass thumb grows while pressed. */
private const val GlassThumbScale = 1.45f

/**
 * The pressed-state morph is one 0→1 progress driving size, the solid fill fading out, and the
 * glass rim fading in, so the grow and the material change can never drift apart. The size is
 * animated for real (not a `graphicsLayer` scale), because Liquid samples the backdrop from the
 * node's laid-out bounds — a scaled layer would refract the wrong strip of the track.
 */
@Composable
private fun GlassSliderThumb(
    liquidState: LiquidState,
    pressed: Boolean,
    centerX: () -> Float,
    width: Dp,
    height: Dp,
    solidColor: Color,
    glassTint: Color,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
    )
    val scale = 1f + (GlassThumbScale - 1f) * progress
    val w = width * scale
    val h = height * scale
    val shape = RoundedCornerShape(50)
    val density = LocalDensity.current
    val halfWidthPx = with(density) { w.toPx() } / 2f

    Box(
        modifier = modifier
            .offset { IntOffset((centerX() - halfWidthPx).roundToInt(), 0) }
            .size(w, h)
            .clip(shape)
            .liquid(liquidState) {
                this.shape = shape
                frost = 1.dp
                curve = 0.5f
                refraction = 0.45f
                edge = 0.75f
                dispersion = 0.2f
                saturation = 1.3f
                tint = glassTint
            }
            // The overshooting spring can dip progress slightly below 0 / above 1.
            .background(solidColor.copy(alpha = solidColor.alpha * (1f - progress).coerceIn(0f, 1f)))
            .border(
                width = 1.dp,
                color = lerp(Color.Transparent, Color.White.copy(alpha = 0.7f), progress.coerceIn(0f, 1f)),
                shape = shape,
            ),
    )
}

/**
 * Reports true from the first finger down until every pointer lifts. Reads the Final pass without
 * consuming, so it sees the gesture even after the drag/tap detectors have consumed it.
 */
private fun Modifier.pointerInputPressed(onPressedChange: (Boolean) -> Unit): Modifier =
    pointerInput(onPressedChange) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            onPressedChange(true)
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
            } while (event.changes.any { it.pressed })
            onPressedChange(false)
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
