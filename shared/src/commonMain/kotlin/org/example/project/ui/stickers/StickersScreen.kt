package org.example.project.ui.stickers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.example.project.data.network.NetworkImageLoader
import org.example.project.i18n.tr
import org.example.project.ui.adjust.AdjustmentType
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.ToolDropMillis
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.dropSpec
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.settleSpec
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.PanelSlider
import org.example.project.ui.text.TextItem
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextLayersOverlay
import org.example.project.ui.text.TextNudgePanel
import org.example.project.ui.text.TextPositionPanel
import org.example.project.ui.text.TextSelectionBox
import org.example.project.ui.text.TextSelectionToolbar
import org.example.project.ui.text.TextShadowPanel
import org.example.project.ui.text.TextSliderPanel
import org.example.project.ui.text.TextToolbarHeight
import org.example.project.ui.text.rotatedHalfSize
import org.example.project.ui.text.snapTextRotation
import org.koin.compose.koinInject

/** Something fetched from the sticker library: still on its way, failed, or here. */
internal sealed interface StickerLoad<out T> {
    data object Loading : StickerLoad<Nothing>
    data object Failed : StickerLoad<Nothing>
    data class Ready<T>(val value: T) : StickerLoad<T>
}

/**
 * The editor's Stickers tool. With no sticker selected its panel is the server's sticker library
 * ([StickersRepository]): category pills at the bottom, the category's sub-category tiles over them,
 * and the tile's stickers in a grid under the ✕ / ✓ row; a tap flies one onto the photo. A tap on a
 * placed sticker selects it and swaps the library for the Text tool's floating glass bar of
 * [StickerOption]s, each opening its panel (✕ puts the sticker back as it was, ✓ keeps it).
 *
 * Like the Text tool it doesn't bake: [onApply] gets the stickers, which the editor keeps live over
 * the photo ([initialStickers] is what it opens on, [selectedStickerId] the one tapped there). The
 * editor's live [texts] are drawn over the stage, untouchable here. Premium stickers and effects
 * preview for anyone, but applying them sends a non-subscriber to the paywall.
 */
@Composable
internal fun StickersTool(
    sourceImage: ImageBitmap,
    initialStickers: List<PlacedSticker>,
    selectedStickerId: Long?,
    texts: TextLayers?,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (List<PlacedSticker>) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val repository: StickersRepository = koinInject()

    // Which pill and tile are picked is saveable, so the paywall's round trip comes back to them.
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var subcategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<StickerLoad<List<StickerCategory>>>(StickerLoad.Loading) }
    var subcategories by remember { mutableStateOf<StickerLoad<List<StickerSubcategory>>>(StickerLoad.Loading) }
    var stickers by remember { mutableStateOf<StickerLoad<List<StickerAsset>>>(StickerLoad.Loading) }
    // Bumped by Retry, which reloads whichever level failed.
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        if (categories is StickerLoad.Ready) return@LaunchedEffect
        categories = StickerLoad.Loading
        categories = load { repository.getCategories() }
    }
    val categoryList = (categories as? StickerLoad.Ready)?.value
    val category = categoryList?.let { list -> list.firstOrNull { it.id == categoryId } ?: list.firstOrNull() }

    LaunchedEffect(category?.id, categories, attempt) {
        subcategories = StickerLoad.Loading
        subcategories = if (category != null) load { repository.getSubcategories(category.id) } else categories.emptyOr()
    }
    val subcategoryList = (subcategories as? StickerLoad.Ready)?.value
    val subcategory = subcategoryList?.let { list -> list.firstOrNull { it.id == subcategoryId } ?: list.firstOrNull() }

    LaunchedEffect(subcategory?.id, subcategories, attempt) {
        stickers = StickerLoad.Loading
        stickers = if (subcategory != null) load { repository.getStickers(subcategory) } else subcategories.emptyOr()
    }

    StickersContent(
        sourceImage = sourceImage,
        initialStickers = initialStickers,
        selectedStickerId = selectedStickerId,
        texts = texts,
        isSubscribed = isPremium,
        library = StickerLibrary(
            categories = categories,
            selectedCategoryId = category?.id,
            onCategorySelected = {
                categoryId = it
                subcategoryId = null
            },
            subcategories = subcategories,
            selectedSubcategoryId = subcategory?.id,
            onSubcategorySelected = { subcategoryId = it },
            subcategoryThumbnail = { tile -> runCatching { repository.getStickers(tile).firstOrNull()?.thumbnailUrl }.getOrNull() },
            stickers = stickers,
            onRetry = { attempt++ },
        ),
        onBack = onClose,
        onOpenPremium = onOpenPremium,
        onDone = onApply,
        modifier = modifier,
    )
}

