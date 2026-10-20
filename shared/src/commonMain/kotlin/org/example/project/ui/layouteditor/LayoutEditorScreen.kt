package org.example.project.ui.layouteditor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BorderOuter
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.Tune
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
import org.example.project.ui.filter.FilterTool
import org.example.project.ui.freestyle.FreestyleCanvas
import org.example.project.ui.freestyle.FreestyleStickerBaseSizeSp
import org.example.project.ui.freestyle.freestyleChrome
import org.example.project.ui.freestyle.onGestureEnd
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.ratio.RatioTool
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextLayersOverlay
import org.example.project.ui.text.TextToolOverlay
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_filter_editor
import photocollagemaker.shared.generated.resources.ic_frame_editor
import photocollagemaker.shared.generated.resources.ic_padding
import photocollagemaker.shared.generated.resources.ic_ratio_editor
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_trash
import photocollagemaker.shared.generated.resources.ic_undo

/**
 * Fixed height of the panel above the tabs. Every panel fits it, so switching never resizes the
 * canvas — which would move every sticker and text, since they sit at fractions of it.
 */
private val ToolPanelHeight = 176.dp

/** The bottom bar's tools. Text, Filter and Ratio open the photo editor's tools rather than a panel. */
internal enum class LayoutTool(private val englishLabel: String) {
    Canvas("Canvas"),
    Layout("Layout"),
    Edit("Edit"),
    Background("Background"),
    Text("Text"),
    Filter("Filter"),
    Sticker("Emoji"),
    Ratio("Ratio"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/** What the Edit tool's own bar, which slides up in place of the tools, offers. */
internal enum class LayoutEditOption(private val englishLabel: String) {
    Margin("Margin"),
    Border("Border"),
    Frame("Frame"),
    Transform("Transform"),
    ;

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
    fun setFrameColor(color: Color) {}
    fun setFrameWidth(value: Float) {}
    fun setFrameCorner(value: Float) {}
    fun setBackground(color: Color) {}
    fun transformSlot(slotIndex: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) {}
    fun flipPhoto(slotIndex: Int, horizontally: Boolean) {}
    fun setPhotoRotation(slotIndex: Int, degrees: Float) {}
    fun setPhotoStretchX(slotIndex: Int, stretch: Float) {}
    fun setPhotoStretchY(slotIndex: Int, stretch: Float) {}
    fun resetPhotoTransform(slotIndex: Int) {}
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
    val texts by viewModel.texts.collectAsStateWithLifecycle()
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
            texts = texts,
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
                    viewModel.apply(textMeasurer, previewSizePx.width.toFloat(), density.density)
                    onDone()
                }
            },
            onRequestPhoto = { slot ->
                pendingSlot = slot
                photoPicker.launch()
            },
            onOpenText = { previewSizePx, selectedTextId ->
                viewModel.openTextTool(textMeasurer, previewSizePx.width.toFloat(), density.density, selectedTextId)
            },
            onOpenPhotoTool = viewModel::openPhotoTool,
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
        )
        LayoutPhotoToolOverlay(
            session = viewModel.photoTool.value,
            isPremium = isPremium,
            onClose = viewModel::closePhotoTool,
            onApply = viewModel::applyPhotoTool,
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
    texts: TextLayers?,
    isSubscribed: Boolean,
    actions: LayoutEditorActions,
    onBack: () -> Unit,
    onPremium: () -> Unit,
    onDone: (previewSizePx: IntSize) -> Unit,
    onRequestPhoto: (slotIndex: Int) -> Unit,
    onOpenText: (previewSizePx: IntSize, selectedTextId: Long?) -> Unit,
    onOpenPhotoTool: (LayoutPhotoTool, slotIndex: Int) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = layoutChrome()
    val stickerFlights = rememberStickerFlights(FreestyleStickerBaseSizeSp)
    val ready = uiState as? LayoutEditorUiState.Ready
    val edit = ready?.edit
    var tool by rememberSaveable { mutableStateOf(LayoutTool.Layout) }
    var editOption by rememberSaveable { mutableStateOf(LayoutEditOption.Margin) }
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

    // One observer for the whole screen: any finger lift ends the drag or slider step in progress.
    Box(modifier = modifier.fillMaxSize().onGestureEnd(actions::endGesture)) {
        EditorPanelLayout(
            panelHeight = ToolPanelHeight,
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
                    val onPremiumLayout = edit?.template?.title in premiumLayouts
                    ProFeatureBar(visible = onPremiumLayout && !isSubscribed, onClick = onPremium)
                    PreventScreenCapture(enabled = onPremiumLayout && !isSubscribed)
                }
            },
            panel = {
                if (edit != null) {
                    LayoutToolPanel(
                        tool = tool,
                        editOption = editOption,
                        edit = edit,
                        templates = templates,
                        premiumLayouts = premiumLayouts,
                        chrome = chrome,
                        selectedPhotoSlot = selectedPhotoSlot,
                        actions = actions,
                        onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { actions.addSticker(emoji) } },
                    )
                }
            },
            tabs = {
                // The Edit tool's options slide up from the bottom in place of the tools, and back down.
                AnimatedContent(
                    targetState = tool == LayoutTool.Edit,
                    transitionSpec = {
                        (slideInVertically(tween(260)) { it } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(220)) { it } + fadeOut(tween(160)))
                    },
                    label = "layoutTabs",
                ) { editing ->
                    if (editing) {
                        EditOptionTabs(
                            selected = editOption,
                            onSelected = { editOption = it },
                            onBack = { tool = LayoutTool.Layout },
                        )
                    } else {
                        LayoutToolTabs(
                            selected = tool,
                            enabled = edit != null,
                            onSelected = { picked ->
                                when (picked) {
                                    LayoutTool.Text -> {
                                        selectedSlot = null
                                        selectedLayerId = null
                                        onOpenText(previewSizePx, null)
                                    }
                                    LayoutTool.Filter -> withSelectedPhoto { onOpenPhotoTool(LayoutPhotoTool.Filter, it) }
                                    LayoutTool.Ratio -> withSelectedPhoto { onOpenPhotoTool(LayoutPhotoTool.Ratio, it) }
                                    else -> tool = picked
                                }
                            },
                            needsPhoto = selectedPhotoSlot == null,
                        )
                    }
                }
            },
        )

        StickerFlightOverlay(stickerFlights)
    }
}

