package org.example.project.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fletchmckee.liquid.LiquidState

private val MenuWidth = 184.dp
private val MenuRowHeight = 40.dp

/**
 * The Font panel's list of main categories: a [GlassGenieMenu] growing up out of the category title
 * ([anchor]) over the photo. The selected category is the bottom row, sitting right over the title,
 * so the title seems to open into the list; the others follow in their usual order above it, rising
 * in from the title upwards.
 */
@Composable
internal fun FontCategoryMenu(
    open: Boolean,
    anchor: Rect?,
    titles: List<String>,
    selectedIndex: Int,
    liquidState: LiquidState,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val order = titles.indices.filter { it != selectedIndex } + selectedIndex
    GlassGenieMenu(
        open = open,
        anchor = anchor,
        liquidState = liquidState,
        rowCount = order.size,
        rowHeight = MenuRowHeight,
        width = MenuWidth,
        direction = GenieDirection.Up,
        onDismiss = onDismiss,
    ) { cascade ->
        order.forEachIndexed { position, index ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MenuRowHeight)
                    .background(if (selected) scheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(enabled = open) { onSelected(index) }
                    // Bottom-up: the row nearest the title comes first.
                    .cascadeIn(cascade, order.lastIndex - position, order.size, riseDp = 10f),
                contentAlignment = Alignment.Center,
            ) {
                // Inside the row rather than between rows, so the rows add up to the menu's height.
                if (position > 0) {
                    HorizontalDivider(
                        color = scheme.onSurface.copy(alpha = 0.12f),
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
                Text(
                    text = titles[index],
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) scheme.primary else scheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
