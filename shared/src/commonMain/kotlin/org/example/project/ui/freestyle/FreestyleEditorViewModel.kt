package org.example.project.ui.freestyle

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
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.network.NetworkImageLoader
import org.example.project.i18n.tr
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.editor.EditHistory
import org.example.project.ui.layouteditor.LayoutArt
import org.example.project.ui.layouteditor.LayoutArtLibrary
import org.example.project.ui.layouteditor.LayoutArtLoad
import org.example.project.ui.editor.MaxEditHistory
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickersToolSession
import org.example.project.ui.stickers.bakeStickers
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextToolSession
import org.example.project.ui.text.bakeTextLayers

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
 * One state of the freestyle editor: the canvas, and the Stickers tool's stickers and the Text
 * tool's texts over it (stickers under texts), kept live (so a tap opens them again) and baked over
 * everything on Done.
 */
private data class FreestyleFrame(
    val canvas: FreestyleState,
    val texts: TextLayers? = null,
    val stickers: List<PlacedSticker> = emptyList(),
)

/**
 * Decodes every initially-picked image in parallel, scatters them ([scatterPlacements]) as freely
 * draggable/rotatable layers on one open canvas, and exposes every edit the freestyle screen offers — adding more
 * images or stickers, replacing a photo, transforming/reordering/deleting layers, the canvas
 * background fill, and each photo's white frame and corner rounding.
 *
 * Text is the photo editor's Text tool, opened over the screen ([textSession]) on a snapshot of the
 * canvas. Its texts come back as [TextLayers] that stay live over the canvas ([texts]), above every
 * layer, until Done bakes them in. Stickers are the photo editor's Stickers tool, opened the same
 * way ([stickersSession]); its [PlacedSticker]s stay live too ([stickers]), over the layers and
 * under the texts.
 *
 * The Background panel's Background tab reads the layout editor's server backgrounds
 * ([LayoutArtLibrary]); a picked one is [FreestyleState.backgroundArt], and a premium one is a hard
 * gate on Done ([needsPremium]).
 *
 * Every edit goes through an [EditHistory] of whole states (the canvas and its texts) for undo/redo. Continuous
 * edits (a drag, a slider, retyping a label) pass a coalesce key and are folded into one step until
 * the screen reports the gesture over via [endGesture]; bringing a layer to front on selection is
 * not recorded at all, so undo only ever reverts something the user deliberately did.
 *
 * [imagePaths] is passed in from the navigation key via Koin's `parametersOf`.
 */
