package org.example.project.ui.text

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_bold
import photocollagemaker.shared.generated.resources.ic_capitalize
import photocollagemaker.shared.generated.resources.ic_italic
import photocollagemaker.shared.generated.resources.ic_small
import photocollagemaker.shared.generated.resources.ic_start_capital
import photocollagemaker.shared.generated.resources.ic_strike_through
import photocollagemaker.shared.generated.resources.ic_t_none
import photocollagemaker.shared.generated.resources.ic_underline

/**
 * How a text's letters are cased when shown ([applyTextCase]); the Format panel's Capitalized row,
 * in its order. [tinted] is false for None, whose icon is drawn in its own red while not selected.
 */
@Serializable
internal enum class TextCase(private val englishLabel: String, val icon: DrawableResource, val tinted: Boolean = true) {
    None("None", Res.drawable.ic_t_none, tinted = false),
    Upper("Uppercase", Res.drawable.ic_capitalize),
    Title("Capitalize words", Res.drawable.ic_start_capital),
    Lower("Lowercase", Res.drawable.ic_small),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/** The Format panel's top row of toggles, in order. */
private enum class TextFormatToggle(
    private val englishLabel: String,
    val icon: DrawableResource,
    val isOn: (TextItem) -> Boolean,
    val toggle: (TextItem) -> TextItem,
) {
    Bold("Bold", Res.drawable.ic_bold, { it.bold }, { it.copy(bold = !it.bold) }),
    Italic("Italic", Res.drawable.ic_italic, { it.italic }, { it.copy(italic = !it.italic) }),
    Underline("Underline", Res.drawable.ic_underline, { it.underline }, { it.copy(underline = !it.underline) }),
    Strikethrough("Strikethrough", Res.drawable.ic_strike_through, { it.strikethrough }, { it.copy(strikethrough = !it.strikethrough) }),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Format panel: ✕ / Format / ✓ over bold, italic, underline and strikethrough toggles, the
 * alignments and the letter cases. It edits [item] live; the ✕ puts back the text as it was when the
 * panel opened.
 */
@Composable
internal fun TextFormatPanel(
    item: TextItem?,
    onChange: ((TextItem) -> TextItem) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextPanelColumn(title = tr("Format"), onClose = onClose, onDone = onDone, modifier = modifier, placement = TextPanelPlacement.Center) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            for (toggle in TextFormatToggle.entries) {
                FormatButton(
                    painter = painterResource(toggle.icon),
                    contentDescription = toggle.label,
                    selected = item != null && toggle.isOn(item),
                    onClick = { onChange(toggle.toggle) },
                    iconModifier = Modifier.size(34.dp),
                )
            }
        }
        FormatSectionLabel(tr("Alignment"))
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (option in TextAlignOption.entries) {
                FormatButton(
                    painter = rememberVectorPainter(option.icon),
                    contentDescription = option.label,
                    selected = item?.alignment == option,
                    onClick = { onChange { it.copy(alignment = option) } },
                    iconModifier = Modifier.size(28.dp),
                )
            }
        }
        FormatSectionLabel(tr("Capitalized"))
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (case in TextCase.entries) {
                FormatButton(
                    painter = painterResource(case.icon),
                    contentDescription = case.label,
                    selected = (item?.case ?: TextCase.None) == case,
                    tinted = case.tinted,
                    onClick = { onChange { it.copy(case = case) } },
                    // The icons are drawn at their own widths (TT is wider than tt); one height lines them up.
                    iconModifier = Modifier.height(20.dp),
                    // Each in a box as wide as the widest, so the row is evenly spaced.
                    boxModifier = Modifier.size(width = 44.dp, height = 36.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FormatSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/**
 * One of the Format panel's buttons: its icon in the primary colour on a soft wash while [selected],
 * else in the text colour. An untinted icon (the red no-case T) keeps its own colours until it is
 * selected, so every selection reads the same.
 */
@Composable
private fun FormatButton(
    painter: Painter,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconModifier: Modifier,
    tinted: Boolean = true,
    boxModifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .then(boxModifier)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = when {
                selected -> scheme.primary
                !tinted -> Color.Unspecified
                else -> scheme.onSurface
            },
            modifier = iconModifier,
        )
    }
}
