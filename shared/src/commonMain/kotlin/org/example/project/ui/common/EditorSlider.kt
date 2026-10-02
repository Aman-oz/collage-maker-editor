package org.example.project.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.abs
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
 * is the solid [thumbColor] pill, and while pressed it grows by [LiquidThumbPressedScale] into a [LiquidSliderThumb],
 * a Liquid Glass drop that refracts the track (fill included) underneath it. The track canvas is the
 * `liquefiable` source, so it must stay a sibling of the thumb — a liquid node can't sample a
 * layer it is drawn inside.
 *
 * With [valueLabel], a small [thumbColor] bubble showing the current value floats just above the
 * thumb for as long as a finger is down (it takes no room: it is drawn over whatever is above the
 * track), and, unless [showRangeLabels] is off, the ends of [range] (see [SliderRangeLabels]) are
 * printed in a row above the track, which makes the slider that row taller than its bare 40dp.
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
    valueLabel: ((Float) -> String)? = null,
    showRangeLabels: Boolean = valueLabel != null,
    labelColor: Color = fillColor,
) = Column(modifier = modifier.fillMaxWidth()) {
    if (valueLabel != null && showRangeLabels) {
        SliderRangeLabels(
            range = range,
            label = valueLabel,
            color = labelColor,
            // The track stops half a thumb short of the slider's edges; the end labels follow it.
            horizontalPadding = horizontalPadding + thumbWidth / 2,
        )
    }
    BoxWithConstraints(
        modifier = Modifier
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
                .then(if (glassThumb || valueLabel != null) Modifier.pointerInputPressed { pressed = it } else Modifier),
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
            LiquidSliderThumb(
                liquidState = liquidState,
                pressed = pressed,
                centerX = thumbX,
                width = thumbWidth,
                height = thumbHeight,
                accentColor = thumbColor,
                glassTint = glassTint,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        if (valueLabel != null) {
            // The thumb's top edge while pressed (the glass thumb grows under the finger).
            val pressedHalfHeightPx = thumbHalfHeightPx * if (glassThumb) LiquidThumbPressedScale else 1f
            val gapPx = with(density) { 6.dp.toPx() }
            val thumbTopPx = with(density) { maxHeight.toPx() } / 2f - pressedHalfHeightPx
            // Fully qualified: both the enclosing Column and this Box offer a scoped overload.
            androidx.compose.animation.AnimatedVisibility(
                visible = pressed,
                enter = fadeIn() + scaleIn(initialScale = 0.6f, transformOrigin = TransformOrigin(0.5f, 1f)),
                exit = fadeOut() + scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(0.5f, 1f)),
                // Takes no room of its own; centered over the thumb, sitting just above it.
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(0, 0) {
                        placeable.place(
                            x = (thumbX - placeable.width / 2f).roundToInt(),
                            y = (thumbTopPx - gapPx - placeable.height).roundToInt(),
                        )
                    }
                },
            ) {
                Text(
                    text = valueLabel(value),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(thumbColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** A slider value as a whole number: the usual [CenterFillSlider] `valueLabel`. */
internal fun wholeNumberLabel(value: Float): String = value.roundToInt().toString()

/**
 * The start and end of [range], printed in a row whose ends are [horizontalPadding] in. A range that
 * runs below zero (a bipolar adjustment such as -100..100) also gets its middle value, the point the
 * slider rests at; a plain min..max slider only names its two ends.
 */
@Composable
private fun SliderRangeLabels(
    range: ClosedFloatingPointRange<Float>,
    label: (Float) -> String,
    color: Color,
    horizontalPadding: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        @Composable
        fun RangeLabel(value: Float, alignment: Alignment) {
            Text(
                text = label(value),
                color = color,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.align(alignment),
            )
        }
        RangeLabel(range.start, Alignment.CenterStart)
        if (range.start < 0f) RangeLabel((range.start + range.endInclusive) / 2f, Alignment.Center)
        RangeLabel(range.endInclusive, Alignment.CenterEnd)
    }
}

/** How much the liquid thumb grows while a finger is down. */
private const val LiquidThumbPressedScale = 1.5f

/** The most the thumb may stretch along the track while it is moving (a fraction of its width). */
private const val LiquidThumbMaxStretch = 0.28f

/**
 * A slider thumb that rests as a solid [accentColor] pill and, while a finger is down, grows by
 * [LiquidThumbPressedScale] into a Liquid Glass drop that refracts the track (fill included).
 * Moving it stretches it along the track and squashes it vertically, like a drop of water being
 * dragged, then wobbles back once it stops. One press progress drives the size, the solid fill
 * fading out and the glass details (rim, sheen, glow) fading in, so they never drift apart.
 *
 * The stretch comes from a trailing copy of [centerX] that chases the real position on an
 * under-damped spring: the gap between the two is the drop's "inertia", so a fast drag or a tap
 * that jumps the value stretches more, and the spring's overshoot is the settle wobble. The thumb
 * itself is always drawn at the real [centerX], so it never lags the value.
 *
 * Sizes are laid out for real (not a `graphicsLayer` scale), because Liquid samples the backdrop
 * from the node's laid-out bounds — a scaled layer would refract the wrong strip of the track. The
 * glow under the drop is drawn behind the clipped lens by an unclipped outer node, so it can spill
 * past the thumb's bounds without being refracted.
 *
 * [liquidState] must belong to a *sibling* `liquefiable` track: a liquid node can't sample a layer
 * it is drawn inside.
 */
@Composable
internal fun LiquidSliderThumb(
    liquidState: LiquidState,
    pressed: Boolean,
    centerX: Float,
    width: Dp,
    height: Dp,
    accentColor: Color,
    glassTint: Color,
    modifier: Modifier = Modifier,
) {
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
    )
    val trailingX by animateFloatAsState(
        targetValue = centerX,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
    )
    val density = LocalDensity.current
    val baseWidthPx = with(density) { width.toPx() }
    val stretch = (abs(centerX - trailingX) / baseWidthPx * 0.35f).coerceAtMost(LiquidThumbMaxStretch)
    // Only the size may use the press spring's overshoot; the material blends need 0..1.
    val t = press.coerceIn(0f, 1f)
    val scale = 1f + (LiquidThumbPressedScale - 1f) * press
    val w = width * scale * (1f + stretch)
    // Volume is roughly kept: what the drop gains in width it loses in height.
    val h = height * scale * (1f - stretch * 0.45f)
    val halfWidthPx = with(density) { w.toPx() } / 2f
    val shape = RoundedCornerShape(50)
    val rimColor = lerp(accentColor, Color.White, 0.4f)

    Box(
        modifier = modifier
            .offset { IntOffset((centerX - halfWidthPx).roundToInt(), 0) }
            .requiredSize(w, h)
            .drawBehind {
                // A soft accent glow pooled under the drop, as light through water would.
                val glowCenter = Offset(size.width / 2f, size.height * 0.8f)
                if (t <= 0f) return@drawBehind
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.25f * t), Color.Transparent),
                        center = glowCenter,
                        radius = size.width * 0.62f,
                    ),
                    topLeft = Offset(glowCenter.x - size.width * 0.62f, glowCenter.y - size.height * 0.45f),
                    size = Size(size.width * 1.24f, size.height * 0.9f),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .liquid(liquidState) {
                    this.shape = shape
                    frost = 0.5.dp
                    // Kept low on purpose: a strong lens magnifies the thin fill line until it floods
                    // half the drop. This bends the track a little and keeps the fill a line.
                    curve = 0.2f * t
                    refraction = 0.2f * t
                    edge = 0.7f * t
                    dispersion = 0.12f * t
                    saturation = 1.1f
                    contrast = 1.05f
                    tint = glassTint
                }
                .drawWithContent {
                    drawContent()
                    val corner = CornerRadius(size.height / 2f)
                    // The resting solid pill, dissolving into the glass as the press grows.
                    if (t < 1f) drawRoundRect(color = accentColor.copy(alpha = accentColor.alpha * (1f - t)), cornerRadius = corner)
                    if (t <= 0f) return@drawWithContent
                    // A milky body, so the drop reads as frosted water rather than a clear hole.
                    drawRoundRect(color = Color.White.copy(alpha = 0.18f * t), cornerRadius = corner)
                    // Top specular sheen, inset from the rim so it reads as a curved surface.
                    val inset = 2.dp.toPx()
                    val sheenHeight = size.height * 0.5f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.55f * t),
                            1f to Color.White.copy(alpha = 0f),
                            startY = inset,
                            endY = inset + sheenHeight,
                        ),
                        topLeft = Offset(inset * 1.5f, inset),
                        size = Size(size.width - inset * 3f, sheenHeight),
                        cornerRadius = CornerRadius(sheenHeight / 2f),
                    )
                }
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.95f * t), rimColor.copy(alpha = 0.85f * t)),
                    ),
                    shape = shape,
                ),
        )
    }
}

/**
 * Reports true from the first finger down until every pointer lifts. Reads the Final pass without
 * consuming, so it sees the gesture even after the drag/tap detectors have consumed it.
 */
internal fun Modifier.pointerInputPressed(onPressedChange: (Boolean) -> Unit): Modifier =
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

/**
 * Keyed on Unit and reading the latest callbacks through state: keying on the callbacks themselves
 * restarts the detectors whenever a caller passes a fresh lambda (e.g. one capturing the value being
 * edited), which cancels the drag mid-gesture and leaves the thumb stuck.
 */
@Composable
private fun Modifier.pointerInputDragAndTap(
    onDrag: (Float) -> Unit,
    onTap: (Float) -> Unit,
    onDraggingChange: (Boolean) -> Unit = {},
): Modifier {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)
    return this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { pos ->
                    currentOnDraggingChange(true)
                    currentOnDrag(pos.x)
                },
                onDragEnd = { currentOnDraggingChange(false) },
                onDragCancel = { currentOnDraggingChange(false) },
                onDrag = { change, _ ->
                    change.consume()
                    currentOnDrag(change.position.x)
                },
            )
        }
        .pointerInput(Unit) {
            detectTapGestures(onTap = { pos -> currentOnTap(pos.x) })
        }
}
