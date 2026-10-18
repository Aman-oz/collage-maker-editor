package org.example.project.ui.pip

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextMeasurer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.i18n.tr
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.freestyle.isPremiumFreestyle
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.broughtToFront
import org.example.project.ui.freestyle.transformed
import org.example.project.ui.freestyle.withScaleRotation
import org.example.project.ui.templates.SlotTransform
import org.example.project.ui.templates.applyGesture
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextToolSession
import org.example.project.ui.text.bakeTextLayers

data class PipEditorUiState(
    /** The frame and masks; null while they (and the picked photos) load. */
    val assets: PipFrameAssets? = null,
    val edit: PipEdit = PipEdit(),
    /** The blurred [PipEdit.backdropSource]; null until it is computed. */
    val backdrop: ImageBitmap? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null,
)

/**
 * One state of the PIP editor: the picture and its stickers ([edit]), and the Text tool's texts over
 * it ([TextLayers]), kept live (so a tap opens them again) and baked over everything on Done.
 */
private data class PipFrame(val edit: PipEdit, val texts: TextLayers? = null)

/**
 * Drives the Pip editor (the LAS app's `TemplateDetailActivity`): loads [templateName]'s frame and
 * masks, puts the picked [imagePaths] into its slots in order, and bakes the picture plus its
 * stickers and texts into the session on Done.
 *
 * Text is the photo editor's Text tool, opened over the screen ([textSession]) on a snapshot of the
 * picture. Its texts come back as [TextLayers] that stay live over the picture ([texts]), above the
 * stickers, until Done bakes them in.
 *
 * The edit history lives here rather than in the composable because Done pushes the photo editor
 * over this screen, and Back must return to the same photos and layers. Like Set Background,
 * continuous edits (a pinch, a drag) pass a coalesce key and fold into one undo
 * step until the screen reports the gesture over via [endGesture].
 */
