package org.example.project.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.adjust.AdjustTool
import org.example.project.ui.auto.AutoTool
import org.example.project.ui.blur.BlurTool
import org.example.project.ui.colorsplash.ColorSplashTool
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.EditorSidePanelWidth
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.InlineToolHostState
import org.example.project.ui.common.LocalInlineToolHost
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.UndoRedoDoneWidth
import org.example.project.ui.common.EditorToolStrip
import org.example.project.ui.common.EditorToolStripGrid
import org.example.project.ui.common.EditorToolStripItem
import org.example.project.ui.common.isSideBySideEditor
import org.example.project.ui.common.rememberDiscardChangesState
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.crop.CropTool
import org.example.project.ui.draw.DrawTool
import org.example.project.ui.emoji.EmojiTool
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickerLayersOverlay
import org.example.project.ui.stickers.StickersTool
import org.example.project.ui.filter.FilterTool
import org.example.project.ui.frame.FrameTool
import org.example.project.ui.overlay.OverlayTool
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.ratio.RatioTool
import org.example.project.ui.rotate.RotateTool
import org.example.project.ui.shapereveal.SelectiveBlurTool
import org.example.project.ui.shapereveal.SelectiveSplashTool
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextLayersOverlay
import org.example.project.ui.text.TextTool
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_adjust_editor
import photocollagemaker.shared.generated.resources.ic_auto_editor
import photocollagemaker.shared.generated.resources.ic_blur_editor
import photocollagemaker.shared.generated.resources.ic_crop_editor
import photocollagemaker.shared.generated.resources.ic_draw_editor
import photocollagemaker.shared.generated.resources.ic_filter_editor
import photocollagemaker.shared.generated.resources.ic_frame_editor
import photocollagemaker.shared.generated.resources.ic_overlay_editor
import photocollagemaker.shared.generated.resources.ic_ratio_editor
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_rotate_editor
import photocollagemaker.shared.generated.resources.ic_s_blur_editor
import photocollagemaker.shared.generated.resources.ic_s_splash_editor
import photocollagemaker.shared.generated.resources.ic_splash_editor
import photocollagemaker.shared.generated.resources.ic_sticker_editor
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_undo
import org.example.project.data.ads.AdPoints
import org.example.project.ui.common.shimmer
import com.ozi.nextgen.compose.ads_manager.remote_config.model.AdPlacementKeys
import com.ozi.nextgen.compose.ads_manager.ads.type.BannerAdSizeType
import com.ozi.nextgen.compose.ads_kmp.BannerAd
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.layout.onSizeChanged

internal enum class EditorTool(private val englishLabel: String, val icon: DrawableResource) {
    Auto("Auto", Res.drawable.ic_auto_editor),
    Crop("Crop", Res.drawable.ic_crop_editor),
    Filter("Filter", Res.drawable.ic_filter_editor),
    Adjust("Adjust", Res.drawable.ic_adjust_editor),
    Overlay("Overlay", Res.drawable.ic_overlay_editor),
    Ratio("Ratio", Res.drawable.ic_ratio_editor),
    Text("Text", Res.drawable.ic_text_editor),
    Emoji("Emoji", Res.drawable.ic_stickers_editor),
    Sticker("Stickers", Res.drawable.ic_sticker_editor),
    Blur("Blur", Res.drawable.ic_blur_editor),
    SelectiveBlur("s-Blur", Res.drawable.ic_s_blur_editor),
    Rotate("Rotate", Res.drawable.ic_rotate_editor),
    Splash("Splash", Res.drawable.ic_splash_editor),
    SelectiveSplash("s-Splash", Res.drawable.ic_s_splash_editor),
    Draw("Draw", Res.drawable.ic_draw_editor),
    Frame("Frame", Res.drawable.ic_frame_editor),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)

    companion object {
        /** Every tool, for the toolbar; read where it is shown, as the labels are translated. */
        val stripItems: List<EditorToolStripItem> get() = entries.map { EditorToolStripItem(it.label, it.icon) }
    }
}

/** Breathing room above and below the photo while no tool is open. */
private val CanvasVerticalPadding = 12.dp

