package org.example.project.ui.theme

import androidx.compose.runtime.Composable
import org.example.project.data.ThemeMode

/**
 * Keeps the status/navigation bar icons legible when the in-app [mode] differs from the device's
 * dark mode setting. Without it, picking Light while the device is dark leaves white status bar
 * icons on a white background (and vice versa).
 */
@Composable
internal expect fun SystemBarsAppearance(mode: ThemeMode, darkTheme: Boolean)
