package org.example.project.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AppTheme

private val PreviewScreenWidth = 393.dp
private val PreviewScreenHeight = 852.dp

/**
 * Renders [screen] once under the light [AppTheme] and once under dark, side by side, so a
 * single `@Preview` shows both variants at once instead of needing one annotation per theme.
 */
@Composable
fun ThemePreviews(screen: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(modifier = Modifier.width(PreviewScreenWidth).height(PreviewScreenHeight)) {
            AppTheme(darkTheme = false, content = screen)
        }
        Box(modifier = Modifier.width(PreviewScreenWidth).height(PreviewScreenHeight)) {
            AppTheme(darkTheme = true, content = screen)
        }
    }
}