/**
 * One opening of a tool inside the editor. [image] is the photo as it was when the tool opened: the
 * tool keeps editing that snapshot even after its result has gone to the session, so it doesn't
 * re-apply its edit to its own result while it slides away. Identity matters (not a data class):
 * each opening is its own session.
 *
 * The Text and Stickers tools also get the [texts] and [stickers] live over the photo, to open with
 * (each edits its own and shows the other), and the one tapped in the editor ([selectedTextId],
 * [selectedStickerId]) when that is what opened it.
 */
@Stable
internal class ToolSession(
    val tool: EditorTool,
    val image: ImageBitmap,
    val texts: TextLayers? = null,
    val selectedTextId: Long? = null,
    val stickers: List<PlacedSticker> = emptyList(),
    val selectedStickerId: Long? = null,
)

/**
 * Chrome colors for the photo editor. It follows the app's light/dark [MaterialTheme] like the
 * collage and freestyle editors, instead of the always-dark `EditorPalette` the tool screens use.
 */
@Immutable
private data class EditorChrome(
    val background: Color,
    val canvas: Color,
    val control: Color,
    val icon: Color,
    val label: Color,
    val accent: Color,
    val onAccent: Color,
)

@Composable
private fun editorChrome(): EditorChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return EditorChrome(
        background = scheme.surface,
        canvas = if (isLight) Color(0xFFE6E6EB) else Color(0xFF1C1C1E),
        control = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
        icon = scheme.onSurface,
        label = scheme.onSurface.copy(alpha = 0.7f),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
    )
}

@Composable
fun EditorScreen(
    imagePath: String?,
    openFilterOnLoad: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = koinViewModel { parametersOf(imagePath) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    // Bakes the live texts into the photo when something needs it as pixels.
    val textMeasurer = rememberTextMeasurer()

    val discard = rememberDiscardChangesState(
        hasChanges = (uiState as? EditorUiState.Ready)?.canUndo == true,
        onBack = onBack,
    )
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        EditorContent(
            uiState = uiState,
            texts = texts,
            stickers = stickers,
            toolSession = viewModel.toolSession,
            isPremium = isPremium,
            openFilterOnLoad = openFilterOnLoad,
            onClose = discard::requestBack,
            onDone = {
                if (viewModel.consumePremiumOffer()) {
                    onOpenPremium()
                } else {
                    // The save screen reads the session, so the stickers and texts go into the photo first.
                    scope.launch {
                        viewModel.flattenLayers(textMeasurer)
                        onDone()
                    }
                }
            },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onApplyEdit = viewModel::applyEdit,
            onApplyTexts = viewModel::applyTexts,
            onApplyStickers = viewModel::applyStickers,
            onFlatten = { viewModel.flattenLayers(textMeasurer) },
            onOpenPremium = onOpenPremium,
            modifier = Modifier.liquefiable(liquidState),
        )
        DiscardChangesPopup(discard, liquidState)
    }
}

/**
 * The editor and, over it, whichever tool is open. Tools are not destinations: tapping one drops
 * the toolbar away and slides the tool's panel up in its place, with the photo staying put (see
 * [InlineToolHostState] for the choreography). ✓ commits the tool's bitmap through [onApplyEdit],
 * ✕ and system back just close it.
 *
 * The Text and Stickers tools commit [texts] and [stickers] instead ([onApplyTexts],
 * [onApplyStickers]), which stay live over the photo: a tap on one opens its tool again on it. Any
 * other tool opens on the photo with them baked in ([onFlatten]), since it edits pixels and layers
 * left floating over a cropped or turned photo would no longer sit where they were put.
 *
 * @param toolSession the tool the user asked for. It is handed in as the state itself (the
 * ViewModel's, see [EditorViewModel.toolSession]) so the tool's callbacks read it at call time.
 */
