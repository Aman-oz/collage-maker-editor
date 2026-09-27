package org.example.project.ui.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplatesUiState

/**
 * The Frames browser's counterpart of [org.example.project.ui.templates.TemplatesViewModel], with
 * the same flow: fetch the categories, auto-load the first one's frames, and reload on selection.
 * It shares [TemplatesUiState] because both screens render through the same content composable.
 */
class FramesViewModel(
    private val repository: FramesRepository,
    private val settings: AppSettings,
) : ViewModel() {

    /** Subscribers get premium frames; everyone else is sent to the paywall. */
    val isPremium: StateFlow<Boolean> = settings.isPremium

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
                    _uiState.update { it.copy(isLoadingCategories = false, error = e.message ?: "Couldn't load frames") }
                }
        }
    }

    fun selectCategory(category: TemplateCategory) {
        if (_uiState.value.selectedCategoryId == category.id && _uiState.value.frames.isNotEmpty()) return
        _uiState.update { it.copy(selectedCategoryId = category.id, isLoadingFrames = true, frames = emptyList(), error = null) }
        viewModelScope.launch {
            runCatching { repository.getFrames(category.id) }
                .onSuccess { frames ->
                    // Ignore a stale response if the user switched categories meanwhile.
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(frames = frames, isLoadingFrames = false) }
                    }
                }
                .onFailure { e ->
                    if (_uiState.value.selectedCategoryId == category.id) {
                        _uiState.update { it.copy(isLoadingFrames = false, error = e.message ?: "Couldn't load these frames") }
                    }
                }
        }
    }

    fun retry() = loadCategories()
}
