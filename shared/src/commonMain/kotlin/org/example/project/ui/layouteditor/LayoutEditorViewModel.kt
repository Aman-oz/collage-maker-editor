package org.example.project.ui.layouteditor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.network.NetworkImageLoader
import org.example.project.i18n.tr
import org.example.project.ui.collage.CollageCatalog
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.crop.AspectRatioOption
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.filter.PhotoFilter
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.broughtToFront
import org.example.project.ui.freestyle.isPremiumFreestyle
import org.example.project.ui.layouts.LayoutsRepository
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickersToolSession
import org.example.project.ui.stickers.bakeStickers
import org.example.project.ui.freestyle.transformed
import org.example.project.ui.freestyle.withScaleRotation
import org.example.project.ui.templates.SlotTransform
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextToolSession
import org.example.project.ui.text.bakeTextLayers

internal sealed interface LayoutEditorUiState {
    data object Loading : LayoutEditorUiState
    data class Ready(val edit: LayoutEdit, val canUndo: Boolean, val canRedo: Boolean) : LayoutEditorUiState
    data class Error(val message: String) : LayoutEditorUiState
}

/**
 * The photo editor's tools that open over the layout editor: Filter on the whole layout (its pick
 * goes over every photo), Ratio on one slot's photo.
 */
internal enum class LayoutPhotoTool { Filter, Ratio }

/**
 * [tool] open over the layout editor on [image]: for Ratio the photo in slot [slotIndex] as it was
 * when it opened, for Filter the layout without its filter (and [slotIndex] is -1). Identity
 * matters, as for [TextToolSession].
 */
internal class LayoutPhotoToolSession(
    val tool: LayoutPhotoTool,
    val slotIndex: Int,
    val image: ImageBitmap,
    val filterLabel: String? = null,
    val filterIntensity: Float = 1f,
)

/**
 * One state of the layout editor: the layout ([edit]), and the Stickers tool's stickers and the Text
 * tool's texts over it (stickers under texts), kept live until Done.
 */
private data class LayoutFrame(
    val edit: LayoutEdit,
    val texts: TextLayers? = null,
    val stickers: List<PlacedSticker> = emptyList(),
)

/**
 * Drives the layout editor: a collage layout picked on Home or in the Layouts browser, opened with
 * every slot empty for the user to fill one by one.
 *
 * The edit history lives here rather than in the screen because Done pushes the photo editor over
 * it, and the paywall can cover it: Back must find the same layout. As in Set Background, a
 * continuous edit (a drag, a slider) passes a coalesce key and folds into one undo step until the
 * screen reports the gesture over ([endGesture]).
 *
 * Text and Stickers are the photo editor's tools over the screen ([textSession], [stickersSession]),
 * kept live over the layout until Done bakes them; Filter and Ratio are the
 * photo editor's tools over it ([photoTool]): Filter's pick goes over every photo, Ratio's result
 * replaces the selected slot's photo. The sessions are here so they are still open after the
 * paywall.
 *
 * An option's panel edits live, as one would expect, and its ✕ takes back everything since it opened
 * ([beginPanel] / [cancelPanel]), undo history and all.
 */
