package org.example.project.ui.bgremover

import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.rememberDiscardChangesState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.AppLog
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.GlassDialogHost
import org.example.project.ui.common.MaskBrushSizeDefault
import org.example.project.ui.common.MaskBrushSizeRange
import org.example.project.ui.common.MaskStroke
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.UndoRedoButtonSize
import org.example.project.ui.common.UndoRedoIconSize
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.maskBrushRadiusFraction
import org.example.project.ui.common.pointerInputPressed
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_eraser_ai
import photocollagemaker.shared.generated.resources.ic_eraser_auto
import photocollagemaker.shared.generated.resources.ic_eraser_brush
import photocollagemaker.shared.generated.resources.ic_eraser_magic
import photocollagemaker.shared.generated.resources.ic_eraser_manual
import photocollagemaker.shared.generated.resources.ic_recover
import photocollagemaker.shared.generated.resources.ic_recover_magic
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo
import photocollagemaker.shared.generated.resources.ic_zoom

/**
 * Preview-only backdrops for checking the cut edge against light and dark, cycled in this order by
 * the chip over the photo. `null` shows a transparency checkerboard. The real background is chosen
 * in the next step.
 */
private val BackdropCycle = listOf(null, Color.White, Color(0xFF1F1D24), Color(0xFFD6D3DE))

/** The backdrop after [current] in [BackdropCycle], wrapping back to the checkerboard. */
private fun nextBackdrop(current: Color?): Color? =
    BackdropCycle[(BackdropCycle.indexOf(current) + 1) % BackdropCycle.size]

private val CheckerLight = Color.White
private val CheckerDark = Color(0xFFDADADA)
private val NewBadgeColor = Color(0xFFF07A1E)

/** The AI removal's "please wait" animation, under `composeResources/files/`. */
private const val LoadingAnimationPath = "files/loading_animation.json"
private val LoadingAnimationSize = 140.dp

private val TabIconSize = 26.dp

/** The tab's own padding above its icon, which the badge is positioned against. */
private val TabVerticalPadding = 6.dp

/**
 * Where the badge's bottom-right corner sits, measured in from the icon's top-left corner: just
 * clear of the sparkle at the eraser's top-left, so the badge reads as attached to it.
 */
private val BadgeAnchorX = 6.dp
private val BadgeAnchorY = 5.dp

/**
 * The Auto slider's colour until the first tap picks one from the photo. Its track fades from this
 * (keep more) to transparent (erase more).
 */
private val MagicSliderColor = Color(0xFF9C6B4E)

private const val ScreenTag = "BgRemoverScreen"

/** Which tool the bottom bar has picked, and so what a gesture on the photo does. */
private enum class EraseMode { Eraser, Zoom, Auto }

/**
 * Background remover step 2 ("Erase"): brush the background away (or paint it back with Recover),
 * tap a colour region away with Auto, let the server's AI cut the subject out (Ai Magic, premium),
 * zoom in for detail, or preview it over a backdrop colour. Done bakes the transparent cut-out into
 * the session for `SetBackgroundScreen`.
 */
