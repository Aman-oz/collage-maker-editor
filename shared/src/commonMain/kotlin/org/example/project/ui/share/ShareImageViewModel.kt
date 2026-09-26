package org.example.project.ui.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shows and shares the file the save screen exported. It reads [imagePath] rather than the edit
 * session, because the exported file may carry the watermark the session image does not.
 */
class ShareImageViewModel(private val imagePath: String) : ViewModel() {

    private val _image = MutableStateFlow<ImageBitmap?>(null)
    val image: StateFlow<ImageBitmap?> = _image.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching { PlatformFile(imagePath).toImageBitmap() }
                .onSuccess { _image.value = it }
                .onFailure { _errors.tryEmit("This image is no longer available") }
        }
    }

    fun share(target: ShareTarget) {
        viewModelScope.launch {
            runCatching { shareImage(imagePath, target) }
                .onFailure { _errors.tryEmit(it.message ?: "Could not share the image") }
        }
    }
}
