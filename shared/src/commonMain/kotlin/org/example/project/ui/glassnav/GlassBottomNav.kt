package org.example.project.ui.glassnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import kotlinx.coroutines.launch

/**
 * One destination shown in a [GlassBottomNav] — a label/icon pair, plus an optional badge (a
 * count bubble, or a plain notification dot when [badgeCount] is null and [showDot] is true).
 */
data class GlassNavItem(
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int? = null,
    val showDot: Boolean = false,
)

private val BarHorizontalInset = 10.dp
private val BarVerticalInset = 12.dp

/**
 * A floating navigation bar rendered with Apple-style Liquid Glass: content scrolling underneath
 * it (marked with `Modifier.liquefiable(liquidState)`) is sampled, blurred, and gently refracted
 * through this bar, with a soft rim-light along its edge — not just a flat frosted tint. Self-
 * contained and theme-agnostic (colors are parameters with sensible defaults) so it can be
 * dropped into any screen — pair it with the same [LiquidState] the screen's content uses.
 *
 * The bar's own effect is tuned for a *readable panel* (icons/text sit on top), not a distorting
 * lens — low refraction/curve/edge to keep the rim-light subtle, with [containerColor] carrying
 * most of the frosted-glass look via [io.github.fletchmckee.liquid.LiquidScope.tint]. The
 * currently selected item gets its own glass "bubble" tightly wrapping just its icon+label —
 * [selectedColor]-tinted and more strongly refractive.
 *
 * Note on the transition between items: the underlying Liquid shader only ever reads a plain
 * rounded-rect corner radius from [io.github.fletchmckee.liquid.LiquidScope.shape] (see
 * `InternalLiquidScope.shape`) — it has no notion of a concave, two-lobe "connected blob" outline,
 * so a literal metaball merge between the old and new item isn't something this shader can draw.
 * What we do instead to still read as liquid: animate the bubble's leading and trailing edges
 * *independently* ([leadingEdgeSpec] snaps quickly, [trailingEdgeSpec] lags and catches up), so
 * the bubble visibly stretches toward the new item before contracting to fit it, rather than
 * sliding as a fixed-size pill.
 *
 * Item bounds for that bubble come from [NavItemsLayout]'s own placement pass (not
 * `Modifier.onGloballyPositioned` + cross-node coordinate translation), so the bubble can never
 * drift out of alignment with what's actually drawn.
 *
 * @param liquidState the [LiquidState] shared with the scrollable content behind this bar.
 */
@Composable
fun GlassBottomNav(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    liquidState: LiquidState,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.85f),
    selectedColor: Color = Color(0xFFD6FA3C),
    unselectedColor: Color = Color(0xFFB3B3B8),
    badgeColor: Color = Color(0xFFE53935),
) {
    val barShape = RoundedCornerShape(50)
    val indicatorShape = RoundedCornerShape(18.dp)
    val density = LocalDensity.current

    val itemBounds = remember(items.size) {
        mutableStateListOf<Rect?>().apply { repeat(items.size) { add(null) } }
    }
    val selectedBounds = itemBounds.getOrNull(selectedIndex)

    val leftPx = remember { Animatable(0f) }
    val rightPx = remember { Animatable(0f) }
    val topPx = remember { Animatable(0f) }
    val bottomPx = remember { Animatable(0f) }
    var hasPlacedIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBounds) {
        val target = selectedBounds ?: return@LaunchedEffect
        if (!hasPlacedIndicator) {
            leftPx.snapTo(target.left)
            rightPx.snapTo(target.right)
            topPx.snapTo(target.top)
            bottomPx.snapTo(target.bottom)
            hasPlacedIndicator = true
            return@LaunchedEffect
        }
        // The edge moving toward the new item leads (snaps quickly); the other edge lags and
        // catches up, so the bubble stretches elastically toward the target before contracting.
        val movingRight = target.left >= leftPx.value
        launch { leftPx.animateTo(target.left, if (movingRight) trailingEdgeSpec else leadingEdgeSpec) }
        launch { rightPx.animateTo(target.right, if (movingRight) leadingEdgeSpec else trailingEdgeSpec) }
        launch { topPx.animateTo(target.top, symmetricEdgeSpec) }
        launch { bottomPx.animateTo(target.bottom, symmetricEdgeSpec) }
    }

    Box(
        modifier = modifier
            .liquid(liquidState) {
                this.shape = barShape
                frost = 30.dp
                curve = 0.12f
                refraction = 0.08f
                edge = 0.3f
                dispersion = 0.05f
                saturation = 1.1f
                tint = containerColor
            },
    ) {
        if (hasPlacedIndicator) {
            Box(
                modifier = Modifier
                    .offset(x = with(density) { leftPx.value.toDp() }, y = with(density) { topPx.value.toDp() })
                    .size(
                        width = with(density) { (rightPx.value - leftPx.value).toDp() },
                        height = with(density) { (bottomPx.value - topPx.value).toDp() },
                    )
                    .clip(indicatorShape)
                    .liquid(liquidState) {
                        this.shape = indicatorShape
                        frost = 6.dp
                        curve = 0.35f
                        refraction = 0.3f
                        edge = 0.6f
                        dispersion = 0.2f
                        saturation = 1.3f
                        tint = selectedColor.copy(alpha = 0.22f)
                    },
            )
        }

        NavItemsLayout(
            items = items,
            selectedIndex = selectedIndex,
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            badgeColor = badgeColor,
            onItemSelected = onItemSelected,
            onBoundsMeasured = { index, bounds -> itemBounds[index] = bounds },
        )
    }
}

