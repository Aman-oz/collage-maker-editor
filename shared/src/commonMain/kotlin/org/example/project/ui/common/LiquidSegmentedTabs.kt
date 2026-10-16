package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** How much the drop swells while held: past the capsule, like a drop lifted off the glass. */
private val TabDropHeldGrowthWidth = 22.dp
private val TabDropHeldGrowthHeight = 18.dp

/** How much the drop stretches per tab/second of travel, and the cap on it. */
private const val TabStretchPerVelocity = 0.07f
private const val TabMaxStretch = 0.45f

/** Bouncy enough to overshoot visibly, so a press feels like a drop swelling under the finger. */
internal val LiquidDropSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)

/** Chases the finger while held: stiff enough to keep up, loose enough to stretch. */
private val TabFollowSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 700f)

/** Drops the released drop onto its tab with a visible wobble. */
private val TabSettleSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 320f)

/**
 * Clear Liquid Glass: almost no frost, with the look carried by refraction at the curved rim and
 * a bright edge highlight, so the content behind bends at the edges instead of blurring away.
 */
internal fun Modifier.clearLiquidGlass(liquidState: LiquidState, shape: Shape, tint: Color): Modifier =
    clip(shape).liquid(liquidState) {
        this.shape = shape
        frost = 2.dp
        curve = 0.4f
        refraction = 0.35f
        edge = 0.65f
        dispersion = 0.18f
        saturation = 1.2f
        this.tint = tint
    }

/**
 * A glass capsule holding a liquid selection drop behind the selected label, the app's take on
 * [org.example.project.ui.home.HomeBottomBar]'s bubble. A finger down anywhere on the capsule swells
 * the drop past the capsule's height into a clear lens that follows the finger (a tap on another
 * tab flies it there); on release it snaps to the nearest tab, selects it and settles back. One
 * `pointerInput` owns all of it, so the labels carry only accessibility semantics.
 *
 * The capsule and the drop refract whatever is `liquefiable` in [liquidState] — never the capsule
 * itself, which can't sample its own layer. [modifier] sizes the capsule; its tabs split the width
 * equally, so the drop's centre is just a fractional tab index. The drop is sized and placed in
 * layout, never with a `graphicsLayer` scale: Liquid samples the backdrop from layout bounds, so a
 * scaled layer would refract the wrong region.
 */
