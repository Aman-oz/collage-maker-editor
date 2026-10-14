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
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            Row(modifier = Modifier.fillMaxSize()) {
                stage(Modifier.weight(1f).fillMaxHeight())
                Column(modifier = Modifier.width(EditorSidePanelWidth).fillMaxHeight()) {
                    topBar()
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        if (panelFillsSideColumn) {
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
                Box(modifier = Modifier.fillMaxWidth().height(panelHeight)) { panel(false) }
                if (!tabsAbovePanel) tabs(false)
            }
        }
    }
}
