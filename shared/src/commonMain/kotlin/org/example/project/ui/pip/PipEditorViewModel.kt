package org.example.project.ui.pip

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
import org.example.project.ui.freestyle.FreestyleFill
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.broughtToFront
import org.example.project.ui.freestyle.retyped
import org.example.project.ui.freestyle.transformed
import org.example.project.ui.freestyle.withScaleRotation
import org.example.project.ui.freestyle.withText
import org.example.project.ui.templates.SlotTransform
import org.example.project.ui.templates.applyGesture
import org.example.project.ui.text.TextFontStyleOption

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
 * Drives the Pip editor (the LAS app's `TemplateDetailActivity`): loads [templateName]'s frame and
 * masks, puts the picked [imagePaths] into its slots in order, and bakes the picture plus its text
 * and stickers into the session on Done.
 *
 * The edit history lives here rather than in the composable because Done pushes the photo editor
 * over this screen, and Back must return to the same photos and layers. Like Set Background,
 * continuous edits (a pinch, a drag, retyping a label) pass a coalesce key and fold into one undo
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
     * for a non-subscriber whose text and sticker layers are premium by the freestyle editor's rule
     * ([isPremiumFreestyle]: more than two stickers, or text in the premium red or Stylish font).
     * It is an offer, not a gate, so asking marks it as made and the next Done goes through either
     * way.
     */
    fun consumePremiumOffer(): Boolean {
        if (settings.isPremium.value || premiumOfferShown || !history.current.layers.isPremiumFreestyle()) return false
        premiumOfferShown = true
        return true
    }

    val template: PipTemplate? = pipTemplate(templateName)

    private var assets: PipFrameAssets? = null

    private var history = EditHistory(PipEdit(), maxSize = MaxPipHistory)

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
            history = EditHistory(PipEdit(photos = placed), maxSize = MaxPipHistory)
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

    /**
     * Bakes the picture into the session; see [bakePip] for the preview arguments. Returns false
     * while nothing is loaded yet, so the screen stays put.
     */
    fun apply(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float): Boolean {
        val template = template ?: return false
        val assets = assets ?: return false
        val edit = history.current
        val backdrop = edit.backdropSource?.let { source -> backdrops[source] ?: blurredBackdrop(source) }
        session.set(bakePip(template, assets, backdrop, edit, textMeasurer, previewWidthPx, previewDensity))
        return true
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

    private fun restore(restored: EditHistory<PipEdit>) {
        history = restored
        gestureKey = null
        publish()
    }

    private fun publish() {
        val edit = history.current
        val source = edit.backdropSource
        if (source != null && source !in backdrops) blurBackdrop(source)
        _uiState.value = PipEditorUiState(
            assets = assets,
            edit = edit,
            backdrop = source?.let { backdrops[it] },
            canUndo = history.canUndo,
            canRedo = history.canRedo,
        )
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
