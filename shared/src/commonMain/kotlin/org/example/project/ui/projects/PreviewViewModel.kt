package org.example.project.ui.projects

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.ProjectsRepository
import org.example.project.ui.share.ShareTarget
import org.example.project.ui.share.shareImage

/** Full-screen view of one saved project, with share and delete. */
class PreviewViewModel(
    private val imagePath: String,
    private val repository: ProjectsRepository,
) : ViewModel() {

    /**
     * Starts as the grid's cached thumbnail so the shared-element transition has an image on its
     * first frame, then swaps to the full-resolution decode once it is ready.
     */
    private val _image = MutableStateFlow(repository.thumbnails.value[imagePath])
    val image: StateFlow<ImageBitmap?> = _image.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _deleted = MutableStateFlow(false)

    /** Flips to `true` once the project is gone, so the screen can leave. */
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { PlatformFile(imagePath).toImageBitmap() }
                .onSuccess { _image.value = it }
                .onFailure { if (_image.value == null) _loadFailed.value = true }
        }
    }

    fun share() {
        viewModelScope.launch {
            runCatching {
                // Android's FileProvider only exposes the cache directory, so share a cache copy.
                val source = PlatformFile(imagePath)
                val shareCopy = FileKit.cacheDir / source.name
                source copyTo shareCopy
                shareImage(shareCopy.path, ShareTarget.More)
            }.onFailure { _errors.tryEmit(it.message ?: "Could not share the image") }
        }
    }

    fun delete() {
        viewModelScope.launch {
            runCatching { repository.delete(imagePath) }
                .onSuccess { _deleted.value = true }
                .onFailure { _errors.tryEmit(it.message ?: "Could not delete the project") }
        }
    }
}
