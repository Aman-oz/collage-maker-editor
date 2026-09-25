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
 * [imagePath] is passed in from the navigation key via Koin's `parametersOf`. When it is `null` the
 * image was already put in the session by another editor (e.g. a baked collage), so nothing is
 * decoded here.
 */
class EditorViewModel(
    private val imagePath: String?,
    private val session: ImageEditSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        loadImage()
        observeSession()
    }

    private fun loadImage() {
        if (imagePath == null) {
            // The session is in-memory only, so after process death there is nothing to restore.
            if (session.image.value == null) _uiState.value = EditorUiState.Error("This image is no longer available")
            return
        }
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
