package org.example.project.ui.setbackground

import androidx.compose.runtime.mutableStateOf
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
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.broughtToFront
import org.example.project.ui.freestyle.transformed
import org.example.project.ui.freestyle.withScaleRotation
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickersToolSession
import org.example.project.ui.stickers.bakeStickers
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextToolSession
import org.example.project.ui.text.bakeTextLayers

data class SetBackgroundUiState(
    val edit: SetBackgroundEdit = SetBackgroundEdit(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

/**
 * One state of Set Background: the backdrop and emoji ([edit]), and the Stickers tool's stickers and
 * the Text tool's texts over the photo (stickers under texts), kept live (so a tap opens them again)
 * and baked over everything on Done.
 */
private data class SetBackgroundFrame(
    val edit: SetBackgroundEdit,
    val texts: TextLayers? = null,
    val stickers: List<PlacedSticker> = emptyList(),
)

/**
 * Reads the transparent cut-out from [ImageEditSession], edits its backdrop and sticker layers, and
 * writes the flattened photo back on Done.
 *
 * Text is the photo editor's Text tool, opened over the screen ([textSession]) on a snapshot of the
 * photo with its backdrop and stickers. Its texts come back as [TextLayers] that stay live over the
 * photo ([texts]), above the emoji and stickers, until Done bakes them in. Stickers are the photo
 * editor's Stickers tool, opened the same way ([stickersSession]); its [PlacedSticker]s stay live
 * too ([stickers]), under the texts.
 *
 * The edit history lives here rather than in the composable because Done pushes the photo editor
 * over this screen, and Back must return to the same backdrop and layers. Like the freestyle
 * editor, continuous edits (a drag) pass a coalesce key and fold into one undo
 * step until the screen reports the gesture over via [endGesture].
 */
class SetBackgroundViewModel(
    private val session: ImageEditSession,
    private val settings: AppSettings,
    private val imageLoader: NetworkImageLoader,
) : ViewModel() {

    private var premiumOfferShown = false

    /**
     * Whether Done should open the paywall once before flattening the photo: `true` a single time,
     * for a non-subscriber whose edit [isPremium] (the texts are the Text tool's, which makes its own
     * offer). It is an offer, not a gate, so asking marks it as made and the next Done goes through
     * either way.
     */
    fun consumePremiumOffer(): Boolean {
        if (settings.isPremium.value || premiumOfferShown || !history.current.edit.isPremium()) return false
        premiumOfferShown = true
        return true
    }

    /**
     * Captured once rather than read live: Done replaces the session image with the flattened
     * photo, and Back from the editor must still find the transparent cut-out here.
     */
    val cutOut: ImageBitmap? = session.image.value

    private var history = EditHistory(SetBackgroundFrame(SetBackgroundEdit()), maxSize = MaxSetBackgroundHistory)

    /** Whether the user is subscribed; the Text tool offers everyone else the paywall on its Done. */
    internal val isPremium: StateFlow<Boolean> = settings.isPremium

    /**
     * The Text tool open over the screen, or null. Here rather than in the screen because the paywall
     * (offered from the tool's Done) covers the screen, which leaves composition: the tool has to
     * still be open on return.
     */
    internal val textSession = mutableStateOf<TextToolSession?>(null)

    private val _texts = MutableStateFlow<TextLayers?>(null)

    /** The texts over the photo, not yet baked; null with none. */
    internal val texts: StateFlow<TextLayers?> = _texts.asStateFlow()

    /** The Stickers tool open over the photo, or null; here for the same reason as [textSession]. */
    internal val stickersSession = mutableStateOf<StickersToolSession?>(null)

    private val _stickers = MutableStateFlow<List<PlacedSticker>>(emptyList())

    /** The Stickers tool's stickers over the photo, not yet baked. */
    internal val stickers: StateFlow<List<PlacedSticker>> = _stickers.asStateFlow()

    private var baking = false

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

    /**
     * Bakes the edit at the cut-out's resolution, its stickers and then its texts on top; see
     * [bakeBackground] for the preview arguments. Suspends while the stickers' SVGs are drawn at
     * full size; false with no cut-out, or while a bake is already under way.
     */
    suspend fun apply(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float): Boolean {
        val image = cutOut ?: return false
        if (baking) return false
        baking = true
        try {
            val frame = history.current
            val photo = bakeBackground(image, frame.edit, textMeasurer, previewWidthPx, previewDensity)
            val withStickers = bakeStickers(photo, frame.stickers, imageLoader::renderSvg)
            session.set(frame.texts?.let { bakeTextLayers(withStickers, textMeasurer, it) } ?: withStickers)
            return true
        } finally {
            baking = false
        }
    }

    /**
     * Opens the Stickers tool on the photo as Done would bake it, without the stickers and texts,
     * which the tool draws itself. [selectedStickerId] is the sticker tapped on the photo, if one was.
     */
    internal fun openStickersTool(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float, selectedStickerId: Long?) {
        val image = cutOut ?: return
        if (stickersSession.value != null || previewWidthPx <= 0f) return
        val frame = history.current
        val snapshot = bakeBackground(image, frame.edit, textMeasurer, previewWidthPx, previewDensity)
        stickersSession.value = StickersToolSession(snapshot, frame.stickers, selectedStickerId, frame.texts)
    }

    /** Leaves the Stickers tool without its changes. */
    internal fun closeStickersTool() {
        stickersSession.value = null
    }

    /** Commits the Stickers tool's stickers as one undoable step and closes it. */
    internal fun applyStickers(stickers: List<PlacedSticker>) {
        stickersSession.value = null
        if (stickers == history.current.stickers) return
        history = history.push(history.current.copy(stickers = stickers))
        gestureKey = null
        publish()
    }

    /**
     * Opens the Text tool on the photo as it is now, baked as Done would bake it (without the texts,
     * which the tool shows itself). [selectedTextId] is the text tapped on the photo, if one was.
     */
    internal fun openTextTool(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float, selectedTextId: Long?) {
        val image = cutOut ?: return
        if (textSession.value != null || previewWidthPx <= 0f) return
        val snapshot = bakeBackground(image, history.current.edit, textMeasurer, previewWidthPx, previewDensity)
        textSession.value = TextToolSession(snapshot, history.current.texts, selectedTextId)
    }

    /** Leaves the Text tool without its changes. */
    internal fun closeTextTool() {
        textSession.value = null
    }

    /** Commits the Text tool's texts as one undoable step (none, when every text was deleted) and closes it. */
    internal fun applyTexts(layers: TextLayers) {
        textSession.value = null
        history = history.push(history.current.copy(texts = layers.takeIf { it.texts.isNotEmpty() }))
        gestureKey = null
        publish()
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
        val next = history.current.copy(edit = transform(history.current.edit))
        if (next == history.current) return
        history = when {
            !record -> history.copy(current = next)
            coalesceKey != null && coalesceKey == gestureKey -> history.copy(current = next)
            else -> history.push(next)
        }
        if (record) gestureKey = coalesceKey
        publish()
    }

    private fun restore(restored: EditHistory<SetBackgroundFrame>) {
        history = restored
        gestureKey = null
        publish()
    }

    private fun publish() {
        _uiState.value = SetBackgroundUiState(history.current.edit, history.canUndo, history.canRedo)
        _texts.value = history.current.texts
        _stickers.value = history.current.stickers
    }

    private companion object {
        /** Like the freestyle editor's: a step is a small edit sharing its bitmaps, not a full bitmap. */
        const val MaxSetBackgroundHistory = 50
    }
}
