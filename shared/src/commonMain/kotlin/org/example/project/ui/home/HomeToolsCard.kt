package org.example.project.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One entry of the Home "Tools" card. */
internal class HomeTool(val label: String, val icon: DrawableResource, val onClick: () -> Unit)

private val CardShape = RoundedCornerShape(14.dp)
private val ToolCircleSize = 56.dp

/** How much a tool's drop swells while pressed. It overflows its cell, like a drop lifting off. */
private const val ToolDropPressedScale = 1.22f

/** Where each tool's circle sits vertically: the card's top padding, the item's, then half the circle. */
private val ToolCircleCenterY = 14.dp + 4.dp + ToolCircleSize / 2

/** One soft light per tool in the card's backdrop, so the glass has colour to bend and magnify. */
private val ToolGlowColors = listOf(
    Color(0xFF8B5CF6),
    Color(0xFFFF6BAE),
    Color(0xFFFFA24C),
    Color(0xFF22D3EE),
)

/**
 * How long a tapped tool waits before navigating. The drop is held swollen for this long, so a
 * quick tap shows the whole swell (and the card's bounce) before the next screen slides up.
 */
internal const val ToolNavigationDelayMillis = 320L

/** The resting circle: a white-to-light-grey gradient with a thin grey border. */
private val ToolCircleBottom = Color(0xFFE9E9EE)
private val ToolCircleBorder = Color(0xFFDCDCE2)

private val ToolDropSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)
private val CardBounceSpring = spring<Float>(dampingRatio = 0.32f, stiffness = 380f)
private const val CardBounceSqueeze = 0.03f
private val CardBounceDip = 2.dp

/**
 * The Home "Tools" card as Liquid Glass: a frosted glass panel, and a clear glass drop behind each
 * tool's icon that swells into a magnifying water drop while pressed.
 *
 * The Home content is already the bottom bar's `liquefiable` backdrop, and a liquid node can't
 * sample a layer it is drawn inside, so the card carries its own small backdrop: the surface with a
 * soft coloured light behind each tool. The glass panel and the drops are its siblings drawn on top.
 *
 * Touching any tool bounces the whole card (on touch down, like the bottom bar): it squeezes in and
 * dips, then overshoots before settling. That is done in layout, not with a `graphicsLayer` scale,
 * because Liquid samples the backdrop from the node's layout bounds.
 */
@Composable
internal fun HomeToolsCard(tools: List<HomeTool>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    val liquidState = rememberLiquidState()
    val scope = rememberCoroutineScope()
    val bounce = remember { Animatable(0f) }
    val bounceCard: () -> Unit = {
        scope.launch {
            bounce.animateTo(1f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
            bounce.animateTo(0f, CardBounceSpring)
        }
    }
    val glowAlpha = if (isLight) 0.3f else 0.4f
    // Hoisted so the backdrop can light up only the tool being pressed.
    val interactions = remember(tools.size) { List(tools.size) { MutableInteractionSource() } }
    // A tap "pulses" its tool until it navigates. In a scrolling screen the press only registers
    // for an instant on a quick tap, so without this a tap would barely show the drop at all.
    val pulsing = remember(tools.size) { mutableStateListOf(*Array(tools.size) { false }) }
    // Set while a tap waits out [ToolNavigationDelayMillis], so a double tap can't navigate twice.
    var launching by remember { mutableStateOf(false) }
    val pressProgress = interactions.mapIndexed { i, source ->
        val pressed by source.collectIsPressedAsState()
        animateFloatAsState(if (pressed || pulsing[i]) 1f else 0f, ToolDropSpring)
    }
    // Whether the current gesture's touch-down already bounced the card. A quick tap in this
    // scrolling screen can press and release within one frame, so the press is never observed and
    // the tap has to bounce the card itself; a held press must not bounce it a second time.
    var pressBounced by remember { mutableStateOf(false) }
    val bounceOnPress: () -> Unit = {
        pressBounced = true
        bounceCard()
    }
    val tapTool: (Int) -> Unit = tap@{ i ->
        if (!pressBounced) bounceCard()
        pressBounced = false
        if (launching) return@tap
        launching = true
        pulsing[i] = true
        scope.launch {
            try {
                delay(ToolNavigationDelayMillis)
                tools[i].onClick()
            } finally {
                pulsing[i] = false
                launching = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val b = bounce.value
                val width = (constraints.maxWidth * (1f - CardBounceSqueeze * b)).roundToInt()
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place((constraints.maxWidth - width) / 2, (CardBounceDip.toPx() * b).roundToInt())
                }
            }
            .shadow(elevation = 10.dp, shape = CardShape, ambientColor = HomeAccent, spotColor = HomeAccent),
    ) {
        // The backdrop the glass bends: the plain surface, lit by a soft light behind a tool only while
        // it is pressed, so at rest the card is exactly the plain white card.
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(CardShape)
                .liquefiable(liquidState),
        ) {
            drawRect(scheme.surface)
            val cellWidth = size.width / tools.size
            val centerY = ToolCircleCenterY.toPx()
            tools.indices.forEach { i ->
                if (pressProgress[i].value <= 0f) return@forEach
                val center = Offset(cellWidth * (i + 0.5f), centerY)
                val radius = ToolCircleSize.toPx() * 0.95f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ToolGlowColors[i % ToolGlowColors.size].copy(alpha = glowAlpha * pressProgress[i].value.coerceIn(0f, 1f)),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
        }
        // The frosted glass panel over it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CardShape)
                .liquid(liquidState) {
                    shape = CardShape
                    frost = 14.dp
                    curve = 0.2f
                    refraction = 0.15f
                    edge = 0.3f
                    saturation = 1.3f
                    tint = scheme.surface.copy(alpha = 0.35f)
                }
                .border(1.dp, scheme.outlineVariant, CardShape),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp)) {
            tools.forEachIndexed { i, tool ->
                ToolItem(
                    tool = tool,
                    interaction = interactions[i],
                    press = pressProgress[i].value,
                    liquidState = liquidState,
                    onPressStart = bounceOnPress,
                    onTap = { tapTool(i) },
                )
            }
        }
    }
}

