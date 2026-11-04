package org.example.project.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * One tool of an [EditorToolStrip]: its icon over its label. [iconSize] is the box the icon is drawn
 * in: the photo editor's vectors are 46dp with the glyph inset to a 33dp area, so they are drawn at
 * the default 42dp; an icon whose glyph fills more of its viewport passes less, so the glyphs match.
 */
internal class EditorToolStripItem(val label: String, val icon: DrawableResource, val iconSize: Dp = 42.dp)

/**
 * The photo editor's toolbar: a row of [items] scrolling sideways, with a "more this way" arrow at
 * whichever edge has tools hidden past it. [background] is the bar's own colour, which the tools
 * fade into under the arrow. Also the layout editor's, so the two editors' tools look alike.
 */
@Composable
internal fun EditorToolStrip(
    items: List<EditorToolStripItem>,
    onItemSelected: (Int) -> Unit,
    background: Color,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val scope = rememberCoroutineScope()
    // One arrow, on the side there is more to see: at the right edge until the row is scrolled to
    // its end, then at the left edge pointing back. None when every tool fits on screen.
    val moreEdge by remember(listState) {
        derivedStateOf {
            when {
                listState.canScrollForward -> Alignment.CenterEnd
                listState.canScrollBackward -> Alignment.CenterStart
                else -> null
            }
        }
    }

    Box(modifier = modifier) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            // Centred when every tool fits (a wide tablet) rather than bunched at the left.
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, item ->
                EditorToolStripButton(item = item, onClick = { onItemSelected(index) })
            }
        }

        // matchParentSize, so the overlay follows the toolbar's height instead of setting it; the
        // two arrow panels then fill that height at their own edge.
        Box(modifier = Modifier.matchParentSize()) {
            ToolbarMoreArrow(
                background = background,
                visible = moreEdge == Alignment.CenterEnd,
                pointsToEnd = true,
                onClick = { scope.launch { listState.animateScrollToItem(items.lastIndex) } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            ToolbarMoreArrow(
                background = background,
                visible = moreEdge == Alignment.CenterStart,
                pointsToEnd = false,
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/**
 * Every tool at once, in rows, for the landscape column: the column is phone-width but has height
 * to spare, so the tools fit in a few rows with no scrolling (and no "more" arrows).
 */
@Composable
internal fun EditorToolStripGrid(
    items: List<EditorToolStripItem>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(68.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        // Centred in the column's height when the rows don't fill it.
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        modifier = modifier.fillMaxWidth(),
    ) {
        gridItemsIndexed(items) { index, item ->
            Box(contentAlignment = Alignment.Center) {
                EditorToolStripButton(item = item, onClick = { onItemSelected(index) })
            }
        }
    }
}

/** The part of the panel that takes the tap and holds the chevron. */
private val ToolbarArrowWidth = 40.dp

/** The whole panel, fade included: the tools dissolve into the bar's colour across this width. */
private val ToolbarArrowFadeWidth = 76.dp

/** How far the chevron drifts toward the hidden tools and back, as a hint that the row scrolls. */
private val ToolbarArrowNudge = 4.dp

/**
 * The "there are more tools this way" panel at one edge of the toolbar, as tall as the bar: the
 * tools fade out into the bar's own colour (the same brush the language list fades out with at its
 * bottom edge), and a chevron on the solid end keeps nudging toward the hidden tools. A tap on the
 * chevron's strip scrolls the row to that end. The panel slides in from its edge when it appears
 * and back out when the row reaches that end.
 *
 * Only the chevron's strip takes touches; the fade lets them through, so a tool dissolving under
 * it is still tappable.
 */
@Composable
private fun ToolbarMoreArrow(
    background: Color,
    visible: Boolean,
    pointsToEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = if (pointsToEnd) 1f else -1f
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { (it * direction).roundToInt() },
        exit = fadeOut() + slideOutHorizontally { (it * direction).roundToInt() },
    ) {
        val nudge by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        )
        // Clear on the tools' side, fully the bar's colour by the time it reaches the chevron.
        val clear = background.copy(alpha = 0f)
        val fade = if (pointsToEnd) {
            Brush.horizontalGradient(0f to clear, 0.6f to background, 1f to background)
        } else {
            Brush.horizontalGradient(0f to background, 0.4f to background, 1f to clear)
        }
        Box(
            modifier = Modifier.width(ToolbarArrowFadeWidth).fillMaxHeight().background(fade),
            contentAlignment = if (pointsToEnd) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier.width(ToolbarArrowWidth).fillMaxHeight().clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (pointsToEnd) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = tr("More tools"),
                    tint = MaterialTheme.colorScheme.onSurface,
                    // Moved in the layer, so the endless nudge never recomposes or re-lays-out the bar.
                    modifier = Modifier.size(26.dp).graphicsLayer { translationX = ToolbarArrowNudge.toPx() * nudge * direction },
                )
            }
        }
    }
}

@Composable
private fun EditorToolStripButton(item: EditorToolStripItem, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(60.dp)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = item.label,
                modifier = Modifier.size(item.iconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
