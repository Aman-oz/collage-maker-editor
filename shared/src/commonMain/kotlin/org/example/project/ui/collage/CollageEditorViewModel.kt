package org.example.project.ui.collage

import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.data.centraldata.CentralDataConfig
import org.example.project.ui.layouteditor.LayoutArt
import org.example.project.ui.layouteditor.LayoutArtLibrary
import org.example.project.ui.layouteditor.LayoutArtLoad
import org.example.project.data.AppSettings
import org.example.project.data.ImageEditSession
import org.example.project.data.network.NetworkImageLoader
import org.example.project.i18n.tr
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickersToolSession
import org.example.project.ui.stickers.bakeStickers
import org.example.project.ui.templates.SlotTransform

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
 * were picked, and seeds the first one. Selecting a layout re-flows the photos into its slots. A
 * premium layout can be tried like any other, but only a subscriber can finish with one: for
 * everyone else Done opens the paywall ([needsPremium]); a free layout with a premium border
 * width or ratio gets the paywall once, as an offer ([consumePremiumOffer]). The Border tab's width (`space`) and corner
 * radius (`corner`) feed straight into the polygon renderer.
 *
 * Stickers are the photo editor's Stickers tool, opened over the screen ([stickersSession]) on a
 * snapshot of the collage. Its [PlacedSticker]s stay live over the collage ([stickers]), so a tap
 * opens them again, until Done bakes them in. With no undo history here, each apply simply replaces
 * the list (the tool has its own undo while it is open).
 */