@Composable
fun BackgroundRemoverEditorScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackgroundRemoverEditorViewModel = koinViewModel(),
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    BackgroundRemoverEditorContent(
        sourceImage = viewModel.sourceImage,
        ops = viewModel.ops,
        canRedo = viewModel.redoOps.isNotEmpty(),
        isPremium = isPremium,
        aiRunning = viewModel.aiRunning,
        aiApplied = viewModel.hasAiCutOut,
        messages = viewModel.messages,
        onAiMagic = viewModel::removeBackgroundWithAi,
        onOpenPremium = onOpenPremium,
        onPush = viewModel::push,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onBack = onBack,
        onDone = { original, ops ->
            viewModel.applyResult(bakeCutOut(original, ops))
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun BackgroundRemoverEditorContent(
    sourceImage: ImageBitmap?,
    ops: List<EraseOp>,
    canRedo: Boolean,
    isPremium: Boolean,
    aiRunning: Boolean,
    aiApplied: Boolean,
    messages: Flow<String>,
    onAiMagic: () -> Unit,
    onOpenPremium: () -> Unit,
    onPush: (EraseOp) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBack: () -> Unit,
    onDone: (original: ImageBitmap, ops: List<EraseOp>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Must be a bitmap this app created (the ViewModel copies it), since the ops redraw it scaled.
    val original = sourceImage
    var mode by remember { mutableStateOf(EraseMode.Eraser) }
    // Manual and Auto each remember their own erase/recover pick.
    var brushRestore by remember { mutableStateOf(false) }
    var magicRestore by remember { mutableStateOf(false) }
    var backdrop by remember { mutableStateOf<Color?>(null) }
    var brushSize by remember { mutableFloatStateOf(MaskBrushSizeDefault) }
    var magicStrength by remember { mutableFloatStateOf(MagicStrengthDefault) }
    // The colour under the last Auto tap, shown on the tolerance slider.
    var magicColor by remember(original) { mutableStateOf(MagicSliderColor) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var comparing by remember { mutableStateOf(false) }
    var magicRunning by remember { mutableStateOf(false) }
    var sampler by remember(original) { mutableStateOf<MagicSampler?>(null) }
    var currentStroke by remember { mutableStateOf(emptyList<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }
    var showAiPremiumDialog by remember { mutableStateOf(false) }

    // A function rather than a val: the drag gesture's pointerInput block outlives recompositions,
    // so it must read brushSize/zoom state live instead of a value captured when it started.
    fun radiusFraction() = brushRadiusFraction(maskBrushRadiusFraction(brushSize), zoom)

    fun push(op: EraseOp) = onPush(op)

    /**
     * Zooms to [target] about the stage centre. The layer scales about its own centre, so scaling
     * [pan] by the same ratio keeps the point under the stage centre put. At or below fit there is
     * nothing to pan to, so the photo re-centres.
     */
    fun setZoom(target: Float) {
        val next = clampZoom(target)
        pan = if (next <= FitZoom) Offset.Zero else pan * (next / zoom)
        zoom = next
    }

    fun showMessage(message: String) {
        AppLog.d(ScreenTag, "Snackbar: $message")
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    /** Auto tap: cuts (or recovers) the colour region around [point], a fraction of the photo size. */
    fun applyMagic(point: Offset) {
        val image = original ?: return
        if (magicRunning) return
        magicRunning = true
        val restore = magicRestore
        val tolerance = magicTolerance(magicStrength)
        scope.launch {
            runCatching {
                val ready = sampler ?: withContext(Dispatchers.Default) { MagicSampler(image) }.also { sampler = it }
                magicColor = ready.colorAt(point)
                withContext(Dispatchers.Default) { ready.regionMask(point, tolerance) }
            }
                .onSuccess { push(EraseOp.MagicRegion(it, restore)) }
                .onFailure { error ->
                    AppLog.e(ScreenTag, "Auto region detection failed at $point", error)
                    showMessage("Could not detect that area")
                }
            magicRunning = false
        }
    }

    LaunchedEffect(messages) { messages.collect(::showMessage) }

    // Composed before the unlock dialog's handler, so with that dialog open back closes it first.
    val discard = rememberDiscardChangesState(hasChanges = ops.isNotEmpty(), onBack = onBack)

    // Back closes the unlock dialog rather than leaving the eraser, like the LAS app's dialog.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = showAiPremiumDialog,
        onBackCompleted = { showAiPremiumDialog = false },
    )

    // The unlock dialog is Liquid Glass over the eraser, so the eraser is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.surface)
                .liquefiable(liquidState)
                .safeDrawingPadding(),
        ) {
            ToolTopBar(
                title = if (mode == EraseMode.Auto) "Auto" else "Erase",
                onClose = discard::requestBack,
                onDone = { original?.let { onDone(it, ops) } },
                doneEnabled = original != null && !magicRunning && !aiRunning,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .pointerInput(mode) {
                        if (mode != EraseMode.Zoom) return@pointerInput
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            setZoom(zoom * zoomChange)
                            if (zoom > FitZoom) pan += panChange
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (original != null) {
                    // The chip sits outside the zoom layer so it stays put, and at its size, over the photo.
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .aspectRatio(original.width.toFloat() / original.height),
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = pan.x
                                    translationY = pan.y
                                },
                        ) {
                            val density = LocalDensity.current
                            val widthPx = with(density) { maxWidth.toPx() }
                            val heightPx = with(density) { maxHeight.toPx() }
                            val imageSize = Size(widthPx, heightPx)

                            fun fractionFor(p: Offset) = Offset((p.x / widthPx).coerceIn(0f, 1f), (p.y / heightPx).coerceIn(0f, 1f))

                            val currentBackdrop = backdrop
                            if (currentBackdrop == null) {
                                Checkerboard(modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(currentBackdrop))
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // The erase ops clear pixels; an offscreen layer keeps them from also
                                    // clearing the backdrop drawn underneath.
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .pointerInput(mode, widthPx, heightPx) {
                                        if (mode != EraseMode.Auto) return@pointerInput
                                        detectTapGestures(onTap = { applyMagic(fractionFor(it)) })
                                    }
                                    .pointerInput(mode, widthPx, heightPx) {
                                        if (mode != EraseMode.Eraser) return@pointerInput
                                        detectDragGestures(
                                            onDragStart = { position ->
                                                isDragging = true
                                                cursorPositionPx = position
                                                currentStroke = listOf(fractionFor(position))
                                            },
                                            onDragEnd = {
                                                isDragging = false
                                                if (currentStroke.isNotEmpty()) {
                                                    push(EraseOp.Brush(MaskStroke(currentStroke, radiusFraction()), brushRestore))
                                                }
                                                currentStroke = emptyList()
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                currentStroke = emptyList()
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                cursorPositionPx = change.position
                                                currentStroke = currentStroke + fractionFor(change.position)
                                            },
                                        )
                                    },
                            ) {
                                if (comparing) {
                                    drawImageScaled(original, IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()))
                                } else {
                                    val live = if (currentStroke.isNotEmpty()) {
                                        listOf(EraseOp.Brush(MaskStroke(currentStroke, radiusFraction()), brushRestore))
                                    } else {
                                        emptyList()
                                    }
                                    drawErasedImage(original, ops + live, imageSize)
                                }
                                if (mode == EraseMode.Eraser && (isDragging || isAdjustingBrush)) {
                                    drawCircle(
                                        color = scheme.primary,
                                        radius = radiusFraction() * widthPx,
                                        center = if (isDragging) cursorPositionPx else center,
                                        style = Stroke(width = 2.dp.toPx() / zoom),
                                    )
                                }
                            }
                        }

                        BackdropChip(
                            next = nextBackdrop(backdrop),
                            onClick = { backdrop = nextBackdrop(backdrop) },
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                        )
                    }

                    if (magicRunning) CircularProgressIndicator()
                    if (aiRunning) AiProgressOverlay()
                } else {
                    Text("No image to edit", color = scheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                }

                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Zoom shows no erase/recover pair; the row keeps its height so the photo doesn't jump.
                when (mode) {
                    EraseMode.Eraser -> {
                        DuotoneToggle(
                            icon = vectorResource(Res.drawable.ic_eraser_brush),
                            contentDescription = "Erase",
                            selected = !brushRestore,
                            onClick = { brushRestore = false },
                        )
                        DuotoneToggle(
                            icon = vectorResource(Res.drawable.ic_recover),
                            contentDescription = "Recover",
                            selected = brushRestore,
                            onClick = { brushRestore = true },
                        )
                    }
                    EraseMode.Auto -> {
                        TintedToggle(
                            icon = vectorResource(Res.drawable.ic_eraser_magic),
                            contentDescription = "Magic erase",
                            selected = !magicRestore,
                            onClick = { magicRestore = false },
                        )
                        TintedToggle(
                            icon = vectorResource(Res.drawable.ic_recover_magic),
                            contentDescription = "Magic recover",
                            selected = magicRestore,
                            onClick = { magicRestore = true },
                        )
                    }
                    EraseMode.Zoom -> Spacer(modifier = Modifier.size(ToggleSize))
                }
                Spacer(modifier = Modifier.weight(1f))
                UndoRedoButton(
                    icon = vectorResource(Res.drawable.ic_undo),
                    contentDescription = "Undo",
                    enabled = ops.isNotEmpty(),
                    onClick = onUndo,
                )
                Spacer(modifier = Modifier.width(4.dp))
                UndoRedoButton(
                    icon = vectorResource(Res.drawable.ic_redo),
                    contentDescription = "Redo",
                    enabled = canRedo,
                    onClick = onRedo,
                )
                Spacer(modifier = Modifier.width(16.dp))
                HoldToCompareButton(onPressedChange = { comparing = it })
            }

            // Fixed height so switching tools never moves the photo.
            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                when (mode) {
                    EraseMode.Eraser -> CenterFillSlider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        range = MaskBrushSizeRange,
                        referenceValue = MaskBrushSizeRange.start,
                        onDraggingChange = { isAdjustingBrush = it },
                        trackColor = scheme.onSurface.copy(alpha = 0.12f),
                        fillColor = scheme.primary,
                        thumbColor = scheme.primary,
                        thumbWidth = 32.dp,
                        thumbHeight = 18.dp,
                        horizontalPadding = 12.dp,
                        glassThumb = true,
                        glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                    )
                    EraseMode.Auto -> MagicStrengthSlider(
                        value = magicStrength,
                        onValueChange = { magicStrength = it },
                        color = magicColor,
                    )
                    // Starts from the centre (fit): left zooms out, right zooms in. It reads the
                    // zoom back, so a pinch keeps the thumb in step.
                    EraseMode.Zoom -> CenterFillSlider(
                        value = zoomToSlider(zoom),
                        onValueChange = { setZoom(sliderToZoom(it)) },
                        range = ZoomSliderRange,
                        referenceValue = 0f,
                        trackColor = scheme.onSurface.copy(alpha = 0.12f),
                        fillColor = scheme.primary,
                        thumbColor = scheme.primary,
                        thumbWidth = 32.dp,
                        thumbHeight = 18.dp,
                        horizontalPadding = 12.dp,
                        glassThumb = true,
                        glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_eraser_manual),
                    label = "Eraser",
                    selected = mode == EraseMode.Eraser,
                    onClick = { mode = EraseMode.Eraser },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_zoom),
                    label = "Zoom",
                    selected = mode == EraseMode.Zoom,
                    onClick = { mode = EraseMode.Zoom },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_eraser_auto),
                    label = "Auto",
                    selected = mode == EraseMode.Auto,
                    enabled = original != null,
                    onClick = { mode = EraseMode.Auto },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_eraser_ai),
                    label = "Ai Magic",
                    selected = aiRunning,
                    badge = if (aiApplied) null else "New",
                    enabled = original != null && !aiRunning,
                    onClick = {
                        AppLog.d(ScreenTag, "Ai Magic tapped (premium: $isPremium, applied: $aiApplied)")
                        if (isPremium) onAiMagic() else showAiPremiumDialog = true
                    },
                )
            }
        }

        DiscardChangesPopup(discard, liquidState)

        GlassDialogHost(
            visible = showAiPremiumDialog,
            liquidState = liquidState,
            onDismiss = { showAiPremiumDialog = false },
            // As in the LAS app, only the X closes it, and turning the offer down that way lands
            // on the free Auto tool instead.
            dismissOnScrimClick = false,
            footer = {
                AiMagicCloseButton(
                    onClick = {
                        showAiPremiumDialog = false
                        mode = EraseMode.Auto
                    },
                )
            },
        ) {
            AiMagicUnlockDialog(
                // There is no rewarded-ad SDK yet, so "Unlock free" grants the removal straight away.
                onUnlockFree = {
                    AppLog.d(ScreenTag, "Ai Magic unlocked free from the premium dialog")
                    showAiPremiumDialog = false
                    onAiMagic()
                },
                onGetPro = {
                    showAiPremiumDialog = false
                    onOpenPremium()
                },
            )
        }
    }
}

