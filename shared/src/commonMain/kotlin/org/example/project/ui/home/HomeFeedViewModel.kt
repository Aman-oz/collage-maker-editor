package org.example.project.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The Home rows; [isLoading] while they are first fetched. */
data class HomeFeedUiState(
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = false,
)

/**
 * Loads the template and frame rows under Home's tools card. They are extras, so a failure (no API
 * key, offline) just leaves them out rather than showing an error; [refresh] tries again.
 */
class HomeFeedViewModel(private val repository: HomeFeedRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Fetches the rows again, unless they are loading or already shown. */
    fun refresh() {
        val state = _uiState.value
        if (state.isLoading || state.sections.isNotEmpty() || !repository.isConfigured) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val sections = runCatching { repository.getSections() }.getOrDefault(emptyList())
            _uiState.update { it.copy(sections = sections, isLoading = false) }
        }
    }
}
