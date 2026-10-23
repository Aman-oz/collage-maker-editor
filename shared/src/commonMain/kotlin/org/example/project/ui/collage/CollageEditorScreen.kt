package org.example.project.ui.collage

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.isSpecified
import org.example.project.ui.layouteditor.ArtLibraryBody
import org.example.project.ui.layouteditor.LayoutArt
import org.example.project.ui.layouteditor.LayoutArtLoad
import org.example.project.ui.layouteditor.layoutChrome
import org.example.project.ui.layouteditor.rememberArtTransition
import org.example.project.ui.layouteditor.rememberLayoutArtImage
import org.example.project.ui.shop.ShopTab
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.PanelSegmentedTabs
import org.example.project.i18n.tr
import org.example.project.ui.common.EditorPanelLayout
import org.example.project.ui.common.TopBarButtonSize
import androidx.compose.ui.text.style.TextOverflow
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.rememberDiscardChangesState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickerLayersOverlay
import org.example.project.ui.stickers.StickersToolOverlay
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val SpaceRange = 0f..30f
private val CornerRange = 0f..60f

/** Fixed height of the tool area, so switching tabs never resizes the canvas above it. */
private val ToolPanelHeight = 132.dp

/** The Background panel is taller: its tabs over the server library's grid, tiles and pills. */
private val BackgroundPanelHeight = 300.dp

/**
 * Background swatches. The Background tab is a two-row horizontal grid that fills column by
 * column, so the list is ordered as (top, bottom) pairs.
 */
private val CollageBackgroundColors = listOf(
    Color.White, Color(0xFFE5E5EA),
    Color(0xFF1F2933), Color.Black,
    Color(0xFF1A8FD1), Color(0xFF136C94),
    Color(0xFF14204F), Color(0xFF050B4A),
    Color(0xFFF25565), Color(0xFFCB1F2E),
    Color(0xFF9747F5), Color(0xFFA35CF0),
    Color(0xFFF6AE45), Color(0xFFC3BB91),
    Color(0xFF4CAF50), Color(0xFF14B8A6),
    Color(0xFFEC4899), Color(0xFFFFB300),
)

private enum class CollageTool(private val englishLabel: String) {
    Layouts("Layouts"),
    Border("Border"),
    Background("Background"),
    Ratio("Ratio"),

    /** Not a tab of its own: it opens the photo editor's Stickers tool over the screen. */
    Stickers("Stickers"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * Chrome colors for the collage editor. Unlike the other tools it follows the app's light/dark
 * [MaterialTheme] (like Home) rather than the always-dark `EditorPalette`, with a grey stage so the
 * collage's own (often white) background stays visible.
 */
@Immutable
private data class CollageChrome(
    val isLight: Boolean,
    val surface: Color,
    val stage: Color,
    val emptySlot: Color,
    val buttonBackground: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
)

@Composable
private fun collageChrome(): CollageChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return CollageChrome(
        isLight = isLight,
        surface = scheme.surface,
        stage = if (isLight) Color(0xFFE6E6EB) else Color(0xFF0E0E12),
        emptySlot = if (isLight) Color(0xFFF2F2F5) else Color(0xFF2C2C2E),
        buttonBackground = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
        content = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.55f),
        accent = scheme.primary,
        track = if (isLight) Color(0xFFD9D6E3) else Color(0xFF3A3A40),
    )
}

