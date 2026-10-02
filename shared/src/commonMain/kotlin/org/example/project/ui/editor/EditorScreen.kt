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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
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
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.InlineToolHostState
import org.example.project.ui.common.LocalInlineToolHost
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.UndoRedoDoneWidth
import org.example.project.ui.common.rememberDiscardChangesState
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.crop.CropTool
import org.example.project.ui.draw.DrawTool
import org.example.project.ui.emoji.EmojiTool
import org.example.project.ui.filter.FilterTool
import org.example.project.ui.frame.FrameTool
import org.example.project.ui.overlay.OverlayTool
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.ratio.RatioTool
import org.example.project.ui.rotate.RotateTool
import org.example.project.ui.shapereveal.SelectiveBlurTool
import org.example.project.ui.shapereveal.SelectiveSplashTool
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
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_undo

internal enum class EditorTool(private val englishLabel: String, val icon: DrawableResource) {
    Auto("Auto", Res.drawable.ic_auto_editor),
    Crop("Crop", Res.drawable.ic_crop_editor),
    Filter("Filter", Res.drawable.ic_filter_editor),
    Adjust("Adjust", Res.drawable.ic_adjust_editor),
    Overlay("Overlay", Res.drawable.ic_overlay_editor),
    Ratio("Ratio", Res.drawable.ic_ratio_editor),
    Text("Text", Res.drawable.ic_text_editor),
    Sticker("Sticker", Res.drawable.ic_stickers_editor),
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
}

/** Breathing room above and below the photo while no tool is open. */
private val CanvasVerticalPadding = 12.dp

/**
 * One opening of a tool inside the editor. [image] is the photo as it was when the tool opened: the
 * tool keeps editing that snapshot even after its result has gone to the session, so it doesn't
 * re-apply its edit to its own result while it slides away. Identity matters (not a data class):
 * each opening is its own session.
 */
