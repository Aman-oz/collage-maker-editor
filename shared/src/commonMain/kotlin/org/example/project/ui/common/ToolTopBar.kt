package org.example.project.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top bar of the theme-aware tool screens (Auto, Crop): ✕ on the left, a centered title, and a
 * filled accent ✓ on the right. Unlike the dark [EditorCircleIconButton] bars, it follows
 * [MaterialTheme], so it works in both light and dark mode.
 */
@Composable
internal fun ToolTopBar(
    title: String,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    doneEnabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    // A Box rather than a Row keeps the title centered on the screen regardless of the button
    // widths; its side padding reserves room for the buttons so a long title ellipsizes instead
    // of running under them.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        TopBarCircleButton(
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            onClick = onClose,
            background = scheme.onSurface.copy(alpha = 0.06f),
            tint = scheme.onSurface,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            color = scheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 52.dp),
        )
        TopBarCircleButton(
            icon = Icons.Filled.Check,
            contentDescription = "Done",
            onClick = onDone,
            enabled = doneEnabled,
            background = if (doneEnabled) scheme.primary else scheme.primary.copy(alpha = 0.35f),
            tint = scheme.onPrimary,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun TopBarCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    background: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