@Composable
fun CollageEditorScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    layoutName: String? = null,
    onPremium: () -> Unit = {},
    viewModel: CollageEditorViewModel = koinViewModel { parametersOf(imagePaths, layoutName) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerState by viewModel.pickerState.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasChanges.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val backgroundLoad by viewModel.backgroundArtCategories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val discard = rememberDiscardChangesState(hasChanges = hasChanges, onBack = onBack)
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { toastMessage = it } }

    val slotImagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slotIndex = pendingSlotIndex
        if (file != null && slotIndex != null) viewModel.setSlotImage(slotIndex, file.path)
        pendingSlotIndex = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        CollageEditorContent(
            uiState = uiState,
            pickerState = pickerState,
            isSubscribed = isSubscribed,
            stickers = stickers,
            onClose = discard::requestBack,
            onPremium = onPremium,
            onDone = { canvasWidthPx, spacePx, cornerPx ->
                // A premium layout can be laid out by anyone, but creating the collage from it
                // takes a subscription. The collage lives in the ViewModel, so it is still here
                // when they come back from the paywall.
                // A free layout with a premium border width or ratio shows the paywall once, as
                // an offer: the next Done goes through whether or not they subscribed.
                if (viewModel.needsPremium || viewModel.consumePremiumOffer()) {
                    onPremium()
                } else {
                    scope.launch {
                        if (viewModel.applyCollage(canvasWidthPx, spacePx, cornerPx)) onOpenEditor()
                    }
                }
            },
            onTemplateSelected = viewModel::applyTemplate,
            onSwapImages = viewModel::swapImages,
            onSlotTransform = viewModel::transformSlot,
            onRequestSlotImage = { slotIndex ->
                pendingSlotIndex = slotIndex
                slotImagePicker.launch()
            },
            onSpaceChange = viewModel::updateSpace,
            onCornerChange = viewModel::updateCorner,
            onBackgroundColorChange = viewModel::updateBackgroundColor,
            backgroundLoad = backgroundLoad,
            onLoadBackgroundArt = viewModel::loadBackgroundArt,
            onBackgroundArtSelected = viewModel::toggleBackgroundArt,
            onRatioChange = viewModel::updateRatio,
            onOpenStickers = viewModel::openStickersTool,
            modifier = Modifier.liquefiable(liquidState),
        )

        // The photo editor's Stickers tool, over the whole screen.
        StickersToolOverlay(
            session = viewModel.stickersSession.value,
            isPremium = isSubscribed,
            onClose = viewModel::closeStickersTool,
            onApply = viewModel::applyStickers,
            onOpenPremium = onPremium,
        )

        CollageToast(
            message = toastMessage,
            onDismissed = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        DiscardChangesPopup(discard, liquidState)
    }
}

