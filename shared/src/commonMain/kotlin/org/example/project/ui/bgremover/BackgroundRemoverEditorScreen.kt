package org.example.project.ui.bgremover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.MaskBrushSizeDefault
import org.example.project.ui.common.MaskBrushSizeRange
import org.example.project.ui.common.MaskStroke
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.common.drawImageScaled
import org.example.project.ui.common.maskBrushRadiusFraction
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
private val NewBadgeColor = Color(0xFFFF8A3D)

/**
 * The Auto slider's colour until the first tap picks one from the photo. Its track fades from this
 * (keep more) to transparent (erase more).
 */
private val MagicSliderColor = Color(0xFF9C6B4E)

/** Which tool the bottom bar has picked, and so what a gesture on the photo does. */
private enum class EraseMode { Eraser, Zoom, Auto }

/**
 * Background remover step 2 ("Erase"): brush the background away (or paint it back with Recover),
 * tap a colour region away with Auto, zoom in for detail, or preview it over a backdrop colour.
 * Done bakes the transparent cut-out into the session for `SetBackgroundScreen`.
 */
@Composable
fun BackgroundRemoverEditorScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackgroundRemoverEditorViewModel = koinViewModel(),
) {
    BackgroundRemoverEditorContent(
        sourceImage = viewModel.sourceImage,
        ops = viewModel.ops,
        canRedo = viewModel.redoOps.isNotEmpty(),
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

    // A function rather than a val: the drag gesture's pointerInput block outlives recompositions,
    // so it must read brushSize/zoom state live instead of a value captured when it started.
    fun radiusFraction() = brushRadiusFraction(maskBrushRadiusFraction(brushSize), zoom)

    fun push(op: EraseOp) = onPush(op)

    fun showMessage(message: String) {
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
                .onFailure { showMessage("Could not detect that area") }
            magicRunning = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = if (mode == EraseMode.Auto) "Auto" else "Erase",
            onClose = onBack,
            onDone = { original?.let { onDone(it, ops) } },
            doneEnabled = original != null && !magicRunning,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .pointerInput(mode) {
                    if (mode != EraseMode.Zoom) return@pointerInput
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = clampZoom(zoom * zoomChange)
                        pan = if (zoom == MinZoom) Offset.Zero else pan + panChange
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
            PlainIconButton(
                icon = vectorResource(Res.drawable.ic_undo),
                contentDescription = "Undo",
                enabled = ops.isNotEmpty(),
                onClick = onUndo,
            )
            Spacer(modifier = Modifier.width(4.dp))
            PlainIconButton(
                icon = vectorResource(Res.drawable.ic_redo),
                contentDescription = "Redo",
                enabled = canRedo,
                onClick = onRedo,
            )
            Spacer(modifier = Modifier.width(16.dp))
            HoldToCompareButton(onPressedChange = { comparing = it })
        }

        // Fixed height for the same reason: Zoom leaves the slot empty.
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
                    thumbWidth = 26.dp,
                    thumbHeight = 14.dp,
                    horizontalPadding = 12.dp,
                    glassThumb = true,
                    glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                )
                EraseMode.Auto -> MagicStrengthSlider(
                    value = magicStrength,
                    onValueChange = { magicStrength = it },
                    color = magicColor,
                )
                EraseMode.Zoom -> Unit
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
                selected = false,
                badge = "New",
                onClick = { showMessage("Ai Magic is coming soon") },
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
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(shape)
            .clickable(onClickLabel = "Change preview background", onClick = onClick),
    ) {
        if (next == null) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawCheckerboard(size.width / 4f) }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(next))
        }
        Box(modifier = Modifier.fillMaxSize().border(2.dp, Color(0xFF6B6975), shape))
    }
}

private val ToggleSize = 40.dp

@Composable
private fun ToggleBox(contentDescription: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(ToggleSize)
            .clip(CircleShape)
            .clickable(onClickLabel = contentDescription, onClick = onClick),
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
 * checkerboard, hinting that further right erases more similar colours. The thumb is filled with
 * [color] too. [value] is `0..1`.
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
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val trackHeightPx = with(density) { 30.dp.toPx() }
        val insetPx = with(density) { 3.dp.toPx() }
        val thumbRadiusPx = trackHeightPx / 2f - insetPx
        val startX = insetPx + thumbRadiusPx
        val travel = (widthPx - startX * 2).coerceAtLeast(1f)

        fun valueAt(x: Float) = ((x - startX) / travel).coerceIn(0f, 1f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .pointerInput(travel) { detectTapGestures { latestOnValueChange(valueAt(it.x)) } }
                .pointerInput(travel) {
                    detectHorizontalDragGestures(onDragStart = { latestOnValueChange(valueAt(it.x)) }) { change, _ ->
                        change.consume()
                        latestOnValueChange(valueAt(change.position.x))
                    }
                },
        ) {
            val pill = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(size.height / 2f))) }
            clipPath(pill) {
                drawCheckerboard(size.height / 4f)
                drawRect(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0f))))
            }
            val thumbCenter = Offset(startX + value.coerceIn(0f, 1f) * travel, size.height / 2f)
            drawCircle(Color.Black.copy(alpha = 0.18f), radius = thumbRadiusPx + 1.dp.toPx(), center = thumbCenter + Offset(0f, 1.dp.toPx()))
            drawCircle(Color.White, radius = thumbRadiusPx, center = thumbCenter)
            drawCircle(color, radius = thumbRadiusPx - 2.dp.toPx(), center = thumbCenter)
        }
    }
}

@Composable
private fun PlainIconButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Theme-aware press-and-hold compare: shows the untouched photo while held. */
@Composable
private fun HoldToCompareButton(onPressedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(onPressedChange) {
                detectTapGestures(
                    onPress = {
                        onPressedChange(true)
                        tryAwaitRelease()
                        onPressedChange(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_before_after),
            contentDescription = "Press and hold to compare with the original",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            if (badge != null) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 14.dp, y = (-8).dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NewBadgeColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
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

@Preview
@Composable
private fun BackgroundRemoverEditorPreview() {
    ThemePreviews {
        BackgroundRemoverEditorContent(
            sourceImage = ImageBitmap(360, 480),
            ops = emptyList(),
            canRedo = false,
            onPush = {},
            onUndo = {},
            onRedo = {},
            onBack = {},
            onDone = { _, _ -> },
        )
    }
}