private val leadingEdgeSpec = spring<Float>(dampingRatio = 0.55f, stiffness = 420f)
private val trailingEdgeSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 160f)
private val symmetricEdgeSpec = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

/**
 * Measures every item at its own natural size (so the selected bubble in [GlassBottomNav] can
 * wrap it tightly, matching the reference design), then spaces them evenly — replicating
 * `Arrangement.SpaceEvenly` by hand so [onBoundsMeasured] reports the *exact* px rect each item
 * was placed at, in this layout's own coordinate space.
 */
@Composable
private fun NavItemsLayout(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    selectedColor: Color,
    unselectedColor: Color,
    badgeColor: Color,
    onItemSelected: (Int) -> Unit,
    onBoundsMeasured: (index: Int, bounds: Rect) -> Unit,
) {
    Layout(
        content = {
            items.forEachIndexed { index, item ->
                GlassNavItemView(
                    item = item,
                    selected = index == selectedIndex,
                    selectedColor = selectedColor,
                    unselectedColor = unselectedColor,
                    badgeColor = badgeColor,
                    onClick = { onItemSelected(index) },
                )
            }
        },
    ) { measurables, constraints ->
        val hInset = BarHorizontalInset.roundToPx()
        val vInset = BarVerticalInset.roundToPx()
        val placeables = measurables.map { it.measure(Constraints()) }
        val contentWidth = (constraints.maxWidth - hInset * 2).coerceAtLeast(0)
        val itemsWidth = placeables.sumOf { it.width }
        val gap = ((contentWidth - itemsWidth) / (placeables.size + 1)).coerceAtLeast(0)
        val maxItemHeight = placeables.maxOfOrNull { it.height } ?: 0
        val totalHeight = maxItemHeight + vInset * 2

        layout(constraints.maxWidth, totalHeight) {
            var x = hInset + gap
            placeables.forEachIndexed { index, placeable ->
                val y = vInset + (maxItemHeight - placeable.height) / 2
                placeable.placeRelative(x, y)
                onBoundsMeasured(index, Rect(x.toFloat(), y.toFloat(), (x + placeable.width).toFloat(), (y + placeable.height).toFloat()))
                x += placeable.width + gap
            }
        }
    }
}

@Composable
private fun GlassNavItemView(
    item: GlassNavItem,
    selected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) selectedColor else unselectedColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(imageVector = item.icon, contentDescription = item.label, tint = tint, modifier = Modifier.size(24.dp))

            if (item.badgeCount != null && item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 9.dp, y = (-6).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (item.badgeCount > 9) "9+" else "${item.badgeCount}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (item.showDot) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                )
            }
        }

        Text(
            text = item.label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
