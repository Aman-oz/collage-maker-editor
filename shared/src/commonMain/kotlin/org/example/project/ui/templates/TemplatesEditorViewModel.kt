package org.example.project.ui.templates

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
import org.example.project.data.ImageEditSession
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.common.copyBitmap

sealed interface TemplatesEditorUiState {
    data object Loading : TemplatesEditorUiState
    data class Ready(
        val frameImage: ImageBitmap,
        val images: Map<Int, ImageBitmap> = emptyMap(),
        /** Pinch/pan per slot; a slot with no entry shows its photo at the plain center-cropped fit. */
        val transforms: Map<Int, SlotTransform> = emptyMap(),
    ) : TemplatesEditorUiState
    data class Error(val message: String) : TemplatesEditorUiState
}

/**
 * Drives the Templates editor (the KMP equivalent of `NewFrameEditor`): downloads the selected
 * template's decorative frame image, lets the user drop a photo into each slot, and bakes the frame
 * plus the placed photos into one image on Done.
 *
 * [frame] is passed in from the navigation key via Koin's `parametersOf`.
 */
class TemplatesEditorViewModel(
    private val frame: TemplateFrame,
    private val session: ImageEditSession,
    private val imageLoader: NetworkImageLoader,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TemplatesEditorUiState>(TemplatesEditorUiState.Loading)
    val uiState: StateFlow<TemplatesEditorUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val slotCount: Int get() = frame.slotCount

    init {
        loadFrame()
    }

    private fun loadFrame() {
        viewModelScope.launch {
            val bitmap = imageLoader.load(frame.imageUrl)
            _uiState.value = if (bitmap != null) {
                // Copy so the frame draws reliably through Canvas (platform-decoded bitmaps don't).
                TemplatesEditorUiState.Ready(frameImage = copyBitmap(bitmap))
            } else {
                TemplatesEditorUiState.Error("Couldn't load this template")
            }
        }
    }

    /** Index of the first slot with no photo yet, for the "Add Image" button. */
    fun firstEmptySlotIndex(): Int? {
        val ready = _uiState.value as? TemplatesEditorUiState.Ready ?: return null
        return (0 until frame.slotCount).firstOrNull { it !in ready.images.keys }
    }

    fun setSlotImage(slotIndex: Int, path: String) {
        viewModelScope.launch {
            runCatching { copyBitmap(PlatformFile(path).toImageBitmap()) }
                .onSuccess { image ->
                    val ready = _uiState.value as? TemplatesEditorUiState.Ready ?: return@onSuccess
                    // A replaced photo starts from the fit again; the old zoom was framed for another picture.
                    _uiState.value = ready.copy(
                        images = ready.images + (slotIndex to image),
                        transforms = ready.transforms - slotIndex,
                    )
                }
                .onFailure { _messages.tryEmit("Couldn't open that photo") }
        }
    }

    /** Applies one pinch/pan step to the photo in [slotIndex], measured in a [slotWidth]x[slotHeight] px slot. */
    fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) {
        val ready = _uiState.value as? TemplatesEditorUiState.Ready ?: return
        if (slotIndex !in ready.images) return
        val current = ready.transforms[slotIndex] ?: SlotTransform()
        val next = current.applyGesture(panX, panY, zoom, slotWidth, slotHeight)
        _uiState.value = ready.copy(transforms = ready.transforms + (slotIndex to next))
    }

    /** Bakes the frame + placed photos into the shared editing session. Returns false if not ready. */
    fun applyTemplate(): Boolean {
        val ready = _uiState.value as? TemplatesEditorUiState.Ready ?: return false
        if (ready.images.isEmpty()) {
            _messages.tryEmit("Add at least one photo first")
            return false
        }
        session.set(bakeTemplate(frame, ready.frameImage, ready.images, ready.transforms))
        return true
    }
}