class PipEditorViewModel(
    templateName: String,
    private val imagePaths: List<String>,
    private val session: ImageEditSession,
    private val assetLoader: PipAssetLoader,
    private val settings: AppSettings,
) : ViewModel() {

    private var premiumOfferShown = false

    /**
     * Whether Done should open the paywall once before creating the picture: `true` a single time,
     * for a non-subscriber whose sticker layers are premium by the freestyle editor's rule
     * ([isPremiumFreestyle]: more than two stickers). The texts are the Text tool's, which makes its
     * own offer. It is an offer, not a gate, so asking marks it as made and the next Done goes
     * through either way.
     */
    fun consumePremiumOffer(): Boolean {
        if (settings.isPremium.value || premiumOfferShown || !history.current.edit.layers.isPremiumFreestyle()) return false
        premiumOfferShown = true
        return true
    }

    val template: PipTemplate? = pipTemplate(templateName)

    /** Whether the user is subscribed; the Text tool offers everyone else the paywall on its Done. */
    internal val isPremium: StateFlow<Boolean> = settings.isPremium

    /**
     * The Text tool open over the editor, or null. Here rather than in the screen because the paywall
     * (offered from the tool's Done) covers the editor, which leaves composition: the tool has to
     * still be open on return.
     */
    internal val textSession = mutableStateOf<TextToolSession?>(null)

    private val _texts = MutableStateFlow<TextLayers?>(null)

    /** The texts over the picture, not yet baked; null with none. */
    internal val texts: StateFlow<TextLayers?> = _texts.asStateFlow()

    private var assets: PipFrameAssets? = null

    private var history = EditHistory(PipFrame(PipEdit()), maxSize = MaxPipHistory)

    /** The coalesce key of the continuous edit in progress, or null between gestures. */
    private var gestureKey: String? = null

    private var nextLayerId = 0L

    /**
     * Blurred backdrops by source photo (identity), so undoing a photo swap on the first slot
     * restores its backdrop at once instead of blurring it again.
     */
    private val backdrops = mutableMapOf<ImageBitmap, ImageBitmap>()
    private val pendingBackdrops = mutableSetOf<ImageBitmap>()

    private val _uiState = MutableStateFlow(PipEditorUiState())
    val uiState: StateFlow<PipEditorUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val template = template
        if (template == null) {
            _uiState.value = PipEditorUiState(error = tr("This template isn't available"))
            return
        }
        viewModelScope.launch {
            val photos = imagePaths.take(template.slots.size).map { path -> async { loadPhoto(path) } }
            val loaded = assetLoader.frameAssets(template)
            if (loaded == null) {
                _uiState.value = PipEditorUiState(error = tr("Couldn't load this template"))
                return@launch
            }
            assets = loaded
            val placed = photos.awaitAll().withIndex()
                .mapNotNull { (index, photo) -> photo?.let { index to it } }
                .toMap()
            // The picked photos are the starting point, not an undoable step.
            history = EditHistory(PipFrame(PipEdit(photos = placed)), maxSize = MaxPipHistory)
            publish()
        }
    }

    /** Puts the photo at [path] into [slotIndex], starting from the plain fit again. */
    fun replacePhoto(slotIndex: Int, path: String) {
        viewModelScope.launch {
            val photo = loadPhoto(path) ?: return@launch
            update { it.copy(photos = it.photos + (slotIndex to photo), transforms = it.transforms - slotIndex) }
        }
    }

    /** Applies one pinch/pan step to the photo in [slotIndex], measured in a [slotWidth]x[slotHeight] px window. */
    fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) =
        update(coalesceKey = "slot:$slotIndex") { edit ->
            if (slotIndex !in edit.photos) return@update edit
            val current = edit.transforms[slotIndex] ?: SlotTransform()
            edit.copy(transforms = edit.transforms + (slotIndex to current.applyGesture(panX, panY, zoom, slotWidth, slotHeight)))
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
     * Bakes the picture into the session, its texts on top; see [bakePip] for the preview arguments.
     * Returns false while nothing is loaded yet, so the screen stays put.
     */
    fun apply(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float): Boolean {
        val picture = bakePicture(textMeasurer, previewWidthPx, previewDensity) ?: return false
        val texts = history.current.texts
        session.set(texts?.let { bakeTextLayers(picture, textMeasurer, it) } ?: picture)
        return true
    }

    /**
     * Opens the Text tool on the picture as it is now, baked as Done would bake it (without the texts,
     * which the tool shows itself). [selectedTextId] is the text tapped on the picture, if one was.
     */
    internal fun openTextTool(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float, selectedTextId: Long?) {
        if (textSession.value != null || previewWidthPx <= 0f) return
        val snapshot = bakePicture(textMeasurer, previewWidthPx, previewDensity) ?: return
        textSession.value = TextToolSession(snapshot, history.current.texts, selectedTextId)
    }

    /** Leaves the Text tool without its changes. */
    internal fun closeTextTool() {
        textSession.value = null
    }

    /** Commits the Text tool's texts as one undoable step (none, when every text was deleted) and closes it. */
    internal fun applyTexts(layers: TextLayers) {
        textSession.value = null
        if (assets == null) return
        history = history.push(history.current.copy(texts = layers.takeIf { it.texts.isNotEmpty() }))
        gestureKey = null
        publish()
    }

    /** The picture and its stickers, baked; null while nothing is loaded yet. */
    private fun bakePicture(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float): ImageBitmap? {
        val template = template ?: return null
        val assets = assets ?: return null
        val edit = history.current.edit
        val backdrop = edit.backdropSource?.let { source -> backdrops[source] ?: blurredBackdrop(source) }
        return bakePip(template, assets, backdrop, edit, textMeasurer, previewWidthPx, previewDensity)
    }

    private suspend fun loadPhoto(path: String): ImageBitmap? =
        // Copied so it is a canvas-backed bitmap drawImageCropped can redraw reliably.
        runCatching { copyBitmap(PlatformFile(path).toImageBitmap()) }.getOrNull()

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
        transform: (PipEdit) -> PipEdit,
    ) {
        if (assets == null) return
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

    private fun restore(restored: EditHistory<PipFrame>) {
        history = restored
        gestureKey = null
        publish()
    }

    private fun publish() {
        val edit = history.current.edit
        val source = edit.backdropSource
        if (source != null && source !in backdrops) blurBackdrop(source)
        _uiState.value = PipEditorUiState(
            assets = assets,
            edit = edit,
            backdrop = source?.let { backdrops[it] },
            canUndo = history.canUndo,
            canRedo = history.canRedo,
        )
        _texts.value = history.current.texts
    }

    private fun blurBackdrop(source: ImageBitmap) {
        if (!pendingBackdrops.add(source)) return
        viewModelScope.launch {
            val blurred = withContext(Dispatchers.Default) { blurredBackdrop(source) }
            pendingBackdrops.remove(source)
            backdrops[source] = blurred
            publish()
        }
    }

    private companion object {
        /** Like Set Background's: a step is a small edit sharing its bitmaps, not a full bitmap. */
        const val MaxPipHistory = 50
    }
}
