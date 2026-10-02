package org.example.project.ui.setbackground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleFill
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.broughtToFront
import org.example.project.ui.freestyle.retyped
import org.example.project.ui.freestyle.transformed
import org.example.project.ui.freestyle.withScaleRotation
import org.example.project.ui.freestyle.withText
import org.example.project.ui.text.TextFontStyleOption

data class SetBackgroundUiState(
    val edit: SetBackgroundEdit = SetBackgroundEdit(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

/**
 * Reads the transparent cut-out from [ImageEditSession], edits its backdrop and text/sticker layers,
 * and writes the flattened photo back on Done.
 *
 * The edit history lives here rather than in the composable because Done pushes the photo editor
 * over this screen, and Back must return to the same backdrop and layers. Like the freestyle
 * editor, continuous edits (a drag, retyping a label) pass a coalesce key and fold into one undo
 * step until the screen reports the gesture over via [endGesture].
 */
class SetBackgroundViewModel(
    private val session: ImageEditSession,
    private val settings: AppSettings,
) : ViewModel() {

    private var premiumOfferShown = false

    /**
     * Whether Done should open the paywall once before flattening the photo: `true` a single time,
     * for a non-subscriber whose edit [isPremium]. It is an offer, not a gate, so asking marks it as
     * made and the next Done goes through either way.
     */
    fun consumePremiumOffer(): Boolean {
        if (settings.isPremium.value || premiumOfferShown || !history.current.isPremium()) return false
        premiumOfferShown = true
        return true
    }

    /**
     * Captured once rather than read live: Done replaces the session image with the flattened
     * photo, and Back from the editor must still find the transparent cut-out here.
     */
    val cutOut: ImageBitmap? = session.image.value

    private var history = EditHistory(SetBackgroundEdit(), maxSize = MaxSetBackgroundHistory)

    /** The coalesce key of the continuous edit in progress, or null between gestures. */
    private var gestureKey: String? = null

    private var nextLayerId = 0L

    private val _uiState = MutableStateFlow(SetBackgroundUiState())
    val uiState: StateFlow<SetBackgroundUiState> = _uiState.asStateFlow()

    private val _photos = MutableStateFlow(emptyList<ImageBitmap>())

    /** Photos the user picked as backgrounds, oldest first, so they can switch back to one. */
    val photos: StateFlow<List<ImageBitmap>> = _photos.asStateFlow()

    fun selectBackdrop(backdrop: Backdrop) = update { it.copy(backdrop = backdrop) }

    /** Loads the photo at [path] and makes it the backdrop straight away. */
    fun addPhoto(path: String) {
        viewModelScope.launch {
            // Copied so it is a canvas-backed bitmap drawImageCropped can redraw reliably.
            runCatching { copyBitmap(PlatformFile(path).toImageBitmap()) }
                .onSuccess { image ->
                    _photos.update { it + image }
                    selectBackdrop(Backdrop.Photo(image))
                }
        }
    }

    fun addSticker(emoji: String) = addLayer(FreestyleContent.StickerContent(emoji))

    fun addText(text: String, fill: FreestyleFill, font: TextFontStyleOption, background: FreestyleFill?) {
        if (text.isBlank()) return
        addLayer(FreestyleContent.TextContent(text, fill, font, background))
    }

    fun updateText(id: Long, text: String, fill: FreestyleFill, font: TextFontStyleOption, background: FreestyleFill?) =
        updateLayers { it.withText(id, text, fill, font, background) }

    /** Live-retypes text layer [id] while its text bar is open; the whole session is one undo step. */
    fun retypeText(id: Long, text: String) = updateLayers(coalesceKey = "text:$id") { it.retyped(id, text) }

    fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) =
        updateLayers(coalesceKey = "transform:$id") { it.transformed(id, panFraction, zoomDelta, rotationDeltaDegrees) }

    fun setLayerScaleRotation(id: Long, scale: Float, rotationDegrees: Float) =
        updateLayers(coalesceKey = "transform:$id") { it.withScaleRotation(id, scale, rotationDegrees) }

    /** Not recorded: selecting a layer raises it, and undo should only revert deliberate edits. */
    fun bringToFront(id: Long) = updateLayers(record = false) { it.broughtToFront(id) }

    fun removeLayer(id: Long) = updateLayers { layers -> layers.filterNot { it.id == id } }

    /** Closes the continuous edit in progress, so the next one becomes its own undo step. */
    fun endGesture() {
        gestureKey = null
    }

    fun undo() = restore(history.undo())

    fun redo() = restore(history.redo())

    /** Bakes the edit at the cut-out's resolution; see [bakeBackground] for the preview arguments. */
    fun apply(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float) {
        val image = cutOut ?: return
        session.set(bakeBackground(image, history.current, textMeasurer, previewWidthPx, previewDensity))
    }

    private fun addLayer(content: FreestyleContent) =
        updateLayers { it + FreestyleLayer(id = nextLayerId++, content = content) }

    private inline fun updateLayers(
        coalesceKey: String? = null,
        record: Boolean = true,
        transform: (List<FreestyleLayer>) -> List<FreestyleLayer>,
    ) = update(coalesceKey, record) { it.copy(layers = transform(it.layers)) }

    /**
     * Applies [transform] to the current edit. With [record] it becomes a new undo step unless
     * [coalesceKey] matches the gesture in progress, in which case it replaces that step's result;
     * without [record] the current edit changes in place and history is untouched.
     */
    private inline fun update(
        coalesceKey: String? = null,
        record: Boolean = true,
        transform: (SetBackgroundEdit) -> SetBackgroundEdit,
    ) {
        val next = transform(history.current)
        if (next == history.current) return
        history = when {
            !record -> history.copy(current = next)
            coalesceKey != null && coalesceKey == gestureKey -> history.copy(current = next)
            else -> history.push(next)
        }
        if (record) gestureKey = coalesceKey
        publish()
    }

    private fun restore(restored: EditHistory<SetBackgroundEdit>) {
        history = restored
        gestureKey = null
        publish()
    }

    private fun publish() {
        _uiState.value = SetBackgroundUiState(history.current, history.canUndo, history.canRedo)
    }

    private companion object {
        /** Like the freestyle editor's: a step is a small edit sharing its bitmaps, not a full bitmap. */
        const val MaxSetBackgroundHistory = 50
    }
}