/** The library's state and its lambdas, handed to the content in one piece. */
internal class StickerLibrary(
    val categories: StickerLoad<List<StickerCategory>>,
    val selectedCategoryId: String?,
    val onCategorySelected: (String) -> Unit,
    val subcategories: StickerLoad<List<StickerSubcategory>>,
    val selectedSubcategoryId: String?,
    val onSubcategorySelected: (String) -> Unit,
    val subcategoryThumbnail: suspend (StickerSubcategory) -> String?,
    val stickers: StickerLoad<List<StickerAsset>>,
    val onRetry: () -> Unit,
)

private suspend fun <T> load(block: suspend () -> T): StickerLoad<T> = try {
    StickerLoad.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    StickerLoad.Failed
}

/**
 * What the next level down shows when this one has nothing picked: nothing either once this one has
 * loaded (empty), else this one's own failure or wait, so Retry and the spinner show through.
 */
private fun <T> StickerLoad<*>.emptyOr(): StickerLoad<List<T>> = when (this) {
    is StickerLoad.Ready -> StickerLoad.Ready(emptyList())
    StickerLoad.Failed -> StickerLoad.Failed
    StickerLoad.Loading -> StickerLoad.Loading
}

/** What the tool's panel shows: the library, the options bar, or one option's panel. */
private enum class StickerPanel {
    Library, Options, Reshape, Eraser, Crop, Effects, Fill, Outline, Adjust, Opacity, Shadow, Position, Transform, Size, Rotate, Nudge, Mirror;

    /** A panel for one option, as opposed to the library or the bar. */
    val isOption: Boolean get() = this != Library && this != Options
}

/** Tall enough for the library: its ✕ / ✓ row, the grid and the two rows under it. */
private val StickerPanelHeight = 340.dp
private val ToolbarGap = 10.dp

/** How long the brush preview stays after the size slider stops, and how long it takes to fade. */
private const val BrushPreviewHoldMillis = 600L
private const val BrushPreviewFadeMillis = 250