@Composable
private fun CollageEditorContent(
    uiState: CollageEditorUiState,
    pickerState: CollagePickerState,
    isSubscribed: Boolean,
    stickers: List<PlacedSticker>,
    onClose: () -> Unit,
    onPremium: () -> Unit,
    onDone: (canvasWidthPx: Float, spacePx: Float, cornerPx: Float) -> Unit,
    onTemplateSelected: (TemplateItem) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onSlotTransform: (index: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
    onRequestSlotImage: (Int) -> Unit,
    onSpaceChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    backgroundLoad: LayoutArtLoad,
    onLoadBackgroundArt: () -> Unit,
    onBackgroundArtSelected: (LayoutArt) -> Unit,
    onRatioChange: (CollageRatio) -> Unit,
    onOpenStickers: (canvasWidthPx: Float, spacePx: Float, cornerPx: Float, selectedStickerId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = collageChrome()
    // Saveable: back from the paywall or the Shop (the Background tab's search), the same tab is open.
    var selectedTool by rememberSaveable(stateSaver = CollageToolSaver) { mutableStateOf(CollageTool.Layouts) }
    // The Background panel grows to hold the server library, the collage giving way above it.
    val panelHeight by animateDpAsState(
        targetValue = if (selectedTool == CollageTool.Background) BackgroundPanelHeight else ToolPanelHeight,
        animationSpec = tween(260),
        label = "collagePanelHeight",
    )
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    /** Opens the Stickers tool on the collage at the preview's measures, as Done bakes it. */
    fun openStickers(selectedStickerId: Long?) {
        val state = (uiState as? CollageEditorUiState.Ready)?.collage ?: return
        selectedSlot = null
        onOpenStickers(canvasWidthPx, with(density) { state.space.dp.toPx() }, with(density) { state.corner.dp.toPx() }, selectedStickerId)
    }

    EditorPanelLayout(
        panelHeight = panelHeight,
        tabsAbovePanel = true,
        // Beside the collage the column has height to spare, so Layouts and Background grow into it.
        panelFillsSideColumn = true,
        modifier = modifier
            .background(chrome.surface)
            .safeDrawingPadding(),
        topBar = {
            CollageTopBar(
                chrome = chrome,
                onClose = onClose,
                onPremium = onPremium,
                onDone = {
                    val state = (uiState as? CollageEditorUiState.Ready)?.collage
                    if (state != null) {
                        val spacePx = with(density) { state.space.dp.toPx() }
                        val cornerPx = with(density) { state.corner.dp.toPx() }
                        onDone(canvasWidthPx, spacePx, cornerPx)
                    }
                },
                doneEnabled = uiState is CollageEditorUiState.Ready,
            )
        },
        stage = { stageModifier ->
            Column(modifier = stageModifier) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(chrome.stage),
                    contentAlignment = Alignment.Center,
                ) {
                    when (uiState) {
                        CollageEditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

                        is CollageEditorUiState.Ready -> CollagePreview(
                            state = uiState.collage,
                            stickers = stickers,
                            onStickerTapped = { id -> openStickers(id) },
                            chrome = chrome,
                            selectedSlot = selectedSlot,
                            onCanvasWidthPxChanged = { canvasWidthPx = it },
                            onSlotTap = { index ->
                                val hasImage = uiState.collage.images.containsKey(index)
                                selectedSlot = when {
                                    !hasImage -> {
                                        onRequestSlotImage(index)
                                        null
                                    }
                                    selectedSlot == null -> index
                                    selectedSlot == index -> null
                                    else -> {
                                        onSwapImages(selectedSlot!!, index)
                                        null
                                    }
                                }
                            },
                            // Double tap replaces the slot's photo; the selection is dropped so a stale one can't
                            // swap the new photo away on the next tap.
                            onSlotDoubleTap = { index ->
                                selectedSlot = null
                                onRequestSlotImage(index)
                            },
                            onSlotTransform = onSlotTransform,
                        )

                        is CollageEditorUiState.Error -> Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }

                // A premium layout can be tried by anyone; for a non-subscriber this bar under the photo
                // says so and leads to the paywall, as Done does.
                val readyCollage = (uiState as? CollageEditorUiState.Ready)?.collage
                val onPremiumLayout = readyCollage?.template?.isPremium == true || readyCollage?.backgroundArt?.isPremium == true
                ProFeatureBar(visible = onPremiumLayout && !isSubscribed, onClick = onPremium)
                PreventScreenCapture(enabled = onPremiumLayout && !isSubscribed)
            }
        },
        tabs = {
            CollageToolTabs(
                chrome = chrome,
                selected = selectedTool,
                onToolSelected = { tool ->
                    if (tool == CollageTool.Stickers) {
                        openStickers(null)
                    } else {
                        selectedTool = tool
                        selectedSlot = null
                    }
                },
            )
        },
        panel = { sideBySide ->
            // Beside the collage the panel gets the column's whole height: Layouts and Background
            // become vertical grids that fill it, Border and Ratio keep their usual height at the
            // bottom, just above the tabs that open them.
            val fixedHeight = if (sideBySide) Modifier.fillMaxWidth().height(ToolPanelHeight) else Modifier.fillMaxSize()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                if (uiState is CollageEditorUiState.Ready) {
                    val collage = uiState.collage
                    when (selectedTool) {
                        CollageTool.Layouts -> LayoutsTab(
                            chrome = chrome,
                            pickerState = pickerState,
                            vertical = sideBySide,
                            onTemplateSelected = onTemplateSelected,
                        )
                        CollageTool.Border -> BorderTab(
                            chrome = chrome,
                            state = collage,
                            onSpaceChange = onSpaceChange,
                            onCornerChange = onCornerChange,
                            modifier = fixedHeight,
                        )
                        CollageTool.Background -> BackgroundPanel(
                            chrome = chrome,
                            collage = collage,
                            load = backgroundLoad,
                            sideBySide = sideBySide,
                            onLoad = onLoadBackgroundArt,
                            onColorChange = onBackgroundColorChange,
                            onArtSelected = onBackgroundArtSelected,
                        )
                        CollageTool.Ratio -> RatioTab(
                            chrome = chrome,
                            selected = collage.ratio,
                            onRatioChange = onRatioChange,
                            modifier = fixedHeight,
                        )
                        // Opens its tool over the screen instead (see onToolSelected).
                        CollageTool.Stickers -> Unit
                    }
                } else if (uiState is CollageEditorUiState.Loading && selectedTool == CollageTool.Layouts) {
                    // The layout list arrives from the server along with the photos; show the strip's
                    // placeholders instead of an empty panel until then.
                    LayoutsTab(chrome = chrome, pickerState = pickerState, vertical = sideBySide, onTemplateSelected = onTemplateSelected)
                }
            }
        },
    )
}

