package org.example.project.ui.layouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.templates.TemplateCategory

/** The Layouts browser: its photo-count tabs and the selected tab's layouts. */
data class LayoutsUiState(
    val categories: List<TemplateCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val layouts: List<CollageLayout> = emptyList(),
    val isLoadingCategories: Boolean = false,
    val isLoadingLayouts: Boolean = false,
    val error: String? = null,
)

/** Loads the tabs, then the first tab's layouts; selecting another tab loads its own. */
class LayoutsViewModel(private val repository: LayoutsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LayoutsUiState())
    val uiState: StateFlow<LayoutsUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, error = null) }
            runCatching { repository.getCategories() }
                .onSuccess { categories ->
                    _uiState.update { it.copy(categories = categories, isLoadingCategories = false) }
                    val selectedId = _uiState.value.selectedCategoryId
                    (categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull())?.let { selectCategory(it) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingCategories = false, error = e.message ?: tr("Couldn't load collage layouts")) }
                }
        }
    }

    fun selectCategory(category: TemplateCategory) {
        val state = _uiState.value
        if (state.selectedCategoryId == category.id && (state.layouts.isNotEmpty() || state.isLoadingLayouts)) return
        _uiState.update { it.copy(selectedCategoryId = category.id, isLoadingLayouts = true, layouts = emptyList(), error = null) }
        viewModelScope.launch {
            runCatching { repository.getLayouts(category.id) }
                .onSuccess { layouts ->
                    // Ignore a stale response if the user switched tabs meanwhile.
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(layouts = layouts, isLoadingLayouts = false) }
                    }
                }
                .onFailure { e ->
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(isLoadingLayouts = false, error = e.message ?: tr("Couldn't load collage layouts")) }
                    }
                }
        }
    }
}