/**
 * A tool: a round background with the icon on it, and the label. At rest it is the plain light
 * circle (a white-to-grey gradient, a thin grey border and a small shadow). Pressed, a ripple spreads
 * over it and it swells by [ToolDropPressedScale] past its cell, clearing into a magnifying water
 * drop over the tool's light, with the icon zooming a little with it. One press progress drives size and material so they can't drift apart; the size
 * is laid out for real (`requiredSize`) so Liquid samples the right region.
 */
@Composable
private fun RowScope.ToolItem(
    tool: HomeTool,
    interaction: MutableInteractionSource,
    press: Float,
    liquidState: LiquidState,
    onPressStart: () -> Unit,
    onTap: () -> Unit,
) {
    val pressed by interaction.collectIsPressedAsState()
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    // Bounce the card on touch down; clickable's onClick only fires on release.
    LaunchedEffect(pressed) { if (pressed) currentOnPressStart() }
    // The spring overshoots slightly past 0 / 1; only the size and zoom may use the overshoot.
    val t = press.coerceIn(0f, 1f)
    val dropSize = ToolCircleSize * (1f + (ToolDropPressedScale - 1f) * press)
    // The resting circle's own grey border brightens into the drop's white rim.
    val rim = lerp(ToolCircleBorder, Color.White, t)
    // Opaque at rest (the plain circle); mostly clear once pressed, so the drop shows through.
    val bodyAlpha = 1f - 0.72f * t

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onTap)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(ToolCircleSize), contentAlignment = Alignment.Center) {
            // The resting circle's small shadow, a sibling under the glass (a liquid node doesn't
            // render inside the layer `shadow` creates). It fades out as the drop clears, or it would
            // show through as a grey disc.
            if (t < 1f) {
                Box(
                    Modifier
                        .requiredSize(dropSize)
                        .shadow(elevation = 2.dp * (1f - t), shape = CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .requiredSize(dropSize)
                    .clip(CircleShape)
                    .liquid(liquidState) {
                        shape = CircleShape
                        // Gentle at rest; a clear, magnifying lens when pressed. Kept moderate so the
                        // backdrop is magnified rather than twisted at the rim.
                        curve = 0.25f + 0.25f * t
                        refraction = 0.15f + 0.2f * t
                        edge = 0.25f
                        saturation = 1.3f
                        tint = Color.White.copy(alpha = 0.1f)
                    }
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = bodyAlpha), ToolCircleBottom.copy(alpha = bodyAlpha)),
                            ),
                        )
                        // Light focused by the drop onto its lower inside edge.
                        drawCircle(
                            brush = Brush.verticalGradient(
                                0.6f to Color.White.copy(alpha = 0f),
                                1f to Color.White.copy(alpha = 0.5f * t),
                            ),
                        )
                    }
                    // Bounded to the circle; the accent ripple spreads over the drop as it swells.
                    .indication(interaction, ripple(color = HomeAccent))
                    .border(
                        width = 1.dp + 0.5.dp * t,
                        // Even at rest; while pressed it fades at the sides like light on a drop.
                        brush = Brush.verticalGradient(
                            0f to rim,
                            0.5f to rim.copy(alpha = 1f - 0.75f * t),
                            1f to rim.copy(alpha = 1f - 0.3f * t),
                        ),
                        shape = CircleShape,
                    ),
            )
            // The icon is a plain image, so a graphicsLayer zoom is safe on it (not on the glass).
            val iconZoom = 1f + 0.12f * press
            Image(
                painter = painterResource(tool.icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp).graphicsLayer {
                    scaleX = iconZoom
                    scaleY = iconZoom
                },
            )
        }
        Text(
            text = tool.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
