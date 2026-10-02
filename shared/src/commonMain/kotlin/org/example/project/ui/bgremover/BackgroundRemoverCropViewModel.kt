package org.example.project.ui.bgremover

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
import org.example.project.i18n.tr

sealed interface BackgroundRemoverCropUiState {
    data object Loading : BackgroundRemoverCropUiState
    data class Ready(val image: ImageBitmap) : BackgroundRemoverCropUiState
    data class Error(val message: String) : BackgroundRemoverCropUiState
}

/**
 * First step of the background remover: decodes the picked photo for cropping. Unlike the editor's
 * Crop tool the photo is not in [ImageEditSession] yet — only the cropped result goes there, which
 * [org.example.project.ui.bgremover.BackgroundRemoverEditorViewModel] then picks up.
 */
class BackgroundRemoverCropViewModel(
    private val imagePath: String,
    private val session: ImageEditSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackgroundRemoverCropUiState>(BackgroundRemoverCropUiState.Loading)
    val uiState: StateFlow<BackgroundRemoverCropUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = runCatching { PlatformFile(imagePath).toImageBitmap() }.fold(
                onSuccess = { BackgroundRemoverCropUiState.Ready(it) },
                onFailure = { BackgroundRemoverCropUiState.Error(it.message ?: tr("Could not open this image")) },
            )
        }
    }

    fun applyCrop(bitmap: ImageBitmap) {
        session.set(bitmap)
    }
}
