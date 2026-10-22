package org.example.project.ui.layouteditor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import org.example.project.i18n.tr
import org.example.project.ui.collage.detectSlotTransformGestures
import org.example.project.ui.collage.fitAspect
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.collage.hitTestSlot
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.EditorPanelLayout
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.StickerFlightOverlay
import org.example.project.ui.common.StickerFlights
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.rememberDiscardChangesState
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.rememberStickerFlights
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.crop.AspectRatioOption
import org.example.project.ui.freestyle.FreestyleCanvas
import org.example.project.ui.freestyle.FreestyleStickerBaseSizeSp
import org.example.project.ui.freestyle.freestyleChrome
import org.example.project.ui.freestyle.onGestureEnd
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.ratio.RatioTool
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextLayersOverlay
import org.example.project.ui.text.TextToolOverlay
import org.example.project.data.network.NetworkImageLoader
import org.example.project.ui.common.copyBitmap
import org.koin.compose.koinInject
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickerLayersOverlay
import org.example.project.ui.stickers.StickersToolOverlay
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.icon_rotate
import photocollagemaker.shared.generated.resources.icon_mirror
import photocollagemaker.shared.generated.resources.icon_transfom
import org.jetbrains.compose.resources.DrawableResource
import org.example.project.ui.common.settleSpec
import org.example.project.ui.common.dropSpec
import org.example.project.ui.common.ToolDropMillis
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.GlassOptionsBar
import org.example.project.ui.text.GlassBarOption
import org.example.project.ui.filter.PhotoFilter
import org.example.project.ui.filter.FilterPickerTool
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.rememberLazyListState
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_filter_editor
import photocollagemaker.shared.generated.resources.ic_ratio_editor
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_sticker_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_trash
import photocollagemaker.shared.generated.resources.ic_undo
import photocollagemaker.shared.generated.resources.icon_border
import photocollagemaker.shared.generated.resources.icon_canvas
import photocollagemaker.shared.generated.resources.icon_frame
import photocollagemaker.shared.generated.resources.icon_layout
import photocollagemaker.shared.generated.resources.icon_margin

/**
 * Fixed height of the panel area under the canvas, which holds the glass options bar or an option's
 * panel. Every panel fits it, so opening one never resizes the canvas — which would move every
 * sticker and text, since they sit at fractions of it.
 */
private val LayoutPanelHeight = 340.dp

/**
 * The glass bar's options. Text, Stickers, Filter and Ratio open the photo editor's tools over the screen;
 * the rest open a panel in the bar's place. Transform, Mirror, Rotate and Ratio work on the selected
 * photo.
 */
internal enum class LayoutOption(private val englishLabel: String, val icon: DrawableResource) {
    Canvas("Canvas", Res.drawable.icon_canvas),
    Layout("Layout", Res.drawable.icon_layout),
    Margin("Margin", Res.drawable.icon_margin),
    Border("Border", Res.drawable.icon_border),
    Transform("Transform", Res.drawable.icon_transfom),
    Mirror("Mirror", Res.drawable.icon_mirror),
    Rotate("Rotate", Res.drawable.icon_rotate),
    Frame("Frame", Res.drawable.icon_frame),
    Background("Background", Res.drawable.ic_background),
    Text("Text", Res.drawable.ic_text_editor),
    Filter("Filter", Res.drawable.ic_filter_editor),
    Emoji("Emoji", Res.drawable.ic_stickers_editor),
    Sticker("Stickers", Res.drawable.ic_sticker_editor),
    Ratio("Ratio", Res.drawable.ic_ratio_editor),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * Everything the layout editor's controls can do to the layout. Each has a default that does
 * nothing, so a preview passes a bare `object : LayoutEditorActions {}`.
 */
internal interface LayoutEditorActions {
    fun selectTemplate(template: TemplateItem) {}
    fun setCanvas(option: AspectRatioOption) {}
    fun setMargin(value: Float) {}
    fun setSpace(value: Float) {}
    fun setCorner(value: Float) {}
    fun setShadow(value: Float) {}
    fun setBorderWidth(value: Float) {}
    fun setBorderOpacity(value: Float) {}
    fun setBorderColor(color: Color) {}
    fun setFrameArt(art: LayoutArt?) {}
    fun loadFrameArt() {}
    fun setBackgroundArt(art: LayoutArt?) {}
    fun setBackgroundOpacity(value: Float) {}
    fun loadBackgroundArt() {}
    fun setFrameColor(color: Color) {}
    fun setFrameWidth(value: Float) {}
    fun setFrameCorner(value: Float) {}
    fun setBackground(color: Color) {}
    fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) {}
    fun flipPhoto(slotIndex: Int, horizontally: Boolean) {}
    fun setPhotoRotation(slotIndex: Int, degrees: Float) {}
    fun setPhotoTiltX(slotIndex: Int, tilt: Float) {}
    fun setPhotoTiltY(slotIndex: Int, tilt: Float) {}

