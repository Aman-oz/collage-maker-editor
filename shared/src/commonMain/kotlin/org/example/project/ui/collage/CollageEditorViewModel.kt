package org.example.project.ui.collage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.ImageEditSession
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.copyBitmap

sealed interface CollageEditorUiState {
    data object Loading : CollageEditorUiState
    data class Ready(val collage: CollageState) : CollageEditorUiState
    data class Error(val message: String) : CollageEditorUiState
}

/** The layout picker's state (the Layouts tab): the layouts for the current photo count. */
data class CollagePickerState(
    val templates: List<TemplateItem> = emptyList(),
    val selectedTemplateId: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Drives the collage editor, mirroring the LAS `CollageActivity`: it decodes the picked photos, loads
 * the `collages.json` layout catalog, keeps only the layouts whose slot count matches how many photos
 * were picked, and seeds the first one. Selecting a free layout re-flows the photos into its slots;
 * premium layouts raise a one-shot [messages] toast and are not applied. The Border tab's width
 * (`space`) and corner radius (`corner`) feed straight into the polygon renderer.
 */
class CollageEditorViewModel(
    private val imagePaths: List<String>,
    private val session: ImageEditSession,
    private val catalog: CollageCatalog,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CollageEditorUiState>(CollageEditorUiState.Loading)
    val uiState: StateFlow<CollageEditorUiState> = _uiState.asStateFlow()

    private val _pickerState = MutableStateFlow(CollagePickerState())
    val pickerState: StateFlow<CollagePickerState> = _pickerState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // The photos, decoded once and copied so they draw reliably through Canvas (see copyBitmap).
    private var photos: List<ImageBitmap> = emptyList()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val imagesResult = runCatching {
                imagePaths.map { path -> async { copyBitmap(PlatformFile(path).toImageBitmap()) } }.awaitAll()
            }
            val images = imagesResult.getOrElse {
                _uiState.value = CollageEditorUiState.Error(it.message ?: "Could not open these images")
                return@launch
            }
            photos = images

            val all = runCatching { catalog.loadAll() }.getOrElse {
                _pickerState.value = _pickerState.value.copy(isLoading = false, error = it.message ?: "Couldn't load collage layouts")
                emptyList()
            }
            // Like CollageActivity: show only layouts whose slot count matches the photo count,
            // falling back to the nearest available count if there's no exact match.
            val count = images.size
            var templates = all.filter { it.imageCount == count }
            if (templates.isEmpty() && all.isNotEmpty()) {
                val nearest = all.minByOrNull { kotlin.math.abs(it.imageCount - count) }!!.imageCount
                templates = all.filter { it.imageCount == nearest }
            }

            if (templates.isEmpty()) {
                // No layouts at all — still let the user see their first photo full-frame would be
                // ideal, but without geometry we surface an error like the LAS "no templates" toast.
                _uiState.value = CollageEditorUiState.Error("No collage layouts available")
                _pickerState.value = _pickerState.value.copy(isLoading = false)
                return@launch
            }

            val first = templates.first()
            _pickerState.value = CollagePickerState(templates = templates, selectedTemplateId = first.id, isLoading = false)
            _uiState.value = CollageEditorUiState.Ready(CollageState(template = first, images = fillSlots(first)))
        }
    }

    /** Maps the loaded photos onto the first slots of [template], in order. */
    private fun fillSlots(template: TemplateItem): Map<Int, ImageBitmap> {
        val count = minOf(photos.size, template.imageCount)
        return (0 until count).associateWith { photos[it] }
    }

    fun applyTemplate(template: TemplateItem) {
        if (template.isPremium) {
            _messages.tryEmit("“${template.title}” is a premium layout")
            return
        }
        _pickerState.value = _pickerState.value.copy(selectedTemplateId = template.id)
        updateReady { state ->
            // Keep photos already placed; drop any whose slot no longer exists in the new layout.
            val kept = state.images.filterKeys { it < template.imageCount }
            state.copy(template = template, images = kept)
        }
    }

    fun swapImages(fromIndex: Int, toIndex: Int) {
        updateReady { state ->
            val from = state.images[fromIndex]
            val to = state.images[toIndex]
            val images = state.images.toMutableMap()
            if (to != null) images[fromIndex] = to else images.remove(fromIndex)
            if (from != null) images[toIndex] = from else images.remove(toIndex)
            state.copy(images = images)
        }
    }

    fun setSlotImage(slotIndex: Int, path: String) {
        viewModelScope.launch {
            runCatching { copyBitmap(PlatformFile(path).toImageBitmap()) }
                .onSuccess { image ->
                    updateReady { state -> state.copy(images = state.images + (slotIndex to image)) }
                }
                .onFailure { _messages.tryEmit("Couldn't open that photo") }
        }
    }

    fun updateSpace(space: Float) = updateReady { it.copy(space = space) }

    fun updateCorner(corner: Float) = updateReady { it.copy(corner = corner) }

    fun updateBackgroundColor(color: Color) = updateReady { it.copy(backgroundColor = color) }

    fun updateRatio(ratio: CollageRatio) = updateReady { it.copy(ratio = ratio) }

    /** Bakes the current collage into one bitmap and hands it to the shared editing session. */
    fun applyCollage(previewWidthPx: Float, spacePx: Float, cornerPx: Float) {
        val state = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return
        session.set(bakeCollage(state, previewWidthPx, spacePx, cornerPx))
    }

    private inline fun updateReady(transform: (CollageState) -> CollageState) {
        val current = _uiState.value as? CollageEditorUiState.Ready ?: return
        _uiState.value = CollageEditorUiState.Ready(transform(current.collage))
    }
}