@Composable
private fun CollagePreview(
    state: CollageState,
    stickers: List<PlacedSticker>,
    onStickerTapped: (Long) -> Unit,
    chrome: CollageChrome,
    selectedSlot: Int?,
    onCanvasWidthPxChanged: (Float) -> Unit,
    onSlotTap: (Int) -> Unit,
    onSlotDoubleTap: (Int) -> Unit,
    onSlotTransform: (index: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The pointerInput blocks outlive recompositions, so read the latest values through state.
    val currentImages by rememberUpdatedState(state.images)
    val currentOnSlotTap by rememberUpdatedState(onSlotTap)
    val currentOnSlotDoubleTap by rememberUpdatedState(onSlotDoubleTap)
    val currentOnSlotTransform by rememberUpdatedState(onSlotTransform)

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val (widthPx, heightPx) = fitAspect(
            maxW = with(density) { maxWidth.toPx() },
            maxH = with(density) { maxHeight.toPx() },
            aspect = state.ratio.aspect,
        )
        LaunchedEffect(widthPx) { onCanvasWidthPxChanged(widthPx) }

        val spacePx = with(density) { state.space.dp.toPx() }
        val cornerPx = with(density) { state.corner.dp.toPx() }
        val geometries = remember(state.template, widthPx, heightPx, spacePx, cornerPx) {
            computeSlotGeometries(state.template, widthPx, heightPx, spacePx, cornerPx)
        }

        val backgroundArt = rememberArtTransition(rememberLayoutArtImage(state.backgroundArt))
        Box(modifier = Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(geometries) {
                        detectTapGestures(
                            onTap = { pos -> hitTestSlot(geometries, pos)?.let { currentOnSlotTap(it.item.index) } },
                            onDoubleTap = { pos -> hitTestSlot(geometries, pos)?.let { currentOnSlotDoubleTap(it.item.index) } },
                        )
                    }
                    .pointerInput(geometries) {
                        detectSlotTransformGestures(
                            geometries = geometries,
                            hasImage = { index -> index in currentImages },
                            onTransform = { g, pan, zoom -> currentOnSlotTransform(g.item.index, pan.x, pan.y, zoom, g.wPx, g.hPx) },
                        )
                    },
            ) {
                drawCollage(
                    geometries = geometries,
                    images = state.images,
                    transforms = state.transforms,
                    background = state.backgroundColor,
                    canvasW = widthPx,
                    canvasH = heightPx,
                    emptySlotColor = chrome.emptySlot,
                    selectedIndex = selectedSlot,
                    selectionColor = chrome.accent,
                    backgroundImage = backgroundArt.current,
                    backgroundAlpha = backgroundArt.progress.value,
                    previousBackgroundImage = backgroundArt.previous.takeIf { backgroundArt.progress.value < 1f },
                )
            }

            // "Add photo" affordance centered in each empty slot.
            for (g in geometries) {
                if (!state.images.containsKey(g.item.index) && g.touchPolygon.isNotEmpty()) {
                    val cx = g.touchPolygon.sumOf { it.x.toDouble() }.toFloat() / g.touchPolygon.size
                    val cy = g.touchPolygon.sumOf { it.y.toDouble() }.toFloat() / g.touchPolygon.size
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = tr("Add photo"),
                        tint = chrome.muted,
                        modifier = Modifier
                            .offset(
                                x = with(density) { cx.toDp() } - 14.dp,
                                y = with(density) { cy.toDp() } - 14.dp,
                            )
                            .size(28.dp),
                    )
                }
            }

            // The Stickers tool's stickers, over the collage as Done bakes them; a tap on one opens the tool on it.
            StickerLayersOverlay(stickers, Modifier.fillMaxSize(), onStickerTapped = onStickerTapped)
        }
    }
}