@Composable
private fun StickersContent(
    sourceImage: ImageBitmap,
    initialStickers: List<PlacedSticker>,
    selectedStickerId: Long?,
    texts: TextLayers?,
    isSubscribed: Boolean,
    library: StickerLibrary,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onDone: (List<PlacedSticker>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val loader: NetworkImageLoader = koinInject()
    // Saveable: the paywall covers the editor, and the stickers placed before it must still be there.
    var placed by rememberSaveable(stateSaver = PlacedStickersSaver) { mutableStateOf(initialStickers) }
    var nextId by rememberSaveable { mutableLongStateOf((initialStickers.maxOfOrNull { it.id } ?: -1L) + 1L) }
    var selectedId by rememberSaveable { mutableStateOf(selectedStickerId) }
    var panelName by rememberSaveable { mutableStateOf(if (selectedStickerId != null) StickerPanel.Options.name else StickerPanel.Library.name) }
    val panel = StickerPanel.valueOf(panelName)
    // Once the panel has switched, the options bar comes back through the two beats, not the tool's opening.
    var panelChanged by remember { mutableStateOf(false) }
    // The sticker and the stacking as they were when an option's panel opened: its ✕ puts them back.
    var beforePanel by remember { mutableStateOf<PlacedSticker?>(null) }
    var orderBeforePanel by remember { mutableStateOf(emptyList<Long>()) }
    var photoSizePx by remember { mutableStateOf(IntSize.Zero) }
    var photoTopLeftInRoot by remember { mutableStateOf(Offset.Zero) }
    var moreMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    val flights = remember { StickerImageFlights() }
    // As in the Text tool: the ⋯ menu is glass over the whole tool, the toolbar glass over the photo.
    val liquidState = rememberLiquidState()
    val photoLiquidState = rememberLiquidState()
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    // The Crop panel's frame, in display fractions of the uncut sticker, and its shape.
    var cropDraft by remember { mutableStateOf<Rect?>(null) }
    var cropRatio by remember { mutableStateOf(StickerCropOptions.first()) }
    // The Eraser panel's mode and brush, the stroke being drawn, and what Redo puts back.
    var eraserRestore by remember { mutableStateOf(false) }
    var brushSize by remember { mutableFloatStateOf(35f) }
    // The brush shown on the sticker while its size slider moves, fading out once it stops.
    val brushPreview = remember { Animatable(0f) }
    var brushSizeChanges by remember { mutableIntStateOf(0) }
    LaunchedEffect(brushSizeChanges) {
        if (brushSizeChanges == 0) return@LaunchedEffect
        brushPreview.snapTo(1f)
        delay(BrushPreviewHoldMillis)
        brushPreview.animateTo(0f, tween(BrushPreviewFadeMillis))
    }
    var liveStroke by remember { mutableStateOf<StickerStroke?>(null) }
    var eraserRedo by remember { mutableStateOf(emptyList<StickerStroke>()) }
    var strokesAtOpen by remember { mutableIntStateOf(0) }
    var adjustType by remember { mutableStateOf(AdjustmentType.Brightness) }

    val selected = placed.firstOrNull { it.id == selectedId }
    val photoAspect = if (photoSizePx.height > 0) photoSizePx.width.toFloat() / photoSizePx.height else 1f
    val showPro = needsPremium(placed) && !isSubscribed

    fun update(id: Long?, transform: (PlacedSticker) -> PlacedSticker) {
        if (id == null) return
        placed = placed.map { if (it.id == id) transform(it) else it }
    }

    fun add(sticker: PlacedSticker) {
        placed = placed + sticker
        selectedId = sticker.id
        nextId = maxOf(nextId, sticker.id) + 1
    }

    fun openPanel(target: StickerPanel) {
        val sticker = selected ?: return
        beforePanel = sticker
        orderBeforePanel = placed.map { it.id }
        panelChanged = true
        when (target) {
            StickerPanel.Crop -> {
                cropDraft = sourceToDisplayCrop(sticker, sticker.crop)
                cropRatio = StickerCropOptions.first()
            }
            StickerPanel.Eraser -> {
                strokesAtOpen = sticker.eraseStrokes.size
                eraserRedo = emptyList()
            }
            // An option with nothing to show yet starts from a visible default; the ✕ takes it away again.
            StickerPanel.Outline -> if (sticker.outlineWidth <= 0f) update(sticker.id) { it.copy(outlineWidth = DefaultOutlineWidth) }
            StickerPanel.Shadow -> if (sticker.shadowOpacity <= 0f) update(sticker.id) { it.copy(shadowOpacity = DefaultStickerShadowOpacity) }
            else -> Unit
        }
        panelName = target.name
    }

    /** Leaves an option's panel for the bar, putting the sticker back as it was when [revert]. */
    fun closePanel(revert: Boolean) {
        val before = beforePanel
        if (revert && before != null) {
            val order = orderBeforePanel
            placed = placed.map { if (it.id == before.id) before else it }
                .sortedBy { sticker -> order.indexOf(sticker.id).let { if (it < 0) Int.MAX_VALUE else it } }
        } else if (panel == StickerPanel.Crop) {
            cropDraft?.let { draft -> update(selectedId) { stickerWithCrop(it, displayToSourceCrop(it, draft), photoAspect) } }
        }
        cropDraft = null
        liveStroke = null
        beforePanel = null
        panelName = (if (placed.any { it.id == selectedId }) StickerPanel.Options else StickerPanel.Library).name
    }

    fun select(id: Long) {
        if (panel.isOption) closePanel(revert = false)
        selectedId = id
        panelName = StickerPanel.Options.name
    }

    fun deselect() {
        if (panel == StickerPanel.Crop || panel == StickerPanel.Eraser) return
        if (panel.isOption) closePanel(revert = false)
        selectedId = null
        panelName = StickerPanel.Library.name
    }

    fun delete(id: Long) {
        placed = placed.filterNot { it.id == id }
        if (selectedId == id) {
            selectedId = null
            panelName = StickerPanel.Library.name
        }
    }

    fun finish() {
        when {
            placed == initialStickers -> onBack()
            needsPremium(placed) && !isSubscribed -> onOpenPremium()
            else -> onDone(placed)
        }
    }

    val pickColor = rememberColorPickerLauncher(
        initial = Color(
            when (panel) {
                StickerPanel.Outline -> selected?.outlineArgb ?: DefaultOutlineArgb
                StickerPanel.Shadow -> selected?.shadowArgb ?: DefaultStickerShadowArgb
                else -> selected?.fillArgb ?: Color.White.toArgb()
            },
        ),
        onPicked = { color ->
            val argb = color.toArgb()
            when (panel) {
                StickerPanel.Outline -> update(selectedId) { it.copy(outlineArgb = argb) }
                StickerPanel.Shadow -> update(selectedId) { it.copy(shadowArgb = argb) }
                else -> update(selectedId) { it.copy(fillArgb = argb) }
            }
        },
    )
    val stickerPreview by produceState<ImageBitmap?>(null, selected?.thumbnailUrl) {
        value = selected?.thumbnailUrl?.takeIf { it.isNotEmpty() }?.let { loader.load(it) }
    }

    // What the stage draws: the selected sticker whole while its crop is being set, and with the
    // stroke being drawn while erasing.
    val stroke = liveStroke
    val shown = placed.map { sticker ->
        when {
            sticker.id != selectedId -> sticker
            panel == StickerPanel.Crop -> stickerWithCrop(sticker, Rect(0f, 0f, 1f, 1f), photoAspect)
            stroke != null -> sticker.copy(eraseStrokes = sticker.eraseStrokes + stroke)
            else -> sticker
        }
    }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
        ToolScaffold(modifier = Modifier.liquefiable(liquidState)) {
            Column(
                modifier = Modifier
                    .toolStage()
                    .background(scheme.onSurface.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        // A tap off the stickers deselects.
                        .pointerInput(Unit) { detectTapGestures { deselect() } }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Sized to the photo itself, so the stickers' fractions are of the displayed image.
                    // Only the inner layer is clipped: the handles and the toolbar may hang past it.
                    Box(
                        modifier = Modifier
                            .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                            .onSizeChanged { photoSizePx = it }
                            .onGloballyPositioned { photoTopLeftInRoot = it.positionInRoot() },
                    ) {
                        fun sizeOf(sticker: PlacedSticker): IntSize {
                            val width = sticker.widthFraction * photoSizePx.width
                            return IntSize(width.roundToInt().coerceAtLeast(1), (width / sticker.aspectRatio).roundToInt().coerceAtLeast(1))
                        }
                        fun topLeftOf(sticker: PlacedSticker): IntOffset {
                            val size = sizeOf(sticker)
                            return IntOffset(
                                (sticker.centerX * photoSizePx.width - size.width / 2f).roundToInt(),
                                (sticker.centerY * photoSizePx.height - size.height / 2f).roundToInt(),
                            )
                        }

                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                            Image(
                                bitmap = sourceImage,
                                contentDescription = tr("Photo preview"),
                                modifier = Modifier.fillMaxSize().liquefiable(photoLiquidState),
                                contentScale = ContentScale.Fit,
                            )
                            for (sticker in shown) key(sticker.id) { StickerLayer(sticker) }
                            if (texts != null) TextLayersOverlay(layers = texts, onTextTapped = null, modifier = Modifier.fillMaxSize())
                        }

                        if (photoSizePx != IntSize.Zero) {
                            when (panel) {
                                StickerPanel.Crop -> {
                                    val full = shown.firstOrNull { it.id == selectedId }
                                    val draft = cropDraft
                                    if (full != null && draft != null) {
                                        StickerCropFrame(
                                            full = full,
                                            crop = draft,
                                            photoSize = photoSizePx,
                                            // Original keeps the uncut sticker's own shape.
                                            ratio = if (cropRatio == OriginalCropOption) full.aspectRatio else cropRatio.ratio,
                                            onCropChange = { cropDraft = it },
                                        )
                                    }
                                }
                                StickerPanel.Eraser -> {
                                    val sticker = selected
                                    if (sticker != null) {
                                        val brushPx = (MinEraserRadius + brushSize / 100f * (MaxEraserRadius - MinEraserRadius)) *
                                            sticker.widthFraction * photoSizePx.width
                                        StickerEraserInput(
                                            brushRadiusPx = brushPx,
                                            onStroke = { points ->
                                                val width = photoSizePx.width.toFloat()
                                                val height = photoSizePx.height.toFloat()
                                                liveStroke = StickerStroke(
                                                    points = points.flatMap { point ->
                                                        val onSticker = photoPointToSticker(sticker, point, width, height)
                                                        listOf(onSticker.x, onSticker.y)
                                                    },
                                                    radius = brushRadiusOnSticker(sticker, brushPx, width),
                                                    restore = eraserRestore,
                                                )
                                            },
                                            previewAt = Offset(sticker.centerX * photoSizePx.width, sticker.centerY * photoSizePx.height),
                                            previewAlpha = { brushPreview.value },
                                            onStrokeEnd = {
                                                liveStroke?.let { done -> update(sticker.id) { it.copy(eraseStrokes = it.eraseStrokes + done) } }
                                                liveStroke = null
                                                eraserRedo = emptyList()
                                            },
                                        )
                                    }
                                }
                                else -> for (sticker in shown) {
                                    key(sticker.id) {
                                        // Snapshotted when a corner drag starts; the drag scales from it.
                                        var scaleStart by remember { mutableStateOf(sticker) }
                                        val isSelected = selectedId == sticker.id
                                        TextSelectionBox(
                                            // While reshaping, its corners are the reshape frame's instead.
                                            selected = isSelected && panel != StickerPanel.Reshape,
                                            locked = false,
                                            boxSizePx = sizeOf(sticker),
                                            rotationDegrees = sticker.rotationDegrees,
                                            showEdgeHandles = false,
                                            onTap = { select(sticker.id) },
                                            onMove = { pan ->
                                                if (selectedId != sticker.id) select(sticker.id)
                                                update(sticker.id) { moveSticker(it, pan.x / photoSizePx.width, pan.y / photoSizePx.height) }
                                            },
                                            onPinch = { zoom, turn ->
                                                update(sticker.id) {
                                                    scaleSticker(it, zoom).copy(rotationDegrees = snapTextRotation(it.rotationDegrees + turn))
                                                }
                                            },
                                            onScaleStart = { scaleStart = placed.first { it.id == sticker.id } },
                                            onScale = { factor ->
                                                update(sticker.id) { scaleSticker(scaleStart, factor).copy(centerX = it.centerX, centerY = it.centerY) }
                                            },
                                            onEdgeDrag = { _, _ -> },
                                            modifier = Modifier.offset { topLeftOf(sticker) },
                                        )
                                    }
                                }
                            }

                            if (panel == StickerPanel.Reshape) {
                                selected?.let { sticker ->
                                    StickerReshapeFrame(
                                        sticker = sticker,
                                        photoSize = photoSizePx,
                                        onCornerDrag = { index, delta ->
                                            update(sticker.id) {
                                                val width = it.widthFraction * photoSizePx.width
                                                moveStickerCorner(it, index, delta, width, width / it.aspectRatio)
                                            }
                                        },
                                    )
                                }
                            }

                            // The quick actions over the selected sticker, while no option's panel (but
                            // Reshape, which works on the sticker itself) is open.
                            if (selected != null && (!panel.isOption || panel == StickerPanel.Reshape)) {
                                var toolbarWidthPx by remember { mutableIntStateOf(0) }
                                TextSelectionToolbar(
                                    photoLiquidState = photoLiquidState,
                                    onDuplicate = { add(duplicateSticker(selected, nextId)) },
                                    onDelete = { delete(selected.id) },
                                    onMore = { moreMenuAnchor = it },
                                    modifier = Modifier
                                        .onSizeChanged { toolbarWidthPx = it.width }
                                        .offset {
                                            // Over the turned sticker's bounding box, or under it when
                                            // there's no room above.
                                            val size = sizeOf(selected)
                                            val halfHeight = rotatedHalfSize(size.width.toFloat(), size.height.toFloat(), selected.rotationDegrees).height
                                            val toolbarHeightPx = TextToolbarHeight.toPx()
                                            val gapPx = ToolbarGap.toPx()
                                            val centerX = selected.centerX * photoSizePx.width
                                            val centerY = selected.centerY * photoSizePx.height
                                            val above = centerY - halfHeight - gapPx - toolbarHeightPx
                                            val y = if (above >= -toolbarHeightPx / 2f) above else centerY + halfHeight + gapPx
                                            IntOffset((centerX - toolbarWidthPx / 2f).roundToInt(), y.roundToInt())
                                        },
                                )
                            }
                        }
                    }
                }
                // Under the photo while a non-subscriber has a premium sticker or effect on it;
                // applying leads to the paywall the same way.
                ProFeatureBar(visible = showPro, onClick = onOpenPremium)
                PreventScreenCapture(enabled = showPro)
            }

            ToolPanel {
                CompositionLocalProvider(LocalTextPanelSideBySide provides sideBySide) {
                    AnimatedContent(
                        targetState = panel,
                        // The editor's two beats: the outgoing view drops away first, then the incoming one rises.
                        transitionSpec = {
                            (slideInVertically(settleSpec(delayMillis = ToolDropMillis)) { it } +
                                fadeIn(settleSpec(delayMillis = ToolDropMillis))) togetherWith
                                (slideOutVertically(dropSpec()) { it } + fadeOut(dropSpec()))
                        },
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .then(if (sideBySide) Modifier.fillMaxHeight() else Modifier.height(StickerPanelHeight)),
                    ) { target ->
                        val sticker = selected
                        val id = selectedId
                        val closeReverting = { closePanel(revert = true) }
                        val closeKeeping = { closePanel(revert = false) }
                        when (target) {
                            StickerPanel.Library -> Column(modifier = Modifier.fillMaxSize()) {
                                ToolTopBar(title = tr("Stickers"), onClose = onBack, onDone = ::finish)
                                StickerGrid(
                                    stickers = library.stickers,
                                    selectedAssetId = sticker?.assetId,
                                    onRetry = library.onRetry,
                                    onStickerTapped = { asset, image, imageBoundsInRoot ->
                                        val newSticker = newPlacedSticker(nextId, asset, photoSizePx.width.toFloat(), photoSizePx.height.toFloat())
                                        // Taken now, so a second tap during the flight gets an id of its own.
                                        nextId += 1
                                        val width = newSticker.widthFraction * photoSizePx.width
                                        val height = width / newSticker.aspectRatio
                                        val landing = Rect(
                                            photoTopLeftInRoot + Offset(
                                                newSticker.centerX * photoSizePx.width - width / 2f,
                                                newSticker.centerY * photoSizePx.height - height / 2f,
                                            ),
                                            Size(width, height),
                                        )
                                        flights.launch(image, fitRect(imageBoundsInRoot, newSticker.aspectRatio), landing) { add(newSticker) }
                                    },
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                )
                                // Raised over the grid by a shadow along its top, so the rows read as a separate tray.
                                Column(modifier = Modifier.fillMaxWidth().topShadow(scheme.onSurface)) {
                                    SubcategoryRow(
                                        subcategories = library.subcategories,
                                        selectedId = library.selectedSubcategoryId,
                                        thumbnailOf = library.subcategoryThumbnail,
                                        onSelected = library.onSubcategorySelected,
                                    )
                                    CategoryRow(
                                        categories = library.categories,
                                        selectedId = library.selectedCategoryId,
                                        onSelected = library.onCategorySelected,
                                    )
                                }
                            }

                            StickerPanel.Options -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                                StickerOptionsBar(
                                    onOptionSelected = { option ->
                                        when (option) {
                                            StickerOption.Duplicate -> sticker?.let { add(duplicateSticker(it, nextId)) }
                                            StickerOption.Reshape -> openPanel(StickerPanel.Reshape)
                                            StickerOption.Eraser -> openPanel(StickerPanel.Eraser)
                                            StickerOption.Crop -> openPanel(StickerPanel.Crop)
                                            StickerOption.Effects -> openPanel(StickerPanel.Effects)
                                            StickerOption.Fill -> openPanel(StickerPanel.Fill)
                                            StickerOption.Outline -> openPanel(StickerPanel.Outline)
                                            StickerOption.Adjust -> openPanel(StickerPanel.Adjust)
                                            StickerOption.Opacity -> openPanel(StickerPanel.Opacity)
                                            StickerOption.Shadow -> openPanel(StickerPanel.Shadow)
                                            StickerOption.Position -> openPanel(StickerPanel.Position)
                                            StickerOption.Transform -> openPanel(StickerPanel.Transform)
                                            StickerOption.Size -> openPanel(StickerPanel.Size)
                                            StickerOption.Rotate -> openPanel(StickerPanel.Rotate)
                                            StickerOption.Nudge -> openPanel(StickerPanel.Nudge)
                                            StickerOption.Mirror -> openPanel(StickerPanel.Mirror)
                                        }
                                    },
                                    // As the Text tool's bar: its ✕ finishes, keeping the stickers.
                                    onClose = ::finish,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    // Back from a panel the bar waits out the first beat before it rises.
                                    cascadeDelayMillis = if (panelChanged) ToolDropMillis else 0,
                                )
                            }

                            StickerPanel.Reshape -> StickerReshapePanel(
                                preview = stickerPreview,
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Eraser -> StickerEraserPanel(
                                restore = eraserRestore,
                                onRestoreChange = { eraserRestore = it },
                                brushSize = brushSize,
                                onBrushSizeChange = {
                                    brushSize = it
                                    brushSizeChanges++
                                },
                                canUndo = (sticker?.eraseStrokes?.size ?: 0) > strokesAtOpen,
                                canRedo = eraserRedo.isNotEmpty(),
                                onUndo = {
                                    val last = sticker?.eraseStrokes?.lastOrNull()
                                    if (last != null && sticker.eraseStrokes.size > strokesAtOpen) {
                                        update(id) { it.copy(eraseStrokes = it.eraseStrokes.dropLast(1)) }
                                        eraserRedo = eraserRedo + last
                                    }
                                },
                                onRedo = {
                                    val next = eraserRedo.lastOrNull()
                                    if (next != null) {
                                        update(id) { it.copy(eraseStrokes = it.eraseStrokes + next) }
                                        eraserRedo = eraserRedo.dropLast(1)
                                    }
                                },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Crop -> StickerCropPanel(
                                selected = cropRatio,
                                onSelected = { choice ->
                                    cropRatio = choice
                                    val full = sticker?.let { stickerWithCrop(it, Rect(0f, 0f, 1f, 1f), photoAspect) }
                                    val ratio = choice.ratio
                                    when {
                                        // The whole sticker, as it came.
                                        choice == OriginalCropOption -> cropDraft = Rect(0f, 0f, 1f, 1f)
                                        ratio != null && full != null -> cropDraft = centeredCrop(ratio, full.aspectRatio)
                                    }
                                },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Effects -> StickerEffectsPanel(
                                preview = stickerPreview,
                                filterLabel = sticker?.filterLabel,
                                intensity = sticker?.filterIntensity ?: 1f,
                                onFilterSelected = { label -> update(id) { it.copy(filterLabel = label, filterIntensity = 1f) } },
                                onIntensityChange = { value -> update(id) { it.copy(filterIntensity = value) } },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Fill -> StickerColorPanel(
                                title = tr("Fill"),
                                slider = PanelSlider(tr("Intensity"), sticker?.fillIntensity ?: 100f) { value ->
                                    update(id) { it.copy(fillIntensity = value) }
                                },
                                selectedArgb = sticker?.fillArgb,
                                onColorSelected = { color -> update(id) { it.copy(fillArgb = color.toArgb()) } },
                                onPickCustom = pickColor,
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Outline -> StickerColorPanel(
                                title = tr("Outline"),
                                slider = PanelSlider(tr("Thickness"), sticker?.outlineWidth ?: 0f) { value ->
                                    update(id) { it.copy(outlineWidth = value) }
                                },
                                selectedArgb = sticker?.outlineArgb,
                                onColorSelected = { color -> update(id) { it.copy(outlineArgb = color.toArgb()) } },
                                onPickCustom = pickColor,
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Adjust -> StickerAdjustPanel(
                                adjust = sticker?.adjust ?: StickerAdjust(),
                                type = adjustType,
                                onTypeSelected = { adjustType = it },
                                onChange = { adjust -> update(id) { it.copy(adjust = adjust) } },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Opacity -> TextSliderPanel(
                                title = tr("Opacity"),
                                sliders = listOf(
                                    PanelSlider(tr("Opacity"), (sticker?.opacity ?: 1f) * 100f) { value ->
                                        update(id) { it.copy(opacity = value / 100f) }
                                    },
                                ),
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            // The Text tool's panels, fed the sticker's shadow / place as a text's.
                            StickerPanel.Shadow -> TextShadowPanel(
                                item = sticker?.shadowAsText(),
                                onChange = { change -> update(id) { it.withShadowOf(change(it.shadowAsText())) } },
                                onPickCustom = pickColor,
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Nudge -> TextNudgePanel(
                                item = sticker?.placeAsText(),
                                onChange = { change -> update(id) { it.withPlaceOf(change(it.placeAsText())) } },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Position -> TextPositionPanel(
                                onArrange = { move -> if (id != null) placed = arrangeStickers(placed, id, move) },
                                onAlign = { align ->
                                    if (photoSizePx.width > 0 && photoSizePx.height > 0) {
                                        update(id) {
                                            val width = it.widthFraction * photoSizePx.width
                                            val half = rotatedHalfSize(width, width / it.aspectRatio, it.rotationDegrees)
                                            alignSticker(it, align, half.width / photoSizePx.width, half.height / photoSizePx.height)
                                        }
                                    }
                                },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Transform -> TextSliderPanel(
                                title = tr("Transform"),
                                sliders = listOf(
                                    PanelSlider(tr("Horizontal"), sticker?.tiltX ?: StickerTiltFlat) { value -> update(id) { it.copy(tiltX = value) } },
                                    PanelSlider(tr("Vertical"), sticker?.tiltY ?: StickerTiltFlat) { value -> update(id) { it.copy(tiltY = value) } },
                                ),
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Size -> TextSliderPanel(
                                title = tr("Size"),
                                sliders = listOf(
                                    PanelSlider(tr("Size"), stickerSizeToSlider(sticker?.widthFraction ?: DefaultStickerSizeFraction)) { value ->
                                        update(id) { it.copy(widthFraction = sliderToStickerSize(value)) }
                                    },
                                ),
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Rotate -> TextSliderPanel(
                                title = tr("Rotate"),
                                sliders = listOf(
                                    PanelSlider(
                                        tr("Angle {0}°", (sticker?.rotationDegrees ?: 0f).roundToInt()),
                                        rotationToSlider(sticker?.rotationDegrees ?: 0f),
                                    ) { value -> update(id) { it.copy(rotationDegrees = snapTextRotation(sliderToRotation(value))) } },
                                ),
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )

                            StickerPanel.Mirror -> StickerMirrorPanel(
                                flippedHorizontally = sticker?.flippedHorizontally == true,
                                flippedVertically = sticker?.flippedVertically == true,
                                onFlipHorizontally = { update(id) { it.copy(flippedHorizontally = !it.flippedHorizontally) } },
                                onFlipVertically = { update(id) { it.copy(flippedVertically = !it.flippedVertically) } },
                                onClose = closeReverting,
                                onDone = closeKeeping,
                            )
                        }
                    }
                }
            }
        }

        // Composed after the editor's handler, so back leaves a menu or an option's panel before the tool.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = moreMenuAnchor != null || panel.isOption,
            onBackCompleted = {
                if (moreMenuAnchor != null) moreMenuAnchor = null else closePanel(revert = true)
            },
        )
        StickerMoreMenu(
            open = moreMenuAnchor != null,
            anchor = moreMenuAnchor?.translate(-rootOffset),
            liquidState = liquidState,
            onAction = { action ->
                moreMenuAnchor = null
                when (action) {
                    StickerMoreAction.FlipHorizontally -> update(selectedId) { it.copy(flippedHorizontally = !it.flippedHorizontally) }
                    StickerMoreAction.FlipVertically -> update(selectedId) { it.copy(flippedVertically = !it.flippedVertically) }
                    StickerMoreAction.ToFront -> selectedId?.let { placed = restackSticker(placed, it, toFront = true) }
                    StickerMoreAction.ToBack -> selectedId?.let { placed = restackSticker(placed, it, toFront = false) }
                }
            },
            onDismiss = { moreMenuAnchor = null },
        )
        StickerImageFlightOverlay(flights)
    }
}

/** The sticker's shadow as a text's, for the Text tool's Shadow panel; [withShadowOf] takes it back. */
private fun PlacedSticker.shadowAsText() = TextItem(
    id = -1L,
    shadowArgb = shadowArgb,
    shadowOpacity = shadowOpacity,
    shadowDx = shadowDx,
    shadowDy = shadowDy,
    shadowBlur = shadowBlur,
)

private fun PlacedSticker.withShadowOf(text: TextItem) = copy(
    shadowArgb = text.shadowArgb,
    shadowOpacity = text.shadowOpacity,
    shadowDx = text.shadowDx,
    shadowDy = text.shadowDy,
    shadowBlur = text.shadowBlur,
)

/** The sticker's place and turn as a text's, for the Text tool's Nudge panel; [withPlaceOf] takes it back. */
private fun PlacedSticker.placeAsText() = TextItem(id = -1L, centerX = centerX, centerY = centerY, rotationDegrees = rotationDegrees)

private fun PlacedSticker.withPlaceOf(text: TextItem) =
    copy(centerX = text.centerX, centerY = text.centerY, rotationDegrees = text.rotationDegrees)

@Preview
@Composable
private fun StickersScreenPreview() {
    ThemePreviews {
        StickersContent(
            sourceImage = ImageBitmap(360, 480),
            initialStickers = emptyList(),
            selectedStickerId = null,
            texts = null,
            isSubscribed = false,
            library = StickerLibrary(
                categories = StickerLoad.Ready(listOf(StickerCategory("1", "Baby"), StickerCategory("2", "Animal"))),
                selectedCategoryId = "1",
                onCategorySelected = {},
                subcategories = StickerLoad.Ready(listOf(StickerSubcategory("a", "1", "Newborn"), StickerSubcategory("b", "1", "Baby Boy"))),
                selectedSubcategoryId = "a",
                onSubcategorySelected = {},
                subcategoryThumbnail = { null },
                stickers = StickerLoad.Loading,
                onRetry = {},
            ),
            onBack = {},
            onOpenPremium = {},
            onDone = {},
        )
    }
}