class CollageEditorViewModel(
    private val imagePaths: List<String>,
    /** The layout to open on (its name, e.g. `collage_3_5`), when picked on Home; else the first that fits. */
    private val layoutName: String?,
    private val session: ImageEditSession,
    private val catalog: CollageCatalog,
    private val settings: AppSettings,
    private val imageLoader: NetworkImageLoader,
    private val artLibrary: LayoutArtLibrary,
) : ViewModel() {

    /**
     * Whether Done has to go to the paywall instead of creating the collage: the layout in use is a
     * premium one and the user is not subscribed. Read at the moment of Done, so it is already
     * `false` for someone who has just come back from subscribing.
     */
    val needsPremium: Boolean
        get() {
            val collage = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return false
            return (collage.template.isPremium || collage.backgroundArt?.isPremium == true) && !settings.isPremium.value
        }

    /** Whether the user subscribes; a non-subscriber on a premium layout sees the Pro banner. */
    val isSubscribed: StateFlow<Boolean> = settings.isPremium

    private var premiumOfferShown = false

    /**
     * Whether Done should open the paywall once before creating a collage on a free layout: `true`
     * a single time, for a non-subscriber whose collage [isPremiumCollageEdit]. Unlike
     * [needsPremium] it is an offer, not a gate, so asking marks it as made and the next Done goes
     * through either way.
     */
    fun consumePremiumOffer(): Boolean {
        val collage = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return false
        if (settings.isPremium.value || premiumOfferShown) return false
        if (!isPremiumCollageEdit(collage.space, collage.ratio)) return false
        premiumOfferShown = true
        return true
    }

    private val _uiState = MutableStateFlow<CollageEditorUiState>(CollageEditorUiState.Loading)
    val uiState: StateFlow<CollageEditorUiState> = _uiState.asStateFlow()

    private val _pickerState = MutableStateFlow(CollagePickerState())
    val pickerState: StateFlow<CollagePickerState> = _pickerState.asStateFlow()

    private val _hasChanges = MutableStateFlow(false)

    /**
     * Whether the user has changed the collage since it opened; leaving then asks to discard. There
     * is no undo history here, so it is a flag set by every user edit and never cleared.
     */
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /**
     * The Stickers tool open over the editor, or null. Here rather than in the screen because the
     * paywall (offered from the tool's Done) covers the editor, which leaves composition: the tool
     * has to still be open on return.
     */
    internal val stickersSession = mutableStateOf<StickersToolSession?>(null)

    private val _stickers = MutableStateFlow<List<PlacedSticker>>(emptyList())

    /** The Stickers tool's stickers over the collage, not yet baked. */
    internal val stickers: StateFlow<List<PlacedSticker>> = _stickers.asStateFlow()

    private var baking = false
    private var openingStickers = false

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
                _uiState.value = CollageEditorUiState.Error(it.message ?: tr("Could not open these images"))
                return@launch
            }
            photos = images

            val all = runCatching { catalog.loadAll() }.getOrElse {
                _pickerState.value = _pickerState.value.copy(isLoading = false, error = it.message ?: tr("Couldn't load collage layouts"))
                emptyList()
            }
            // Like CollageActivity: show only layouts whose slot count matches the photo count,
            // falling back to the nearest available count if there's no exact match. A layout
            // picked on Home sets the count instead, so it stays on offer if fewer photos came.
            val requested = layoutName?.let { name -> all.firstOrNull { it.title == name } }
            val count = requested?.imageCount ?: images.size
            var templates = all.filter { it.imageCount == count }
            if (templates.isEmpty() && all.isNotEmpty()) {
                val nearest = all.minByOrNull { kotlin.math.abs(it.imageCount - count) }!!.imageCount
                templates = all.filter { it.imageCount == nearest }
            }

            if (templates.isEmpty()) {
                // No layouts at all — still let the user see their first photo full-frame would be
                // ideal, but without geometry we surface an error like the LAS "no templates" toast.
                _uiState.value = CollageEditorUiState.Error(tr("No collage layouts available"))
                _pickerState.value = _pickerState.value.copy(isLoading = false)
                return@launch
            }

            val first = requested ?: templates.first()
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
        _pickerState.value = _pickerState.value.copy(selectedTemplateId = template.id)
        updateReady { state ->
            // Keep photos already placed; drop any whose slot no longer exists in the new layout. The
            // pan/zoom was framed for the old slot shapes, so every photo starts from the fit again.
            val kept = state.images.filterKeys { it < template.imageCount }
            state.copy(template = template, images = kept, transforms = emptyMap())
        }
    }

    fun swapImages(fromIndex: Int, toIndex: Int) {
        updateReady { state ->
            val from = state.images[fromIndex]
            val to = state.images[toIndex]
            val images = state.images.toMutableMap()
            if (to != null) images[fromIndex] = to else images.remove(fromIndex)
            if (from != null) images[toIndex] = from else images.remove(toIndex)
            state.copy(images = images, transforms = state.transforms - fromIndex - toIndex)
        }
    }

    fun setSlotImage(slotIndex: Int, path: String) {
        viewModelScope.launch {
            runCatching { copyBitmap(PlatformFile(path).toImageBitmap()) }
                .onSuccess { image ->
                    // A replaced photo starts from the fit again; the old zoom was framed for another picture.
                    updateReady { state ->
                        state.copy(images = state.images + (slotIndex to image), transforms = state.transforms - slotIndex)
                    }
                }
                .onFailure { _messages.tryEmit(tr("Couldn't open that photo")) }
        }
    }

    /** Applies one pinch/drag step to the photo in [slotIndex], measured in a [slotWidth]x[slotHeight] px slot. */
    fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) {
        updateReady { state ->
            val image = state.images[slotIndex] ?: return
            val current = state.transforms[slotIndex] ?: SlotTransform()
            val next = current.applyCollageGesture(
                panX, panY, zoom,
                imageW = image.width.toFloat(),
                imageH = image.height.toFloat(),
                slotW = slotWidth,
                slotH = slotHeight,
            )
            state.copy(transforms = state.transforms + (slotIndex to next))
        }
    }

    fun updateSpace(space: Float) = updateReady { it.copy(space = space) }

    fun updateCorner(corner: Float) = updateReady { it.copy(corner = corner) }

    // A colour takes a background picture off.
    fun updateBackgroundColor(color: Color) = updateReady { it.copy(backgroundColor = color, backgroundArt = null) }

    private val _backgroundArtCategories = MutableStateFlow<LayoutArtLoad>(LayoutArtLoad.Loading)

    /** The server's backgrounds (the layout editor's library), read when the Background tab first opens. */
    internal val backgroundArtCategories: StateFlow<LayoutArtLoad> = _backgroundArtCategories.asStateFlow()

    /** Reads the server's backgrounds, once; a failure can be retried. */
    fun loadBackgroundArt() {
        if (_backgroundArtCategories.value is LayoutArtLoad.Ready) return
        _backgroundArtCategories.value = LayoutArtLoad.Loading
        viewModelScope.launch {
            _backgroundArtCategories.value = runCatching { artLibrary.getCategories(CentralDataConfig.BACKGROUNDS_SUPER_CATEGORY_ID) }
                .fold({ LayoutArtLoad.Ready(it) }, { LayoutArtLoad.Failed })
        }
    }

    /**
     * Puts [art] behind the slots, or takes it off when it is the one in use. A free picture (or any,
     * for a subscriber) is kept on the disk; a premium one a non-subscriber only tries isn't, and
     * makes Done a gate ([needsPremium]).
     */
    internal fun toggleBackgroundArt(art: LayoutArt) {
        val current = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return
        val next = art.takeUnless { it.id == current.backgroundArt?.id }
        if (next != null && (!next.isPremium || settings.isPremium.value)) {
            viewModelScope.launch { imageLoader.loadAsset(next.imageUrl, keep = true) }
        }
        updateReady { it.copy(backgroundArt = next) }
    }

    /** [state]'s background picture at full size, for a bake; null for a colour. */
    private suspend fun backgroundImage(state: CollageState): ImageBitmap? {
        val art = state.backgroundArt ?: return null
        val loaded = imageLoader.loadAsset(art.imageUrl, keep = !art.isPremium || settings.isPremium.value) ?: return null
        return withContext(Dispatchers.Default) { copyBitmap(loaded) }
    }

    fun updateRatio(ratio: CollageRatio) = updateReady { it.copy(ratio = ratio) }

    /**
     * Bakes the current collage and its stickers into one bitmap and hands it to the shared editing
     * session. Suspends while the stickers' SVGs are drawn at full size; false before the collage is
     * in, or while a bake is already under way.
     */
    suspend fun applyCollage(previewWidthPx: Float, spacePx: Float, cornerPx: Float): Boolean {
        val state = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return false
        if (baking) return false
        baking = true
        try {
            val collage = bakeCollage(state, previewWidthPx, spacePx, cornerPx, backgroundImage = backgroundImage(state))
            session.set(bakeStickers(collage, _stickers.value, imageLoader::renderSvg))
            return true
        } finally {
            baking = false
        }
    }

    /**
     * Opens the Stickers tool on the collage as Done would bake it, without the stickers, which the
     * tool draws itself. The measures are the preview's, as for [applyCollage]. [selectedStickerId]
     * is the sticker tapped on the collage, if one was.
     */
    internal fun openStickersTool(previewWidthPx: Float, spacePx: Float, cornerPx: Float, selectedStickerId: Long?) {
        val state = (_uiState.value as? CollageEditorUiState.Ready)?.collage ?: return
        if (stickersSession.value != null || openingStickers || previewWidthPx <= 0f) return
        openingStickers = true
        // The background picture is read (from memory or the disk, as a rule) before the snapshot.
        viewModelScope.launch {
            try {
                val snapshot = bakeCollage(state, previewWidthPx, spacePx, cornerPx, backgroundImage = backgroundImage(state))
                stickersSession.value = StickersToolSession(snapshot, _stickers.value, selectedStickerId, texts = null)
            } finally {
                openingStickers = false
            }
        }
    }

    /** Leaves the Stickers tool without its changes. */
    internal fun closeStickersTool() {
        stickersSession.value = null
    }

    /** Keeps the Stickers tool's stickers over the collage and closes it. */
    internal fun applyStickers(stickers: List<PlacedSticker>) {
        stickersSession.value = null
        if (stickers == _stickers.value) return
        _stickers.value = stickers
        _hasChanges.value = true
    }

    /** Every user edit goes through here (the initial layout is set directly), so it marks the change. */
    private inline fun updateReady(transform: (CollageState) -> CollageState) {
        val current = _uiState.value as? CollageEditorUiState.Ready ?: return
        val next = transform(current.collage)
        if (next != current.collage) _hasChanges.value = true
        _uiState.value = CollageEditorUiState.Ready(next)
    }
}