/**
 * Pinch-to-zoom and drag for the photo in whichever slot the gesture starts on. It is
 * `detectTransformGestures` with the target slot fixed at the first touch, so a drag that wanders
 * across a border keeps moving the same photo. Past touch slop it consumes the moves, which cancels
 * the tap detector — a drag never also selects or swaps. Gestures on empty slots are left alone.
 */
internal suspend fun PointerInputScope.detectSlotTransformGestures(
    geometries: List<SlotGeometry>,
    hasImage: (Int) -> Boolean,
    onTransform: (slot: SlotGeometry, pan: Offset, zoom: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slot = hitTestSlot(geometries, down.position) ?: return@awaitEachGesture
        if (!hasImage(slot.item.index)) return@awaitEachGesture

        val touchSlop = viewConfiguration.touchSlop
        var pastTouchSlop = false
        var zoomSoFar = 1f
        var panSoFar = Offset.Zero
        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (!pastTouchSlop) {
                zoomSoFar *= zoom
                panSoFar += pan
                val zoomMotion = abs(1f - zoomSoFar) * event.calculateCentroidSize(useCurrent = false)
                pastTouchSlop = zoomMotion > touchSlop || panSoFar.getDistance() > touchSlop
            }
            if (pastTouchSlop) {
                if (zoom != 1f || pan != Offset.Zero) onTransform(slot, pan, zoom)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun CollageTopBar(
    chrome: CollageChrome,
    onClose: () -> Unit,
    onPremium: () -> Unit,
    onDone: () -> Unit,
    doneEnabled: Boolean,
) {
    // A Box like ToolTopBar keeps the title centered on the screen; its side padding reserves room
    // for the widest button group so a long title ellipsizes instead of running under it.
    Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Back"),
            onClick = onClose,
            contentColor = chrome.content,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = tr("Collages"),
            color = chrome.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The premium button (48dp) + gap + Done.
            modifier = Modifier.padding(horizontal = 48.dp + 4.dp + TopBarButtonSize + 8.dp),
        )
        Row(modifier = Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            val premiumBounce = rememberSpringBounce()
            IconButton(
                onClick = onPremium,
                interactionSource = premiumBounce.interactionSource,
                modifier = Modifier.springBounce(premiumBounce),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun CollageToolTabs(chrome: CollageChrome, selected: CollageTool, onToolSelected: (CollageTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Scrolls for a narrow phone or a long translation, where the tabs no longer fit.
            .horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CollageTool.entries.forEach { tool ->
            CollageToolTab(chrome = chrome, tool = tool, selected = tool == selected, onClick = { onToolSelected(tool) })
        }
    }
}

@Composable
private fun CollageToolTab(chrome: CollageChrome, tool: CollageTool, selected: Boolean, onClick: () -> Unit) {
    // IntrinsicSize.Max lets the underline match the label's width.
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(IntrinsicSize.Max)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tool.label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) chrome.content else chrome.muted,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) chrome.accent else Color.Transparent),
        )
    }
}

