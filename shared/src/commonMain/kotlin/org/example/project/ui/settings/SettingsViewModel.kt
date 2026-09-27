package org.example.project.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.example.project.data.AppSettings
import org.example.project.data.ThemeMode

class SettingsViewModel(private val settings: AppSettings) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settings.themeMode

    fun setThemeMode(mode: ThemeMode) = settings.setThemeMode(mode)
}