@Stable
internal class ToolSession(val tool: EditorTool, val image: ImageBitmap)

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
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    val discard = rememberDiscardChangesState(
        hasChanges = (uiState as? EditorUiState.Ready)?.canUndo == true,
        onBack = onBack,
    )
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        EditorContent(
            uiState = uiState,
            toolSession = viewModel.toolSession,
            isPremium = isPremium,
            openFilterOnLoad = openFilterOnLoad,
            onClose = discard::requestBack,
            onDone = { if (viewModel.consumePremiumOffer()) onOpenPremium() else onDone() },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onApplyEdit = viewModel::applyEdit,
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
 * @param toolSession the tool the user asked for. It is handed in as the state itself (the
 * ViewModel's, see [EditorViewModel.toolSession]) so the tool's callbacks read it at call time.
 */
@Composable
private fun EditorContent(
    uiState: EditorUiState,
    toolSession: MutableState<ToolSession?>,
    isPremium: Boolean,
    openFilterOnLoad: Boolean,
    onClose: () -> Unit,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onApplyEdit: (ImageBitmap) -> Unit,
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.background)
                .safeDrawingPadding(),
        ) {
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
                EditorTopBar(
                    chrome = chrome,
                    canUndo = ready?.canUndo == true,
                    canRedo = ready?.canRedo == true,
                    onClose = onClose,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onDone = onDone,
                )
            }

            EditorCanvas(
                chrome = chrome,
                uiState = uiState,
                host = host,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            EditorBottomArea(host = host) {
                EditorToolbar(
                    chrome = chrome,
                    selectedTool = null,
                    onToolSelected = { tool ->
                        // Taps that land while the toolbar is already on its way out are ignored.
                        if (session == null && ready != null) session = ToolSession(tool, ready.image)
                    },
                )
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
                    )
                }
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
        EditorTool.Text -> TextTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Sticker -> EmojiTool(image, isPremium, onClose, onApply, onOpenPremium)
        EditorTool.Blur -> BlurTool(image, onClose, onApply)
        EditorTool.SelectiveBlur -> SelectiveBlurTool(image, onClose, onApply)
        EditorTool.Rotate -> RotateTool(image, onClose, onApply)
        EditorTool.Splash -> ColorSplashTool(image, onClose, onApply)
        EditorTool.SelectiveSplash -> SelectiveSplashTool(image, onClose, onApply)
        EditorTool.Draw -> DrawTool(image, onClose, onApply)
        EditorTool.Frame -> FrameTool(image, onClose, onApply)
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

@Composable
private fun EditorTopBar(
    chrome: EditorChrome,
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    // A Box like ToolTopBar keeps the title centered on the screen; its side padding reserves room
    // for the widest button group so a long title ellipsizes instead of running under it.
    Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Back"),
            onClick = onClose,
            contentColor = chrome.icon,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = tr("Edit Photo"),
            color = chrome.icon,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = UndoRedoDoneWidth),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
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

            is EditorUiState.Ready -> Image(
                bitmap = uiState.image,
                contentDescription = tr("Selected photo"),
                modifier = Modifier
                    .fillMaxSize()
                    .insetBy {
                        val inset = lerp(0, host.photoInset.roundToPx(), host.layout.value)
                        IntOffset(inset, inset)
                    },
                contentScale = ContentScale.Fit,
            )

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

@Composable
private fun EditorToolbar(
    chrome: EditorChrome,
    selectedTool: EditorTool?,
    onToolSelected: (EditorTool) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // One arrow, on the side there is more to see: at the right edge until the row is scrolled to
    // its end, then at the left edge pointing back. None when every tool fits on screen.
    val moreEdge by remember {
        derivedStateOf {
            when {
                listState.canScrollForward -> Alignment.CenterEnd
                listState.canScrollBackward -> Alignment.CenterStart
                else -> null
            }
        }
    }

    Box {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(EditorTool.entries) { tool ->
                EditorToolItem(
                    chrome = chrome,
                    tool = tool,
                    selected = tool == selectedTool,
                    onClick = { onToolSelected(tool) },
                )
            }
        }

        // matchParentSize, so the overlay follows the toolbar's height instead of setting it; the
        // two arrow panels then fill that height at their own edge.
        Box(modifier = Modifier.matchParentSize()) {
            ToolbarMoreArrow(
                chrome = chrome,
                visible = moreEdge == Alignment.CenterEnd,
                pointsToEnd = true,
                onClick = { scope.launch { listState.animateScrollToItem(EditorTool.entries.lastIndex) } },
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            ToolbarMoreArrow(
                chrome = chrome,
                visible = moreEdge == Alignment.CenterStart,
                pointsToEnd = false,
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

/** The part of the panel that takes the tap and holds the chevron. */
private val ToolbarArrowWidth = 40.dp

/** The whole panel, fade included: the tools dissolve into the bar's colour across this width. */
private val ToolbarArrowFadeWidth = 76.dp

/** How far the chevron drifts toward the hidden tools and back, as a hint that the row scrolls. */
private val ToolbarArrowNudge = 4.dp

/**
 * The "there are more tools this way" panel at one edge of the toolbar, as tall as the bar: the
 * tools fade out into the bar's own colour (the same brush the language list fades out with at its
 * bottom edge), and a chevron on the solid end keeps nudging toward the hidden tools. A tap on the
 * chevron's strip scrolls the row to that end. The panel slides in from its edge when it appears
 * and back out when the row reaches that end.
 *
 * Only the chevron's strip takes touches; the fade lets them through, so a tool dissolving under
 * it is still tappable.
 */
@Composable
private fun ToolbarMoreArrow(
    chrome: EditorChrome,
    visible: Boolean,
    pointsToEnd: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = if (pointsToEnd) 1f else -1f
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { (it * direction).roundToInt() },
        exit = fadeOut() + slideOutHorizontally { (it * direction).roundToInt() },
    ) {
        val nudge by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        )
        // Clear on the tools' side, fully the bar's colour by the time it reaches the chevron.
        val clear = chrome.background.copy(alpha = 0f)
        val fade = if (pointsToEnd) {
            Brush.horizontalGradient(0f to clear, 0.6f to chrome.background, 1f to chrome.background)
        } else {
            Brush.horizontalGradient(0f to chrome.background, 0.4f to chrome.background, 1f to clear)
        }
        Box(
            modifier = Modifier.width(ToolbarArrowFadeWidth).fillMaxHeight().background(fade),
            contentAlignment = if (pointsToEnd) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier.width(ToolbarArrowWidth).fillMaxHeight().clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (pointsToEnd) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = tr("More tools"),
                    tint = chrome.icon,
                    // Moved in the layer, so the endless nudge never recomposes or re-lays-out the bar.
                    modifier = Modifier.size(26.dp).graphicsLayer { translationX = ToolbarArrowNudge.toPx() * nudge * direction },
                )
            }
        }
    }
}

@Composable
private fun EditorToolItem(chrome: EditorChrome, tool: EditorTool, selected: Boolean, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(60.dp)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
//                .clip(CircleShape)
//                .background(if (selected) chrome.accent else chrome.control)
            ,
            contentAlignment = Alignment.Center,
        ) {
            // The vectors are 46dp with the glyph inset to a 33dp area, so draw them larger than
            // the default 24dp icon size to keep the glyph at roughly Material icon scale.
            Icon(
                painter = painterResource(tool.icon),
                contentDescription = tool.label,
                modifier = Modifier.fillMaxSize(),
                tint = if (selected) chrome.onAccent else chrome.icon,
            )
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            color = if (selected) chrome.accent else chrome.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
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
            toolSession = remember { mutableStateOf(null) },
            isPremium = false,
            openFilterOnLoad = false,
            onClose = {},
            onDone = {},
            onUndo = {},
            onRedo = {},
            onApplyEdit = {},
            onOpenPremium = {},
        )
    }
}
