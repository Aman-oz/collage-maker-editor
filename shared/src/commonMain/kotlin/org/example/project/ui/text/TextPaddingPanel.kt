package org.example.project.ui.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.wholeNumberLabel

/**
 * The Padding panel's sliders, two to a row: each side on its own, then Vertical (top and bottom
 * together) and Horizontal (left and right together). A pair shows the middle of its two sides and
 * sets both to the value it is moved to.
 */
private enum class TextPaddingSlider(
    private val englishLabel: String,
    val value: (TextItem) -> Float,
    val set: (TextItem, Float) -> TextItem,
) {
    Top("Top", { it.paddingTop }, { item, v -> item.copy(paddingTop = v) }),
    Bottom("Bottom", { it.paddingBottom }, { item, v -> item.copy(paddingBottom = v) }),
    Left("Left", { it.paddingLeft }, { item, v -> item.copy(paddingLeft = v) }),
    Right("Right", { it.paddingRight }, { item, v -> item.copy(paddingRight = v) }),
    Vertical(
        "Vertical",
        { (it.paddingTop + it.paddingBottom) / 2f },
        { item, v -> item.copy(paddingTop = v, paddingBottom = v) },
    ),
    Horizontal(
        "Horizontal",
        { (it.paddingLeft + it.paddingRight) / 2f },
        { item, v -> item.copy(paddingLeft = v, paddingRight = v) },
    ),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Padding panel: ✕ / Padding / ✓ over six 0–100 sliders setting how far the active text's
 * background plate reaches past its words on each side ([textPaddingEm]). It edits the text live; the
 * ✕ puts back the padding it had when the panel opened. The plate is what shows the padding, so a
 * text without one gets a line saying so.
 */
@Composable
internal fun TextPaddingPanel(
    item: TextItem?,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shown = item ?: TextItem(id = -1)
    TextPanelColumn(
        title = tr("Padding"),
        onClose = onClose,
        onDone = onDone,
        modifier = modifier,
        placement = TextPanelPlacement.Center,
    ) {
        if (item != null && item.backgroundArgb == null) {
            Text(
                text = tr("Padding shows around a background color"),
                fontSize = 12.sp,
                color = scheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 2.dp),
            )
        }
        for (pair in TextPaddingSlider.entries.chunked(2)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                for (slider in pair) {
                    PaddingSliderCell(
                        label = slider.label,
                        value = slider.value(shown),
                        onValueChange = { value -> onChange { slider.set(it, value) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** A compact 0–100 slider under its label and current value, half a row wide. */
@Composable
private fun PaddingSliderCell(label: String, value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = value.roundToInt().toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.primary,
            )
        }
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = 0f..100f,
            referenceValue = 0f,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 28.dp,
            thumbHeight = 16.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            valueLabel = ::wholeNumberLabel,
            // The value is printed by the label; the ends would crowd a half-width slider.
            showRangeLabels = false,
        )
    }
}
