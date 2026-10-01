package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * How long a tapped button holds its squish before its action runs, so the tap is seen (and felt)
 * before the screen changes. It also covers quick taps whose press is never observed: inside a
 * scrollable, a tap can press and release within one frame.
 */
internal const val TapFeedbackMillis = 200L

/**
 * State for the "bubble" tap effect: the button squishes while held, springs back past its size on
 * release, and a ring bursts out of its edge and fades.
 *
 * Pass [interactionSource] to the button's `clickable`/`Button` so the press is observed, and call
 * [tap] from its `onClick` with the button's action: it holds the squish for [TapFeedbackMillis],
 * bursts the ring, then runs the action, and ignores further taps until then (so a double tap on
 * Back can't pop twice). [pressProgress] (0 at rest, 1 squished) is for content that can't take a
 * `graphicsLayer` scale, such as Liquid glass, and must be resized in layout instead.
 */
@Stable
internal class BubbleClickState(
    val interactionSource: MutableInteractionSource,
    internal val pressProgress: State<Float>,
    private val holding: MutableState<Boolean>,
    private val burstProgress: Animatable<Float, *>,
    private val scope: CoroutineScope,
) {
    internal val burst: Float get() = burstProgress.value

    /** Squish scale for plain content: 1 at rest, [BubblePressedScale] fully pressed. */
    internal val pressScale: Float get() = 1f - (1f - BubblePressedScale) * pressProgress.value

    fun burst() {
        scope.launch {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }
    }

    fun tap(action: () -> Unit) {
        if (holding.value) return
        holding.value = true
        burst()
        scope.launch {
            try {
                delay(TapFeedbackMillis)
                action()
            } finally {
                holding.value = false
            }
        }
    }
}

private const val BubblePressedScale = 0.88f

@Composable
internal fun rememberBubbleClick(): BubbleClickState {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val holding = remember { mutableStateOf(false) }
    // Underdamped, so the release overshoots slightly before settling: that's the "bubble".
    val pressProgress = animateFloatAsState(
        targetValue = if (pressed || holding.value) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = 520f),
    )
    val burstProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    return remember(interactionSource, pressProgress, holding, burstProgress, scope) {
        BubbleClickState(interactionSource, pressProgress, holding, burstProgress, scope)
    }
}

/**
 * Applies [state]'s squish and burst ring. Put it before the button's background so the background
 * scales with the press; the ring is drawn behind at the unscaled bounds and spreads past them,
 * following a pill outline (a circle for a square button).
 *
 * Only for plain (non-Liquid) content: Liquid samples its backdrop from layout bounds, so a
 * `graphicsLayer` scale would make glass refract the wrong region.
 */
internal fun Modifier.bubbleClick(state: BubbleClickState, color: Color): Modifier =
    bubbleBurstRing(state, color).graphicsLayer {
        val scale = state.pressScale
        scaleX = scale
        scaleY = scale
    }

/** Only [state]'s burst ring, for glass buttons that do their squish in layout. */
internal fun Modifier.bubbleBurstRing(state: BubbleClickState, color: Color): Modifier =
    drawBehind {
        val p = state.burst
        if (p >= 1f) return@drawBehind
        val spread = 10.dp.toPx() * p
        val alpha = 1f - p
        val topLeft = Offset(-spread, -spread)
        val ringSize = Size(size.width + spread * 2, size.height + spread * 2)
        val radius = CornerRadius(ringSize.minDimension / 2f)
        drawRoundRect(color.copy(alpha = 0.22f * alpha), topLeft, ringSize, radius)
        drawRoundRect(
            color = color.copy(alpha = 0.7f * alpha),
            topLeft = topLeft,
            size = ringSize,
            cornerRadius = radius,
            style = Stroke(width = 2.dp.toPx() * (1f - p * 0.6f)),
        )
    }
