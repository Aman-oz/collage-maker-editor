package org.example.project.ui.pip

import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.rememberDiscardChangesState
import org.example.project.i18n.tr
import org.example.project.ui.common.StickerFlightOverlay
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.rememberStickerFlights
import org.example.project.ui.freestyle.FreestyleStickerBaseSizeSp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
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
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.topBar
import org.example.project.ui.freestyle.FreestyleCanvas
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleFill
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.StickersPanel
import org.example.project.ui.freestyle.TextEntry
import org.example.project.ui.freestyle.TextEntryBar
import org.example.project.ui.freestyle.TextPanel
import org.example.project.ui.freestyle.freestyleChrome
import org.example.project.ui.freestyle.onGestureEnd
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.TextColorOptions
import org.example.project.ui.text.TextFontStyleOption
import org.example.project.ui.text.TextFontStyles
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_undo

/**
 * Fixed height of the panel above the bottom tabs. Both panels fit it, so switching tabs never
 * resizes the picture — which would move every text/sticker layer, since they sit at fractions of it.
 */
private val ToolPanelHeight = 190.dp

/** Mask alpha (0..255) above which a touch counts as landing on that slot's window. */
private const val MaskHitAlpha = 16

/** The bottom tabs, each opening its panel under the picture. */
private enum class PipTool { Text, Stickers }

/**
 * Pip editor: the chosen template's frame over a blurred copy of the first photo, with each picked
 * photo showing through its window. Pinch/drag a window to reframe its photo, tap an empty one (or
 * double-tap any) to pick a photo for it, and add text and stickers over the whole picture (the
 * freestyle editor's layers). Done bakes it all into the session and opens the photo editor.
 */
@Composable
fun PipEditorScreen(
    templateName: String,
    imagePaths: List<String>,
    onBack: () -> Unit,
    onApplied: () -> Unit,
    onPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PipEditorViewModel = koinViewModel { parametersOf(templateName, imagePaths) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }
    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slot = pendingSlotIndex
        if (file != null && slot != null) viewModel.replacePhoto(slot, file.path)
        pendingSlotIndex = null
    }

    // Leaving with edits made (anything undoable) asks first, for ✕ and system back alike.
    val discard = rememberDiscardChangesState(hasChanges = uiState.canUndo, onBack = onBack)
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        PipEditorContent(
            template = viewModel.template,
            uiState = uiState,
            onBack = discard::requestBack,
            onDone = { previewSizePx ->
                // Premium text or stickers show a non-subscriber the paywall once, as an offer. The
                // edit lives in the ViewModel, so it is still here when they come back, and the next
                // Done goes through whether or not they subscribed.
                if (viewModel.consumePremiumOffer()) {
                    onPremium()
                } else if (viewModel.apply(textMeasurer, previewSizePx.width.toFloat(), density.density)) {
                    onApplied()
                }
            },
            onPickSlotPhoto = { index ->
                pendingSlotIndex = index
                photoPicker.launch()
            },
            onTransformSlot = viewModel::transformSlot,
            onAddSticker = viewModel::addSticker,
            onAddText = viewModel::addText,
            onUpdateText = viewModel::updateText,
            onRetypeText = viewModel::retypeText,
            onTransformLayer = viewModel::transformLayer,
            onScaleRotateLayer = viewModel::setLayerScaleRotation,
            onSelectLayer = viewModel::bringToFront,
            onDeleteLayer = viewModel::removeLayer,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onGestureEnd = viewModel::endGesture,
            modifier = Modifier.liquefiable(liquidState),
        )
        DiscardChangesPopup(discard, liquidState)
    }
}

