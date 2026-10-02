package org.example.project.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr

/**
 * The ✕ / title / ✓ row of the theme-aware tools — the head of a [ToolScaffold] panel, and the top
 * bar of the full-screen crop: a glass ✕ on the left, a centered
 * title, and a primary glass ✓ on the right (both [GlassTopBarButton]). Unlike the dark
 * [EditorCircleIconButton] bars, it follows [MaterialTheme], so it works in light and dark mode.
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
        modifier = modifier.topBar(),
        contentAlignment = Alignment.Center,
    ) {
        GlassTopBarButton(
            icon = Icons.Filled.Close,
            contentDescription = tr("Close"),
            onClick = onClose,
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
            modifier = Modifier.padding(horizontal = TopBarButtonSize + 16.dp),
        )
        GlassTopBarButton(
            icon = Icons.Filled.Check,
            contentDescription = tr("Done"),
            onClick = onDone,
            enabled = doneEnabled,
            style = GlassButtonStyle.Primary,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