internal class LayoutEditorViewModel(
    private val layoutName: String,
    private val session: ImageEditSession,
    private val catalog: CollageCatalog,
    private val layouts: LayoutsRepository,
    private val settings: AppSettings,
    private val imageLoader: NetworkImageLoader,
) : ViewModel(), LayoutEditorActions {

    private val _uiState = MutableStateFlow<LayoutEditorUiState>(LayoutEditorUiState.Loading)
    val uiState: StateFlow<LayoutEditorUiState> = _uiState.asStateFlow()

    private val _templates = MutableStateFlow<List<TemplateItem>>(emptyList())

    /** Every layout, for the Layout panel. */
    val templates: StateFlow<List<TemplateItem>> = _templates.asStateFlow()

    private val _premiumLayouts = MutableStateFlow<Set<String>>(emptySet())

    /**
     * The names of the premium layouts. They come from the server's Layouts category, not from
     * [TemplateItem.isPremium], which is the collage editor's flag: the two are priced separately.
     * Empty until loaded, and offline, so nothing is locked that the server didn't lock.
     */
    val premiumLayouts: StateFlow<Set<String>> = _premiumLayouts.asStateFlow()

    private val _texts = MutableStateFlow<TextLayers?>(null)

    /** The texts over the layout, not yet baked; null with none. */
    val texts: StateFlow<TextLayers?> = _texts.asStateFlow()

    private val _stickers = MutableStateFlow<List<PlacedSticker>>(emptyList())

    /** The Stickers tool's stickers over the layout, not yet baked. */
    val stickers: StateFlow<List<PlacedSticker>> = _stickers.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)

    /** Whether anything was done since the editor opened; leaving then asks to discard. */
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Whether the user is subscribed; the Filter and Text tools offer everyone else the paywall. */
    val isPremium: StateFlow<Boolean> = settings.isPremium

    /** The Text tool open over the screen, or null. */
    val textSession = mutableStateOf<TextToolSession?>(null)

    /** The Stickers tool open over the screen, or null. */
    val stickersSession = mutableStateOf<StickersToolSession?>(null)

    /** The Filter or Ratio tool open over the screen, or null. */
    val photoTool = mutableStateOf<LayoutPhotoToolSession?>(null)

    private var history: EditHistory<LayoutFrame>? = null

    /** The coalesce key of the continuous edit in progress, or null between gestures. */
    private var gestureKey: String? = null

    /** The history as the open option's panel found it; null while none is open. */
    private var panelStart: EditHistory<LayoutFrame>? = null

    private var nextLayerId = 0L
    private var premiumOfferShown = false

    private val current: LayoutFrame? get() = history?.current

    /**
     * Whether Done has to go to the paywall instead: the layout is a premium one and the user is not
     * subscribed. Read at the moment of Done, so it is already `false` after subscribing.
     */
    val needsPremium: Boolean
        get() = current?.edit?.template?.title in _premiumLayouts.value && !settings.isPremium.value

    /**
     * Whether Done should open the paywall once first: for a non-subscriber whose stickers are
     * premium by the freestyle editor's rule. An offer, not a gate: the next Done goes through.
     */
    fun consumePremiumOffer(): Boolean {
        val edit = current?.edit ?: return false
        if (settings.isPremium.value || premiumOfferShown || !edit.layers.isPremiumFreestyle()) return false
        premiumOfferShown = true
        return true
    }

    init {
        viewModelScope.launch {
            if (layouts.isConfigured) {
                _premiumLayouts.value = runCatching { layouts.premiumLayoutNames() }.getOrElse { emptySet() }
            }
        }
        viewModelScope.launch {
            val all = runCatching { catalog.loadAll() }.getOrElse { emptyList() }
            _templates.value = all
            val template = all.firstOrNull { it.title == layoutName }
            if (template == null) {
                _uiState.value = LayoutEditorUiState.Error(tr("No collage layouts available"))
                return@launch
            }
            history = EditHistory(LayoutFrame(LayoutEdit(template)), maxSize = MaxLayoutHistory)
            publish()
        }
    }

    /** Loads the photo at [path] into slot [slotIndex], replacing what was there. */
    fun setSlotPhoto(slotIndex: Int, path: String) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.Default) { loadPhoto(path) } }
                .onSuccess { image -> updatePhotos { it + (slotIndex to LayoutPhoto(image)) } }
                .onFailure { _messages.tryEmit(tr("Couldn't open that photo")) }
        }
    }

    /** The photo, its longer side at most [LayoutPhotoMaxSide], copied so it redraws reliably (see `copyBitmap`). */
    private suspend fun loadPhoto(path: String): ImageBitmap {
        val decoded = copyBitmap(PlatformFile(path).toImageBitmap())
        val (w, h) = layoutPhotoLoadSize(decoded.width, decoded.height)
        if (w == decoded.width && h == decoded.height) return decoded
        val output = ImageBitmap(w, h)
        Canvas(output).drawImageRect(
            image = decoded,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(decoded.width, decoded.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(w, h),
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
        return output
    }

    override fun removePhoto(slotIndex: Int) = updatePhotos { it - slotIndex }

    override fun selectTemplate(template: TemplateItem) = update { it.copy(template = template) }

    override fun setCanvas(option: AspectRatioOption) = update { it.copy(canvas = option) }

    override fun setMargin(value: Float) = update("margin") { it.copy(margin = value) }

    override fun setSpace(value: Float) = update("space") { it.copy(space = value) }

    override fun setCorner(value: Float) = update("corner") { it.copy(corner = value) }

    override fun setShadow(value: Float) = update("shadow") { it.copy(shadow = value) }

    override fun setBorderWidth(value: Float) = update("borderWidth") { it.copy(borderWidth = value) }

    override fun setBorderOpacity(value: Float) = update("borderOpacity") { it.copy(borderOpacity = value) }

    override fun setBorderColor(color: Color) = update {
        // Picking a colour with no outline yet would change nothing visible; give it one.
        it.copy(borderColor = color, borderWidth = if (it.borderWidth > 0f) it.borderWidth else DefaultBorderWidth)
    }

    override fun setFrameColor(color: Color) = update { it.copy(frameColor = color) }

    override fun setFrameWidth(value: Float) = update("frameWidth") { it.copy(frameWidth = value) }

    override fun setFrameCorner(value: Float) = update("frameCorner") { it.copy(frameCorner = value) }

    override fun setBackground(color: Color) = update { it.copy(background = color) }

    override fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) =
        updatePhoto(slotIndex, "slot:$slotIndex") { photo ->
            photo.copy(
                transform = photo.transform.applyLayoutGesture(
                    panX, panY, zoom,
                    imageW = photo.image.width.toFloat(),
                    imageH = photo.image.height.toFloat(),
                    slotW = slotWidth,
                    slotH = slotHeight,
                    photo = photo.shape,
                ),
            )
        }

    override fun flipPhoto(slotIndex: Int, horizontally: Boolean) = updatePhoto(slotIndex) { photo ->
        if (horizontally) photo.copy(flipX = !photo.flipX) else photo.copy(flipY = !photo.flipY)
    }

    override fun setPhotoRotation(slotIndex: Int, degrees: Float) =
        updatePhoto(slotIndex, "rotate:$slotIndex") { it.copy(rotationDegrees = degrees) }

    override fun setPhotoTiltX(slotIndex: Int, tilt: Float) =
        updatePhoto(slotIndex, "tiltX:$slotIndex") { it.copy(tiltX = tilt) }

    override fun setPhotoTiltY(slotIndex: Int, tilt: Float) =
        updatePhoto(slotIndex, "tiltY:$slotIndex") { it.copy(tiltY = tilt) }

    override fun beginPanel() {
        panelStart = history
        gestureKey = null
    }

    override fun cancelPanel() {
        val start = panelStart ?: return
        panelStart = null
        history = start
        gestureKey = null
        publish()
    }

    override fun endPanel() {
        panelStart = null
        gestureKey = null
    }

    override fun addSticker(emoji: String) = updateLayers {
        it + FreestyleLayer(id = nextLayerId++, content = FreestyleContent.StickerContent(emoji))
    }

    override fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) =
        updateLayers("layer:$id") { it.transformed(id, panFraction, zoomDelta, rotationDeltaDegrees) }

    override fun setLayerScaleRotation(id: Long, scale: Float, rotationDegrees: Float) =
        updateLayers("layer:$id") { it.withScaleRotation(id, scale, rotationDegrees) }

    /** Not recorded: selecting a sticker raises it, and undo should only revert deliberate edits. */
    override fun bringToFront(id: Long) = updateLayers(record = false) { it.broughtToFront(id) }

    override fun removeLayer(id: Long) = updateLayers { layers -> layers.filterNot { it.id == id } }

    override fun endGesture() {
        gestureKey = null
    }

    override fun undo() = restore { it.undo() }

    override fun redo() = restore { it.redo() }

    private var baking = false

    /**
     * Bakes the layout, its emoji, stickers and texts at full size into the session (see
     * [bakeLayout] and [bakeStickers]). Suspends while the stickers' SVGs are drawn at full size;
     * false when a bake was already under way, so a second Done doesn't go on twice.
     */
    suspend fun apply(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float): Boolean {
        val frame = current ?: return false
        if (baking) return false
        baking = true
        try {
            val picture = bakeLayout(frame.edit, textMeasurer, previewWidthPx, previewDensity)
            val withStickers = bakeStickers(picture, frame.stickers, imageLoader::renderSvg)
            session.set(frame.texts?.let { bakeTextLayers(withStickers, textMeasurer, it) } ?: withStickers)
            return true
        } finally {
            baking = false
        }
    }

    /**
     * Opens the Text tool on the layout as Done would bake it (without the texts, which the tool
     * shows itself). [selectedTextId] is the text tapped on the layout, if one was.
     */
    fun openTextTool(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float, selectedTextId: Long?) {
        val frame = current ?: return
        if (textSession.value != null || previewWidthPx <= 0f) return
        val snapshot = bakeLayout(frame.edit, textMeasurer, previewWidthPx, previewDensity)
        textSession.value = TextToolSession(snapshot, frame.texts, selectedTextId)
    }

    fun closeTextTool() {
        textSession.value = null
    }

    /** Commits the Text tool's texts as one undo step (none, when every text was deleted) and closes it. */
    fun applyTexts(layers: TextLayers) {
        textSession.value = null
        val history = history ?: return
        push(history.current.copy(texts = layers.takeIf { it.texts.isNotEmpty() }))
    }

    /**
     * Opens the Stickers tool on the layout as Done would bake it, without the stickers and texts,
     * which the tool draws itself. [selectedStickerId] is the sticker tapped on the layout, if one was.
     */
    fun openStickersTool(textMeasurer: TextMeasurer, previewWidthPx: Float, previewDensity: Float, selectedStickerId: Long?) {
        val frame = current ?: return
        if (stickersSession.value != null || previewWidthPx <= 0f) return
        val snapshot = bakeLayout(frame.edit, textMeasurer, previewWidthPx, previewDensity)
        stickersSession.value = StickersToolSession(snapshot, frame.stickers, selectedStickerId, frame.texts)
    }

    fun closeStickersTool() {
        stickersSession.value = null
    }

    /** Commits the Stickers tool's stickers as one undo step and closes it. */
    fun applyStickers(stickers: List<PlacedSticker>) {
        stickersSession.value = null
        val history = history ?: return
        if (stickers == history.current.stickers) return
        push(history.current.copy(stickers = stickers))
    }

    /** Opens Ratio on the photo in [slotIndex]; nothing when that slot has none. */
    fun openRatioTool(slotIndex: Int) {
        val photo = current?.edit?.photos?.get(slotIndex) ?: return
        if (photoTool.value == null) photoTool.value = LayoutPhotoToolSession(LayoutPhotoTool.Ratio, slotIndex, photo.image)
    }

    /**
     * Opens Filter on the whole layout: the photos as they are without the filter (and without the
     * stickers, which it doesn't touch), opened on the filter they have now.
     */
    fun openFilterTool(textMeasurer: TextMeasurer) {
        val edit = current?.edit ?: return
        if (photoTool.value != null || edit.photos.isEmpty()) return
        val snapshot = bakeLayout(
            edit.copy(filterLabel = null, layers = emptyList()),
            textMeasurer,
            previewWidthPx = 0f,
            previewDensity = 1f,
            longSide = FilterPreviewLongSide,
        )
        photoTool.value = LayoutPhotoToolSession(LayoutPhotoTool.Filter, -1, snapshot, edit.filterLabel, edit.filterIntensity)
    }

    /** Puts [filter] at [intensity] over every photo, as one undo step, and closes the tool. */
    fun applyFilter(filter: PhotoFilter, intensity: Float) {
        photoTool.value = null
        update { it.copy(filterLabel = filter.label.takeUnless { filter.isNoneOption }, filterIntensity = intensity) }
    }

    fun closePhotoTool() {
        photoTool.value = null
    }

    /**
     * Puts Ratio's result in its slot as one undo step. A new ratio is another picture to frame, so
     * it starts from the fit again.
     */
    fun applyRatio(image: ImageBitmap) {
        val opened = photoTool.value ?: return
        photoTool.value = null
        updatePhoto(opened.slotIndex) { photo -> photo.copy(image = image, transform = SlotTransform()) }
    }

    private inline fun updatePhoto(slotIndex: Int, coalesceKey: String? = null, transform: (LayoutPhoto) -> LayoutPhoto) {
        val photo = current?.edit?.photos?.get(slotIndex) ?: return
        updatePhotos(coalesceKey) { it + (slotIndex to transform(photo)) }
    }

    private inline fun updatePhotos(coalesceKey: String? = null, transform: (Map<Int, LayoutPhoto>) -> Map<Int, LayoutPhoto>) =
        update(coalesceKey) { it.copy(photos = transform(it.photos)) }

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
        transform: (LayoutEdit) -> LayoutEdit,
    ) {
        val history = history ?: return
        val next = history.current.copy(edit = transform(history.current.edit))
        if (next == history.current) return
        this.history = when {
            !record -> history.copy(current = next)
            coalesceKey != null && coalesceKey == gestureKey -> history.copy(current = next)
            else -> history.push(next)
        }
        if (record) {
            gestureKey = coalesceKey
            _hasChanges.value = true
        }
        publish()
    }

    private fun push(frame: LayoutFrame) {
        val history = history ?: return
        this.history = history.push(frame)
        gestureKey = null
        _hasChanges.value = true
        publish()
    }

    private inline fun restore(step: (EditHistory<LayoutFrame>) -> EditHistory<LayoutFrame>) {
        val history = history ?: return
        this.history = step(history)
        gestureKey = null
        publish()
    }

    private fun publish() {
        val history = history ?: return
        _uiState.value = LayoutEditorUiState.Ready(history.current.edit, history.canUndo, history.canRedo)
        _texts.value = history.current.texts
        _stickers.value = history.current.stickers
    }

    private companion object {
        /** A step shares its photos with the others, so it is small; the cap matches Set Background's. */
        const val MaxLayoutHistory = 50

        /** The outline a border colour picked at zero width gets. */
        const val DefaultBorderWidth = 3f

        /** The Filter tool's preview of the whole layout: enough for the screen, quick to make. */
        const val FilterPreviewLongSide = 1080
    }
}
