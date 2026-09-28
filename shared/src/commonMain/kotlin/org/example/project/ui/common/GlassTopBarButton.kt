package org.example.project.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState

/** Height every screen's top bar uses, so the title and buttons sit at the same spot app-wide. */
internal val TopBarHeight = 56.dp

/** Side padding of every top bar. */
internal val TopBarHorizontalPadding = 16.dp

/** Diameter of the circular top-bar buttons (glass back/close/done, and undo/redo beside them). */
internal val TopBarButtonSize = 40.dp

/** The standard top-bar frame: full width, [TopBarHeight] tall, [TopBarHorizontalPadding] sides. */
internal fun Modifier.topBar(): Modifier =
    fillMaxWidth().height(TopBarHeight).padding(horizontal = TopBarHorizontalPadding)

internal enum class GlassButtonStyle {
    /** Clear glass over a faint content-colored wash: back / close. */
    Neutral,

    /** Glass over the primary color: done. */
    Primary,
}

/**
 * A circular Liquid Glass button for the top bars' back/close and done actions.
 *
 * Top bars sit above the content rather than over it, so there is nothing of the screen for the
 * glass to refract. Instead each button carries its own backdrop: a small diagonal gradient drawn
 * in a `liquefiable` sibling layer that only this button's `liquid` layer samples. The gradient
 * gives the rim refraction and dispersion something to bend, which is what makes it read as glass
 * rather than a flat disc. The backdrop must stay a sibling — a liquid node can't sample itself.
 *
 * On Android below 13 Liquid falls back to a plain tint, which still leaves the backdrop and rim
 * visible, so the button is never invisible.
 */
@Composable
internal fun GlassTopBarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Neutral,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onAccentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val liquidState = rememberLiquidState()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val backdrop = when (style) {
        // Light mode needs a stronger wash: over a white screen the faint dark gradient that
        // reads as glass in dark mode vanishes.
        GlassButtonStyle.Neutral -> Brush.linearGradient(
            if (isLight) {
                listOf(contentColor.copy(alpha = 0.05f), contentColor.copy(alpha = 0.16f))
            } else {
                listOf(contentColor.copy(alpha = 0.16f), contentColor.copy(alpha = 0.04f))
            },
        )
        GlassButtonStyle.Primary -> Brush.linearGradient(
            listOf(lerp(accentColor, Color.White, 0.3f), accentColor, lerp(accentColor, Color.Black, 0.15f)),
        )
    }
    val glassTint = when (style) {
        GlassButtonStyle.Neutral -> Color.White.copy(alpha = if (isLight) 0.1f else 0.08f)
        GlassButtonStyle.Primary -> Color.White.copy(alpha = 0.12f)
    }
    val iconTint = when (style) {
        GlassButtonStyle.Neutral -> contentColor
        GlassButtonStyle.Primary -> onAccentColor
    }

    Box(
        modifier = modifier
            .size(TopBarButtonSize)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .liquefiable(liquidState)
                .background(backdrop),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .liquid(liquidState) {
                    shape = CircleShape
                    frost = 2.dp
                    curve = 0.45f
                    refraction = 0.4f
                    edge = 0.7f
                    dispersion = 0.2f
                    saturation = 1.25f
                    tint = glassTint
                }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                // A white hairline highlight so the rim still reads on a flat backdrop. A white rim
                // is invisible on a white screen, so light-mode neutral buttons get a faint dark
                // outer edge with the highlight inset just inside it.
                .then(
                    if (isLight && style == GlassButtonStyle.Neutral) {
                        Modifier
                            .border(0.75.dp, contentColor.copy(alpha = 0.14f), CircleShape)
                            .padding(0.75.dp)
                            .border(0.75.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    } else {
                        Modifier.border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = if (style == GlassButtonStyle.Primary) 0.55f else 0.22f),
                            shape = CircleShape,
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