/**
 * The layout picker: a two-row strip that scrolls sideways under the collage, or, with [vertical]
 * (beside it, in landscape), a grid that fills the column and scrolls down.
 */
@Composable
private fun LayoutsTab(
    chrome: CollageChrome,
    pickerState: CollagePickerState,
    vertical: Boolean,
    onTemplateSelected: (TemplateItem) -> Unit,
) {
    when {
        // Placeholder tiles in the strip's own grid, each with a spinner, while the list loads.
        pickerState.isLoading -> CollageOptionGrid(vertical = vertical, cellSize = LayoutThumbnailSize, userScrollEnabled = false) {
            items(LayoutPlaceholderCount) { LayoutThumbnailPlaceholder(chrome) }
        }
        pickerState.error != null && pickerState.templates.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(pickerState.error, color = chrome.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        else -> CollageOptionGrid(vertical = vertical, cellSize = LayoutThumbnailSize) {
            items(pickerState.templates) { template ->
                LayoutThumbnail(
                    chrome = chrome,
                    template = template,
                    selected = template.id == pickerState.selectedTemplateId,
                    onClick = { onTemplateSelected(template) },
                )
            }
        }
    }
}

/** Enough placeholder tiles to fill the strip on a wide screen. */
private const val LayoutPlaceholderCount = 24

private val LayoutThumbnailSize = 56.dp

/**
 * The grid the Layouts and Background tabs lay their tiles out in: two rows scrolling sideways, or,
 * [vertical], as many [cellSize] columns as fit, scrolling down. The tiles keep their own size;
 * in the vertical grid each sits centred in its cell, so the columns spread evenly.
 */
@Composable
private fun CollageOptionGrid(
    vertical: Boolean,
    cellSize: Dp,
    spacing: Dp = 8.dp,
    userScrollEnabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
    content: LazyGridScope.() -> Unit,
) {
    if (vertical) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(cellSize + spacing),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            userScrollEnabled = userScrollEnabled,
            modifier = modifier,
            content = content,
        )
    } else {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            userScrollEnabled = userScrollEnabled,
            modifier = modifier,
            content = content,
        )
    }
}

/** A [LayoutThumbnail]-sized tile with a spinner, shown while the layout list loads. */
@Composable
private fun LayoutThumbnailPlaceholder(chrome: CollageChrome) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .size(LayoutThumbnailSize)
            .clip(RoundedCornerShape(8.dp))
            .background(chrome.muted.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = chrome.accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LayoutThumbnail(chrome: CollageChrome, template: TemplateItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            // Centred in a vertical grid's cell, which can be wider than the tile.
            .wrapContentSize()
            .springBounce(bounce)
            .size(LayoutThumbnailSize)
            .clip(shape)
            .border(1.5.dp, if (selected) chrome.accent else Color.Transparent, shape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = (if (chrome.isLight) template.preview else template.previewDark).orEmpty(),
            contentDescription = template.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (template.isPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(14.dp),
            )
        }
    }
}

@Composable
private fun BorderTab(
    chrome: CollageChrome,
    state: CollageState,
    onSpaceChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        // Two labelled sliders only just fit the panel's fixed height, hence the tight padding.
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BorderSliderRow(
            chrome = chrome,
            icon = Icons.Outlined.SpaceDashboard,
            contentDescription = tr("Border width"),
            value = state.space,
            range = SpaceRange,
            onValueChange = onSpaceChange,
        )
        BorderSliderRow(
            chrome = chrome,
            icon = Icons.Outlined.RoundedCorner,
            contentDescription = tr("Corner radius"),
            value = state.corner,
            range = CornerRange,
            onValueChange = onCornerChange,
        )
    }
}

