package org.example.project.ui.text

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.fletchmckee.liquid.LiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.project.ui.common.glassPanel

/** Which way a [GlassGenieMenu] unfolds from its anchor. */
internal enum class GenieDirection {
    /** Up out of the anchor, centred on it, its last row landing right over the anchor. */
    Up,

    /** Down from under the anchor, its left edge on the anchor's. */
    Down,
}

/** The gap between the anchor and a menu that unfolds [GenieDirection.Down] from it. */
private val DownGap = 6.dp

/** The least room a menu keeps from the edges of the area it is drawn in. */
private val EdgeMargin = 8.dp

/**
 * A list in the discard dialog's frosted Liquid Glass that grows out of the thing it was opened from
 * ([anchor], in this composable's own coordinates) and shrinks back into it, like a Mac window
 * minimizing to the Dock. The Text tool's category menu and its ⋯ menu are both this.
 *
 * That genie move is two progress values, so the menu doesn't simply scale. Opening, the height
 * leads and the width follows: a pillar rises out of the anchor and then spreads. Closing, the width
 * goes first and the height then folds into the anchor. The glass is resized in layout rather than
 * scaled by a graphicsLayer, because Liquid samples its backdrop from layout bounds. The [content]
 * is laid out once at full size, at the menu's final place, and simply revealed as the glass grows
 * around it; it is handed a cascade (0 → 1, see [cascadeIn]) to bring its rows in one by one.
 *
 * Behind it the area dims, like under the dialog; a tap there calls [onDismiss].
 *
 * @param liquidState the backdrop: whatever is drawn `liquefiable` beside this menu. A Popup would
 * be another window, which Liquid can't sample.
 */
@Composable
internal fun GlassGenieMenu(
    open: Boolean,
    anchor: Rect?,
    liquidState: LiquidState,
    rowCount: Int,
    rowHeight: Dp,
    width: Dp,
    direction: GenieDirection,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(cascade: Animatable<Float, AnimationVector1D>) -> Unit,
) {
    val heightProgress = remember { Animatable(0f) }
    val widthProgress = remember { Animatable(0f) }
    val cascade = remember { Animatable(0f) }
    // Outlives `open` by the closing animation.
    var shown by remember { mutableStateOf(false) }
    // Kept from opening so the menu shrinks back into the same place even once the anchor is gone.
    var lastAnchor by remember { mutableStateOf<Rect?>(null) }
    if (open && anchor != null) lastAnchor = anchor

    LaunchedEffect(open) {
        if (open) {
            shown = true
            cascade.snapTo(0f)
            coroutineScope {
                launch { heightProgress.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
                launch {
                    delay(90)
                    widthProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                }
                launch {
                    delay(120)
                    cascade.animateTo(1f, cascadeSpec(rowCount))
                }
            }
        } else if (shown) {
            coroutineScope {
                launch { widthProgress.animateTo(0f, tween(220, easing = FastOutLinearInEasing)) }
                launch {
                    delay(70)
                    heightProgress.animateTo(0f, tween(260, easing = FastOutLinearInEasing))
                }
            }
            shown = false
        }
    }

    val from = lastAnchor
    if (!shown || from == null) return
    val noRipple = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val target = with(density) {
            val w = width.toPx()
            val h = rowHeight.toPx() * rowCount
            val margin = EdgeMargin.toPx()
            val areaWidth = constraints.maxWidth.toFloat()
            val areaHeight = constraints.maxHeight.toFloat()
            val left = when (direction) {
                GenieDirection.Up -> from.center.x - w / 2f
                GenieDirection.Down -> from.left
            }.coerceIn(margin, (areaWidth - w - margin).coerceAtLeast(margin))
            val top = when (direction) {
                GenieDirection.Up -> from.center.y + rowHeight.toPx() / 2f - h
                GenieDirection.Down -> from.bottom + DownGap.toPx()
            }.coerceIn(margin, (areaHeight - h - margin).coerceAtLeast(margin))
            Rect(left, top, left + w, top + h)
        }
        // From the anchor to the target: the sides along the width's progress, top and bottom along
        // the height's.
        fun current() = Rect(
            left = lerp(from.left, target.left, widthProgress.value),
            top = lerp(from.top, target.top, heightProgress.value),
            right = lerp(from.right, target.right, widthProgress.value),
            bottom = lerp(from.bottom, target.bottom, heightProgress.value),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = heightProgress.value }
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(interactionSource = noRipple, indication = null, enabled = open, onClick = onDismiss),
        )
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val rect = current()
                    val placeable = measurable.measure(
                        Constraints.fixed(rect.width.roundToInt().coerceAtLeast(0), rect.height.roundToInt().coerceAtLeast(0)),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
                    }
                }
                .glassPanel(liquidState)
                // Swallows taps between rows so they don't reach the dismissing scrim.
                .clickable(interactionSource = noRipple, indication = null, onClick = {}),
        ) {
            Column(
                modifier = Modifier
                    // At full size and its final place, whatever the glass around it is doing.
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(
                            Constraints.fixed(target.width.roundToInt(), target.height.roundToInt()),
                        )
                        val rect = current()
                        layout(rect.width.roundToInt().coerceAtLeast(0), rect.height.roundToInt().coerceAtLeast(0)) {
                            placeable.place((target.left - rect.left).roundToInt(), (target.top - rect.top).roundToInt())
                        }
                    }
                    // The words fade with the width, so closing they are gone before the glass folds.
                    .graphicsLayer { alpha = widthProgress.value },
            ) {
                content(cascade)
            }
        }
    }
}
