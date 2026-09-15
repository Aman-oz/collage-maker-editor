package org.example.project.ui.glassnav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

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

/**
 * A floating, frosted-glass bottom navigation bar: content scrolling underneath it (marked with
 * `Modifier.hazeSource(hazeState)`) shows through blurred, like an iOS tab bar. Self-contained and
 * theme-agnostic (colors are parameters with sensible defaults) so it can be dropped into any
 * screen — pair it with the same [HazeState] the screen's scrolling content uses.
 *
 * @param hazeState the [HazeState] shared with the scrollable content behind this bar.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassBottomNav(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF1C1C1E),
    selectedColor: Color = Color(0xFFD6FA3C),
    unselectedColor: Color = Color(0xFFB3B3B8),
    badgeColor: Color = Color(0xFFE53935),
) {
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeMaterials.thin(containerColor))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.12f), shape = shape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
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
) {
    val tint = if (selected) selectedColor else unselectedColor

    Column(
        modifier = Modifier
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
