package org.example.project.ui.editor

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.ImageEditSession

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data class Ready(val image: ImageBitmap) : EditorUiState
    data class Error(val message: String) : EditorUiState
}

/**
 * Decodes the picked image so the editing tools can work on it, and keeps [uiState] in sync with
 * [ImageEditSession] so edits made in other tool screens (crop, filter, ...) reflect back here.
 *
 * [imagePath] is passed in from the navigation key via Koin's `parametersOf`.
 */
class EditorViewModel(
    private val imagePath: String,
    private val session: ImageEditSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadImage()
        observeSession()
    }

    private fun loadImage() {
        viewModelScope.launch {
            runCatching { PlatformFile(imagePath).toImageBitmap() }
                .onSuccess { session.set(it) }
                .onFailure { _uiState.value = EditorUiState.Error(it.message ?: "Could not open this image") }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            session.image.collect { bitmap ->
                if (bitmap != null) _uiState.value = EditorUiState.Ready(bitmap)
            }
        }
    }
}
