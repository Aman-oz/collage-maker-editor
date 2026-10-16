package org.example.project.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.LiquidSegmentedTabs
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.wholeNumberLabel
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

/** Which colour of a text the Color panel edits. */
internal enum class TextColorTab(private val englishLabel: String) {
    Text("Text"),
    Background("Background"),
    Stroke("Stroke"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * The Color panel's swatches, laid out top-then-bottom down its two rows: after the colour wheel
 * (which takes the first column's top) the rows read white, cyan, teal, pink… and black, red,
 * yellow, purple…
 */
internal val TextPanelColors: List<Color> = listOf(
    Color.Black,
    Color.White, Color(0xFFEF4A5E),
    Color(0xFF22D3E6), Color(0xFFFFD966),
    Color(0xFF10C9A8), Color(0xFF7C4DFF),
    Color(0xFFFF6AD5), Color(0xFF1F2430),
    Color(0xFFF2A93B), Color(0xFF3A8FD6),
    Color(0xFF9E9E9E), Color(0xFF151A4A),
    Color(0xFF2E7D32), Color(0xFF8D6E63),
    Color(0xFFFF8A80), Color(0xFFB39DDB),
    Color(0xFFA5D6A7), Color(0xFFFFF59D),
)

private val TextPanelColorArgbs: Set<Int> = TextPanelColors.map { it.toArgb() }.toSet()

/**
 * The Color panel: ✕ / Color / ✓, a Text / Background / Stroke switch, the selected colour's opacity
 * (and, for the background, its corner) and two scrolling rows of swatches, the colour wheel first.
 * It edits [item] live; the ✕ puts back the text as it was when the panel opened.
 */
@Composable
internal fun TextColorPanel(
    item: TextItem?,
    tab: TextColorTab,
    onTabSelected: (TextColorTab) -> Unit,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onPickCustom: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedArgb = when (tab) {
        TextColorTab.Text -> item?.colorArgb
        TextColorTab.Background -> item?.backgroundArgb
        TextColorTab.Stroke -> item?.strokeArgb
    }
    val opacity = when (tab) {
        TextColorTab.Text -> item?.textOpacity
        TextColorTab.Background -> item?.backgroundOpacity
        TextColorTab.Stroke -> item?.strokeOpacity
    } ?: 1f
    val setOpacity = { value: Float ->
        val fraction = value / 100f
        onChange {
            when (tab) {
                TextColorTab.Text -> it.copy(textOpacity = fraction)
                TextColorTab.Background -> it.copy(backgroundOpacity = fraction)
                TextColorTab.Stroke -> it.copy(strokeOpacity = fraction)
            }
        }
    }
    val setColor = { color: Color ->
        val argb = color.toArgb()
        onChange {
            when (tab) {
                TextColorTab.Text -> it.copy(colorArgb = argb)
                TextColorTab.Background -> it.copy(backgroundArgb = argb)
                TextColorTab.Stroke -> it.copy(strokeArgb = argb)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        ToolTopBar(title = tr("Color"), onClose = onClose, onDone = onDone)
        TextColorTabs(
            selected = tab,
            onSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            LabeledPercentSlider(
                label = tr("Opacity"),
                value = opacity * 100f,
                onValueChange = setOpacity,
                modifier = Modifier.weight(1f),
            )
            if (tab == TextColorTab.Background) {
                LabeledPercentSlider(
                    label = tr("Corner"),
                    value = item?.backgroundCorner ?: DefaultTextPlateCorner,
                    onValueChange = { value -> onChange { it.copy(backgroundCorner = value) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().height(SwatchSize * 2 + 6.dp),
        ) {
            item {
                ColorSwatch(selected = selectedArgb != null && selectedArgb !in TextPanelColorArgbs, onClick = onPickCustom) {
                    ColorPickerFill(it)
                }
            }
            items(TextPanelColors) { color ->
                ColorSwatch(selected = color.toArgb() == selectedArgb, onClick = { setColor(color) }) {
                    Box(modifier = it.background(color))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * The Color panel's three-way switch: the gallery's liquid tabs. The panel is a flat surface with
 * nothing behind it to bend, so the tabs carry their own backdrop, a soft wash in a `liquefiable`
 * layer under them that only their glass samples (as [org.example.project.ui.common.GlassTopBarButton]
 * does); the capsule's rim and the drop's lens refract it.
 */
@Composable
private fun TextColorTabs(
    selected: TextColorTab,
    onSelected: (TextColorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    val liquidState = rememberLiquidState()
    val backdrop = Brush.linearGradient(
        if (isLight) {
            listOf(scheme.primary.copy(alpha = 0.04f), scheme.onSurface.copy(alpha = 0.10f), scheme.primary.copy(alpha = 0.08f))
        } else {
            listOf(scheme.primary.copy(alpha = 0.12f), scheme.onSurface.copy(alpha = 0.04f), scheme.primary.copy(alpha = 0.10f))
        },
    )
    Box(modifier = modifier.fillMaxWidth().height(TabsHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .liquefiable(liquidState)
                .background(backdrop),
        )
        LiquidSegmentedTabs(
            labels = TextColorTab.entries.map { it.label },
            selectedIndex = selected.ordinal,
            onSelected = { onSelected(TextColorTab.entries[it]) },
            liquidState = liquidState,
            capsuleTint = if (isLight) Color(0xFFDCDCE0).copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.18f),
            modifier = Modifier.matchParentSize(),
            labelStyle = MaterialTheme.typography.labelLarge,
        )
    }
}

private val TabsHeight = 44.dp

/** A small label over a 0–100 slider with its end values, as the Font size panel's. */
@Composable
private fun LabeledPercentSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            modifier = Modifier.padding(start = 12.dp),
        )
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = 0f..100f,
            referenceValue = 0f,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            valueLabel = ::wholeNumberLabel,
        )
    }
}
