package org.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Brand = Color(0xFF6C4DF6)
private val BrandDark = Color(0xFF4B32C3)
private val Accent = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF1B0066),
    secondary = Accent,
    onSecondary = Color.White,
    background = Color(0xFFFBF9FF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1B1F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9BCFF),
    onPrimary = Color(0xFF2A1173),
    primaryContainer = BrandDark,
    onPrimaryContainer = Color(0xFFE7E0FF),
    secondary = Accent,
    onSecondary = Color(0xFF3B0A0A),
    background = Color(0xFF121216),
    onBackground = Color(0xFFE5E1E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE5E1E6),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
