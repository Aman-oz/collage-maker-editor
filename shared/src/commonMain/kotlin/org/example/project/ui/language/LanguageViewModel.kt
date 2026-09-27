package org.example.project.ui.language

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.AppSettings

/**
 * Holds the language selection. Preselects the language saved last time, or on first run the
 * device language (English if unsupported).
 */
class LanguageViewModel(private val settings: AppSettings) : ViewModel() {

    val languages: List<AppLanguage> = SupportedLanguages

    private val _selectedCode = MutableStateFlow(
        settings.languageCode?.takeIf { code -> languages.any { it.code == code } }
            ?: resolveDefaultLanguageCode(deviceLanguageTag()),
    )
    val selectedCode: StateFlow<String> = _selectedCode.asStateFlow()

    fun select(code: String) {
        _selectedCode.value = code
    }

    /** Persists the current selection; called on Done. */
    fun save() {
        settings.languageCode = _selectedCode.value
    }
}
