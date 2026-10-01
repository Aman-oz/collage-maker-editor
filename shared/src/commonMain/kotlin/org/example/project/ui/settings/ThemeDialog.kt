package org.example.project.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.ThemeMode
import org.example.project.ui.common.DialogShape
import org.example.project.ui.preview.ThemePreviews

private data class ThemeOption(val mode: ThemeMode, val label: String, val icon: ImageVector)

private val ThemeOptions = listOf(
    ThemeOption(ThemeMode.System, "System Theme", Icons.Outlined.BrightnessAuto),
    ThemeOption(ThemeMode.Light, "Light Theme", Icons.Outlined.LightMode),
    ThemeOption(ThemeMode.Dark, "Dark Theme", Icons.Outlined.DarkMode),
)

/**
 * Theme picker panel, shown inside a [GlassDialogHost]. The choice is only applied on Apply, so browsing the options (or dismissing)
 * leaves the current theme untouched.
 */
@Composable
internal fun ThemeDialog(
    current: ThemeMode,
    onApply: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(current) { mutableStateOf(current) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Choose Theme",
            color = colors.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Pick how the app looks to you",
            color = colors.onSurface.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeOptions.forEach { option ->
                ThemeOptionRow(
                    option = option,
                    selected = option.mode == selected,
                    onClick = { selected = option.mode },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onApply(selected) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = SettingsAccent, contentColor = Color.White),
        ) {
            Text(text = "Apply", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onDismiss) {
            Text(text = "Cancel", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ThemeOptionRow(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        if (selected) SettingsAccent else colors.onSurface.copy(alpha = 0.18f),
    )
    val background by animateColorAsState(
        // Translucent so the glass panel behind still reads through the rows.
        if (selected) SettingsAccent.copy(alpha = 0.16f) else colors.surface.copy(alpha = 0.45f),
    )
    val iconTint by animateColorAsState(if (selected) SettingsAccent else colors.onSurface)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, borderColor, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = option.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            text = option.label,
            modifier = Modifier.weight(1f),
            color = colors.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        SettingsRadio(selected = selected)
    }
}

/** Accent ring + dot when selected, plain ring otherwise; matches the language screen's radio. */
@Composable
private fun SettingsRadio(selected: Boolean) {
    val ringColor by animateColorAsState(
        if (selected) SettingsAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
    val dotSize by animateDpAsState(if (selected) 10.dp else 0.dp)

    Box(
        modifier = Modifier.size(20.dp).border(1.5.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(dotSize).background(SettingsAccent, CircleShape))
    }
}

@Preview
@Composable
private fun ThemeDialogPreview() {
    ThemePreviews {
        Box(Modifier.padding(24.dp).background(MaterialTheme.colorScheme.surface, DialogShape)) {
            ThemeDialog(current = ThemeMode.System, onApply = {}, onDismiss = {})
        }
    }
}
