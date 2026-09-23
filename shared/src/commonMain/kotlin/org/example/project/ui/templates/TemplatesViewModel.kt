package org.example.project.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Screen state for the Templates browser.
 *
 * @param categories the category pills (the LAS `templates` StateFlow → rvCategory).
 * @param frames the selected category's templates (the LAS `frames` StateFlow → rvFrames).
 */
data class TemplatesUiState(
    val categories: List<TemplateCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val frames: List<TemplateFrame> = emptyList(),
    val isLoadingCategories: Boolean = false,
    val isLoadingFrames: Boolean = false,
    val error: String? = null,
)

/**
 * Loads the template catalog exactly like the LAS `TemplatesViewModel`: fetch the categories, then
 * auto-load the first category's templates; selecting another category reloads its templates.
 */
class TemplatesViewModel(private val repository: TemplatesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, error = null) }
            runCatching { repository.getCategories() }
                .onSuccess { categories ->
                    _uiState.update { it.copy(categories = categories, isLoadingCategories = false) }
                    categories.firstOrNull()?.let { selectCategory(it) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingCategories = false, error = e.message ?: "Couldn't load templates") }
                }
        }
    }

    fun selectCategory(category: TemplateCategory) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.frames.isNotEmpty()) return
        _uiState.update { it.copy(selectedCategoryId = category.id, isLoadingFrames = true, frames = emptyList(), error = null) }
        viewModelScope.launch {
            runCatching { repository.getTemplates(category.id) }
                .onSuccess { frames ->
                    // Ignore a stale response if the user switched categories meanwhile.
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(frames = frames, isLoadingFrames = false) }
                    }
                }
                .onFailure { e ->
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(isLoadingFrames = false, error = e.message ?: "Couldn't load these templates") }
                    }
                }
        }
    }

    fun retry() = loadCategories()
}