@Composable
private fun EditorContent(
    uiState: EditorUiState,
    texts: TextLayers?,
    stickers: List<PlacedSticker>,
    toolSession: MutableState<ToolSession?>,
    isPremium: Boolean,
    openFilterOnLoad: Boolean,
    onClose: () -> Unit,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onApplyEdit: (ImageBitmap) -> Unit,
    onApplyTexts: (TextLayers) -> Unit,
    onApplyStickers: (List<PlacedSticker>) -> Unit,
    onFlatten: suspend () -> ImageBitmap?,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = editorChrome()
    val ready = uiState as? EditorUiState.Ready
    var session by toolSession
    // Created with the session, so an editor that comes back from under the paywall with a tool
    // still open shows it open, instead of replaying the opening.
    val host = remember { InlineToolHostState(initial = session) }
    LaunchedEffect(session) { host.show(session) }

    // Saveable so a recreated editor doesn't open the filter a second time.
    var filterOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ready != null) {
        if (openFilterOnLoad && ready != null && !filterOpened) {
            filterOpened = true
            session = ToolSession(EditorTool.Filter, ready.image)
        }
    }

    // Composed after the screen's discard handler, so with a tool open back closes the tool first.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = session != null,
        onBackCompleted = { session = null },
    )

    val topBar = @Composable { titleAtStart: Boolean ->
        EditorTopBar(
            chrome = chrome,
            titleAtStart = titleAtStart,
            canUndo = ready?.canUndo == true,
            canRedo = ready?.canRedo == true,
            onClose = onClose,
            onUndo = onUndo,
            onRedo = onRedo,
            onDone = onDone,
        )
    }
    val scope = rememberCoroutineScope()
    // Set while the live layers are being baked for a tool, so a second tap doesn't open another.
    var flattening by remember { mutableStateOf(false) }
    val onToolSelected = { tool: EditorTool ->
        // Taps that land while the toolbar is already on its way out are ignored.
        if (session == null && ready != null && !flattening) {
            if (tool == EditorTool.Text || tool == EditorTool.Sticker) {
                session = ToolSession(tool, ready.image, texts, stickers = stickers)
            } else {
                flattening = true
                scope.launch {
                    try {
                        val image = onFlatten() ?: ready.image
                        if (session == null) session = ToolSession(tool, image)
                    } finally {
                        flattening = false
                    }
                }
            }
        }
    }
    val onTextTapped = { id: Long ->
        if (session == null && ready != null && !flattening) {
            session = ToolSession(EditorTool.Text, ready.image, texts, selectedTextId = id, stickers = stickers)
        }
    }
    val onStickerTapped = { id: Long ->
        if (session == null && ready != null && !flattening) {
            session = ToolSession(EditorTool.Sticker, ready.image, texts, stickers = stickers, selectedStickerId = id)
        }
    }

    // The portrait top bar's natural height, and the banner's height including its divider (0 while
    // there is no banner): the banner is pinned right under the top bar's place, over everything.
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var bannerHeightPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.background)
                .safeDrawingPadding(),
        ) {
            if (isSideBySideEditor(maxWidth, maxHeight)) {
                // A phone on its side: the photo on the left at full height, the top bar and every
                // tool in a column on the right. The column has the width the tools' panels take
                // there (see ToolScaffold), so the photo needn't resize as a tool opens, only inset.
                Row(modifier = Modifier.fillMaxSize()) {
                    EditorCanvas(
                        chrome = chrome,
                        uiState = uiState,
                        texts = texts,
                        stickers = stickers,
                        onTextTapped = onTextTapped,
                        onStickerTapped = onStickerTapped,
                        host = host,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    EditorSideColumn(host = host) {
                        topBar(true)
                        EditorToolStripGrid(
                            items = EditorTool.stripItems,
                            onItemSelected = { onToolSelected(EditorTool.entries[it]) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fades with the toolbar, then gives up its height as the tool's layout settles in.
                    Box(
                        modifier = Modifier
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val height = (placeable.height * (1f - host.layout.value)).roundToInt()
                                layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
                            }
                            .graphicsLayer { alpha = 1f - host.editorBars.value },
                    ) {
                        Box(modifier = Modifier.onSizeChanged { topBarHeightPx = it.height }) {
                            topBar(false)
                        }
                    }
                    // Room for the banner (EditorBannerOverlay), which rides up with the top bar as
                    // it folds away but stays visible; the tool's stage keeps the same room on top
                    // (InlineToolHostState.topInsetPx), so the photo never passes under it.
                    Spacer(modifier = Modifier.height(with(LocalDensity.current) { bannerHeightPx.toDp() }))

                    EditorCanvas(
                        chrome = chrome,
                        uiState = uiState,
                        texts = texts,
                        stickers = stickers,
                        onTextTapped = onTextTapped,
                        onStickerTapped = onStickerTapped,
                        host = host,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )

                    EditorBottomArea(host = host) {
                        EditorToolStrip(
                            items = EditorTool.stripItems,
                            onItemSelected = { onToolSelected(EditorTool.entries[it]) },
                            background = chrome.background,
                        )
                    }
                }
            }
        }

        val mounted = host.mounted
        if (mounted != null) {
            key(mounted) {
                CompositionLocalProvider(LocalInlineToolHost provides host) {
                    // `mounted` outlives `session` while the tool slides away; the identity checks
                    // keep a second tap during that slide from closing or applying twice.
                    EditorToolContent(
                        session = mounted,
                        isPremium = isPremium,
                        onOpenPremium = onOpenPremium,
                        onClose = { if (session === mounted) session = null },
                        onApply = { bitmap ->
                            if (session === mounted) {
                                onApplyEdit(bitmap)
                                session = null
                            }
                        },
                        onApplyTexts = { layers ->
                            if (session === mounted) {
                                onApplyTexts(layers)
                                session = null
                            }
                        },
                        onApplyStickers = { placed ->
                            if (session === mounted) {
                                onApplyStickers(placed)
                                session = null
                            }
                        },
                    )
                }
            }
        }

        // Last, so it stays above the open tool too.
        if (!isPremium) {
            EditorBannerOverlay(
                host = host,
                topBarHeightPx = topBarHeightPx,
                background = chrome.background,
                onHeightChanged = {
                    bannerHeightPx = it
                    host.topInsetPx = it
                },
            )
        }
    }
}

/**
 * The editor's adaptive banner, always visible in portrait, right under the top bar. When a tool
 * opens the top bar folds away and the banner rides up into its place on the same
 * [InlineToolHostState.layout] value, so it moves in step with the photo below; it never fades.
 * A thin divider closes it off while it shows. Draws nothing (and reports 0) when the placement is
 * disabled or blocked in Remote Config, the load fails, or the phone is on its side.
 */
@Composable
private fun EditorBannerOverlay(
    host: InlineToolHostState<*>,
    topBarHeightPx: Int,
    background: Color,
    onHeightChanged: (Int) -> Unit,
) {
    DisposableEffect(Unit) { onDispose { onHeightChanged(0) } }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        if (isSideBySideEditor(maxWidth, maxHeight)) {
            SideEffect { onHeightChanged(0) }
            return@BoxWithConstraints
        }
        var adHeightPx by remember { mutableIntStateOf(0) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Read in the layout pass, like the top bar's collapse, so both move frame for frame.
                .offset { IntOffset(0, (topBarHeightPx * (1f - host.layout.value)).roundToInt()) }
                .background(background)
                .onSizeChanged { onHeightChanged(it.height) },
        ) {
            BannerAd(
                placementKey = AdPlacementKeys.INTERNAL_APP_BANNER,
                point = AdPoints.INTERNAL_APP_BANNER_EDITOR,
                adSizeType = BannerAdSizeType.ANCHORED_ADAPTIVE,
                modifier = Modifier.fillMaxWidth().onSizeChanged { adHeightPx = it.height },
                // Fills the space the SDK holds while the banner loads; gone once it loads or fails.
                placeholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .shimmer(),
                    )
                },
            )
            if (adHeightPx > 0) {
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun EditorToolContent(
    session: ToolSession,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onApplyTexts: (TextLayers) -> Unit,
    onApplyStickers: (List<PlacedSticker>) -> Unit,
    onOpenPremium: () -> Unit,
) {
    val image = session.image
    when (session.tool) {
        EditorTool.Auto -> AutoTool(image, onClose, onApply)
        EditorTool.Crop -> CropTool(image, onClose, onApply)
        EditorTool.Filter -> FilterTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Adjust -> AdjustTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Overlay -> OverlayTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Ratio -> RatioTool(image, onClose, onApply)
        EditorTool.Text -> TextTool(
            sourceImage = image,
            initialTexts = session.texts,
            selectedTextId = session.selectedTextId,
            isPremium = isPremium,
            onClose = onClose,
            onApply = onApplyTexts,
            onOpenPremium = onOpenPremium,
            underlay = { StickerLayersOverlay(session.stickers, Modifier.fillMaxSize()) },
        )
        EditorTool.Emoji -> EmojiTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Sticker -> StickersTool(
            sourceImage = image,
            initialStickers = session.stickers,
            selectedStickerId = session.selectedStickerId,
            texts = session.texts,
            isPremium = isPremium,
            onClose = onClose,
            onApply = onApplyStickers,
            onOpenPremium = onOpenPremium,
        )
        EditorTool.Blur -> BlurTool(image, onClose, onApply)
        EditorTool.SelectiveBlur -> SelectiveBlurTool(image, onClose, onApply)
        EditorTool.Rotate -> RotateTool(image, onClose, onApply)
        EditorTool.Splash -> ColorSplashTool(image, onClose, onApply)
        EditorTool.SelectiveSplash -> SelectiveSplashTool(image, onClose, onApply)
        EditorTool.Draw -> DrawTool(image, onClose, onApply)
        EditorTool.Frame -> FrameTool(image, isPremium, onClose, onApply, onOpenPremium)
    }
}

/**
 * The toolbar's slot at the bottom of the editor. Its height runs from the toolbar's own to the open
 * tool's panel height along [InlineToolHostState.layout], so the canvas above ends up exactly the
 * size of the tool's stage, and the toolbar (anchored to the bottom, so the resize doesn't move it)
 * slides off the bottom of the screen along [InlineToolHostState.editorBars]. Both are read in the
 * layout pass, so the animation relayouts without recomposing.
 */
@Composable
private fun EditorBottomArea(host: InlineToolHostState<*>, toolbar: @Composable () -> Unit) {
    val bottomInsetPx = WindowInsets.safeDrawing.getBottom(LocalDensity.current)
    Layout(content = toolbar, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0))
        val panelHeight = host.panelHeightPx.takeIf { it > 0 } ?: placeable.height
        val height = lerp(placeable.height, panelHeight, host.layout.value)
        val slide = ((placeable.height + bottomInsetPx) * host.editorBars.value).roundToInt()
        layout(constraints.maxWidth, height) {
            placeable.place(0, height - placeable.height + slide)
        }
    }
}

