package org.example.project.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.example.project.ui.theme.AppTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_home_filled
import photocollagemaker.shared.generated.resources.ic_home_outline
import photocollagemaker.shared.generated.resources.ic_plus_icon
import photocollagemaker.shared.generated.resources.ic_project_filled
import photocollagemaker.shared.generated.resources.ic_project_outline

/** Tabs hosted by [HomeScreen]. The centre "+" button is an action (new collage), not a tab. */
enum class HomeTab { Home, Projects }

private val BarHeight = 64.dp
private val HumpHeight = 20.dp
private val HumpHalfWidth = 64.dp
private val BarCornerRadius = 12.dp
private val FabSize = 56.dp
private val FabPressedGrowth = 8.dp

/** Gap between the hump's peak and the top of the FAB, so the hump reads as a halo around it. */
private val FabHumpGap = 10.dp

/** The FAB's touch/burst area, larger than the FAB so the ring has room to spread and grow. */
private val FabBurstSize = FabSize + FabPressedGrowth + 16.dp
private val BubbleWidth = 84.dp
private val BubbleHeight = 52.dp

/**
 * How much the bubble swells while a finger holds it. It is deliberately larger than the bar, so
 * the held bubble overflows it like a drop lifted off the glass.
 */
private val BubbleHeldGrowthWidth = 30.dp
private val BubbleHeldGrowthHeight = 30.dp

/** The accent lightened (violet-300) for icons and labels on the dark theme. */
private val HomeAccentOnDark = Color(0xFFC4B5FD)

/** Maps the drawables' [HomeAccent] (#8B5CF6) onto [HomeAccentOnDark]; white clamps to white. */
private val AccentOnDarkFilter = ColorFilter.lighting(
    multiply = Color.White,
    add = Color(red = 0xC4 - 0x8B, green = 0xB5 - 0x5C, blue = 0xFD - 0xF6),
)

/** The bar is three equal slots: Home, the "+" action, Projects. */
private const val SlotCount = 3
private fun HomeTab.slot(): Int = if (this == HomeTab.Home) 0 else 2

/**
 * How much the selection bubble stretches per slot/second of travel, and the cap on it. The
 * stretch is what makes the bubble read as liquid: it elongates as it flies and snaps back round
 * as the spring settles.
 */
private const val StretchPerVelocity = 0.07f
private const val MaxStretch = 0.55f

/** Bouncy enough to overshoot visibly, so presses feel like a bubble swelling under the finger. */
private val BubbleSpring = spring<Float>(dampingRatio = 0.45f, stiffness = 420f)

/** Chases the finger while the bubble is held: stiff enough to keep up, loose enough to stretch. */
private val FollowSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 700f)

/** Dark mode's bar glass colour, also the selection drop's body on dark. */
private val DarkBarColor = Color(0xFF1C1B22)

/** Light mode's selection drop body: the accent washed nearly to white. */
private val LightDropBody = Color(0xFFE6DDFD)

/** Light mode's bar body: a faint cool off-white, so the white selection glass reads against it. */
private val BarOffWhite = Color(0xFFF1F0F5)

/** The whole bar's tap bounce: how much it narrows (a fraction of its width) and how far it dips. */
private const val BarBounceSqueeze = 0.025f
private val BarBounceDip = 3.dp
private val BarBounceSpring = spring<Float>(dampingRatio = 0.32f, stiffness = 380f)

/** Drops the released bubble onto its tab with a visible wobble. */
private val SettleSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 320f)

/**
 * Liquid Glass bottom bar with a raised hump in the middle that cradles the floating "+" button.
 *
 * The selection bubble is draggable: a finger down anywhere on the tabs swells it into a large
 * clear drop that follows the finger (tapping another tab flies it there); on release it snaps to
 * the nearest tab, selects it, and settles back into the resting bubble. One `pointerInput` on the
 * bar owns all of this, so the items themselves carry only accessibility semantics.
 *
 * [liquidState] must be the state the screen content behind the bar is `liquefiable` with; the bar
 * is drawn above that content and never inside it, or the glass would sample itself.
 *
 * Liquid's lens is a rounded-rect SDF, so it cannot follow the hump. The bar is instead a plain
 * rectangular lens clipped to [HumpedBarShape], with the rim light drawn by hand along the humped
 * top edge (the lens's own edge light is off, as it would trace the rectangle). The bar draws
 * [HumpHeight] above its flat top, so it is laid out that much taller.
 *
 * Every glass element here animates its *layout* size and position, never a `graphicsLayer`
 * scale: Liquid samples the backdrop from the node's layout bounds, so a scaled layer would
 * refract the wrong region. Only the icons (plain images) use `graphicsLayer`.
 */