    /** An option's panel opened: its ✕ ([cancelPanel]) takes back everything from here. */
    fun beginPanel() {}
    fun cancelPanel() {}
    fun endPanel() {}
    fun removePhoto(slotIndex: Int) {}
    fun addSticker(emoji: String) {}
    fun transformLayer(id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) {}
    fun setLayerScaleRotation(id: Long, scale: Float, rotationDegrees: Float) {}
    fun bringToFront(id: Long) {}
    fun removeLayer(id: Long) {}
    fun undo() {}
    fun redo() {}
    fun endGesture() {}
}

/**
 * The layout editor: a collage layout picked on Home or in the Layouts browser, opened with every
 * slot empty and a ＋ in each. A tap on one picks its photo, which can then be dragged and pinched
 * inside it. Done bakes the layout, its stickers and texts into the session and [onDone] opens the
 * photo editor; this screen stays underneath, so Back returns to the same layout.
 */
@Composable
internal fun LayoutEditorScreen(
    layoutName: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LayoutEditorViewModel = koinViewModel { parametersOf(layoutName) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val premiumLayouts by viewModel.premiumLayouts.collectAsStateWithLifecycle()
    val frameLoad by viewModel.frameArtCategories.collectAsStateWithLifecycle()
    val backgroundLoad by viewModel.backgroundArtCategories.collectAsStateWithLifecycle()
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val discard = rememberDiscardChangesState(hasChanges = hasChanges, onBack = onBack)
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var pendingSlot by remember { mutableStateOf<Int?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { toastMessage = it } }

    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slot = pendingSlot
        if (file != null && slot != null) viewModel.setSlotPhoto(slot, file.path)
        pendingSlot = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        LayoutEditorContent(
            uiState = uiState,
            templates = templates,
            premiumLayouts = premiumLayouts,
            frameLoad = frameLoad,
            backgroundLoad = backgroundLoad,
            texts = texts,
            stickers = stickers,
            isSubscribed = isPremium,
            actions = viewModel,
            onBack = discard::requestBack,
            onPremium = onPremium,
            onDone = { previewSizePx ->
                // A premium layout can be filled by anyone, but finishing with it takes a
                // subscription; premium stickers get the paywall once, as an offer. The layout lives
                // in the ViewModel, so it is all still here when they come back.
                if (viewModel.needsPremium || viewModel.consumePremiumOffer()) {
                    onPremium()
                } else {
                    scope.launch {
                        if (viewModel.apply(textMeasurer, previewSizePx.width.toFloat(), density.density)) onDone()
                    }
                }
            },
            onRequestPhoto = { slot ->
                pendingSlot = slot
                photoPicker.launch()
            },
            onOpenText = { previewSizePx, selectedTextId ->
                viewModel.openTextTool(textMeasurer, previewSizePx.width.toFloat(), density.density, selectedTextId)
            },
            onOpenStickers = { previewSizePx, selectedStickerId ->
                viewModel.openStickersTool(textMeasurer, previewSizePx.width.toFloat(), density.density, selectedStickerId)
            },
            onOpenRatio = viewModel::openRatioTool,
            onOpenFilter = { viewModel.openFilterTool(textMeasurer) },
            onMessage = { toastMessage = it },
            modifier = Modifier.liquefiable(liquidState),
        )

        LayoutToast(
            message = toastMessage,
            onDismissed = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // The photo editor's tools, over the whole screen.
        TextToolOverlay(
            session = viewModel.textSession.value,
            isPremium = isPremium,
            onClose = viewModel::closeTextTool,
            onApply = viewModel::applyTexts,
            onOpenPremium = onPremium,
            // The live stickers sit under the texts, as Done bakes them.
            underlay = { StickerLayersOverlay(stickers, Modifier.fillMaxSize()) },
        )
        StickersToolOverlay(
            session = viewModel.stickersSession.value,
            isPremium = isPremium,
            onClose = viewModel::closeStickersTool,
            onApply = viewModel::applyStickers,
            onOpenPremium = onPremium,
        )
        LayoutPhotoToolOverlay(
            session = viewModel.photoTool.value,
            isPremium = isPremium,
            onClose = viewModel::closePhotoTool,
            onApplyRatio = viewModel::applyRatio,
            onApplyFilter = viewModel::applyFilter,
            onOpenPremium = onPremium,
        )

        DiscardChangesPopup(discard, liquidState)
    }
}

@Composable
private fun LayoutEditorContent(
    uiState: LayoutEditorUiState,
    templates: List<TemplateItem>,
    premiumLayouts: Set<String>,
    frameLoad: LayoutArtLoad,
    backgroundLoad: LayoutArtLoad,
    texts: TextLayers?,
    stickers: List<PlacedSticker>,
    isSubscribed: Boolean,
    actions: LayoutEditorActions,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onDone: (previewSizePx: IntSize) -> Unit,
    onRequestPhoto: (slotIndex: Int) -> Unit,
    onOpenText: (previewSizePx: IntSize, selectedTextId: Long?) -> Unit,
    onOpenStickers: (previewSizePx: IntSize, selectedStickerId: Long?) -> Unit,
    onOpenRatio: (slotIndex: Int) -> Unit,
    onOpenFilter: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = layoutChrome()
    val stickerFlights = rememberStickerFlights(FreestyleStickerBaseSizeSp)
    val ready = uiState as? LayoutEditorUiState.Ready
    val edit = ready?.edit
    // The option whose panel is open in the glass bar's place, or null for the bar.
    var openOption by rememberSaveable { mutableStateOf<LayoutOption?>(null) }
    // Once a panel has been open, the bar comes back through the two beats, not the screen's opening.
    var panelChanged by remember { mutableStateOf(false) }
    val optionsListState = rememberLazyListState()
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var previewSizePx by remember { mutableStateOf(IntSize.Zero) }

    // A slot no longer shown, or emptied (by undo, say), can't stay selected.
    val selectedPhotoSlot = selectedSlot?.takeIf { slot ->
        edit != null && slot in edit.photos && edit.template.photoItemList.any { it.index == slot }
    }

    /** Runs [action] on the selected photo, or says to pick one first. */
    fun withSelectedPhoto(action: (Int) -> Unit) {
        val slot = selectedPhotoSlot
        if (slot != null) action(slot) else onMessage(tr("Tap a photo to select it"))
    }

    fun openPanel(option: LayoutOption) {
        actions.beginPanel()
        panelChanged = true
        openOption = option
    }

    /** Closes the open panel, keeping its edits, or with [revert] taking them back. */
    fun closePanel(revert: Boolean) {
        if (revert) actions.cancelPanel() else actions.endPanel()
        openOption = null
    }

    fun onOptionSelected(option: LayoutOption) {
        when (option) {
            LayoutOption.Text -> {
                selectedSlot = null
                selectedLayerId = null
                onOpenText(previewSizePx, null)
            }
            LayoutOption.Sticker -> {
                selectedSlot = null
                selectedLayerId = null
                onOpenStickers(previewSizePx, null)
            }
            LayoutOption.Filter -> onOpenFilter()
            LayoutOption.Ratio -> withSelectedPhoto(onOpenRatio)
            LayoutOption.Transform, LayoutOption.Mirror, LayoutOption.Rotate -> withSelectedPhoto { openPanel(option) }
            else -> openPanel(option)
        }
    }

    // With a panel open, back closes it as its ✕ does. Composed after the screen's own handler, so it goes first.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = openOption != null,
        onBackCompleted = { closePanel(revert = true) },
    )

    // One observer for the whole screen: any finger lift ends the drag or slider step in progress.
    Box(modifier = modifier.fillMaxSize().onGestureEnd(actions::endGesture)) {
        EditorPanelLayout(
            panelHeight = LayoutPanelHeight,
            panelFillsSideColumn = true,
            modifier = Modifier
                .background(chrome.surface)
                // The keyboard is left out on purpose: the Text tool's slides up over the screen, which
                // keeps its size under it (the canvas's size is what its stickers are placed against).
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
            topBar = {
                LayoutTopBar(
                    canUndo = ready?.canUndo == true,
                    canRedo = ready?.canRedo == true,
                    doneEnabled = edit != null && edit.photos.keys.any { slot -> edit.template.photoItemList.any { it.index == slot } },
                    onBack = onBack,
                    onUndo = actions::undo,
                    onRedo = actions::redo,
                    onDone = { onDone(previewSizePx) },
                )
            },
            stage = { stageModifier ->
                Column(modifier = stageModifier) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(chrome.stage)
                            // Taps that reach here (off the canvas) deselect.
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    selectedSlot = null
                                    selectedLayerId = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (uiState) {
                            LayoutEditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)
                            is LayoutEditorUiState.Error -> Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp),
                            )
                            is LayoutEditorUiState.Ready -> LayoutStage(
                                edit = uiState.edit,
                                texts = texts,
                                stickers = stickers,
                                chrome = chrome,
                                selectedSlot = selectedPhotoSlot,
                                selectedLayerId = selectedLayerId,
                                stickerFlights = stickerFlights,
                                actions = actions,
                                onSlotTap = { slot ->
                                    selectedLayerId = null
                                    if (slot in uiState.edit.photos) {
                                        selectedSlot = if (selectedSlot == slot) null else slot
                                    } else {
                                        selectedSlot = null
                                        onRequestPhoto(slot)
                                    }
                                },
                                // Double tap replaces the photo.
                                onSlotDoubleTap = { slot ->
                                    selectedSlot = null
                                    onRequestPhoto(slot)
                                },
                                onOffSlotTap = {
                                    selectedSlot = null
                                    selectedLayerId = null
                                },
                                onLayerSelected = { id ->
                                    selectedSlot = null
                                    selectedLayerId = id
                                    actions.bringToFront(id)
                                },
                                onLayerDeleted = { id ->
                                    actions.removeLayer(id)
                                    selectedLayerId = null
                                },
                                onTextTapped = { id ->
                                    selectedLayerId = null
                                    onOpenText(previewSizePx, id)
                                },
                                onStickerTapped = { id ->
                                    selectedSlot = null
                                    selectedLayerId = null
                                    onOpenStickers(previewSizePx, id)
                                },
                                onPreviewSizeChanged = { previewSizePx = it },
                            )
                        }

                        val photoSlot = selectedPhotoSlot
                        if (photoSlot != null) {
                            SelectedPhotoActions(
                                onReplace = {
                                    selectedSlot = null
                                    onRequestPhoto(photoSlot)
                                },
                                onRemove = {
                                    selectedSlot = null
                                    actions.removePhoto(photoSlot)
                                },
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                            )
                        }
                    }

                    // A premium layout can be tried by anyone; for a non-subscriber this bar under the
                    // canvas says so and leads to the paywall, as Done does.
                    val onPremiumLayout = edit?.template?.title in premiumLayouts || edit?.frameArt?.isPremium == true ||
                        edit?.backgroundArt?.isPremium == true
                    ProFeatureBar(visible = onPremiumLayout && !isSubscribed, onClick = onPremium)
                    PreventScreenCapture(enabled = onPremiumLayout && !isSubscribed)
                }
            },
            panel = { sideBySide ->
                if (edit != null) {
                    CompositionLocalProvider(LocalTextPanelSideBySide provides sideBySide) {
                        // The editor's two beats: the outgoing bar or panel drops away first, then the
                        // incoming one rises.
                        AnimatedContent(
                            targetState = openOption,
                            transitionSpec = {
                                (slideInVertically(settleSpec(delayMillis = ToolDropMillis)) { it } +
                                    fadeIn(settleSpec(delayMillis = ToolDropMillis))) togetherWith
                                    (slideOutVertically(dropSpec()) { it } + fadeOut(dropSpec()))
                            },
                            contentAlignment = Alignment.BottomCenter,
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            label = "layoutPanel",
                        ) { option ->
                            if (option == null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = if (sideBySide) Alignment.Center else Alignment.BottomCenter,
                                ) {
                                    GlassOptionsBar(
                                        options = LayoutOption.entries.map { GlassBarOption(it.label, it.icon) },
                                        onOptionSelected = { onOptionSelected(LayoutOption.entries[it]) },
                                        // The bar's ✕ leaves the editor, as its top bar's back does.
                                        onClose = onBack,
                                        listState = optionsListState,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                        cascadeDelayMillis = if (panelChanged) ToolDropMillis else 0,
                                    )
                                }
                            } else {
                                LayoutOptionPanel(
                                    option = option,
                                    edit = edit,
                                    templates = templates,
                                    premiumLayouts = premiumLayouts,
                                    frameLoad = frameLoad,
                                    backgroundLoad = backgroundLoad,
                                    chrome = chrome,
                                    selectedPhotoSlot = selectedPhotoSlot,
                                    actions = actions,
                                    onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { actions.addSticker(emoji) } },
                                    onClose = { closePanel(revert = true) },
                                    onDone = { closePanel(revert = false) },
                                )
                            }
                        }
                    }
                }
            },
            // The glass bar sits in the panel's place.
            tabs = {},
        )

        StickerFlightOverlay(stickerFlights)
    }
}

