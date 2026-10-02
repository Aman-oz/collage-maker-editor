package org.example.project.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.example.project.i18n.tr

/** Which part of a text label a colour row is colouring: the words, or the plate behind them. */
internal enum class TextColorTarget(private val englishLabel: String, val icon: ImageVector) {
    Text("Text", Icons.Filled.FormatColorText),
    Background("Background", Icons.Filled.FormatColorFill),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * The words / background switch that sits beside a text colour row (the photo editor's Text tool and
 * the freestyle Text panel); the selected half is a soft accent tint.
 */
@Composable
internal fun TextColorTargetSwitch(
    selected: TextColorTarget,
    onSelected: (TextColorTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
    ) {
        for (target in TextColorTarget.entries) {
            val isSelected = target == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) scheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelected(target) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = target.icon,
                    contentDescription = target.label,
                    tint = if (isSelected) scheme.primary else scheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