class FreestyleEditorViewModel(
    private val imagePaths: List<String>,
    private val session: ImageEditSession,
    private val settings: AppSettings,
    private val imageLoader: NetworkImageLoader,
    private val artLibrary: LayoutArtLibrary,
) : ViewModel() {

    private val _backgroundArtCategories = MutableStateFlow<LayoutArtLoad>(LayoutArtLoad.Loading)

    /** The server's backgrounds for the Background panel's Background tab; loaded when it first opens. */
    internal val backgroundArtCategories: StateFlow<LayoutArtLoad> = _backgroundArtCategories.asStateFlow()

    /** Background pictures copied once for drawing (see `copyBitmap`), by URL. */
    private val artCopies = mutableMapOf<String, ImageBitmap>()

    /** Set while a tool is being opened on a snapshot, which waits for the background picture. */
    private var openingTool = false

    private var premiumOfferShown = false

    /** Whether the user is subscribed; the Text tool offers everyone else the paywall on its Done. */
    internal val isPremium: StateFlow<Boolean> = settings.isPremium

    /**
     * The Text tool open over the editor, or null. Here rather than in the screen because the paywall
     * (offered from the tool's Done) covers the editor, which leaves composition: the tool has to
     * still be open on return.
     */
    internal val textSession = mutableStateOf<TextToolSession?>(null)

    private val _texts = MutableStateFlow<TextLayers?>(null)

    /** The texts over the canvas, not yet baked; null with none. */
    internal val texts: StateFlow<TextLayers?> = _texts.asStateFlow()

    /** The Stickers tool open over the canvas, or null; here for the same reason as [textSession]. */
    internal val stickersSession = mutableStateOf<StickersToolSession?>(null)

    private val _stickers = MutableStateFlow<List<PlacedSticker>>(emptyList())

    /** The Stickers tool's stickers over the canvas, not yet baked. */
    internal val stickers: StateFlow<List<PlacedSticker>> = _stickers.asStateFlow()

    private var baking = false

    /**
     * Whether Done should open the paywall once before creating the freestyle: `true` a single
     * time, for a non-subscriber whose canvas [isPremiumFreestyle]. It is an offer, not a gate, so
     * asking marks it as made and the next Done goes through either way.
     */
    /**
     * Whether Done must open the paywall instead of creating the freestyle: a non-subscriber with a
     * premium background picture on. Unlike [consumePremiumOffer] this asks every time.
     */
    val needsPremium: Boolean
        get() = !settings.isPremium.value &&
            (_uiState.value as? FreestyleEditorUiState.Ready)?.freestyle?.backgroundArt?.isPremium == true

    fun consumePremiumOffer(): Boolean {
        val freestyle = (_uiState.value as? FreestyleEditorUiState.Ready)?.freestyle ?: return false
        if (settings.isPremium.value || premiumOfferShown || !freestyle.layers.isPremiumFreestyle()) return false
        premiumOfferShown = true
        return true
    }

    private val _uiState = MutableStateFlow<FreestyleEditorUiState>(FreestyleEditorUiState.Loading)
    val uiState: StateFlow<FreestyleEditorUiState> = _uiState.asStateFlow()

    private var nextLayerId = 0L

    private var history: EditHistory<FreestyleFrame>? = null

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
                    val placements = scatterPlacements(images.map { it.height.toFloat() / it.width.toFloat() })
                    val layers = images.zip(placements) { image, placement ->
                        FreestyleLayer(
                            id = nextId(),
                            content = FreestyleContent.ImageContent(image),
                            offsetFraction = placement.offsetFraction,
                            scale = placement.scale,
                            rotationDegrees = placement.rotationDegrees,
                        )
                    }
                    history = EditHistory(FreestyleFrame(FreestyleState(layers = layers)), maxSize = MaxFreestyleHistory)
                    publish()
                }
                .onFailure { _uiState.value = FreestyleEditorUiState.Error(it.message ?: tr("Could not open these images")) }
        }
    }

    fun addImage(path: String) {
        viewModelScope.launch {
            runCatching { PlatformFile(path).toImageBitmap() }
                .onSuccess { image -> addLayer(FreestyleContent.ImageContent(image)) }
        }
    }

    fun addSticker(emoji: String) = addLayer(FreestyleContent.StickerContent(emoji))

    private fun addLayer(content: FreestyleContent) {
        updateReady { state -> state.copy(layers = state.layers + FreestyleLayer(id = nextId(), content = content)) }
    }

    /** Applies one continuous drag/pinch/rotate gesture tick to the layer identified by [id]. */
    fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) {
        updateReady(coalesceKey = "transform:$id") { state ->
            state.copy(layers = state.layers.transformed(id, panFraction, zoomDelta, rotationDeltaDegrees))
        }
    }

    /** Sets [id]'s absolute size and angle, as dragged by its corner resize/rotate handle. */
    fun setLayerScaleRotation(id: Long, scale: Float, rotationDegrees: Float) {
        updateReady(coalesceKey = "transform:$id") { state ->
            state.copy(layers = state.layers.withScaleRotation(id, scale, rotationDegrees))
        }
    }

    fun bringToFront(id: Long) {
        updateReady(record = false) { state -> state.copy(layers = state.layers.broughtToFront(id)) }
    }

    fun removeLayer(id: Long) {
        updateReady { state -> state.copy(layers = state.layers.filterNot { it.id == id }) }
    }

    /** A colour or gradient takes the background picture's place, if there was one. */
    fun updateBackground(background: FreestyleFill) = updateReady { it.copy(background = background, backgroundArt = null) }

    /**
     * Puts [art] behind the layers in the fill's place; picking the one already in use takes it off,
     * back to the fill.
     */
    internal fun toggleBackgroundArt(art: LayoutArt) {
        val current = (_uiState.value as? FreestyleEditorUiState.Ready)?.freestyle ?: return
        val next = art.takeUnless { it.id == current.backgroundArt?.id }
        keepArt(next)
        updateReady { it.copy(backgroundArt = next) }
    }

    /** Reads the server's backgrounds, once; a failure can be retried. */
    internal fun loadBackgroundArt() {
        if (_backgroundArtCategories.value is LayoutArtLoad.Ready) return
        _backgroundArtCategories.value = LayoutArtLoad.Loading
        viewModelScope.launch {
            _backgroundArtCategories.value = runCatching { artLibrary.getCategories(CentralDataConfig.BACKGROUNDS_SUPER_CATEGORY_ID) }
                .fold({ LayoutArtLoad.Ready(it) }, { LayoutArtLoad.Failed })
        }
    }

    /**
     * Downloads [art]'s picture to the disk cache when it is free (or the user subscribes), so it opens
     * from there next time; a premium picture only being tried out isn't kept, as with fonts.
     */
    private fun keepArt(art: LayoutArt?) {
        if (art == null || (art.isPremium && !settings.isPremium.value)) return
        viewModelScope.launch { imageLoader.loadAsset(art.imageUrl, keep = true) }
    }

    /**
     * [state]'s background picture for a bake, downloaded once (the preview has usually loaded it
     * already) and copied so it redraws reliably; null with none, or when it can't be loaded.
     */
    private suspend fun backgroundImage(state: FreestyleState): ImageBitmap? {
        val url = state.backgroundArt?.imageUrl ?: return null
        artCopies[url]?.let { return it }
        val loaded = imageLoader.loadAsset(url, keep = false) ?: return null
        return withContext(Dispatchers.Default) { copyBitmap(loaded) }.also { artCopies[url] = it }
    }

    /** Bakes the canvas (no stickers or texts) for a tool to open on, then hands it to [open]. */
    private fun openOnSnapshot(
        textMeasurer: TextMeasurer,
        previewWidthPx: Float,
        previewHeightPx: Float,
        previewDensity: Float,
        open: (frame: FreestyleFrame, snapshot: ImageBitmap) -> Unit,
    ) {
        val frame = history?.current ?: return
        if (openingTool || previewWidthPx <= 0f || previewHeightPx <= 0f) return
        openingTool = true
        viewModelScope.launch {
            try {
                val background = backgroundImage(frame.canvas)
                open(frame, bakeFreestyle(frame.canvas, textMeasurer, previewWidthPx, previewHeightPx, previewDensity, backgroundImage = background))
            } finally {
                openingTool = false
            }
        }
    }

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

    /**
     * Opens the Text tool on the canvas as it is now, baked at the on-screen canvas's
     * [previewWidthPx] x [previewHeightPx] aspect ratio as Done would bake it (without the texts,
     * which the tool shows itself). [selectedTextId] is the text tapped on the canvas, if one was.
     */
    internal fun openTextTool(
        textMeasurer: TextMeasurer,
        previewWidthPx: Float,
        previewHeightPx: Float,
        previewDensity: Float,
        selectedTextId: Long?,
    ) {
        if (textSession.value != null) return
        openOnSnapshot(textMeasurer, previewWidthPx, previewHeightPx, previewDensity) { frame, snapshot ->
            textSession.value = TextToolSession(snapshot, frame.texts, selectedTextId)
        }
    }

    /**
     * Opens the Stickers tool on the canvas as Done would bake it (see [openTextTool]), without the
     * stickers and texts, which the tool draws itself. [selectedStickerId] is the sticker tapped on
     * the canvas, if one was.
     */
    internal fun openStickersTool(
        textMeasurer: TextMeasurer,
        previewWidthPx: Float,
        previewHeightPx: Float,
        previewDensity: Float,
        selectedStickerId: Long?,
    ) {
        if (stickersSession.value != null) return
        openOnSnapshot(textMeasurer, previewWidthPx, previewHeightPx, previewDensity) { frame, snapshot ->
            stickersSession.value = StickersToolSession(snapshot, frame.stickers, selectedStickerId, frame.texts)
        }
    }

    /** Leaves the Stickers tool without its changes. */
    internal fun closeStickersTool() {
        stickersSession.value = null
    }

    /** Commits the Stickers tool's stickers as one undoable step and closes it. */
    internal fun applyStickers(stickers: List<PlacedSticker>) {
        stickersSession.value = null
        val current = history ?: return
        if (stickers == current.current.stickers) return
        history = current.push(current.current.copy(stickers = stickers))
        gestureKey = null
        publish()
    }

    /** Leaves the Text tool without its changes. */
    internal fun closeTextTool() {
        textSession.value = null
    }

    /** Commits the Text tool's texts as one undoable step (none, when every text was deleted) and closes it. */
    internal fun applyTexts(layers: TextLayers) {
        textSession.value = null
        val current = history ?: return
        history = current.push(current.current.copy(texts = layers.takeIf { it.texts.isNotEmpty() }))
        gestureKey = null
        publish()
    }

    private inline fun restore(step: (EditHistory<FreestyleFrame>) -> EditHistory<FreestyleFrame>) {
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
     * Bakes the current canvas into one bitmap, its stickers and then its texts on top, and hands it
     * to the shared editing session. The output keeps the on-screen canvas's [previewWidthPx] x
     * [previewHeightPx] aspect ratio; [previewDensity] converts the layers' dp frame widths/corners
     * into preview pixels. Suspends while the stickers' SVGs are drawn at full size; false before the
     * canvas is in, or while a bake is already under way.
     */
    suspend fun applyFreestyle(textMeasurer: TextMeasurer, previewWidthPx: Float, previewHeightPx: Float, previewDensity: Float): Boolean {
        val frame = history?.current ?: return false
        if (baking) return false
        baking = true
        try {
            val background = backgroundImage(frame.canvas)
            val canvas = bakeFreestyle(
                frame.canvas, textMeasurer, previewWidthPx, previewHeightPx, previewDensity, backgroundImage = background,
            )
            val withStickers = bakeStickers(canvas, frame.stickers, imageLoader::renderSvg)
            session.set(frame.texts?.let { bakeTextLayers(withStickers, textMeasurer, it) } ?: withStickers)
            return true
        } finally {
            baking = false
        }
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
        val next = current.current.copy(canvas = transform(current.current.canvas))
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
        _uiState.value = FreestyleEditorUiState.Ready(current.current.canvas, current.canUndo, current.canRedo)
        _texts.value = current.current.texts
        _stickers.value = current.current.stickers
    }

    private companion object {
        /**
         * Higher than the photo editor's [MaxEditHistory]: a step here is a small [FreestyleState]
         * whose layers share their decoded bitmaps (and texts), not a full-resolution bitmap of its own.
         */
        const val MaxFreestyleHistory = 50
    }
}
