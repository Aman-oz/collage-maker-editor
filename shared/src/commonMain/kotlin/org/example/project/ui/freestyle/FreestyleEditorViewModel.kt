package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextMeasurer
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
import org.example.project.ui.text.TextFontStyleOption

sealed interface FreestyleEditorUiState {
    data object Loading : FreestyleEditorUiState
    data class Ready(val freestyle: FreestyleState) : FreestyleEditorUiState
    data class Error(val message: String) : FreestyleEditorUiState
}

/**
 * Decodes every initially-picked image in parallel, scatters them as freely draggable/rotatable
 * layers on one open canvas, and exposes every edit the freestyle screen offers — adding more
 * images, stickers or text, transforming/reordering/deleting layers, and styling the canvas itself
 * (background color, border width/color, corner rounding).
 *
 * [imagePaths] is passed in from the navigation key via Koin's `parametersOf`.
 */
class FreestyleEditorViewModel(
    private val imagePaths: List<String>,
    private val session: ImageEditSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FreestyleEditorUiState>(FreestyleEditorUiState.Loading)
    val uiState: StateFlow<FreestyleEditorUiState> = _uiState.asStateFlow()

    private var nextLayerId = 0L

    init {
        loadInitialImages()
    }

    private fun loadInitialImages() {
        viewModelScope.launch {
            runCatching {
                imagePaths.map { path -> async { PlatformFile(path).toImageBitmap() } }.awaitAll()
            }
                .onSuccess { images ->
                    val layers = images.mapIndexed { index, image ->
                        scatteredLayer(index, images.size, FreestyleContent.ImageContent(image))
                    }
                    _uiState.value = FreestyleEditorUiState.Ready(FreestyleState(layers = layers))
                }
                .onFailure { _uiState.value = FreestyleEditorUiState.Error(it.message ?: "Could not open these images") }
        }
    }

    /** Spreads initial layers into a loose, gently-rotated cascade rather than stacking them exactly. */
    private fun scatteredLayer(index: Int, total: Int, content: FreestyleContent): FreestyleLayer {
        val step = index - (total - 1) / 2f
        val offsetFraction = Offset(
            (0.5f + step * 0.05f).coerceIn(0.2f, 0.8f),
            (0.5f + step * 0.04f).coerceIn(0.2f, 0.8f),
        )
        val rotationDegrees = ScatterRotations[index % ScatterRotations.size]
        return FreestyleLayer(id = nextId(), content = content, offsetFraction = offsetFraction, rotationDegrees = rotationDegrees)
    }

    fun addImage(path: String) {
        viewModelScope.launch {
            runCatching { PlatformFile(path).toImageBitmap() }
                .onSuccess { image -> addLayer(FreestyleContent.ImageContent(image)) }
        }
    }

    fun addSticker(emoji: String) = addLayer(FreestyleContent.StickerContent(emoji))

    fun addText(text: String, color: Color, font: TextFontStyleOption) {
        if (text.isBlank()) return
        addLayer(FreestyleContent.TextContent(text, color, font))
    }

    private fun addLayer(content: FreestyleContent) {
        updateReady { state -> state.copy(layers = state.layers + FreestyleLayer(id = nextId(), content = content)) }
    }

    /** Applies one continuous drag/pinch/rotate gesture tick to the layer identified by [id]. */
    fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) {
        updateReady { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    if (layer.id != id) {
                        layer
                    } else {
                        layer.copy(
                            offsetFraction = Offset(
                                (layer.offsetFraction.x + panFraction.x).coerceIn(0f, 1f),
                                (layer.offsetFraction.y + panFraction.y).coerceIn(0f, 1f),
                            ),
                            scale = (layer.scale * zoomDelta).coerceIn(FreestyleLayerScaleRange),
                            rotationDegrees = layer.rotationDegrees + rotationDeltaDegrees,
                        )
                    }
                },
            )
        }
    }

    fun bringToFront(id: Long) {
        updateReady { state ->
            if (state.layers.lastOrNull()?.id == id) return@updateReady state
            val layer = state.layers.find { it.id == id } ?: return@updateReady state
            state.copy(layers = state.layers.filterNot { it.id == id } + layer)
        }
    }

    fun removeLayer(id: Long) {
        updateReady { state -> state.copy(layers = state.layers.filterNot { it.id == id }) }
    }

    fun updateBackgroundColor(color: Color) = updateReady { it.copy(backgroundColor = color) }

    fun updateBorderWidth(width: Float) = updateReady { it.copy(borderWidth = width) }

    fun updateBorderColor(color: Color) = updateReady { it.copy(borderColor = color) }

    fun updateCornerRadius(radius: Float) = updateReady { it.copy(cornerRadius = radius) }

    /** Bakes the current canvas into one bitmap and hands it to the shared editing session. */
    fun applyFreestyle(textMeasurer: TextMeasurer, previewSizePx: Float) {
        val state = (_uiState.value as? FreestyleEditorUiState.Ready)?.freestyle ?: return
        session.set(bakeFreestyle(state, textMeasurer, previewSizePx))
    }

    private fun nextId(): Long = nextLayerId++

    private inline fun updateReady(transform: (FreestyleState) -> FreestyleState) {
        val current = _uiState.value as? FreestyleEditorUiState.Ready ?: return
        _uiState.value = FreestyleEditorUiState.Ready(transform(current.freestyle))
    }

    private companion object {
        val ScatterRotations = listOf(-8f, 7f, -5f, 9f, -10f, 4f)
    }
}