@Composable
internal fun HomeBottomBar(
    liquidState: LiquidState,
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val barTint = if (isLight) Color.White.copy(alpha = 0.5f) else DarkBarColor.copy(alpha = 0.55f)
    // Light only: an off-white body under the rim, so the clear selection bubble has something to
    // stand out from instead of melting into an equally white bar.
    val barFill = if (isLight) BarOffWhite.copy(alpha = 0.78f) else Color.Transparent
    val rim = Color.White.copy(alpha = if (isLight) 0.9f else 0.25f)

    val scope = rememberCoroutineScope()
    // The bubble's centre as a fractional slot index; shared by the gesture and the bubble.
    val position = remember { Animatable(selectedTab.slot().toFloat()) }
    var held by remember { mutableStateOf(false) }
    // The slot the held bubble is over, so that item's icon swells with it.
    val hoveredSlot by remember { derivedStateOf { position.value.roundToInt() } }
    var burstTab by remember { mutableStateOf<HomeTab?>(null) }
    var burstCount by remember { mutableIntStateOf(0) }
    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val currentOnSelectTab by rememberUpdatedState(onSelectTab)

    // Selection changed from outside (or the release below): glide there, unless a finger has it.
    LaunchedEffect(selectedTab) {
        if (!held) position.animateTo(selectedTab.slot().toFloat(), SettleSpring)
    }

    // 0 at rest; a tap kicks it to 1 and a loose spring brings it back with an overshoot.
    val bounce = remember { Animatable(0f) }
    fun bounceBar() {
        scope.launch {
            bounce.animateTo(1f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
            bounce.animateTo(0f, BarBounceSpring)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            // The bar squeezes in and dips, then overshoots wider and lifted before settling. This is
            // done in layout (real width and placement), not a graphicsLayer scale, so the glass keeps
            // sampling the backdrop that is actually under it.
            .layout { measurable, constraints ->
                val b = bounce.value
                val width = (constraints.maxWidth * (1f - BarBounceSqueeze * b)).roundToInt()
                val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                layout(constraints.maxWidth, placeable.height) {
                    placeable.place((constraints.maxWidth - width) / 2, (BarBounceDip.toPx() * b).roundToInt())
                }
            },
    ) {
        // The shadow is its own layer under the glass: the glass paints over it with the refracted
        // backdrop, so only the soft halo above the bar shows.
        Box(
            Modifier
                .matchParentSize()
                .shadow(
                    elevation = 14.dp,
                    shape = HumpedBarShape,
                    ambientColor = HomeAccentDeep.copy(alpha = 0.3f),
                    spotColor = HomeAccentDeep.copy(alpha = 0.3f),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(HumpedBarShape)
                .liquid(liquidState) {
                    shape = RectangleShape
                    frost = 12.dp
                    curve = 0.2f
                    refraction = 0.2f
                    edge = 0f
                    dispersion = 0.12f
                    saturation = 1.35f
                    tint = barTint
                }
                .drawWithContent {
                    drawContent()
                    if (barFill.alpha > 0f) drawRect(barFill)
                    val edgePath = humpedPath(size, this, closed = false)
                    val rimBottom = (HumpHeight + BarCornerRadius).toPx()
                    drawPath(
                        path = edgePath,
                        brush = Brush.verticalGradient(listOf(rim, rim.copy(alpha = rim.alpha * 0.45f)), endY = rimBottom),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                },
        )

        Column(modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
            Spacer(Modifier.height(HumpHeight))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .pointerInput(Unit) {
                        val lastSlot = (SlotCount - 1).toFloat()
                        fun slotAt(x: Float) = (x / (size.width / SlotCount.toFloat()) - 0.5f).coerceIn(0f, lastSlot)
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            down.consume()
                            held = true
                            // On touch, not on release, so the bar answers the finger immediately.
                            bounceBar()
                            scope.launch { position.animateTo(slotAt(down.position.x), FollowSpring) }
                            var lastX = down.position.x
                            var dragged = false
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    lastX = change.position.x
                                    if (!change.pressed) break
                                    if (abs(lastX - down.position.x) > viewConfiguration.touchSlop) dragged = true
                                    change.consume()
                                    // A fresh animateTo carries the running velocity over, so the
                                    // bubble keeps its inertia (and its stretch) while following.
                                    scope.launch { position.animateTo(slotAt(lastX), FollowSpring) }
                                }
                            } finally {
                                held = false
                            }
                            val releaseSlot = slotAt(lastX)
                            val tab = when {
                                // A tap beside the "+" (outside its own touch area) selects nothing.
                                !dragged && abs(releaseSlot - 1f) < 0.5f -> currentSelectedTab
                                releaseSlot < 1f -> HomeTab.Home
                                else -> HomeTab.Projects
                            }
                            scope.launch { position.animateTo(tab.slot().toFloat(), SettleSpring) }
                            if (tab != currentSelectedTab || !dragged) {
                                burstTab = tab
                                burstCount++
                            }
                            if (tab != currentSelectedTab) currentOnSelectTab(tab)
                        }
                    },
            ) {
                SelectionBubble(
                    liquidState = liquidState,
                    position = position,
                    held = held,
                    isLight = isLight,
                )

                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    BottomBarItem(
                        label = "Home",
                        icon = if (selectedTab == HomeTab.Home) Res.drawable.ic_home_filled else Res.drawable.ic_home_outline,
                        selected = selectedTab == HomeTab.Home,
                        hovered = held && hoveredSlot == HomeTab.Home.slot(),
                        burstKey = if (burstTab == HomeTab.Home) burstCount else 0,
                        onClick = { onSelectTab(HomeTab.Home) },
                        modifier = Modifier.weight(1f).padding(top = 4.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    BottomBarItem(
                        label = "Projects",
                        icon = if (selectedTab == HomeTab.Projects) {
                            Res.drawable.ic_project_filled
                        } else {
                            Res.drawable.ic_project_outline
                        },
                        selected = selectedTab == HomeTab.Projects,
                        hovered = held && hoveredSlot == HomeTab.Projects.slot(),
                        burstKey = if (burstTab == HomeTab.Projects) burstCount else 0,
                        onClick = { onSelectTab(HomeTab.Projects) },
                        modifier = Modifier.weight(1f).padding(top = 4.dp),
                    )
                }
            }
        }

        // Centred on where the FAB hangs, [FabHumpGap] below the hump's peak.
        CreateButton(
            liquidState = liquidState,
            onClick = onCreate,
            onPressStart = ::bounceBar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = FabHumpGap + FabSize / 2 - FabBurstSize / 2),
        )
    }
}