@Composable
private fun PipEditorContent(
    template: PipTemplate?,
    uiState: PipEditorUiState,
    onBack: () -> Unit,
    onDone: (previewSizePx: IntSize) -> Unit,
    onPickSlotPhoto: (slotIndex: Int) -> Unit,
    onTransformSlot: (index: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
    onAddSticker: (String) -> Unit,
    onAddText: (String, FreestyleFill, TextFontStyleOption, FreestyleFill?) -> Unit,
    onUpdateText: (id: Long, text: String, fill: FreestyleFill, font: TextFontStyleOption, background: FreestyleFill?) -> Unit,
    onRetypeText: (id: Long, text: String) -> Unit,
    onTransformLayer: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onScaleRotateLayer: (id: Long, scale: Float, rotationDegrees: Float) -> Unit,
    onSelectLayer: (id: Long) -> Unit,
    onDeleteLayer: (id: Long) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGestureEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = freestyleChrome()
    val stickerFlights = rememberStickerFlights(FreestyleStickerBaseSizeSp)
    val edit = uiState.edit
    val assets = uiState.assets
    val ready = template != null && assets != null
    var tool by remember { mutableStateOf(PipTool.Text) }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var previewSizePx by remember { mutableStateOf(IntSize.Zero) }
    // Style for the next new text layer; a selected text layer shows and edits its own instead.
    var newTextFont by remember { mutableStateOf(TextFontStyles[1]) }
    var newTextFill by remember { mutableStateOf<FreestyleFill>(FreestyleFill.Solid(TextColorOptions.first())) }
    var newTextBackground by remember { mutableStateOf<FreestyleFill?>(null) }
    // Non-null while the keyboard-docked text bar is open.
    var textEntry by remember { mutableStateOf<TextEntry?>(null) }

    val selectedTextLayer = edit.layers.firstOrNull { it.id == selectedLayerId && it.content is FreestyleContent.TextContent }
    val selectedText = selectedTextLayer?.content as? FreestyleContent.TextContent

    fun commitTextEntry(entry: TextEntry) {
        // Retyping coalesces into one undo step; closing the bar ends it.
        onGestureEnd()
        val layerId = entry.layerId
        if (layerId == null) {
            onAddText(entry.text.trim(), newTextFill, newTextFont, newTextBackground)
        } else if (entry.text.isBlank()) {
            // Clearing a label's text removes it, rather than leaving an invisible layer behind.
            onDeleteLayer(layerId)
            if (selectedLayerId == layerId) selectedLayerId = null
        }
        textEntry = null
    }

    fun openTextEntry(layer: FreestyleLayer?) {
        val text = (layer?.content as? FreestyleContent.TextContent)?.text.orEmpty()
        textEntry = TextEntry(layerId = layer?.id, text = text)
    }

    // One observer for the whole screen: any finger lift ends the drag/pinch step in progress.
    Box(modifier = modifier.fillMaxSize().onGestureEnd(onGestureEnd)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.surface)
                // The keyboard is left out on purpose: it slides up over the panels while the picture
                // keeps its size, and the text bar docks itself above it instead.
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
        ) {
            PipEditorTopBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                doneEnabled = ready,
                onClose = onBack,
                onUndo = onUndo,
                onRedo = onRedo,
                onDone = { onDone(previewSizePx) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Taps that reach here (off the picture) deselect; layers consume their own taps.
                    .pointerInput(Unit) { detectTapGestures(onTap = { selectedLayerId = null }) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    template != null && assets != null -> Box(
                        modifier = Modifier.aspectRatio(assets.frame.width.toFloat() / assets.frame.height),
                    ) {
                        PipCanvas(
                            template = template,
                            assets = assets,
                            backdrop = uiState.backdrop,
                            edit = edit,
                            onSlotTap = { index ->
                                selectedLayerId = null
                                // An empty window has nothing to reframe; go straight to filling it.
                                if (index != null && index !in edit.photos) onPickSlotPhoto(index)
                            },
                            onSlotDoubleTap = { index ->
                                selectedLayerId = null
                                onPickSlotPhoto(index)
                            },
                            onSlotTransform = onTransformSlot,
                        )
                        FreestyleCanvas(
                            stickerFlights = stickerFlights,
                            layers = edit.layers,
                            chrome = chrome,
                            selectedLayerId = selectedLayerId,
                            onCanvasSizeChanged = { previewSizePx = it },
                            onTextLayerDoubleTapped = { layer ->
                                selectedLayerId = layer.id
                                onSelectLayer(layer.id)
                                tool = PipTool.Text
                                openTextEntry(layer)
                            },
                            onLayerSelected = { id ->
                                selectedLayerId = id
                                onSelectLayer(id)
                                // Selecting a label opens the panel that edits it (font, color, retyping).
                                val isText = edit.layers.any { it.id == id && it.content is FreestyleContent.TextContent }
                                if (isText) tool = PipTool.Text
                            },
                            onLayerTransformed = onTransformLayer,
                            onLayerScaleRotated = onScaleRotateLayer,
                            onLayerDeleted = { id ->
                                onDeleteLayer(id)
                                selectedLayerId = null
                            },
                        )
                    }

                    uiState.error != null -> Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )

                    else -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(ToolPanelHeight)) {
                if (ready) {
                    when (tool) {
                        PipTool.Text -> TextPanel(
                            chrome = chrome,
                            selectedText = selectedText,
                            font = selectedText?.font ?: newTextFont,
                            fill = selectedText?.fill ?: newTextFill,
                            background = if (selectedText != null) selectedText.background else newTextBackground,
                            onFieldClick = { openTextEntry(selectedTextLayer) },
                            onFontChange = { font ->
                                if (selectedTextLayer != null && selectedText != null) {
                                    onUpdateText(selectedTextLayer.id, selectedText.text, selectedText.fill, font, selectedText.background)
                                } else {
                                    newTextFont = font
                                }
                            },
                            onFillChange = { fill ->
                                if (selectedTextLayer != null && selectedText != null) {
                                    onUpdateText(selectedTextLayer.id, selectedText.text, fill, selectedText.font, selectedText.background)
                                } else {
                                    newTextFill = fill
                                }
                            },
                            onBackgroundChange = { background ->
                                if (selectedTextLayer != null && selectedText != null) {
                                    onUpdateText(selectedTextLayer.id, selectedText.text, selectedText.fill, selectedText.font, background)
                                } else {
                                    newTextBackground = background
                                }
                            },
                        )
                        PipTool.Stickers -> StickersPanel(
                            chrome = chrome,
                            onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { onAddSticker(emoji) } },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_text_editor),
                    label = tr("Text"),
                    selected = tool == PipTool.Text,
                    enabled = ready,
                    onClick = { tool = PipTool.Text },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_stickers_editor),
                    label = tr("Stickers"),
                    selected = tool == PipTool.Stickers,
                    enabled = ready,
                    onClick = { tool = PipTool.Stickers },
                )
            }
        }

        textEntry?.let { entry ->
            TextEntryBar(
                chrome = chrome,
                entry = entry,
                onTextChange = { text ->
                    textEntry = entry.copy(text = text)
                    // An existing label updates live as it's retyped; a blank one is removed on commit.
                    val layerId = entry.layerId
                    if (layerId != null && text.isNotBlank()) onRetypeText(layerId, text)
                },
                onCommit = { commitTextEntry(entry) },
            )
        }

        StickerFlightOverlay(stickerFlights)
    }
}

