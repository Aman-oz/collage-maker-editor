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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import org.example.project.ui.theme.Brand

/** Height every screen's top bar uses, so the title and buttons sit at the same spot app-wide. */
internal val TopBarHeight = 56.dp

/** Side padding of every top bar. */
internal val TopBarHorizontalPadding = 16.dp

/** Diameter of the circular top-bar buttons (glass back/close/done, and undo/redo beside them). */
internal val TopBarButtonSize = 40.dp

/** The premium crown in a top bar (Home, Templates, Frames): one size wherever it appears. */
internal val TopBarPremiumIconSize = 28.dp

/**
 * Room a centered top-bar title leaves on each side for the widest trailing group, undo + redo +
 * Done (three [TopBarButtonSize] buttons, 8dp apart), plus a gap — so the title stays centered on
 * the screen and ellipsizes before it reaches the buttons.
 */
internal val UndoRedoDoneWidth = TopBarButtonSize * 3 + 16.dp + 8.dp

/** The standard top-bar frame: full width, [TopBarHeight] tall, [TopBarHorizontalPadding] sides. */
internal fun Modifier.topBar(): Modifier =
    fillMaxWidth().height(TopBarHeight).padding(horizontal = TopBarHorizontalPadding)

internal enum class GlassButtonStyle {
    /** Clear glass over a faint content-colored wash: back / close. */
    Neutral,

    /** A solid primary disc with the bubble tap effect: done. */
    Primary,
}

/**
 * A circular top-bar button: Liquid Glass for back/close ([GlassButtonStyle.Neutral]), a solid
 * primary disc for done ([GlassButtonStyle.Primary], see [SolidDoneButton]).
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
    // The brand violet rather than `colorScheme.primary`: the dark scheme's primary is a pastel
    // lavender, which would wash every Done button out on dark screens.
    accentColor: Color = Brand,
    onAccentColor: Color = Color.White,
) {
    if (style == GlassButtonStyle.Primary) {
        SolidDoneButton(icon, contentDescription, onClick, modifier, enabled, accentColor, onAccentColor)
        return
    }
    val liquidState = rememberLiquidState()
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // Light mode needs a stronger wash: over a white screen the faint dark gradient that reads as
    // glass in dark mode vanishes.
    val backdrop = Brush.linearGradient(
        if (isLight) {
            listOf(contentColor.copy(alpha = 0.05f), contentColor.copy(alpha = 0.16f))
        } else {
            listOf(contentColor.copy(alpha = 0.16f), contentColor.copy(alpha = 0.04f))
        },
    )
    val glassTint = Color.White.copy(alpha = if (isLight) 0.1f else 0.08f)
    val iconTint = contentColor
    val bubble = rememberBubbleClick()
    // Glass can't take the Done button's graphicsLayer squish (Liquid samples from layout bounds),
    // so the glass is resized in layout instead; the underdamped press spring overshoots past full
    // size on release, which is the bounce. The slot stays [TopBarButtonSize] so nothing shifts.
    val glassSize = Modifier.layout { measurable, constraints ->
        val side = (constraints.maxWidth * bubble.pressScale).roundToInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(side, side))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place((constraints.maxWidth - side) / 2, (constraints.maxHeight - side) / 2)
        }
    }

    Box(
        modifier = modifier
            .size(TopBarButtonSize)
            .alpha(if (enabled) 1f else 0.4f)
            .bubbleBurstRing(bubble, contentColor.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(glassSize)
                .clip(CircleShape)
                .liquefiable(liquidState)
                .background(backdrop),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(glassSize)
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
                .clickable(
                    interactionSource = bubble.interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { bubble.tap(onClick) },
                )
                // A white hairline highlight so the rim still reads on a flat backdrop. A white rim
                // is invisible on a white screen, so light-mode neutral buttons get a faint dark
                // outer edge with the highlight inset just inside it.
                .then(
                    if (isLight) {
                        Modifier
                            .border(0.75.dp, contentColor.copy(alpha = 0.14f), CircleShape)
                            .padding(0.75.dp)
                            .border(0.75.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    } else {
                        Modifier.border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.22f),
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
                modifier = Modifier.size(22.dp).graphicsLayer {
                    scaleX = bubble.pressScale
                    scaleY = bubble.pressScale
                },
            )
        }
    }
}

/**
 * Done is a flat, solid disc rather than glass so it reads as the screen's one committing action.
 * Being plain, it can take the bubble squish (a `graphicsLayer` scale) that glass can't.
 */
@Composable
internal fun SolidDoneButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Brand,
    onColor: Color = Color.White,
) {
    val bubble = rememberBubbleClick()
    Box(
        modifier = modifier
            .size(TopBarButtonSize)
            .alpha(if (enabled) 1f else 0.4f)
            .bubbleClick(bubble, color)
            .background(color, CircleShape)
            .clickable(
                interactionSource = bubble.interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { bubble.tap(onClick) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = onColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
