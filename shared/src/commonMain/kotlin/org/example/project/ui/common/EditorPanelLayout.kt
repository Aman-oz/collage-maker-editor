package org.example.project.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds

/** Width of the controls column when [EditorPanelLayout] puts it beside the picture. */
internal val EditorSidePanelWidth = 380.dp

/**
 * Below this height a landscape screen (a phone on its side) has no room for the picture above
 * the panels: the top bar, a fixed-height panel and the tabs alone take most of it.
 */
private val SideBySideMaxHeight = 500.dp

/**
 * Whether an editor of this size puts its controls in a column beside the picture rather than
 * under it: a phone on its side. Shared by [EditorPanelLayout], the photo editor and its tools'
 * [ToolScaffold], so they all switch at the same size.
 */
internal fun isSideBySideEditor(width: Dp, height: Dp): Boolean = width > height && height < SideBySideMaxHeight

/**
 * The layout of the Collage, Freestyle, PIP and Set Background editors: top bar, the picture ([stage]),
 * a [panelHeight] tool panel and the [tabs] that switch it, stacked top to bottom.
 *
 * On a phone in landscape that stack leaves the picture a sliver, so there the picture takes the
 * left side at full height and the top bar, panel and tabs stack in a column on the right. The
 * panel keeps its height in both (centred in whatever room the column leaves), so its content
 * lays out the same either way. [panel] and [tabs] are told which arrangement they are in, to fit
 * the column.
 *
 * @param tabsAbovePanel put [tabs] between the picture and the panel, as the collage editor does,
 * rather than under the panel. Beside the picture the tabs are always at the bottom of the column.
 * @param panelFillsSideColumn beside the picture, give [panel] all the column's height between the
 * top bar and the tabs instead of [panelHeight], for a panel that can show more there.
 * @param topBarInSidePanel beside the picture, leave [topBar] to [panel], which then fills the column
 * from the top: for an editor whose panels take the top bar's place, as the photo editor's tools do.
 * @param collapsedPanelHeight with [panelExpansion], under the picture the panel's slot is only this
 * tall (a tool strip's height) while nothing is open, and grows to [panelHeight] as [panelExpansion]
 * goes 0 → 1, so the picture takes the room and moves up as a panel rises, as in the photo editor.
 * [panel] is still laid out at [panelHeight], anchored to the bottom of the slot and clipped by it.
 * @param panelExpansion read in the layout pass, so animating it relayouts without recomposing.
 * @param expandedPanelHeightPx the open panel's own height, when it reports one: the slot grows to
 * that instead of all of [panelHeight], so the picture keeps the room the panel doesn't use. 0 or
 * more than [panelHeight] means [panelHeight].
 */
@Composable
internal fun EditorPanelLayout(
    panelHeight: Dp,
    topBar: @Composable () -> Unit,
    stage: @Composable (Modifier) -> Unit,
    panel: @Composable (sideBySide: Boolean) -> Unit,
    tabs: @Composable (sideBySide: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tabsAbovePanel: Boolean = false,
    panelFillsSideColumn: Boolean = false,
    topBarInSidePanel: Boolean = false,
    collapsedPanelHeight: Dp? = null,
    panelExpansion: (() -> Float)? = null,
    expandedPanelHeightPx: (() -> Int)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            Row(modifier = Modifier.fillMaxSize()) {
                stage(Modifier.weight(1f).fillMaxHeight())
                Column(modifier = Modifier.width(EditorSidePanelWidth).fillMaxHeight()) {
                    if (!topBarInSidePanel) topBar()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        if (panelFillsSideColumn || topBarInSidePanel) {
                            panel(true)
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(panelHeight)) { panel(true) }
                        }
                    }
                    tabs(true)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                topBar()
                stage(Modifier.fillMaxWidth().weight(1f))
                if (tabsAbovePanel) tabs(false)
                if (collapsedPanelHeight != null && panelExpansion != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val fullPx = panelHeight.roundToPx()
                                val collapsedPx = collapsedPanelHeight.roundToPx().coerceAtMost(fullPx)
                                val expandedPx = expandedPanelHeightPx?.invoke()?.takeIf { it in 1..fullPx } ?: fullPx
                                val heightPx = lerp(collapsedPx, expandedPx, panelExpansion().coerceIn(0f, 1f))
                                val placeable = measurable.measure(constraints.copy(minHeight = fullPx, maxHeight = fullPx))
                                layout(placeable.width, heightPx) { placeable.place(0, heightPx - fullPx) }
                            },
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(panelHeight)) { panel(false) }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(panelHeight)) { panel(false) }
                }
                if (!tabsAbovePanel) tabs(false)
            }
        }
    }
}