/**
 * The canvas, shaped by the Canvas panel and as large as fits: the slots (each empty one ringed in
 * dashes with a ＋), the stickers over them and the texts over everything, as Done bakes them.
 */
@Composable
private fun LayoutStage(
    edit: LayoutEdit,
    texts: TextLayers?,
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
                drawLayout(edit, slots, emptySlot = chrome.emptySlot)
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
            // Above the stickers, as Done bakes them; a tap on one opens the Text tool on it.
            if (texts != null) {
                TextLayersOverlay(layers = texts, onTextTapped = onTextTapped, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private val AddBadgeSize = 36.dp

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
 * The tools, scrolling sideways. Filter and Ratio work on the selected photo, so they are dimmed
 * while [needsPhoto]; a tap then says to select one rather than doing nothing.
 */
@Composable
private fun LayoutToolTabs(
    selected: LayoutTool,
    enabled: Boolean,
    needsPhoto: Boolean,
    onSelected: (LayoutTool) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (tool in LayoutTool.entries) {
            val isAction = tool == LayoutTool.Text || tool == LayoutTool.Filter || tool == LayoutTool.Ratio
            BottomTab(
                icon = tool.icon(),
                label = tool.label,
                selected = !isAction && tool == selected,
                enabled = enabled,
                dimmed = needsPhoto && (tool == LayoutTool.Filter || tool == LayoutTool.Ratio),
                onClick = { onSelected(tool) },
            )
        }
    }
}

@Composable
private fun LayoutTool.icon(): ImageVector = when (this) {
    LayoutTool.Canvas -> Icons.Outlined.AspectRatio
    LayoutTool.Layout -> Icons.Outlined.SpaceDashboard
    LayoutTool.Edit -> Icons.Outlined.Tune
    LayoutTool.Background -> vectorResource(Res.drawable.ic_background)
    LayoutTool.Text -> vectorResource(Res.drawable.ic_text_editor)
    LayoutTool.Filter -> vectorResource(Res.drawable.ic_filter_editor)
    LayoutTool.Sticker -> vectorResource(Res.drawable.ic_stickers_editor)
    LayoutTool.Ratio -> vectorResource(Res.drawable.ic_ratio_editor)
}

/** The Edit tool's bar: back to the tools, then its four options. */
@Composable
private fun EditOptionTabs(selected: LayoutEditOption, onSelected: (LayoutEditOption) -> Unit, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTab(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            label = tr("Back"),
            selected = false,
            onClick = onBack,
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (option in LayoutEditOption.entries) {
                BottomTab(
                    icon = when (option) {
                        LayoutEditOption.Margin -> vectorResource(Res.drawable.ic_padding)
                        LayoutEditOption.Border -> Icons.Outlined.BorderOuter
                        LayoutEditOption.Frame -> vectorResource(Res.drawable.ic_frame_editor)
                        LayoutEditOption.Transform -> Icons.Outlined.Transform
                    },
                    label = option.label,
                    selected = option == selected,
                    onClick = { onSelected(option) },
                )
            }
        }
    }
}

@Composable
private fun BottomTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    dimmed: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        selected -> scheme.primary
        enabled && !dimmed -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.35f)
    }
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/**
 * The photo editor's Filter or Ratio tool over the whole screen while [session] is set, fading in
 * and out like [TextToolOverlay]. Back closes it; composed after the screen's own back handler, it
 * takes back first.
 */
@Composable
private fun LayoutPhotoToolOverlay(
    session: LayoutPhotoToolSession?,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
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
                val apply = { image: ImageBitmap -> if (current === opened) onApply(image) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        // Keeps a tap on the tool's empty parts from reaching the layout under it.
                        .pointerInput(Unit) { detectTapGestures {} },
                ) {
                    when (opened.tool) {
                        LayoutPhotoTool.Filter -> FilterTool(opened.image, isPremium, close, apply, onOpenPremium)
                        LayoutPhotoTool.Ratio -> RatioTool(opened.image, close, apply)
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
            texts = null,
            isSubscribed = false,
            actions = object : LayoutEditorActions {},
            onBack = {},
            onPremium = {},
            onDone = {},
            onRequestPhoto = {},
            onOpenText = { _, _ -> },
            onOpenPhotoTool = { _, _ -> },
            onMessage = {},
        )
    }
}
