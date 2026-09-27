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
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.editor.MaxEditHistory
import org.example.project.ui.text.TextFontStyleOption

sealed interface FreestyleEditorUiState {
    data object Loading : FreestyleEditorUiState
    data class Ready(
        val freestyle: FreestyleState,
        val canUndo: Boolean = false,
        val canRedo: Boolean = false,
    ) : FreestyleEditorUiState
    data class Error(val message: String) : FreestyleEditorUiState
}

/**
 * Decodes every initially-picked image in parallel, scatters them as freely draggable/rotatable
 * layers on one open canvas, and exposes every edit the freestyle screen offers — adding more
 * images, stickers or text, replacing a photo, transforming/reordering/deleting layers, the canvas
 * background color, and each photo's white frame and corner rounding.
 *
 * Every edit goes through an [EditHistory] of whole [FreestyleState]s for undo/redo. Continuous
 * edits (a drag, a slider, retyping a label) pass a coalesce key and are folded into one step until
 * the screen reports the gesture over via [endGesture]; bringing a layer to front on selection is
 * not recorded at all, so undo only ever reverts something the user deliberately did.
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

    private var history: EditHistory<FreestyleState>? = null

    /** The coalesce key of the continuous edit in progress, or null between gestures. */
    private var gestureKey: String? = null

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
                    history = EditHistory(FreestyleState(layers = layers), maxSize = MaxFreestyleHistory)
                    publish()
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

    /** Rewrites text layer [id] in place, keeping its placement. */
    fun updateText(id: Long, text: String, color: Color, font: TextFontStyleOption) {
        updateReady { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    if (layer.id == id && layer.content is FreestyleContent.TextContent) {
                        layer.copy(content = FreestyleContent.TextContent(text, color, font))
                    } else {
                        layer
                    }
                },
            )
        }
    }

    /** Live-retypes text layer [id] while its text bar is open; the whole session is one undo step. */
    fun retypeText(id: Long, text: String) {
        updateReady(coalesceKey = "text:$id") { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    val content = layer.content
                    if (layer.id == id && content is FreestyleContent.TextContent) {
                        layer.copy(content = content.copy(text = text))
                    } else {
                        layer
                    }
                },
            )
        }
    }

    private fun addLayer(content: FreestyleContent) {
        updateReady { state -> state.copy(layers = state.layers + FreestyleLayer(id = nextId(), content = content)) }
    }

    /** Applies one continuous drag/pinch/rotate gesture tick to the layer identified by [id]. */
    fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) {
        updateReady(coalesceKey = "transform:$id") { state ->
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

    /** Sets [id]'s absolute size and angle, as dragged by its corner resize/rotate handle. */
    fun setLayerScaleRotation(id: Long, scale: Float, rotationDegrees: Float) {
        updateReady(coalesceKey = "transform:$id") { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    if (layer.id == id) {
                        layer.copy(scale = scale.coerceIn(FreestyleLayerScaleRange), rotationDegrees = rotationDegrees)
                    } else {
                        layer
                    }
                },
            )
        }
    }

    fun bringToFront(id: Long) {
        updateReady(record = false) { state ->
            if (state.layers.lastOrNull()?.id == id) return@updateReady state
            val layer = state.layers.find { it.id == id } ?: return@updateReady state
            state.copy(layers = state.layers.filterNot { it.id == id } + layer)
        }
    }

    fun removeLayer(id: Long) {
        updateReady { state -> state.copy(layers = state.layers.filterNot { it.id == id }) }
    }

    fun updateBackgroundColor(color: Color) = updateReady { it.copy(backgroundColor = color) }

    /** Sets the frame width of image layer [id], or of every image layer when [id] is null. */
    fun updateImageBorderWidth(id: Long?, width: Float) =
        updateImageLayers(id, coalesceKey = "border:$id") { it.copy(borderWidth = width) }

    /** Sets the corner rounding of image layer [id], or of every image layer when [id] is null. */
    fun updateImageCornerRadius(id: Long?, radius: Float) =
        updateImageLayers(id, coalesceKey = "corner:$id") { it.copy(cornerRadius = radius) }

    /** Closes the continuous edit in progress, so the next one becomes its own undo step. */
    fun endGesture() {
        gestureKey = null
    }

    fun undo() = restore { it.undo() }

    fun redo() = restore { it.redo() }

    private inline fun restore(step: (EditHistory<FreestyleState>) -> EditHistory<FreestyleState>) {
        val current = history ?: return
        history = step(current)
        gestureKey = null
        publish()
    }

    /** Swaps the photo of image layer [id] for the one at [path], keeping its placement and frame. */
    fun replaceImage(id: Long, path: String) {
        viewModelScope.launch {
            runCatching { PlatformFile(path).toImageBitmap() }
                .onSuccess { image ->
                    updateImageLayers(id) { it.copy(content = FreestyleContent.ImageContent(image)) }
                }
        }
    }

    /**
     * Bakes the current canvas into one bitmap and hands it to the shared editing session.
     * The output keeps the on-screen canvas's [previewWidthPx] x [previewHeightPx] aspect ratio;
     * [previewDensity] converts the layers' dp frame widths/corners into preview pixels.
     */
    fun applyFreestyle(textMeasurer: TextMeasurer, previewWidthPx: Float, previewHeightPx: Float, previewDensity: Float) {
        val state = (_uiState.value as? FreestyleEditorUiState.Ready)?.freestyle ?: return
        session.set(bakeFreestyle(state, textMeasurer, previewWidthPx, previewHeightPx, previewDensity))
    }

    private inline fun updateImageLayers(
        id: Long?,
        coalesceKey: String? = null,
        crossinline transform: (FreestyleLayer) -> FreestyleLayer,
    ) {
        updateReady(coalesceKey) { state ->
            state.copy(
                layers = state.layers.map { layer ->
                    val targeted = id == null || layer.id == id
                    if (targeted && layer.content is FreestyleContent.ImageContent) transform(layer) else layer
                },
            )
        }
    }

    private fun nextId(): Long = nextLayerId++

    /**
     * Applies [transform] to the current state. With [record] it becomes a new undo step unless
     * [coalesceKey] matches the gesture in progress, in which case it replaces that step's result;
     * without [record] the current state changes in place and history is untouched.
     */
    private inline fun updateReady(
        coalesceKey: String? = null,
        record: Boolean = true,
        transform: (FreestyleState) -> FreestyleState,
    ) {
        val current = history ?: return
        val next = transform(current.current)
        if (next == current.current) return
        history = when {
            !record -> current.copy(current = next)
            coalesceKey != null && coalesceKey == gestureKey -> current.copy(current = next)
            else -> current.push(next)
        }
        if (record) gestureKey = coalesceKey
        publish()
    }

    private fun publish() {
        val current = history ?: return
        _uiState.value = FreestyleEditorUiState.Ready(current.current, current.canUndo, current.canRedo)
    }

    private companion object {
        val ScatterRotations = listOf(-8f, 7f, -5f, 9f, -10f, 4f)

        /**
         * Higher than the photo editor's [MaxEditHistory]: a step here is a small [FreestyleState]
         * whose layers share their decoded bitmaps, not a full-resolution bitmap of its own.
         */
        const val MaxFreestyleHistory = 50
    }
}
