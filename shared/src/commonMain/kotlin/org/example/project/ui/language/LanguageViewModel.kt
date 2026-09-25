package org.example.project.ui.language

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the language selection, preselected from the device language (English if unsupported). */
class LanguageViewModel : ViewModel() {

    val languages: List<AppLanguage> = SupportedLanguages

    private val _selectedCode = MutableStateFlow(resolveDefaultLanguageCode(deviceLanguageTag()))
    val selectedCode: StateFlow<String> = _selectedCode.asStateFlow()

    fun select(code: String) {
        _selectedCode.value = code
    }
}
