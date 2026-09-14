package org.example.project.ui.collage

import androidx.compose.ui.graphics.Color
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
import org.example.project.data.ImageEditSession

sealed interface CollageEditorUiState {
    data object Loading : CollageEditorUiState
    data class Ready(val collage: CollageState) : CollageEditorUiState
    data class Error(val message: String) : CollageEditorUiState
}

/**
 * Decodes every initially-picked image in parallel, seeds a [CollageState] with the first template
 * that fits how many images came in, and exposes every edit the editor screen offers — changing the
 * template, swapping or replacing a slot's image, and adjusting border/corner styling.
 *
 * [imagePaths] is passed in from the navigation key via Koin's `parametersOf`.
 */
class CollageEditorViewModel(
    private val imagePaths: List<String>,
    private val session: ImageEditSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollageEditorUiState>(CollageEditorUiState.Loading)
    val uiState: StateFlow<CollageEditorUiState> = _uiState.asStateFlow()

    init {
        loadInitialImages()
    }

    private fun loadInitialImages() {
        viewModelScope.launch {
            runCatching {
                imagePaths.map { path -> async { PlatformFile(path).toImageBitmap() } }.awaitAll()
            }
                .onSuccess { images ->
                    val imageCount = images.size.coerceIn(CollageImageCounts.first(), CollageImageCounts.last())
                    val template = CollageTemplates.getTemplatesByImageCount(imageCount).first()
                    val slotImages = images.take(template.imageCount)
                        .mapIndexed { index, image -> CollageSlotImage(index, image) }
                    _uiState.value = CollageEditorUiState.Ready(CollageState(template = template, images = slotImages))
                }
                .onFailure { _uiState.value = CollageEditorUiState.Error(it.message ?: "Could not open these images") }
        }
    }

    fun changeTemplate(template: CollageTemplate) {
        updateReady { state ->
            state.copy(
                template = template,
                images = state.images.filter { it.slotIndex < template.imageCount },
            )
        }
    }

    fun swapImages(fromIndex: Int, toIndex: Int) {
        updateReady { state ->
            val fromImage = state.images.find { it.slotIndex == fromIndex }
            val toImage = state.images.find { it.slotIndex == toIndex }
            val untouched = state.images.filterNot { it.slotIndex == fromIndex || it.slotIndex == toIndex }
            val swapped = buildList {
                toImage?.let { add(it.copy(slotIndex = fromIndex)) }
                fromImage?.let { add(it.copy(slotIndex = toIndex)) }
            }
            state.copy(images = untouched + swapped)
        }
    }

    fun setSlotImage(slotIndex: Int, path: String) {
        viewModelScope.launch {
            runCatching { PlatformFile(path).toImageBitmap() }
                .onSuccess { image ->
                    updateReady { state ->
                        val others = state.images.filterNot { it.slotIndex == slotIndex }
                        state.copy(images = others + CollageSlotImage(slotIndex, image))
                    }
                }
        }
    }

    fun updateBorderWidth(width: Float) = updateReady { it.copy(borderWidth = width) }

    fun updateBorderColor(color: Color) = updateReady { it.copy(borderColor = color) }

    fun updateCornerRadius(radius: Float) = updateReady { it.copy(cornerRadius = radius) }

    /** Bakes the current collage into one bitmap and hands it to the shared editing session. */
    fun applyCollage(previewSizePx: Float) {
        val state = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return
        session.set(bakeCollage(state, previewSizePx))
    }

    private inline fun updateReady(transform: (CollageState) -> CollageState) {
        val current = _uiState.value as? CollageEditorUiState.Ready ?: return
        _uiState.value = CollageEditorUiState.Ready(transform(current.collage))
    }
}
