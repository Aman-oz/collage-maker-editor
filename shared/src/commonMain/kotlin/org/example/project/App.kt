package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.data.AppSettings
import org.example.project.data.ThemeMode
import org.example.project.navigation.AppNavDisplay
import org.example.project.ui.theme.AppTheme
import org.example.project.ui.theme.SystemBarsAppearance
import org.koin.compose.koinInject

@Composable
fun App() {
    val settings = koinInject<AppSettings>()
    val themeMode by settings.themeMode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemBarsAppearance(themeMode, darkTheme)

    AppTheme(darkTheme = darkTheme) {
        AppNavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}