/**
 * The canvas, shaped by the Canvas panel and as large as fits: the slots (each empty one ringed in
 * dashes with a ＋), the emoji and stickers over them and the texts over everything, as Done bakes them.
 */
@Composable
private fun LayoutStage(
    edit: LayoutEdit,
    texts: TextLayers?,
    stickers: List<PlacedSticker>,
    chrome: LayoutChrome,
    selectedSlot: Int?,
    selectedLayerId: Long?,
    stickerFlights: StickerFlights,
    actions: LayoutEditorActions,
    onSlotTap: (Int) -> Unit,
    onSlotDoubleTap: (Int) -> Unit,
    onOffSlotTap: () -> Unit,
    onLayerSelected: (Long) -> Unit,
    onLayerDeleted: (Long) -> Unit,
    onTextTapped: (Long) -> Unit,
    onStickerTapped: (Long) -> Unit,
    onPreviewSizeChanged: (IntSize) -> Unit,
) {
    // The pointerInput blocks outlive recompositions, so read the latest values through state.
    val currentPhotos by rememberUpdatedState(edit.photos)
    val currentOnSlotTap by rememberUpdatedState(onSlotTap)
    val currentOnSlotDoubleTap by rememberUpdatedState(onSlotDoubleTap)
    val currentOnOffSlotTap by rememberUpdatedState(onOffSlotTap)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val (widthPx, heightPx) = fitAspect(
            maxW = constraints.maxWidth.toFloat(),
            maxH = constraints.maxHeight.toFloat(),
            aspect = edit.canvasAspect(),
        )
        val slots = remember(edit.template, widthPx, heightPx, edit.margin, edit.space, edit.corner) {
            edit.slots(widthPx, heightPx)
        }

        // The server pictures, drawn by drawLayout as they come in, as the bake draws them; each new
        // one fades in over the one before (the frame settling into place), read only in the draw
        // so the stage isn't recomposed on every frame of it.
        val background = rememberArtTransition(rememberLayoutArtImage(edit.backgroundArt))
        val frame = rememberArtTransition(rememberLayoutArtImage(edit.frameArt, trim = true))

        Box(modifier = Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slots) {
                        detectTapGestures(
                            onTap = { pos ->
                                val slot = hitTestSlot(slots, pos)
                                if (slot != null) currentOnSlotTap(slot.item.index) else currentOnOffSlotTap()
                            },
                            onDoubleTap = { pos ->
                                hitTestSlot(slots, pos)?.let { if (it.item.index in currentPhotos) currentOnSlotDoubleTap(it.item.index) }
                            },
                        )
                    }
                    .pointerInput(slots) {
                        detectSlotTransformGestures(
                            geometries = slots,
                            hasImage = { index -> index in currentPhotos },
                            onTransform = { g, pan, zoom -> actions.transformSlot(g.item.index, pan.x, pan.y, zoom, g.wPx, g.hPx) },
                        )
                    },
            ) {
                val art = LayoutArtImages(
                    background = background.current,
                    frame = frame.current,
                    transition = LayoutArtTransition(
                        previousBackground = background.previous,
                        backgroundProgress = background.progress.value,
                        previousFrame = frame.previous,
                        frameProgress = frame.progress.value,
                    ),
                )
                drawLayout(edit, slots, emptySlot = chrome.emptySlot, art = art)
                drawLayoutGuides(slots, edit.photos.keys, selectedSlot, chrome.accent)
            }


            // The ＋ in the middle of each empty slot. It takes no touches: the tap is the canvas's.
            for (g in slots) {
                if (g.item.index !in edit.photos && g.touchPolygon.isNotEmpty()) {
                    val cx = g.touchPolygon.sumOf { it.x.toDouble() }.toFloat() / g.touchPolygon.size
                    val cy = g.touchPolygon.sumOf { it.y.toDouble() }.toFloat() / g.touchPolygon.size
                    Box(
                        modifier = Modifier
                            .offset(x = with(density) { cx.toDp() } - AddBadgeSize / 2, y = with(density) { cy.toDp() } - AddBadgeSize / 2)
                            .size(AddBadgeSize)
                            .clip(CircleShape)
                            .background(Color(0xFF8E8E93).copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = tr("Add photo"),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            FreestyleCanvas(
                stickerFlights = stickerFlights,
                layers = edit.layers,
                chrome = freestyleChrome(),
                selectedLayerId = selectedLayerId,
                onCanvasSizeChanged = onPreviewSizeChanged,
                // Texts are the Text tool's, over the canvas, not its layers.
                onTextLayerDoubleTapped = {},
                onLayerSelected = onLayerSelected,
                onLayerTransformed = actions::transformLayer,
                onLayerScaleRotated = actions::setLayerScaleRotation,
                onLayerDeleted = onLayerDeleted,
            )
            // The Stickers tool's stickers, over the emoji; a tap on one opens the tool on it.
            StickerLayersOverlay(stickers, Modifier.fillMaxSize(), onStickerTapped = onStickerTapped)
            // Above the stickers, as Done bakes them; a tap on one opens the Text tool on it.
            if (texts != null) {
                TextLayersOverlay(layers = texts, onTextTapped = onTextTapped, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private val AddBadgeSize = 36.dp

/**
 * [art]'s picture for the preview, copied once so it redraws reliably (see `copyBitmap`); null
 * until it is in, or with no [art].
 */
@Composable
private fun rememberLayoutArtImage(art: LayoutArt?, trim: Boolean = false): ImageBitmap? {
    val loader: NetworkImageLoader = koinInject()
    val url = art?.imageUrl
    val image by produceState<ImageBitmap?>(null, url) {
        value = url?.let { loader.loadAsset(it, keep = false) }?.let {
            withContext(Dispatchers.Default) { copyBitmap(it).let { copy -> if (trim) trimFrameArt(copy) else copy } }
        }
    }
    return image
}

/** A picture on the canvas and the one it replaced, while [progress] runs 0 → 1 (see [rememberArtTransition]). */
@Stable
private class ArtTransition(initial: ImageBitmap?) {
    var current by mutableStateOf(initial)
    var previous by mutableStateOf<ImageBitmap?>(null)
    val progress = Animatable(1f)
}

/**
 * Fades [image] in over whatever was shown before it, when it changes (a pick, its removal, undo):
 * the new picture arrives, not the old one vanishing. A picture still downloading keeps the old one
 * up until it is in (see [rememberLayoutArtImage]), so the fade starts with it.
 */
@Composable
private fun rememberArtTransition(image: ImageBitmap?): ArtTransition {
    val transition = remember { ArtTransition(image) }
    LaunchedEffect(image) {
        if (image === transition.current) return@LaunchedEffect
        transition.previous = transition.current
        transition.current = image
        transition.progress.snapTo(0f)
        transition.progress.animateTo(1f, tween(ArtFadeMillis, easing = FastOutSlowInEasing))
        transition.previous = null
    }
    return transition
}

private const val ArtFadeMillis = 420

/** Replace and remove for the selected photo, in a pill over the top of the canvas. */
@Composable
private fun SelectedPhotoActions(onReplace: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.94f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillAction(icon = Icons.Outlined.AddPhotoAlternate, label = tr("Replace"), onClick = onReplace)
        PillAction(icon = vectorResource(Res.drawable.ic_trash), label = tr("Delete"), onClick = onRemove)
    }
}

@Composable
private fun PillAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    Row(
        modifier = Modifier
            .springBounce(bounce)
            .clip(CircleShape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = scheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun LayoutTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    doneEnabled: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // A Box like ToolTopBar keeps the title centered on the screen; its side padding reserves room
    // for the wider undo/redo/done group so a long title ellipsizes instead of running under it.
    Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Back"),
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = tr("Layout"),
            color = scheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TopBarButtonSize * 3),
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            UndoRedoButton(icon = vectorResource(Res.drawable.ic_undo), contentDescription = tr("Undo"), enabled = canUndo, onClick = onUndo)
            Spacer(modifier = Modifier.width(4.dp))
            UndoRedoButton(icon = vectorResource(Res.drawable.ic_redo), contentDescription = tr("Redo"), enabled = canRedo, onClick = onRedo)
            Spacer(modifier = Modifier.width(8.dp))
            GlassTopBarButton(
                icon = Icons.Filled.Check,
                contentDescription = tr("Done"),
                onClick = onDone,
                enabled = doneEnabled,
                style = GlassButtonStyle.Primary,
            )
        }
    }
}

/**
 * The photo editor's Filter (on the whole layout) or Ratio (on one photo) tool over the whole screen
 * while [session] is set, fading in and out like [TextToolOverlay]. Back closes it; composed after
 * the screen's own back handler, it takes back first.
 */
@Composable
private fun LayoutPhotoToolOverlay(
    session: LayoutPhotoToolSession?,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApplyRatio: (ImageBitmap) -> Unit,
    onApplyFilter: (PhotoFilter, Float) -> Unit,
    onOpenPremium: () -> Unit,
) {
    val current by rememberUpdatedState(session)
    var shown by remember { mutableStateOf(session) }
    if (session != null) shown = session
    AnimatedVisibility(visible = session != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(180))) {
        shown?.let { opened ->
            key(opened) {
                // The checks keep a tap landing while the tool fades away from closing or applying twice.
                val close = { if (current === opened) onClose() }
                val applyRatio = { image: ImageBitmap -> if (current === opened) onApplyRatio(image) }
                val applyFilter = { filter: PhotoFilter, intensity: Float -> if (current === opened) onApplyFilter(filter, intensity) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        // Keeps a tap on the tool's empty parts from reaching the layout under it.
                        .pointerInput(Unit) { detectTapGestures {} },
                ) {
                    when (opened.tool) {
                        LayoutPhotoTool.Filter -> FilterPickerTool(
                            sourceImage = opened.image,
                            isPremium = isPremium,
                            initialFilterLabel = opened.filterLabel,
                            initialIntensity = opened.filterIntensity,
                            onClose = close,
                            onApply = applyFilter,
                            onOpenPremium = onOpenPremium,
                        )
                        LayoutPhotoTool.Ratio -> RatioTool(opened.image, close, applyRatio)
                    }
                }
            }
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = session != null,
        onBackCompleted = onClose,
    )
}

@Composable
private fun LayoutToast(message: String?, onDismissed: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2200)
            onDismissed()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(bottom = 120.dp, start = 24.dp, end = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Preview
@Composable
private fun LayoutEditorPreview() {
    ThemePreviews {
        LayoutEditorContent(
            uiState = LayoutEditorUiState.Loading,
            templates = emptyList(),
            premiumLayouts = emptySet(),
            frameLoad = LayoutArtLoad.Loading,
            backgroundLoad = LayoutArtLoad.Loading,
            texts = null,
            stickers = emptyList(),
            isSubscribed = false,
            actions = object : LayoutEditorActions {},
            onBack = {},
            onPremium = {},
            onDone = {},
            onRequestPhoto = {},
            onOpenText = { _, _ -> },
            onOpenStickers = { _, _ -> },
            onOpenRatio = {},
            onOpenFilter = {},
            onMessage = {},
        )
    }
}
