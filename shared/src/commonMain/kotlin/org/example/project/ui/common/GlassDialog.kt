package org.example.project.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

internal val DialogShape = RoundedCornerShape(28.dp)

/**
 * Hosts a dialog panel as a Liquid Glass sheet over the screen.
 *
 * This is deliberately an in-screen overlay rather than a `Dialog`: a `Dialog` opens its own
 * window, and a `liquid` node can only sample `liquefiable` content in the same composition, so
 * glass inside a real dialog would have nothing to refract. Place this as a sibling drawn above
 * the `liquefiable(liquidState)` screen content, never inside it.
 *
 * [footer] sits below the panel, outside the glass (e.g. a close button). Pass
 * `dismissOnScrimClick = false` when the dialog must be closed explicitly through it.
 *
 * The panel animates with fade + slide only. Both are placement/alpha changes, whereas a
 * `graphicsLayer` scale would make Liquid sample the backdrop from the unscaled bounds.
 */
@Composable
internal fun GlassDialogHost(
    visible: Boolean,
    liquidState: LiquidState,
    onDismiss: () -> Unit,
    dismissOnScrimClick: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = noRipple,
                    indication = null,
                    // Still clickable when not dismissing, so taps never reach the screen under it.
                    onClick = { if (dismissOnScrimClick) onDismiss() },
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 10 },
        exit = fadeOut() + slideOutVertically { it / 10 },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .glassPanel(liquidState)
                    // Swallows taps so touching the panel doesn't reach the dismissing scrim.
                    .clickable(interactionSource = noRipple, indication = null, onClick = {}),
            ) {
                content()
            }
            footer?.invoke()
        }
    }
}

/**
 * Frosted Liquid Glass tuned for a readable panel: heavy frost and a surface-colored tint keep the
 * text legible, with modest refraction and a bright rim so it still reads as glass, not a card.
 */
@Composable
internal fun Modifier.glassPanel(liquidState: LiquidState): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val isLight = surface.luminance() > 0.5f
    val rim = Color.White.copy(alpha = if (isLight) 0.7f else 0.18f)
    return clip(DialogShape)
        .liquid(liquidState) {
            shape = DialogShape
            frost = 22.dp
            curve = 0.25f
            refraction = 0.15f
            edge = 0.5f
            dispersion = 0.1f
            saturation = 1.2f
            tint = surface.copy(alpha = if (isLight) 0.6f else 0.55f)
        }
        .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0f))), DialogShape)
}
