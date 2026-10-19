package org.example.project.ui.text

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_align
import photocollagemaker.shared.generated.resources.ic_align_center
import photocollagemaker.shared.generated.resources.ic_align_tblr
import photocollagemaker.shared.generated.resources.ic_arrange
import photocollagemaker.shared.generated.resources.ic_double_layer_arrow
import photocollagemaker.shared.generated.resources.ic_single_layer_arrow

/** The Position panel's tabs, in the order of its bottom bar. */
private enum class PositionTab(private val englishLabel: String, val icon: DrawableResource) {
    Arrange("Arrange", Res.drawable.ic_arrange),
    Align("Align", Res.drawable.ic_align),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * A button of the Position panel. The icons come in one direction each and are turned by
 * [iconDegrees] for the others: the layer arrows point up (forward, to front), the centre-line icon
 * stands upright (center across), and the edge icon sits on its line (bottom).
 */
private class PositionAction(val englishLabel: String, val icon: DrawableResource, val iconDegrees: Float)

private val ArrangeActions: List<Pair<TextLayerMove, PositionAction>> = listOf(
    TextLayerMove.Forward to PositionAction("Forward", Res.drawable.ic_single_layer_arrow, 0f),
    TextLayerMove.ToFront to PositionAction("To front", Res.drawable.ic_double_layer_arrow, 0f),
    TextLayerMove.Backward to PositionAction("Backward", Res.drawable.ic_single_layer_arrow, 180f),
    TextLayerMove.ToBack to PositionAction("To back", Res.drawable.ic_double_layer_arrow, 180f),
)

private val AlignActions: List<Pair<TextPhotoAlign, PositionAction>> = listOf(
    TextPhotoAlign.Top to PositionAction("Top", Res.drawable.ic_align_tblr, 180f),
    TextPhotoAlign.Left to PositionAction("Left", Res.drawable.ic_align_tblr, 90f),
    TextPhotoAlign.Middle to PositionAction("Middle", Res.drawable.ic_align_center, 90f),
    TextPhotoAlign.Center to PositionAction("Center", Res.drawable.ic_align_center, 0f),
    TextPhotoAlign.Bottom to PositionAction("Bottom", Res.drawable.ic_align_tblr, 0f),
    TextPhotoAlign.Right to PositionAction("Right", Res.drawable.ic_align_tblr, -90f),
)

/**
 * The Position panel: ✕ / Position / ✓ over two columns of buttons and an Arrange / Align bar.
 * Arrange moves the active text up and down the stack of texts; Align puts it against an edge of the
 * photo or on its middle. The ✕ puts back where it was and the order of the texts.
 */
@Composable
internal fun TextPositionPanel(
    onArrange: (TextLayerMove) -> Unit,
    onAlign: (TextPhotoAlign) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(PositionTab.Arrange) }
    TextPanelColumn(title = tr("Position"), onClose = onClose, onDone = onDone, modifier = modifier) {
        // One height for both tabs (Align's three rows), so the bar under it never moves.
        Box(
            modifier = Modifier.fillMaxWidth().height(PositionContentHeight).padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (tab) {
                PositionTab.Arrange -> PositionGrid(ArrangeActions, buttonHeight = 48.dp, onClick = onArrange)
                PositionTab.Align -> PositionGrid(AlignActions, buttonHeight = 38.dp, onClick = onAlign)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            for (entry in PositionTab.entries) {
                PanelTabItem(
                    icon = entry.icon,
                    label = entry.label,
                    selected = entry == tab,
                    onClick = { tab = entry },
                    modifier = Modifier.width(76.dp),
                )
            }
        }
    }
}

private val PositionContentHeight = 140.dp

/** [actions] two to a row, read across. */
@Composable
private fun <T> PositionGrid(actions: List<Pair<T, PositionAction>>, buttonHeight: Dp, onClick: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in actions.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                for ((value, action) in row) {
                    PositionButton(action, onClick = { onClick(value) }, modifier = Modifier.weight(1f).height(buttonHeight))
                }
            }
        }
    }
}

@Composable
private fun PositionButton(action: PositionAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val bounce = rememberSpringBounce()
    Row(
        modifier = modifier
            .springBounce(bounce)
            .clip(shape)
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, scheme.onSurface.copy(alpha = 0.45f), shape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(start = 24.dp, end = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            tint = scheme.onSurface,
            modifier = Modifier.size(24.dp).rotate(action.iconDegrees),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = tr(action.englishLabel),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