@Composable
private fun BorderSliderRow(
    chrome: CollageChrome,
    icon: ImageVector,
    contentDescription: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    // Bottom-aligned, with the icon lifted to the middle of the slider's 40dp track: the slider
    // is taller than its track by the row of range labels above it.
    Row(verticalAlignment = Alignment.Bottom) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = chrome.content,
            modifier = Modifier.padding(bottom = 9.dp).size(22.dp),
        )
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            referenceValue = range.start,
            trackColor = chrome.track,
            fillColor = chrome.accent,
            thumbColor = chrome.accent,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 24.dp,
            glassThumb = true,
            glassTint = if (chrome.isLight) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            valueLabel = ::wholeNumberLabel,
            modifier = Modifier.weight(1f),
        )
    }
}

private val BackgroundSwatchSize = 50.dp
private val BackgroundSwatchSpacing = 10.dp

/**
 * The background colours: two rows scrolling sideways, led by the custom colour swatch spanning
 * both, or, with [vertical] (beside the collage, in landscape), a grid that scrolls down, led by
 * that swatch at the colours' own size.
 */
@Composable
private fun BackgroundTab(
    chrome: CollageChrome,
    selected: Color,
    vertical: Boolean,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    // Color.Unspecified while a background picture is in use: then no swatch is ringed.
    val pickCustomColor = rememberColorPickerLauncher(initial = selected.takeIf { it.isSpecified } ?: Color.White, onPicked = onColorChange)
    CollageOptionGrid(vertical = vertical, cellSize = BackgroundSwatchSize, spacing = BackgroundSwatchSpacing, modifier = modifier) {
        // In the strip the custom colour swatch spans both rows, so the colour pairs after it stay
        // aligned. It is the one ringed while the background is a colour of the user's own.
        item(span = { GridItemSpan(if (vertical) 1 else maxLineSpan) }) {
            BackgroundColorSwatch(
                chrome = chrome,
                selected = selected.isSpecified && selected !in CollageBackgroundColors,
                onClick = pickCustomColor,
                height = if (vertical) BackgroundSwatchSize else BackgroundSwatchSize * 2 + BackgroundSwatchSpacing,
            ) { ColorPickerFill(it) }
        }
        items(CollageBackgroundColors) { color ->
            BackgroundColorSwatch(chrome = chrome, selected = color == selected, onClick = { onColorChange(color) }) {
                Box(modifier = it.background(color))
            }
        }
    }
}

private enum class BackgroundPanelTab(private val englishLabel: String) {
    Color("Color"),
    Background("Background"),
    ;

    val label: String get() = tr(englishLabel)
}

private val CollageToolSaver: Saver<CollageTool, String> = Saver(save = { it.name }, restore = { CollageTool.valueOf(it) })
private val BackgroundPanelTabSaver: Saver<BackgroundPanelTab, String> = Saver(save = { it.name }, restore = { BackgroundPanelTab.valueOf(it) })

/**
 * The Background tab, in two tabs like the layout editor's Background panel. Color: the colour
 * swatches. Background: the server's backgrounds ([LayoutArt], cropped to fill the canvas in the
 * colour's place), read when the tab first opens, in the layout editor's library (grid, search
 * button and sub-category tiles, category pills). Tapping the one in use again takes it off; a
 * colour does too. A premium one previews under the Pro bar and gates Done.
 */