/** Transparency checkerboard shown behind the cut-out when no backdrop colour is picked. */
@Composable
private fun Checkerboard(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawCheckerboard(8.dp.toPx()) }
}

private fun DrawScope.drawCheckerboard(cell: Float) {
    drawRect(CheckerLight)
    val columns = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if ((row + column) % 2 == 1) {
                drawRect(CheckerDark, topLeft = Offset(column * cell, row * cell), size = Size(cell, cell))
            }
        }
    }
}

/**
 * The single backdrop chip over the photo's top-left corner. It shows [next], the backdrop a tap
 * switches to, so each tap steps through [BackdropCycle].
 */
@Composable
private fun BackdropChip(next: Color?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val bounce = rememberSpringBounce()
    Box(
        modifier = modifier
            .springBounce(bounce)
            .size(34.dp)
            .clip(shape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClickLabel = "Change preview background",
                onClick = onClick,
            ),
    ) {
        if (next == null) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawCheckerboard(size.width / 4f) }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(next))
        }
        Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFF6B6975), shape))
    }
}

/** Covers the photo while the AI request runs, swallowing touches so no edit lands mid-request. */
@Composable
private fun AiProgressOverlay() {
    // Also shown while the Lottie JSON parses (a frame or two), when the painter draws nothing yet.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = rememberLoopingLottiePainter(LoadingAnimationPath),
                contentDescription = "Removing background",
                modifier = Modifier.size(LoadingAnimationSize),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Processing… Please wait!", color = Color.White, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Loops the Lottie file at [path], as the onboarding pages do. Draws nothing until the composition
 * has parsed, which is a frame or two.
 */
@Composable
private fun rememberLoopingLottiePainter(path: String): Painter {
    val composition by rememberLottieComposition(path) {
        LottieCompositionSpec.JsonString(Res.readBytes(path).decodeToString())
    }
    return rememberLottiePainter(composition = composition, iterations = Compottie.IterateForever)
}

private val ToggleSize = 40.dp

@Composable
private fun ToggleBox(contentDescription: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(ToggleSize)
            .clip(CircleShape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * Manual erase / recover toggle. These icons are a black body with a white +/- inside, so a plain
 * tint would flood the detail too; [duotone] recolours body and detail separately instead.
 */
@Composable
private fun DuotoneToggle(icon: ImageVector, contentDescription: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val body = if (selected) scheme.primary else scheme.onSurface
    val filter = remember(body, scheme.surface) { duotone(body = body, detail = scheme.surface) }
    ToggleBox(contentDescription, onClick) {
        Image(imageVector = icon, contentDescription = contentDescription, colorFilter = filter, modifier = Modifier.size(28.dp))
    }
}

/** Auto erase / recover toggle: single-colour icons, so an ordinary tint works. */
@Composable
private fun TintedToggle(icon: ImageVector, contentDescription: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ToggleBox(contentDescription, onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) scheme.primary else scheme.onSurface,
            modifier = Modifier.size(28.dp),
        )
    }
}

/**
 * Maps black → [body] and white → [detail] (greys in between, so anti-aliased edges stay smooth),
 * leaving alpha alone: `out = body + (detail - body) * in` per channel. Offsets are in 0..255.
 */
private fun duotone(body: Color, detail: Color): ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            detail.red - body.red, 0f, 0f, 0f, body.red * 255f,
            0f, detail.green - body.green, 0f, 0f, body.green * 255f,
            0f, 0f, detail.blue - body.blue, 0f, body.blue * 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

/**
 * Auto tolerance slider: a pill whose [color] (the last tapped colour) fades into the transparency
 * checkerboard, hinting that further right erases more similar colours. [value] is `0..1`.
 *
 * The thumb is a Liquid Glass ring over the track. While a finger is down it swells past the
 * track's height into a bubble that magnifies and splits the colour under it, and springs back on
 * release. As in [CenterFillSlider], the track canvas is the `liquefiable` source and the thumb is
 * its sibling, since a liquid node can't sample a layer it is drawn inside.
 */
@Composable
private fun MagicStrengthSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(MagicTrackHeight).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val trackHeightPx = with(density) { MagicTrackHeight.toPx() }
        val insetPx = with(density) { MagicThumbInset.toPx() }
        val thumbRadiusPx = trackHeightPx / 2f - insetPx
        val startX = insetPx + thumbRadiusPx
        val travel = (widthPx - startX * 2).coerceAtLeast(1f)
        val liquidState = rememberLiquidState()
        var pressed by remember { mutableStateOf(false) }

        fun valueAt(x: Float) = ((x - startX) / travel).coerceIn(0f, 1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .liquefiable(liquidState)
                .pointerInput(travel) { detectTapGestures { latestOnValueChange(valueAt(it.x)) } }
                .pointerInput(travel) {
                    detectHorizontalDragGestures(onDragStart = { latestOnValueChange(valueAt(it.x)) }) { change, _ ->
                        change.consume()
                        latestOnValueChange(valueAt(change.position.x))
                    }
                }
                .pointerInputPressed { pressed = it },
        ) {
            val corner = CornerRadius(size.height / 2f)
            val pill = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), corner)) }
            clipPath(pill) {
                drawCheckerboard(size.height / 4f)
                drawRect(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0f))))
                // A soft top sheen so the pill itself reads as glass, not a flat swatch.
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.28f),
                        0.5f to Color.White.copy(alpha = 0f),
                    ),
                )
            }
            val rim = 1.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(rim / 2f, rim / 2f),
                size = Size(size.width - rim, size.height - rim),
                cornerRadius = CornerRadius(size.height / 2f - rim / 2f),
                style = Stroke(rim),
            )
        }

        MagicGlassThumb(
            liquidState = liquidState,
            pressed = pressed,
            centerX = { startX + value.coerceIn(0f, 1f) * travel },
            diameter = MagicTrackHeight - MagicThumbInset * 2,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

private val MagicTrackHeight = 30.dp
private val MagicThumbInset = 3.dp

/** How much the Auto thumb swells while held; it overflows the track, like a bubble on top of it. */
private const val MagicBubbleScale = 1.9f

/**
 * One spring-driven 0→1 progress morphs the resting ring into the bubble: size, refraction, colour
 * split and rim all follow it, so they can't drift apart. The size is laid out for real (not a
 * `graphicsLayer` scale) because Liquid samples the backdrop from the node's bounds, and
 * `requiredSize` lets the bubble overflow the track's height while staying centred on it.
 */
@Composable
private fun MagicGlassThumb(
    liquidState: LiquidState,
    pressed: Boolean,
    centerX: () -> Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
    )
    // The overshooting spring dips slightly past 0 / 1; only the size may use the overshoot.
    val t = progress.coerceIn(0f, 1f)
    val size = diameter * (1f + (MagicBubbleScale - 1f) * progress)
    val halfPx = with(LocalDensity.current) { size.toPx() } / 2f

    Box(
        modifier = modifier
            .offset { IntOffset((centerX() - halfPx).roundToInt(), 0) }
            .requiredSize(size)
            .clip(CircleShape)
            .liquid(liquidState) {
                shape = CircleShape
                frost = 0.5.dp
                curve = 0.35f + 0.35f * t
                refraction = 0.25f + 0.45f * t
                edge = 0.6f + 0.3f * t
                dispersion = 0.1f + 0.45f * t
                saturation = 1.2f + 0.3f * t
                tint = Color.White.copy(alpha = 0.14f * (1f - t) + 0.04f * t)
            }
            .border(
                width = 2.dp * (1f - t) + 1.dp * t,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.95f - 0.25f * t),
                        Color.White.copy(alpha = 0.75f - 0.55f * t),
                    ),
                ),
                shape = CircleShape,
            ),
    )
}