/**
 * The bar's outline: a rectangle with slightly rounded top corners whose top edge rises into a
 * smooth hump centred horizontally. The flat top edge sits [HumpHeight] below the top; the hump
 * peaks at y = 0. One cubic per side, with both control points pulled halfway in, gives a soft
 * shoulder at the base and a broad, dome-like top that follows the FAB instead of a hard semicircle.
 *
 * With [closed] false it is only the top edge, corner to corner, for stroking the rim light.
 */
private fun humpedPath(size: Size, density: Density, closed: Boolean): Path {
    fun Dp.px() = with(density) { toPx() }
    val flatTop = HumpHeight.px()
    val halfWidth = HumpHalfWidth.px()
    val corner = BarCornerRadius.px()
    val centerX = size.width / 2f
    return Path().apply {
        moveTo(0f, flatTop + corner)
        quadraticTo(0f, flatTop, corner, flatTop)
        lineTo(centerX - halfWidth, flatTop)
        cubicTo(
            centerX - halfWidth * 0.5f, flatTop,
            centerX - halfWidth * 0.5f, 0f,
            centerX, 0f,
        )
        cubicTo(
            centerX + halfWidth * 0.5f, 0f,
            centerX + halfWidth * 0.5f, flatTop,
            centerX + halfWidth, flatTop,
        )
        lineTo(size.width - corner, flatTop)
        quadraticTo(size.width, flatTop, size.width, flatTop + corner)
        if (closed) {
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

private val HumpedBarShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(humpedPath(size, density, closed = true))
}

/**
 * The glass bubble behind the selected tab, centred on [position] (a fractional slot index the bar's
 * gesture drives). It stretches along its travel in proportion to its speed and squashes slightly
 * in height so the volume looks conserved.
 *
 * While [held] it swells past the bar's height into a clear, strongly refracting drop (the tint
 * fades out, the lens and rim strengthen); released, it settles back into the smaller tinted
 * bubble. One press progress drives all of it, so size and material can't drift. It casts no
 * shadow: an elevation shadow haloes the whole outline, top included, and shows through the glass.
 */
@Composable
private fun BoxScope.SelectionBubble(liquidState: LiquidState, position: Animatable<Float, *>, held: Boolean, isLight: Boolean) {
    val press by animateFloatAsState(if (held) 1f else 0f, BubbleSpring)
    // The spring overshoots slightly past 0 / 1; only the size may use the overshoot.
    val t = press.coerceIn(0f, 1f)
    // Light keeps a faint accent wash; on dark an accent wash turns into a purple blob, so there
    // the bubble is clear white glass carried by its rim.
    val restTint = if (isLight) HomeAccent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f)
    val tint = restTint.copy(alpha = restTint.alpha * (1f - 0.75f * t))
    val rim = Color.White.copy(alpha = if (isLight) 0.9f else 0.4f + 0.3f * t)
    // On light a white rim vanishes against the off-white bar, so its lower half shades to accent.
    val rimLow = if (isLight) HomeAccent.copy(alpha = 0.35f + 0.1f * t) else rim.copy(alpha = rim.alpha * (0.45f + 0.3f * t))

    val bubbleLayout = Modifier.slotLayout(
        slotCenter = { position.value },
        width = {
            val stretch = (abs(position.velocity) * StretchPerVelocity).coerceAtMost(MaxStretch)
            BubbleWidth.toPx() * (1f + stretch) + BubbleHeldGrowthWidth.toPx() * press
        },
        height = {
            val stretch = (abs(position.velocity) * StretchPerVelocity).coerceAtMost(MaxStretch)
            BubbleHeight.toPx() * (1f - stretch * 0.22f) + BubbleHeldGrowthHeight.toPx() * press
        },
    )

    Box(
        modifier = Modifier
            .matchParentSize()
            .then(bubbleLayout)
            .clip(CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                // A strong lens warps the backdrop near its rim into twisted purple (light) or grey
                // (dark) swirls, and pulls in the black past the screen's edge. So the lens is gentle,
                // like the magnification of a real drop. No frost: its blur mixes the transparent
                // pixels outside the lens into the rim, which reads as a grey shadow along the top.
                // The rim is carried by [drawWaterDropLight] and the border, not Liquid's edge light,
                // which reads as a grey band on dark.
                frost = 0.dp
                curve = 0.22f + 0.08f * t
                refraction = 0.14f + 0.08f * t
                edge = 0.2f
                dispersion = 0f
                saturation = if (isLight) 1.2f else 1.05f
                this.tint = tint
            }
            // Over the glass for the same reason as the "+" fill: the tint alone barely shows.
            .background(tint, CircleShape)
            .drawWithContent {
                drawContent()
                drawWaterDropLight(isLight = isLight, held = t)
            }
            // Top-to-bottom only: a diagonal gradient on a wide pill leaves uneven, twisted-looking
            // bright patches at two corners.
            .border(
                width = 1.dp + 0.5.dp * t,
                brush = Brush.verticalGradient(
                    0f to rim,
                    0.5f to if (isLight) HomeAccent.copy(alpha = 0.18f) else rim.copy(alpha = rim.alpha * 0.2f),
                    1f to rimLow,
                ),
                shape = CircleShape,
            ),
    )
}

/**
 * The lighting that makes the bubble read as a smooth drop of water rather than a flat lens.
 *
 * First an even body: the lens shows whatever is really behind the bar, and that is uneven —
 * content above the bar comes through its upper half, the screen bottom through its lower half —
 * which reads as a shadow over half the drop. A translucent body in the bar's own colour (off-white
 * on light, the dark glass colour on dark) over the whole drop evens that out.
 *
 * Then only a soft glow where the drop focuses light onto its lower inside edge. There is no top
 * sheen: a half-height wash is exactly what made the drop look split in two. [held] (0..1)
 * strengthens the glow as the drop swells.
 */
private fun DrawScope.drawWaterDropLight(isLight: Boolean, held: Float) {
    val corner = CornerRadius(size.height / 2f)
    drawRoundRect(
        // On light, a soft lavender rather than the bar's off-white, so the selection reads against it.
        color = if (isLight) LightDropBody.copy(alpha = 0.75f) else DarkBarColor.copy(alpha = 0.5f),
        cornerRadius = corner,
    )
    val poolAlpha = if (isLight) 0.3f + 0.15f * held else 0.06f + 0.06f * held
    drawRoundRect(
        brush = Brush.verticalGradient(
            0.62f to Color.White.copy(alpha = 0f),
            1f to Color.White.copy(alpha = poolAlpha),
        ),
        cornerRadius = corner,
    )
}

/**
 * Sizes the modified element to [width] × [height] px and centres it on slot [slotCenter] (a
 * fractional slot index) within the full-bar constraints it receives. The lambdas are read in the
 * layout phase, so an animation only re-lays out the node instead of recomposing the bar.
 */
private fun Modifier.slotLayout(
    slotCenter: () -> Float,
    width: Density.() -> Float,
    height: Density.() -> Float,
): Modifier = layout { measurable, constraints ->
    val w = width().toInt().coerceAtLeast(0)
    val h = height().toInt().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) {
        val slotWidth = constraints.maxWidth / SlotCount.toFloat()
        val x = slotWidth * (slotCenter() + 0.5f) - w / 2f
        val y = (constraints.maxHeight - h) / 2f
        placeable.place(x.toInt(), y.toInt())
    }
}