@Composable
private fun BackgroundPanel(
    chrome: CollageChrome,
    collage: CollageState,
    load: LayoutArtLoad,
    sideBySide: Boolean,
    onLoad: () -> Unit,
    onColorChange: (Color) -> Unit,
    onArtSelected: (LayoutArt) -> Unit,
) {
    var tab by rememberSaveable(stateSaver = BackgroundPanelTabSaver) {
        mutableStateOf(if (collage.backgroundArt != null) BackgroundPanelTab.Background else BackgroundPanelTab.Color)
    }
    LaunchedEffect(tab) { if (tab == BackgroundPanelTab.Background) onLoad() }
    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        PanelSegmentedTabs(
            labels = BackgroundPanelTab.entries.map { it.label },
            selectedIndex = tab.ordinal,
            onSelected = { tab = BackgroundPanelTab.entries[it] },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        val body = Modifier.fillMaxWidth().weight(1f)
        when (tab) {
            BackgroundPanelTab.Color -> Box(modifier = body, contentAlignment = Alignment.Center) {
                BackgroundTab(
                    chrome = chrome,
                    // A picture in use is no colour of the grid's.
                    selected = if (collage.backgroundArt == null) collage.backgroundColor else Color.Unspecified,
                    vertical = sideBySide,
                    onColorChange = onColorChange,
                    // Under the collage, the two rows at the strip's usual height, centred.
                    modifier = if (sideBySide) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(ToolPanelHeight),
                )
            }
            BackgroundPanelTab.Background -> CompositionLocalProvider(LocalTextPanelSideBySide provides sideBySide) {
                ArtLibraryBody(
                    load = load,
                    selected = collage.backgroundArt,
                    cellAspect = 1f,
                    contentScale = ContentScale.Crop,
                    failedMessage = tr("Couldn't load backgrounds"),
                    shopTab = ShopTab.Backgrounds,
                    chrome = layoutChrome(),
                    onSelected = onArtSelected,
                    onRetry = onLoad,
                    modifier = body.padding(bottom = 10.dp),
                )
            }
        }
    }
}

/** [fill] paints the swatch inside its selection ring; it is handed the modifier that sizes it. */
@Composable
private fun BackgroundColorSwatch(
    chrome: CollageChrome,
    selected: Boolean,
    onClick: () -> Unit,
    height: Dp = BackgroundSwatchSize,
    fill: @Composable (Modifier) -> Unit,
) {
    val outer = RoundedCornerShape(12.dp)
    val inner = RoundedCornerShape(9.dp)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            // Centred in a vertical grid's cell, which can be wider than the swatch.
            .wrapContentSize()
            .springBounce(bounce)
            .size(width = BackgroundSwatchSize, height = height)
            .clip(outer)
            .border(2.dp, if (selected) chrome.accent else Color.Transparent, outer)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(4.dp)
            .clip(inner)
            .border(1.dp, chrome.content.copy(alpha = 0.08f), inner),
    ) {
        fill(Modifier.matchParentSize())
    }
}

@Composable
private fun RatioTab(
    chrome: CollageChrome,
    selected: CollageRatio,
    onRatioChange: (CollageRatio) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        items(CollageRatio.entries) { ratio ->
            RatioOption(chrome = chrome, ratio = ratio, selected = ratio == selected, onClick = { onRatioChange(ratio) })
        }
    }
}

@Composable
private fun RatioOption(chrome: CollageChrome, ratio: CollageRatio, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) chrome.accent else chrome.content
    // Every option's shape is fitted into the same box so a tall and a wide ratio read at a glance.
    val (w, h) = fitAspect(48f, 48f, ratio.aspect)
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(w.dp, h.dp)
                    .border(2.dp, tint, RoundedCornerShape(8.dp)),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ratio.label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
        )
    }
}

@Composable
private fun CollageToast(message: String?, onDismissed: () -> Unit, modifier: Modifier = Modifier) {
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
        modifier = modifier.padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
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
private fun CollageEditorScreenPreview() {
    ThemePreviews {
        CollageEditorContent(
            uiState = CollageEditorUiState.Loading,
            pickerState = CollagePickerState(isLoading = true),
            isSubscribed = false,
            stickers = emptyList(),
            onClose = {},
            onPremium = {},
            onDone = { _, _, _ -> },
            onTemplateSelected = {},
            onSwapImages = { _, _ -> },
            onSlotTransform = { _, _, _, _, _, _ -> },
            onRequestSlotImage = {},
            onSpaceChange = {},
            onCornerChange = {},
            onBackgroundColorChange = {},
            backgroundLoad = LayoutArtLoad.Loading,
            onLoadBackgroundArt = {},
            onBackgroundArtSelected = {},
            onRatioChange = {},
            onOpenStickers = { _, _, _, _ -> },
        )
    }
}