/**
 * The PIP picture, drawn by [drawPip] exactly as it bakes, with a "+" over each empty window.
 *
 * Touches are routed to a slot by hit-testing its mask, not just its bounding box, since windows
 * like a bottle's are far from rectangular and their boxes can overlap. The slot is picked once, on
 * the first finger down, so a pinch that wanders outside its window keeps moving the same photo.
 */
@Composable
private fun PipCanvas(
    template: PipTemplate,
    assets: PipFrameAssets,
    backdrop: ImageBitmap?,
    edit: PipEdit,
    onSlotTap: (slotIndex: Int?) -> Unit,
    onSlotDoubleTap: (slotIndex: Int) -> Unit,
    onSlotTransform: (index: Int, panX: Float, panY: Float, zoom: Float, slotWidth: Float, slotHeight: Float) -> Unit,
) {
    val rects = remember(template, assets) {
        template.slots.mapIndexed { index, slot ->
            slot.slotRect(assets.masks[index].width, assets.masks[index].height, assets.frame.width, assets.frame.height)
        }
    }
    // The pointerInput blocks outlive recompositions, so read the latest callbacks through state.
    val currentOnTap by rememberUpdatedState(onSlotTap)
    val currentOnDoubleTap by rememberUpdatedState(onSlotDoubleTap)
    val currentOnTransform by rememberUpdatedState(onSlotTransform)
    // Only read from pointer handlers, never in composition, so a plain holder is enough.
    val gestureSlot = remember { IntArray(1) { NoSlot } }

    fun slotAt(position: Offset, size: IntSize): Int? {
        if (size.width <= 0 || size.height <= 0) return null
        val point = Offset(position.x / size.width, position.y / size.height)
        return slotIndexAt(point, rects) { index, local -> assets.masks[index].isOpaqueAt(local) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rects) {
                    // Initial pass: note the slot before the tap/transform detectors below act on it.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        gestureSlot[0] = slotAt(down.position, size) ?: NoSlot
                    }
                }
                .pointerInput(rects) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val index = gestureSlot[0]
                        if (index == NoSlot) return@detectTransformGestures
                        val rect = rects[index]
                        currentOnTransform(index, pan.x, pan.y, zoom, rect.width * size.width, rect.height * size.height)
                    }
                }
                .pointerInput(rects) {
                    detectTapGestures(
                        onTap = { position -> currentOnTap(slotAt(position, size)) },
                        onDoubleTap = { position -> slotAt(position, size)?.let { currentOnDoubleTap(it) } },
                    )
                },
        ) {
            drawPip(template, assets, backdrop, edit, shadeEmptySlots = true)
        }

        rects.forEachIndexed { index, rect ->
            if (index in edit.photos) return@forEachIndexed
            val iconSize = 32.dp
            Box(
                modifier = Modifier
                    .offset(
                        x = maxWidth * (rect.left + rect.width / 2f) - iconSize / 2,
                        y = maxHeight * (rect.top + rect.height / 2f) - iconSize / 2,
                    )
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = tr("Add photo"), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private const val NoSlot = -1

/** Whether this mask is opaque enough at [local] (a fraction of its size) to count as a hit. */
private fun ImageBitmap.isOpaqueAt(local: Offset): Boolean {
    val x = (local.x * width).toInt().coerceIn(0, width - 1)
    val y = (local.y * height).toInt().coerceIn(0, height - 1)
    val pixel = IntArray(1)
    readPixels(pixel, startX = x, startY = y, width = 1, height = 1)
    return (pixel[0] ushr 24) > MaskHitAlpha
}

@Composable
private fun PipEditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    doneEnabled: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // A Box like ToolTopBar keeps the title centered on the screen; its side padding reserves room
    // for the wider undo/redo/done group so a long title ellipsizes instead of running under it.
    Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
        GlassTopBarButton(
            icon = Icons.Filled.Close,
            contentDescription = tr("Close"),
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = tr("Pip Editor"),
            color = scheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TopBarButtonSize * 3),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

@Composable
private fun BottomTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
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
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
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
private fun PipEditorPreview() {
    ThemePreviews {
        PipEditorContent(
            template = PipTemplates.first(),
            uiState = PipEditorUiState(
                assets = PipFrameAssets(frame = ImageBitmap(612, 612), masks = listOf(ImageBitmap(211, 439))),
            ),
            onBack = {},
            onDone = {},
            onPickSlotPhoto = {},
            onTransformSlot = { _, _, _, _, _, _ -> },
            onAddSticker = {},
            onAddText = { _, _, _, _ -> },
            onUpdateText = { _, _, _, _, _ -> },
            onRetypeText = { _, _ -> },
            onTransformLayer = { _, _, _, _ -> },
            onScaleRotateLayer = { _, _, _ -> },
            onSelectLayer = {},
            onDeleteLayer = {},
            onUndo = {},
            onRedo = {},
            onGestureEnd = {},
        )
    }
}
