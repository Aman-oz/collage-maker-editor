package org.example.project.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private val FabSize = 56.dp
private val BarCornerRadius = 12.dp

/** Gap between the hump's peak and the top of the FAB, so the hump reads as a halo around it. */
private val FabHumpGap = 10.dp

/**
 * Bottom bar with a raised hump in the middle that cradles the floating "+" button.
 *
 * The bar draws [HumpHeight] above its own top edge, so the whole component is laid out that much
 * taller and the flat part starts below the hump. The FAB hangs [FabHumpGap] below the hump's peak
 * rather than poking out of it.
 */
@Composable
internal fun HomeBottomBar(
    selectedTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = HumpedBarShape)
                .background(barColor, HumpedBarShape)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Spacer(Modifier.height(HumpHeight))
            Row(
                modifier = Modifier.fillMaxWidth().height(BarHeight),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomBarItem(
                    label = "Home",
                    icon = if (selectedTab == HomeTab.Home) Res.drawable.ic_home_filled else Res.drawable.ic_home_outline,
                    selected = selectedTab == HomeTab.Home,
                    onClick = { onSelectTab(HomeTab.Home) },
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
                Spacer(Modifier.width(FabSize + 24.dp))
                BottomBarItem(
                    label = "Projects",
                    icon = if (selectedTab == HomeTab.Projects) {
                        Res.drawable.ic_project_filled
                    } else {
                        Res.drawable.ic_project_outline
                    },
                    selected = selectedTab == HomeTab.Projects,
                    onClick = { onSelectTab(HomeTab.Projects) },
                    modifier = Modifier.weight(1f).padding(top = 4.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = FabHumpGap)
                .size(FabSize)
                .shadow(elevation = 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(HomeAccent)
                .clickable(role = Role.Button, onClickLabel = "Create collage", onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_plus_icon),
                contentDescription = "Create collage",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: DrawableResource,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The icons carry their own accent colour (the filled/outline pair marks selection), so no tint.
        Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(28.dp))
        Text(
            text = label,
            color = HomeAccent,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * A rectangle with slightly rounded top corners whose top edge rises into a smooth hump centred
 * horizontally. The flat top edge sits [HumpHeight] below the shape's top; the hump peaks at y = 0.
 * One cubic per side, with both control points pulled halfway in, gives a soft shoulder at the base
 * and a broad, dome-like top that follows the FAB instead of a hard semicircle.
 */
private val HumpedBarShape: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        fun Dp.px() = with(density) { toPx() }
        val flatTop = HumpHeight.px()
        val halfWidth = HumpHalfWidth.px()
        val corner = BarCornerRadius.px()
        val centerX = size.width / 2f
        val path = Path().apply {
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
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
private fun HomeBottomBarPreviewFrame(darkTheme: Boolean, initialTab: HomeTab) {
    AppTheme(darkTheme = darkTheme) {
        // Tab selection is local state so clicking the items works in interactive preview mode.
        var selectedTab by remember { mutableStateOf(initialTab) }
        Box(
            modifier = Modifier
                .width(393.dp)
                .height(160.dp)
                .background(MaterialTheme.colorScheme.background),
        ) {
            HomeBottomBar(
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