/**
 * A ring that bursts out of the tapped item and fades, like a bubble popping. Returns the modifier
 * that draws it and the trigger to fire from `onClick`.
 */
@Composable
private fun rememberBubbleBurst(color: Color): Pair<Modifier, () -> Unit> {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(1f) }
    val modifier = Modifier.drawBehind {
        val p = progress.value
        if (p >= 1f) return@drawBehind
        val baseRadius = size.minDimension / 2f
        val radius = baseRadius * (0.55f + 0.75f * p)
        val alpha = 1f - p
        drawCircle(color.copy(alpha = 0.18f * alpha), radius = radius)
        drawCircle(color.copy(alpha = 0.6f * alpha), radius = radius, style = Stroke(width = 2.dp.toPx() * (1f - p * 0.6f)))
    }
    val trigger = {
        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
        }
        Unit
    }
    return modifier to trigger
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: DrawableResource,
    selected: Boolean,
    hovered: Boolean,
    burstKey: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // The drawables and label are the #8B5CF6 accent, too dark to read on dark glass.
    val accent = if (isLight) HomeAccent else HomeAccentOnDark
    val (burst, fireBurst) = rememberBubbleBurst(accent)
    // The bar's gesture owns taps and drags; it bumps [burstKey] when the bubble lands here.
    LaunchedEffect(burstKey) { if (burstKey > 0) fireBurst() }
    // Icons are plain images, so a graphicsLayer scale is safe here (unlike on the glass).
    val iconScale by animateFloatAsState(
        targetValue = when {
            // Magnified under the held bubble, like an object seen through a drop of water.
            hovered -> 1.18f
            selected -> 1.08f
            else -> 1f
        },
        animationSpec = BubbleSpring,
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .then(burst)
            // Touches are handled by the bar (so the bubble can be dragged between items); this
            // keeps each item a selectable tab for accessibility services.
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                this.selected = selected
                // The semantics onClick (named argument), not the item's own onClick parameter.
                onClick(label = label, action = {
                    onClick()
                    true
                })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The icons carry their own accent colour (the filled/outline pair marks selection), so no
        // tint on light. On dark they are lifted to [HomeAccentOnDark] by adding a fixed offset
        // rather than a SrcIn tint, which would flood the filled icons' white cut-out details.
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = if (isLight) null else AccentOnDarkFilter,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Text(
            text = label,
            // White on dark reads cleanest against the clear glass bubble.
            color = when {
                selected && !isLight -> Color.White
                selected -> accent
                else -> accent.copy(alpha = 0.75f)
            },
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/**
 * The "+" action: accent-tinted glass rather than a flat disc, so the backdrop still bends through
 * its rim. It sits in the hump rather than in a slot, so the bar positions it. It swells on press and the plus turns a quarter each tap.
 */
@Composable
private fun CreateButton(
    liquidState: LiquidState,
    onClick: () -> Unit,
    onPressStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    // Fires on touch down (clickable's onClick only fires on release).
    LaunchedEffect(pressed) { if (pressed) currentOnPressStart() }
    val press by animateFloatAsState(if (pressed) 1f else 0f, BubbleSpring)
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 90f, spring(dampingRatio = 0.55f, stiffness = 260f))
    val (burst, fireBurst) = rememberBubbleBurst(HomeAccent)
    val rim = Color.White.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .size(FabBurstSize)
            .then(burst),
        contentAlignment = Alignment.Center,
    ) {
        val pressedSize = Modifier.layout { measurable, _ ->
            val side = (FabSize + FabPressedGrowth * press).roundToPx().coerceAtLeast(0)
            val placeable = measurable.measure(Constraints.fixed(side, side))
            layout(side, side) { placeable.place(0, 0) }
        }
        // A sibling rather than a `shadow` on the glass: the liquid node doesn't render inside the
        // clipped, elevated layer `shadow` creates.
        Box(
            pressedSize.shadow(elevation = 8.dp, shape = CircleShape, spotColor = HomeAccentDeep, ambientColor = HomeAccentDeep),
        )
        Box(
            modifier = pressedSize
                .clip(CircleShape)
                .liquid(liquidState) {
                    shape = CircleShape
                    frost = 4.dp
                    curve = 0.5f
                    refraction = 0.4f
                    edge = 0.8f
                    dispersion = 0.2f
                    saturation = 1.3f
                    tint = HomeAccent.copy(alpha = 0.82f)
                }
                // Liquid's tint renders far fainter than its alpha suggests (see the gallery's Done
                // button), so the accent is a translucent fill over the glass. It is not opaque, so
                // the refraction still shows through toward the rim.
                .background(
                    Brush.linearGradient(listOf(HomeAccent.copy(alpha = 0.88f), HomeAccentDeep.copy(alpha = 0.92f))),
                    CircleShape,
                )
                .border(1.dp, Brush.verticalGradient(listOf(rim, rim.copy(alpha = 0f))), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Create collage",
                    onClick = {
                        turns += 1f
                        fireBurst()
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_plus_icon),
                contentDescription = "Create collage",
                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun HomeBottomBarPreviewFrame(darkTheme: Boolean, initialTab: HomeTab) {
    AppTheme(darkTheme = darkTheme) {
        val liquidState = rememberLiquidState()
        // Tab selection is local state so clicking the items works in interactive preview mode.
        var selectedTab by remember { mutableStateOf(initialTab) }
        Box(modifier = Modifier.width(393.dp).height(160.dp)) {
            // Something colourful behind the glass, so the refraction is visible in the preview.
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
                    .liquefiable(liquidState),
            ) {
                listOf(Color(0xFF4F46E5), Color(0xFFFF6BAE), Color(0xFFFFA24C), Color(0xFF22D3EE)).forEach {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp).clip(CircleShape).background(it))
                }
            }
            HomeBottomBar(
                liquidState = liquidState,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onCreate = {},
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(widthDp = 810, heightDp = 340)
@Composable
private fun HomeBottomBarPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HomeTab.entries.forEach { tab ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                HomeBottomBarPreviewFrame(darkTheme = false, initialTab = tab)
                HomeBottomBarPreviewFrame(darkTheme = true, initialTab = tab)
            }
        }
    }
}
