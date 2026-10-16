package org.example.project.ui.text

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_angle
import photocollagemaker.shared.generated.resources.ic_arrow_in_circle
import photocollagemaker.shared.generated.resources.ic_blur_editor
import photocollagemaker.shared.generated.resources.ic_color
import photocollagemaker.shared.generated.resources.ic_opacity
import photocollagemaker.shared.generated.resources.ic_spacing

/** The Shadow panel's tabs, in the order of its bottom bar. [tinted] is false for the colour wheel. */
private enum class ShadowTab(private val englishLabel: String, val icon: DrawableResource, val tinted: Boolean = true) {
    Opacity("Opacity", Res.drawable.ic_opacity),
    Distance("Distance", Res.drawable.ic_spacing),
    Angle("Angle", Res.drawable.ic_angle),
    Blur("Blur", Res.drawable.ic_blur_editor),
    Color("Color", Res.drawable.ic_color, tinted = false),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Distance tab's arrows: which way each moves the shadow, and how far its icon (an up arrow) is
 * turned to point that way.
 */
private enum class ShadowNudge(private val englishLabel: String, val step: Offset, val iconDegrees: Float) {
    Up("Move up", Offset(0f, -ShadowNudgeEm), 0f),
    Down("Move down", Offset(0f, ShadowNudgeEm), 180f),
    Left("Move left", Offset(-ShadowNudgeEm, 0f), -90f),
    Right("Move right", Offset(ShadowNudgeEm, 0f), 90f),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Shadow panel: ✕ / Shadow / ✓ over the selected tab's controls, and a bottom bar of tabs —
 * Opacity, Distance (arrows that nudge the shadow), Angle (turns it round the words, its distance
 * kept), Blur and Color. It edits [item] live; the ✕ puts back the text as it was when the panel
 * opened.
 */
@Composable
internal fun TextShadowPanel(
    item: TextItem?,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onPickCustom: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(ShadowTab.Opacity) }
    val shadow = item ?: TextItem(id = -1)
    // Beside the photo the tab's controls get all the room between the bar and the tabs: centred, or
    // the colours as a grid that scrolls down. Under it, one height for every tab, so the tabs never move.
    val sideBySide = LocalTextPanelSideBySide.current
    TextPanelColumn(
        title = tr("Shadow"),
        onClose = onClose,
        onDone = onDone,
        modifier = modifier,
        placement = TextPanelPlacement.Fill,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (sideBySide) Modifier.weight(1f) else Modifier.height(ShadowTabContentHeight)),
            contentAlignment = Alignment.Center,
        ) {
            when (tab) {
                ShadowTab.Opacity -> PanelSliderRow(
                    PanelSlider(tr("Opacity"), shadow.shadowOpacity * 100f) { value -> onChange { it.copy(shadowOpacity = value / 100f) } },
                )
                ShadowTab.Distance -> {
                    val arrow = @Composable { nudge: ShadowNudge, size: Dp ->
                        RepeatingIconButton(
                            painter = painterResource(Res.drawable.ic_arrow_in_circle),
                            contentDescription = nudge.label,
                            iconDegrees = nudge.iconDegrees,
                            size = size,
                            onClick = {
                                onChange {
                                    val offset = nudgeShadowOffset(it.shadowDx, it.shadowDy, nudge.step)
                                    it.copy(shadowDx = offset.x, shadowDy = offset.y)
                                }
                            },
                        )
                    }
                    if (sideBySide) {
                        DirectionPad(
                            rows = listOf(
                                listOf(null, ShadowNudge.Up, null),
                                listOf(ShadowNudge.Left, null, ShadowNudge.Right),
                                listOf(null, ShadowNudge.Down, null),
                            ),
                        ) { arrow(it, DirectionPadButtonSize) }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                            for (nudge in ShadowNudge.entries) arrow(nudge, 44.dp)
                        }
                    }
                }
                ShadowTab.Angle -> {
                    val angle = shadowAngleSlider(shadow.shadowDx, shadow.shadowDy)
                    PanelSliderRow(
                        PanelSlider(tr("Angle {0}°", shadowAngleDegrees(angle).roundToInt()), angle) { value ->
                            onChange {
                                val offset = shadowOffsetAtAngle(it.shadowDx, it.shadowDy, value)
                                it.copy(shadowDx = offset.x, shadowDy = offset.y)
                            }
                        },
                    )
                }
                ShadowTab.Blur -> PanelSliderRow(
                    PanelSlider(tr("Blur"), shadow.shadowBlur) { value -> onChange { it.copy(shadowBlur = value) } },
                )
                ShadowTab.Color -> TextSwatchGrid(
                    selectedArgb = shadow.shadowArgb,
                    onSelected = { color -> onChange { it.copy(shadowArgb = color.toArgb()) } },
                    onPickCustom = onPickCustom,
                    vertical = sideBySide,
                    modifier = if (sideBySide) Modifier.fillMaxSize() else Modifier,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (entry in ShadowTab.entries) {
                PanelTabItem(
                    icon = entry.icon,
                    label = entry.label,
                    tinted = entry.tinted,
                    selected = entry == tab,
                    onClick = { tab = entry },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Tall enough for a slider with its label and range, and for two rows of swatches. */
private val ShadowTabContentHeight = 112.dp

/**
 * One tab of a panel's bottom bar (the Shadow and Position panels): its icon over its label, in the
 * primary colour while [selected]. An untinted icon (the colour wheel) keeps its own colours.
 */
@Composable
internal fun PanelTabItem(
    icon: DrawableResource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tinted: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    val tint = if (selected) scheme.primary else scheme.onSurface
    Column(
        modifier = modifier
            .springBounce(bounce)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (tinted) tint else Color.Unspecified,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = tint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
