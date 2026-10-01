package org.example.project.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** How far a pressed control shrinks. */
private const val SpringPressedScale = 0.86f

/** A quick tap dips to at least this before springing back, so it still visibly bounces. */
private const val SpringTapDipScale = 0.9f

/** Quick and firm on the way down, so the press answers the finger at once. */
private val SpringPressSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 900f)

/** Underdamped on the way back, so the release overshoots past full size before settling. */
private val SpringReleaseSpec = spring<Float>(dampingRatio = 0.35f, stiffness = 450f)

/**
 * The spring-bounce tap effect for the editors' plain controls: the control shrinks while held and
 * springs back past its size on release. Unlike [BubbleClickState] it never delays the action, so
 * it suits controls tapped in quick succession (undo/redo, tool rows, mode tabs).
 *
 * Pass [interactionSource] to the control's `clickable` (or `IconButton`) and put [springBounce]
 * *first* in its modifier chain, so the background and clip scale with it. Press-and-hold controls
 * that run their own gesture call [press] / [release] instead.
 *
 * Only for plain content: Liquid glass samples its backdrop from layout bounds, so a
 * `graphicsLayer` scale would make it refract the wrong region.
 */
@Stable
internal class SpringBounceState(
    val interactionSource: MutableInteractionSource,
    private val scale: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
) {
    internal val value: Float get() = scale.value

    fun press() {
        scope.launch { scale.animateTo(SpringPressedScale, SpringPressSpec) }
    }

    fun release() {
        scope.launch {
            // A quick tap (and any tap inside a scrollable) can press and release within a frame or
            // two, before the shrink shows; dip first so every tap gets its bounce.
            if (scale.value > SpringTapDipScale) scale.animateTo(SpringTapDipScale, tween(durationMillis = 70))
            scale.animateTo(1f, SpringReleaseSpec)
        }
    }
}

@Composable
internal fun rememberSpringBounce(): SpringBounceState {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val state = remember(interactionSource, scale, scope) { SpringBounceState(interactionSource, scale, scope) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            when (it) {
                is PressInteraction.Press -> state.press()
                is PressInteraction.Release, is PressInteraction.Cancel -> state.release()
            }
        }
    }
    return state
}

/** Applies [state]'s scale. Put it first in the chain so everything the control draws scales. */
internal fun Modifier.springBounce(state: SpringBounceState): Modifier =
    graphicsLayer {
        val scale = state.value
        scaleX = scale
        scaleY = scale
    }