/**
 * The landscape editor's right-hand column (top bar and tools). As a tool opens it slides off the
 * right edge along [InlineToolHostState.editorBars], fading as it goes, and the tool's panel slides
 * in from that edge into the same place. Offset in the layout pass rather than by a graphicsLayer
 * translation, which would make the Liquid Glass buttons sample the wrong backdrop.
 */
@Composable
private fun EditorSideColumn(host: InlineToolHostState<*>, content: @Composable ColumnScope.() -> Unit) {
    val endInsetPx = WindowInsets.safeDrawing.getRight(LocalDensity.current, LayoutDirection.Ltr)
    Column(
        modifier = Modifier
            .width(EditorSidePanelWidth)
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(((placeable.width + endInsetPx) * host.editorBars.value).roundToInt(), 0)
                }
            }
            .graphicsLayer { alpha = 1f - host.editorBars.value },
        content = content,
    )
}

/**
 * @param titleAtStart puts the title just after the back button instead of centred. The landscape
 * column is too narrow to centre it clear of the undo / redo / done group, which cut it to "Edit P…".
 */
@Composable
private fun EditorTopBar(
    chrome: EditorChrome,
    titleAtStart: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    val backButton = @Composable { buttonModifier: Modifier ->
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Back"),
            onClick = onClose,
            contentColor = chrome.icon,
            modifier = buttonModifier,
        )
    }
    val title = @Composable { titleModifier: Modifier ->
        Text(
            text = tr("Edit Photo"),
            color = chrome.icon,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = if (titleAtStart) TextAlign.Start else TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = titleModifier,
        )
    }
    val actions = @Composable { actionsModifier: Modifier ->
        Row(
            modifier = actionsModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UndoRedoButton(
                icon = vectorResource(Res.drawable.ic_undo),
                contentDescription = tr("Undo"),
                enabled = canUndo,
                onClick = onUndo,
                tint = chrome.icon,
            )
            UndoRedoButton(
                icon = vectorResource(Res.drawable.ic_redo),
                contentDescription = tr("Redo"),
                enabled = canRedo,
                onClick = onRedo,
                tint = chrome.icon,
            )
            GlassTopBarButton(
                icon = Icons.Filled.Check,
                contentDescription = tr("Done"),
                onClick = onDone,
                style = GlassButtonStyle.Primary,
            )
        }
    }
    if (titleAtStart) {
        Row(modifier = Modifier.topBar(), verticalAlignment = Alignment.CenterVertically) {
            backButton(Modifier)
            title(Modifier.weight(1f).padding(horizontal = 10.dp))
            actions(Modifier)
        }
    } else {
        // A Box like ToolTopBar keeps the title centered on the screen; its side padding reserves room
        // for the widest button group so a long title ellipsizes instead of running under it.
        Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
            backButton(Modifier.align(Alignment.CenterStart))
            title(Modifier.padding(horizontal = UndoRedoDoneWidth))
            actions(Modifier.align(Alignment.CenterEnd))
        }
    }
}

