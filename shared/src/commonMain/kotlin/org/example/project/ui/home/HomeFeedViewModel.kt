package org.example.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.ui.layouts.CollageLayout
import org.example.project.ui.layouts.LayoutsRepository

/** The Home rows; [isLoading] while they are first fetched, [isLoadingLayouts] the Layouts row. */
data class HomeFeedUiState(
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
    val layouts: List<CollageLayout> = emptyList(),
    val isLoadingLayouts: Boolean = false,
)

/**
 * Loads the Layouts row and the template and frame rows under Home's tools card. They are extras,
 * so a failure (no API key, offline) just leaves them out rather than showing an error; [refresh]
 * tries again. The two load separately, so one failing doesn't hide the other.
 */
class HomeFeedViewModel(
    private val repository: HomeFeedRepository,
    private val layoutsRepository: LayoutsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Fetches whichever rows are missing, unless they are loading or already shown. */
    fun refresh() {
        refreshLayouts()
        val state = _uiState.value
        if (state.isLoading || state.sections.isNotEmpty() || !repository.isConfigured) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val sections = runCatching { repository.getSections() }.getOrDefault(emptyList())
            _uiState.update { it.copy(sections = sections, isLoading = false) }
        }
    }

    private fun refreshLayouts() {
        val state = _uiState.value
        if (state.isLoadingLayouts || state.layouts.isNotEmpty() || !layoutsRepository.isConfigured) return
        _uiState.update { it.copy(isLoadingLayouts = true) }
        viewModelScope.launch {
            val layouts = runCatching { layoutsRepository.getAll() }.getOrDefault(emptyList())
            _uiState.update { it.copy(layouts = layouts, isLoadingLayouts = false) }
        }
    }
}
