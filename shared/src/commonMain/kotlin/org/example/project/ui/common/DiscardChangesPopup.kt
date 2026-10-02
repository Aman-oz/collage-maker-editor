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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.LiquidState
import kotlin.math.roundToInt
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.liquefiable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import org.example.project.i18n.tr

/** "Discard Changes" text colour, as in the design. */
private val DiscardRed = Color(0xFFFF3B45)

private val DiscardButtonShape = RoundedCornerShape(percent = 50)

/**
 * Whether an editor's "leave?" popup is up, and the one way the editor should leave: [requestBack]
 * goes straight back when nothing changed and asks first when something did.
 */
@Stable
internal class DiscardChangesState(
    private val hasChanges: State<Boolean>,
    private val onBack: State<() -> Unit>,
) {
    var visible by mutableStateOf(false)
        private set

    /** The editor's ✕ (and system back): leaves, or first asks when there are unsaved changes. */
    fun requestBack() {
        if (hasChanges.value) visible = true else onBack.value()
    }

    fun dismiss() {
        visible = false
    }

    fun discard() {
        visible = false
        onBack.value()
    }
}

/**
 * Remembers the editor's [DiscardChangesState] and routes system back through it: while there are
 * changes, back opens the popup (and, with it open, closes it again) instead of popping the screen.
 * Call it before any other back handler of the screen that should win over it, such as a dialog's.
 */
@Composable
internal fun rememberDiscardChangesState(hasChanges: Boolean, onBack: () -> Unit): DiscardChangesState {
    val currentHasChanges = rememberUpdatedState(hasChanges)
    val currentOnBack = rememberUpdatedState(onBack)
    val state = remember { DiscardChangesState(currentHasChanges, currentOnBack) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = hasChanges || state.visible,
        onBackCompleted = { if (state.visible) state.dismiss() else state.requestBack() },
    )
    return state
}

/**
 * The "discard your changes?" popup, anchored under the top bar's ✕ in the top-left corner like a
 * popover from it. A tap anywhere outside it cancels. It is Liquid Glass, so [liquidState] must be
 * the `liquefiable` state of the editor content it floats over, and this must be drawn above that
 * content as a sibling (see [GlassDialogHost]).
 */
@Composable
internal fun DiscardChangesPopup(state: DiscardChangesState, liquidState: LiquidState) {
    val noRipple = remember { MutableInteractionSource() }
    if (state.visible) {
        // Invisible, but catches the outside tap that cancels, and keeps it from reaching the editor.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(interactionSource = noRipple, indication = null, onClick = state::dismiss),
        )
    }
    // Fade + slide only: a graphicsLayer scale would make Liquid sample the wrong backdrop.
    AnimatedVisibility(
        visible = state.visible,
        enter = fadeIn() + slideInVertically { -it / 8 },
        exit = fadeOut() + slideOutVertically { -it / 8 },
        modifier = Modifier
            .safeDrawingPadding()
            .padding(start = 12.dp, top = TopBarHeight - 4.dp),
    ) {
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        Box(
            modifier = Modifier
                .width(272.dp)
                .glassPanel(liquidState)
                // A defined outline over the glass's soft rim, so the popup stands out from the photo.
                .border(
                    width = 1.5.dp,
                    color = if (isLight) Color.Black.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.32f),
                    shape = DialogShape,
                )
                // Swallows taps so touching the panel doesn't reach the cancelling layer.
                .clickable(interactionSource = noRipple, indication = null, onClick = {}),
        ) {
            DiscardChangesContent(onDiscard = state::discard)
        }
    }
}

@Composable
private fun DiscardChangesContent(onDiscard: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val rim = Color.White.copy(alpha = if (isLight) 0.9f else 0.35f)
    Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 24.dp, bottom = 20.dp)) {
        Text(
            text = tr("Are you sure you want to discard your changes?"),
            color = onSurface,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))
        DiscardGlassButton(onClick = onDiscard, isLight = isLight, rim = rim)
    }
}

/**
 * The glass "Discard Changes" pill, built like [GlassTopBarButton]: it carries its own small
 * backdrop (a red-tinted gradient in a `liquefiable` sibling) for its `liquid` layer to refract, and
 * it bounces with the bubble spring. Glass can't take a `graphicsLayer` squish (Liquid samples from
 * layout bounds), so the pill is resized in layout instead; the underdamped spring overshoots past
 * full size on release, which is the bounce. The slot keeps its size, so nothing around it shifts.
 */
@Composable
private fun DiscardGlassButton(onClick: () -> Unit, isLight: Boolean, rim: Color) {
    val liquidState = rememberLiquidState()
    val bubble = rememberBubbleClick()
    val backdrop = Brush.linearGradient(
        listOf(
            DiscardRed.copy(alpha = if (isLight) 0.08f else 0.14f),
            Color.White.copy(alpha = if (isLight) 0.55f else 0.08f),
            DiscardRed.copy(alpha = if (isLight) 0.2f else 0.26f),
        ),
    )
    val squish = Modifier.layout { measurable, constraints ->
        val width = (constraints.maxWidth * bubble.pressScale).roundToInt().coerceAtLeast(0)
        val height = (constraints.maxHeight * bubble.pressScale).roundToInt().coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place((constraints.maxWidth - width) / 2, (constraints.maxHeight - height) / 2)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .bubbleBurstRing(bubble, DiscardRed),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(squish)
                .clip(DiscardButtonShape)
                .liquefiable(liquidState)
                .background(backdrop),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(squish)
                .clip(DiscardButtonShape)
                .liquid(liquidState) {
                    shape = DiscardButtonShape
                    frost = 3.dp
                    curve = 0.4f
                    refraction = 0.35f
                    edge = 0.6f
                    dispersion = 0.15f
                    saturation = 1.2f
                    tint = Color.White.copy(alpha = if (isLight) 0.18f else 0.06f)
                }
                .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0.15f))), DiscardButtonShape)
                .clickable(
                    interactionSource = bubble.interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = { bubble.tap(onClick) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tr("Discard Changes"),
                modifier = Modifier.graphicsLayer {
                    scaleX = bubble.pressScale
                    scaleY = bubble.pressScale
                },
                color = DiscardRed,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