/**
 * The photo. While a tool opens, [InlineToolHostState.layout] moves it from the editor's framing
 * (edge to edge, with [CanvasVerticalPadding]) to the tool stage's (inset on all sides by the tool's
 * `photoInset`), so the stage can fade in exactly on top of it without the photo jumping.
 */
@Composable
private fun EditorCanvas(
    chrome: EditorChrome,
    uiState: EditorUiState,
    texts: TextLayers?,
    stickers: List<PlacedSticker>,
    onTextTapped: (Long) -> Unit,
    onStickerTapped: (Long) -> Unit,
    host: InlineToolHostState<*>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .insetBy { IntOffset(0, lerp(CanvasVerticalPadding.roundToPx(), 0, host.layout.value)) }
            .clip(RoundedCornerShape(2.dp))
            .background(chrome.canvas),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            EditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

            is EditorUiState.Ready -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .insetBy {
                        val inset = lerp(0, host.photoInset.roundToPx(), host.layout.value)
                        IntOffset(inset, inset)
                    },
                contentAlignment = Alignment.Center,
            ) {
                // Sized to the photo itself, so the stickers and texts over it land where they were put on it.
                Box(modifier = Modifier.aspectRatio(uiState.image.width.toFloat() / uiState.image.height)) {
                    Image(
                        bitmap = uiState.image,
                        contentDescription = tr("Selected photo"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    if (stickers.isNotEmpty()) {
                        StickerLayersOverlay(stickers, Modifier.fillMaxSize(), onStickerTapped = onStickerTapped)
                    }
                    if (texts != null) {
                        TextLayersOverlay(layers = texts, onTextTapped = onTextTapped, modifier = Modifier.fillMaxSize())
                    }
                }
            }

            is EditorUiState.Error -> Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

/** `padding`, but with the amount (x = horizontal, y = vertical) read in the layout pass. */
private fun Modifier.insetBy(inset: Density.() -> IntOffset): Modifier = layout { measurable, constraints ->
    val (horizontal, vertical) = inset()
    val placeable = measurable.measure(constraints.offset(-2 * horizontal, -2 * vertical))
    layout(
        constraints.constrainWidth(placeable.width + 2 * horizontal),
        constraints.constrainHeight(placeable.height + 2 * vertical),
    ) {
        placeable.place(horizontal, vertical)
    }
}

@Preview
@Composable
private fun EditorScreenPreview() {
    // Loading state avoids needing a real decoded image or Koin for the preview — the chrome
    // around the canvas (top bar, toolbar) is identical across states.
    ThemePreviews {
        EditorContent(
            uiState = EditorUiState.Loading,
            texts = null,
            stickers = emptyList(),
            toolSession = remember { mutableStateOf(null) },
            isPremium = false,
            openFilterOnLoad = false,
            onClose = {},
            onDone = {},
            onUndo = {},
            onRedo = {},
            onApplyEdit = {},
            onApplyTexts = {},
            onApplyStickers = {},
            onFlatten = { null },
            onOpenPremium = {},
        )
    }
}
