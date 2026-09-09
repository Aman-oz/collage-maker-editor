package org.example.project.ui.collage

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CollageEditorUiState {
    data object Loading : CollageEditorUiState
    data class Ready(val images: List<ImageBitmap>) : CollageEditorUiState
    data class Error(val message: String) : CollageEditorUiState
}

/**
 * Decodes every picked image in parallel so the collage canvas has all of them ready at once.
 *
 * [imagePaths] is passed in from the navigation key via Koin's `parametersOf`.
 */
class CollageEditorViewModel(private val imagePaths: List<String>) : ViewModel() {

    private val _uiState = MutableStateFlow<CollageEditorUiState>(CollageEditorUiState.Loading)
    val uiState: StateFlow<CollageEditorUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch {
            runCatching {
                imagePaths.map { path -> async { PlatformFile(path).toImageBitmap() } }.awaitAll()
            }
                .onSuccess { _uiState.value = CollageEditorUiState.Ready(it) }
                .onFailure { _uiState.value = CollageEditorUiState.Error(it.message ?: "Could not open these images") }
        }
    }
}