/** Theme-aware press-and-hold compare: shows the untouched photo while held. */
@Composable
private fun HoldToCompareButton(onPressedChange: (Boolean) -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(UndoRedoButtonSize)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(onPressedChange) {
                detectTapGestures(
                    onPress = {
                        bounce.press()
                        onPressedChange(true)
                        tryAwaitRelease()
                        onPressedChange(false)
                        bounce.release()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_before_after),
            contentDescription = "Press and hold to compare with the original",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(UndoRedoIconSize),
        )
    }
}

@Composable
private fun BottomTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    badge: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        selected -> scheme.primary
        enabled -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.35f)
    }
    // The badge hangs off the icon's top-left, past the tab's edge, so it sits in an unclipped
    // wrapper beside the tab instead of inside the clipped (ripple-shaped) column.
    val bounce = rememberSpringBounce()
    Box(modifier = Modifier.springBounce(bounce)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = bounce.interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp, vertical = TabVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(TabIconSize))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
        }
        if (badge != null) {
            NewBadge(
                text = badge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            // TopCenter centres the badge on the icon; shift it so its bottom-right
                            // corner lands on the anchor inside the icon's top-left.
                            val iconLeft = -(TabIconSize / 2).roundToPx()
                            placeable.place(
                                x = iconLeft + BadgeAnchorX.roundToPx() - placeable.width / 2,
                                y = TabVerticalPadding.roundToPx() + BadgeAnchorY.roundToPx() - placeable.height,
                            )
                        }
                    },
            )
        }
    }
}

/** The orange "New" pill on a tab. */
@Composable
private fun NewBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(NewBadgeColor)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

@Preview
@Composable
private fun BackgroundRemoverEditorPreview() {
    ThemePreviews {
        BackgroundRemoverEditorContent(
            sourceImage = ImageBitmap(360, 480),
            ops = emptyList(),
            canRedo = false,
            isPremium = false,
            aiRunning = false,
            aiApplied = false,
            messages = emptyFlow(),
            onAiMagic = {},
            onOpenPremium = {},
            onPush = {},
            onUndo = {},
            onRedo = {},
            onBack = {},
            onDone = { _, _ -> },
        )
    }
}