@Composable
internal fun LiquidSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    liquidState: LiquidState,
    capsuleTint: Color,
    modifier: Modifier = Modifier,
    padding: Dp = 4.dp,
    labelStyle: TextStyle = MaterialTheme.typography.titleSmall,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val rim = Color.White.copy(alpha = if (isLight) 0.75f else 0.35f)
    val scope = rememberCoroutineScope()
    val position = remember { Animatable(selectedIndex.toFloat()) }
    var held by remember { mutableStateOf(false) }
    // The tab the held drop is over, so that label swells with it.
    val hoveredTab by remember { derivedStateOf { position.value.roundToInt() } }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelected by rememberUpdatedState(onSelected)
    val lastTab = (labels.size - 1).coerceAtLeast(0)

    // Selection changed from outside (or by the release below): glide there, unless a finger has it.
    LaunchedEffect(selectedIndex) {
        if (!held) position.animateTo(selectedIndex.toFloat(), TabSettleSpring)
    }

    BoxWithConstraints(modifier = modifier) {
        val tabWidth = (maxWidth - padding * 2) / labels.size.coerceAtLeast(1)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tabWidth, padding, lastTab) {
                    val inset = padding.toPx()
                    fun tabAt(x: Float) = ((x - inset) / tabWidth.toPx() - 0.5f).coerceIn(0f, lastTab.toFloat())
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        held = true
                        scope.launch { position.animateTo(tabAt(down.position.x), TabFollowSpring) }
                        var lastX = down.position.x
                        try {
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                lastX = change.position.x
                                if (!change.pressed) break
                                change.consume()
                                // A fresh animateTo carries the running velocity over, so the drop
                                // keeps its inertia (and its stretch) while following.
                                scope.launch { position.animateTo(tabAt(lastX), TabFollowSpring) }
                            }
                        } finally {
                            held = false
                        }
                        val tab = tabAt(lastX).roundToInt()
                        scope.launch { position.animateTo(tab.toFloat(), TabSettleSpring) }
                        if (tab != currentSelectedIndex) currentOnSelected(tab)
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clearLiquidGlass(liquidState, CircleShape, capsuleTint)
                    .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0.08f))), CircleShape),
            )

            TabSelectionDrop(
                liquidState = liquidState,
                position = position,
                held = held,
                isLight = isLight,
                tabWidth = tabWidth,
                padding = padding,
            )

            Row(modifier = Modifier.fillMaxSize().padding(horizontal = padding)) {
                labels.forEachIndexed { index, label ->
                    val isSelected = index == selectedIndex
                    // Magnified under the held drop, like text seen through water. Text isn't glass,
                    // so a graphicsLayer scale is safe here.
                    val labelScale by animateFloatAsState(
                        targetValue = if (held && hoveredTab == index) 1.12f else 1f,
                        animationSpec = LiquidDropSpring,
                    )
                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .semantics(mergeDescendants = true) {
                                role = Role.Tab
                                selected = isSelected
                                onClick(label = label, action = {
                                    onSelected(index)
                                    true
                                })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = labelStyle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.7f),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                scaleX = labelScale
                                scaleY = labelScale
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The drop behind the selected tab, centred on [position] (a fractional tab index). It stretches
 * along its travel in proportion to its speed and squashes a little in height, so the volume reads
 * as conserved. While [held] it swells past the capsule into a clearer, more strongly refracting
 * lens; released, it settles back. One press progress drives size and material together.
 */
@Composable
private fun BoxScope.TabSelectionDrop(
    liquidState: LiquidState,
    position: Animatable<Float, *>,
    held: Boolean,
    isLight: Boolean,
    tabWidth: Dp,
    padding: Dp,
) {
    val press by animateFloatAsState(if (held) 1f else 0f, LiquidDropSpring)
    // The spring overshoots slightly past 0 / 1; only the size may use the overshoot.
    val t = press.coerceIn(0f, 1f)
    // Resting it is a milky white pill; held it clears toward glass so the lens shows.
    val body = if (isLight) Color.White.copy(alpha = 0.72f - 0.4f * t) else Color.White.copy(alpha = 0.14f - 0.06f * t)
    val rim = Color.White.copy(alpha = if (isLight) 0.95f else 0.4f + 0.3f * t)
    val rimLow = if (isLight) Color(0xFF787880).copy(alpha = 0.25f + 0.1f * t) else rim.copy(alpha = rim.alpha * 0.4f)

    Box(
        modifier = Modifier
            .matchParentSize()
            .layout { measurable, constraints ->
                val stretch = (abs(position.velocity) * TabStretchPerVelocity).coerceAtMost(TabMaxStretch)
                val restHeight = constraints.maxHeight - padding.toPx() * 2
                val w = (tabWidth.toPx() * (1f + stretch) + TabDropHeldGrowthWidth.toPx() * press).roundToInt().coerceAtLeast(0)
                val h = (restHeight * (1f - stretch * 0.22f) + TabDropHeldGrowthHeight.toPx() * press).roundToInt().coerceAtLeast(0)
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val x = padding.toPx() + tabWidth.toPx() * (position.value + 0.5f) - w / 2f
                    placeable.place(x.roundToInt(), (constraints.maxHeight - h) / 2)
                }
            }
            .clip(CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                // Gentle, like the magnification of a real drop; no frost, whose blur drags the
                // transparent pixels outside the lens into the rim as a grey shadow.
                frost = 0.dp
                curve = 0.25f + 0.12f * t
                refraction = 0.18f + 0.12f * t
                edge = 0.35f + 0.25f * t
                dispersion = 0.1f * t
                saturation = 1.2f
                tint = body
            }
            // Over the glass as well: Liquid's tint alone renders far fainter than its alpha.
            .drawWithContent {
                drawContent()
                val corner = CornerRadius(size.height / 2f)
                drawRoundRect(color = body, cornerRadius = corner)
                // Light focused onto the drop's lower inside edge.
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0.6f to Color.White.copy(alpha = 0f),
                        1f to Color.White.copy(alpha = if (isLight) 0.35f + 0.2f * t else 0.08f + 0.06f * t),
                    ),
                    cornerRadius = corner,
                )
            }
            .border(
                width = 1.dp + 0.5.dp * t,
                brush = Brush.verticalGradient(0f to rim, 0.5f to rim.copy(alpha = rim.alpha * 0.2f), 1f to rimLow),
                shape = CircleShape,
            ),
    )
}
