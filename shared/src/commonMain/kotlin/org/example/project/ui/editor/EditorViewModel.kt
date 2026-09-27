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
    data class Ready(
        val image: ImageBitmap,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : EditorUiState
    data class Error(val message: String) : EditorUiState
}

/**
 * Decodes the picked image so the editing tools can work on it, and keeps [uiState] in sync with
 * [ImageEditSession] so edits made in other tool screens (crop, filter, ...) reflect back here.
 *
 * Undo/redo is built on that same observation: every tool writes its result with `session.set`, so
 * each new bitmap the session emits is recorded as one step in an [EditHistory] — no tool needs to
 * know history exists. Undo/redo write the restored bitmap back to the session too, so the save
 * screen and the next tool see it.
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

    /**
     * Whether the session holds *this* editor's image. Until the picked file is decoded, the
     * session may still hold the previous photo; showing it (or letting a tool auto-open on it)
     * would edit the wrong image.
     */
    private var imageLoaded = imagePath == null
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** `null` until this editor's image is in the session; its first bitmap is the base state. */
    private var history: EditHistory<ImageBitmap>? = null

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
                .onSuccess {
                    imageLoaded = true
                    session.set(it)
                }
                .onFailure { _uiState.value = EditorUiState.Error(it.message ?: "Could not open this image") }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            session.image.collect { bitmap ->
                if (bitmap == null || !imageLoaded) return@collect
                val current = history
                history = when {
                    current == null -> EditHistory(bitmap)
                    // Our own undo/redo write echoing back from the session — already recorded.
                    bitmap === current.current -> current
                    else -> current.push(bitmap)
                }
                publish()
            }
        }
    }

    fun undo() = restore { it.undo() }

    fun redo() = restore { it.redo() }

    private inline fun restore(step: (EditHistory<ImageBitmap>) -> EditHistory<ImageBitmap>) {
        val current = history ?: return
        val next = step(current)
        if (next === current) return
        // Update history before writing, so the collector recognizes the echo instead of pushing it.
        history = next
        session.set(next.current)
        publish()
    }

    private fun publish() {
        val current = history ?: return
        _uiState.value = EditorUiState.Ready(current.current, current.canUndo, current.canRedo)
    }
}
