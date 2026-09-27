package org.example.project.ui.freestyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BorderClear
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlin.math.roundToInt
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.emoji.EmojiCategory
import org.example.project.ui.emoji.EmojiCategoryTabs
import org.example.project.ui.emoji.EmojiGrid
import org.example.project.ui.emoji.EmojisByCategory
import org.example.project.ui.emoji.MaxRecentEmojis
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.ColorRow
import org.example.project.ui.text.FontRow
import org.example.project.ui.text.TextColorOptions
import org.example.project.ui.text.TextFontStyleOption
import org.example.project.ui.text.TextFontStyles
import org.example.project.ui.text.rotateVector
import org.example.project.ui.text.snapTextRotation
import org.example.project.ui.text.vectorAngleDegrees
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

private enum class FreestyleTool(val label: String, val icon: ImageVector) {
    Background("Background", Icons.Outlined.Wallpaper),
    Stickers("Stickers", Icons.Outlined.EmojiEmotions),
    Border("Border", Icons.Outlined.BorderClear),
    Text("Text", Icons.Outlined.TextFields),
}

/**
 * Fixed height of the tool panel above the tool row. Every panel fits it, so switching tools never
 * resizes the canvas — which would shift every layer, since positions are fractions of its height.
 */
private val ToolPanelHeight = 190.dp

private val DeleteHandleSize = 30.dp
private val ResizeHandleSize = 24.dp

/**
 * Chrome colors for the freestyle editor. Like the collage editor it follows the app's light/dark
 * [MaterialTheme] rather than the always-dark `EditorPalette`.
 */
@Immutable
private data class FreestyleChrome(
    val isLight: Boolean,
    val surface: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
    val field: Color,
)

@Composable
private fun freestyleChrome(): FreestyleChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return FreestyleChrome(
        isLight = isLight,
        surface = scheme.surface,
        content = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.55f),
        accent = scheme.primary,
        track = if (isLight) Color(0xFFD9D6E3) else Color(0xFF3A3A40),
        field = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
    )
}

@Composable
fun FreestyleEditorScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreestyleEditorViewModel = koinViewModel { parametersOf(imagePaths) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The photo layer the open picker will replace; null means the pick adds a new layer.
    var replaceLayerId by remember { mutableStateOf<Long?>(null) }

    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val targetId = replaceLayerId
        if (file != null) {
            if (targetId != null) viewModel.replaceImage(targetId, file.path) else viewModel.addImage(file.path)
        }
        replaceLayerId = null
    }

    FreestyleEditorContent(
        uiState = uiState,
        onClose = onBack,
        onDone = { canvasSizePx ->
            viewModel.applyFreestyle(textMeasurer, canvasSizePx.width.toFloat(), canvasSizePx.height.toFloat(), density.density)
            onOpenEditor()
        },
        onTransformLayer = viewModel::transformLayer,
        onScaleRotateLayer = viewModel::setLayerScaleRotation,
        onSelectLayer = viewModel::bringToFront,
        onDeleteLayer = viewModel::removeLayer,
        onImageAction = { targetId ->
            replaceLayerId = targetId
            imagePicker.launch()
        },
        onAddSticker = viewModel::addSticker,
        onAddText = viewModel::addText,
        onUpdateText = viewModel::updateText,
        onRetypeText = viewModel::retypeText,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onGestureEnd = viewModel::endGesture,
        onBackgroundColorChange = viewModel::updateBackgroundColor,
        onBorderWidthChange = viewModel::updateImageBorderWidth,
        onCornerRadiusChange = viewModel::updateImageCornerRadius,
        modifier = modifier,
    )
}

@Composable
private fun FreestyleEditorContent(
    uiState: FreestyleEditorUiState,
    onClose: () -> Unit,
    onDone: (canvasSizePx: IntSize) -> Unit,
    onTransformLayer: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onScaleRotateLayer: (id: Long, scale: Float, rotationDegrees: Float) -> Unit,
    onSelectLayer: (id: Long) -> Unit,
    onDeleteLayer: (id: Long) -> Unit,
    onImageAction: (replaceLayerId: Long?) -> Unit,
    onAddSticker: (String) -> Unit,
    onAddText: (String, Color, TextFontStyleOption) -> Unit,
    onUpdateText: (id: Long, text: String, color: Color, font: TextFontStyleOption) -> Unit,
    onRetypeText: (id: Long, text: String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGestureEnd: () -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    onBorderWidthChange: (layerId: Long?, width: Float) -> Unit,
    onCornerRadiusChange: (layerId: Long?, radius: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = freestyleChrome()
    var selectedTool by remember { mutableStateOf(FreestyleTool.Background) }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    // Style for the next new text layer; a selected text layer shows and edits its own instead.
    var newTextFont by remember { mutableStateOf(TextFontStyles[1]) }
    var newTextColor by remember { mutableStateOf(TextColorOptions.first()) }
    // Non-null while the keyboard-docked text bar is open.
    var textEntry by remember { mutableStateOf<TextEntry?>(null) }

    val freestyle = (uiState as? FreestyleEditorUiState.Ready)?.freestyle
    // Border edits and "Replace" target this photo; with none selected they act on every photo / add one.
    val selectedImageLayer = freestyle?.layers?.firstOrNull {
        it.id == selectedLayerId && it.content is FreestyleContent.ImageContent
    }
    val selectedImageId = selectedImageLayer?.id
    val selectedTextLayer = freestyle?.layers?.firstOrNull {
        it.id == selectedLayerId && it.content is FreestyleContent.TextContent
    }
    val selectedText = selectedTextLayer?.content as? FreestyleContent.TextContent

    fun commitTextEntry(entry: TextEntry) {
        // Retyping coalesces into one undo step; closing the bar ends it.
        onGestureEnd()
        val layerId = entry.layerId
        if (layerId == null) {
            onAddText(entry.text.trim(), newTextColor, newTextFont)
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

    // One observer for the whole screen: any finger lift ends the drag/pinch/slider step in progress.
    Box(modifier = modifier.fillMaxSize().onGestureEnd(onGestureEnd)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(chrome.surface)
                // The keyboard is left out on purpose: it slides up over the panels while the canvas
                // keeps its size, and the text bar docks itself above it instead.
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
        ) {
            FreestyleTopBar(
                chrome = chrome,
                canUndo = (uiState as? FreestyleEditorUiState.Ready)?.canUndo == true,
                canRedo = (uiState as? FreestyleEditorUiState.Ready)?.canRedo == true,
                doneEnabled = freestyle != null,
                onClose = onClose,
                onUndo = onUndo,
                onRedo = onRedo,
                onDone = { onDone(canvasSizePx) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(freestyle?.backgroundColor ?: FreestyleState().backgroundColor)
                    // Taps that reach the canvas (off every layer) deselect; layers consume their own taps.
                    .pointerInput(Unit) { detectTapGestures(onTap = { selectedLayerId = null }) },
                contentAlignment = Alignment.Center,
            ) {
                when (uiState) {
                    FreestyleEditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

                    is FreestyleEditorUiState.Ready -> FreestyleCanvas(
                        state = uiState.freestyle,
                        chrome = chrome,
                        selectedLayerId = selectedLayerId,
                        onCanvasSizeChanged = { canvasSizePx = it },
                        onTextLayerDoubleTapped = { layer ->
                            selectedLayerId = layer.id
                            onSelectLayer(layer.id)
                            selectedTool = FreestyleTool.Text
                            openTextEntry(layer)
                        },
                        onLayerSelected = { id ->
                            selectedLayerId = id
                            onSelectLayer(id)
                            // Selecting a label opens the panel that edits it (font, color, retyping).
                            val isText = uiState.freestyle.layers.any { it.id == id && it.content is FreestyleContent.TextContent }
                            if (isText) selectedTool = FreestyleTool.Text
                        },
                        onLayerTransformed = onTransformLayer,
                        onLayerScaleRotated = onScaleRotateLayer,
                        onLayerDeleted = { id ->
                            onDeleteLayer(id)
                            selectedLayerId = null
                        },
                    )

                    is FreestyleEditorUiState.Error -> Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(ToolPanelHeight)) {
                if (freestyle != null) {
                    when (selectedTool) {
                        FreestyleTool.Background -> BackgroundPanel(
                            chrome = chrome,
                            selected = freestyle.backgroundColor,
                            onColorChange = onBackgroundColorChange,
                        )
                        FreestyleTool.Stickers -> StickersPanel(chrome = chrome, onStickerTapped = onAddSticker)
                        FreestyleTool.Border -> BorderPanel(
                            chrome = chrome,
                            state = freestyle,
                            selectedImageLayer = selectedImageLayer,
                            onWidthChange = { onBorderWidthChange(selectedImageId, it) },
                            onRadiusChange = { onCornerRadiusChange(selectedImageId, it) },
                        )
                        FreestyleTool.Text -> TextPanel(
                            chrome = chrome,
                            selectedText = selectedText,
                            font = selectedText?.font ?: newTextFont,
                            color = selectedText?.color ?: newTextColor,
                            onFieldClick = { openTextEntry(selectedTextLayer) },
                            onFontChange = { font ->
                                if (selectedTextLayer != null && selectedText != null) {
                                    onUpdateText(selectedTextLayer.id, selectedText.text, selectedText.color, font)
                                } else {
                                    newTextFont = font
                                }
                            },
                            onColorChange = { color ->
                                if (selectedTextLayer != null && selectedText != null) {
                                    onUpdateText(selectedTextLayer.id, selectedText.text, color, selectedText.font)
                                } else {
                                    newTextColor = color
                                }
                            },
                        )
                    }
                }
            }

            FreestyleToolRow(
                chrome = chrome,
                selected = selectedTool,
                enabled = freestyle != null,
                replacing = selectedImageLayer != null,
                onToolSelected = { selectedTool = it },
                onImageAction = { onImageAction(selectedImageLayer?.id) },
            )
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
    }
}

/**
 * Calls [onEnd] each time the last pointer of a gesture lifts. It watches the final pass without
 * consuming anything, so it sees every gesture inside it while the children handle them as usual.
 */
@Composable
private fun Modifier.onGestureEnd(onEnd: () -> Unit): Modifier {
    val currentOnEnd by rememberUpdatedState(onEnd)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
            } while (event.changes.any { it.pressed })
            currentOnEnd()
        }
    }
}

/**
 * ✕, the title, then undo/redo and the accent ✓ — the photo editor's bar layout in the theme-aware
 * [ToolTopBar] styling. The title takes the leftover width so the bar never overflows.
 */
@Composable
private fun FreestyleTopBar(
    chrome: FreestyleChrome,
    canUndo: Boolean,
    canRedo: Boolean,
    doneEnabled: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarCircleButton(
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            onClick = onClose,
            background = chrome.content.copy(alpha = 0.06f),
            tint = chrome.content,
        )
        Text(
            text = "Freestyle",
            color = chrome.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TopBarCircleButton(
                icon = vectorResource(Res.drawable.ic_undo),
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = canUndo,
                background = chrome.content.copy(alpha = 0.06f),
                tint = chrome.content,
            )
            TopBarCircleButton(
                icon = vectorResource(Res.drawable.ic_redo),
                contentDescription = "Redo",
                onClick = onRedo,
                enabled = canRedo,
                background = chrome.content.copy(alpha = 0.06f),
                tint = chrome.content,
            )
            TopBarCircleButton(
                icon = Icons.Filled.Check,
                contentDescription = "Done",
                onClick = onDone,
                enabled = doneEnabled,
                background = if (doneEnabled) chrome.accent else chrome.accent.copy(alpha = 0.35f),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun TopBarCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    background: Color,
    tint: Color,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.35f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** The text being typed in the keyboard-docked bar; [layerId] is the label being edited, or null for a new one. */
private data class TextEntry(val layerId: Long?, val text: String)

/**
 * A text field docked right above the keyboard, over a scrim covering the rest of the screen.
 * It focuses itself on open so the keyboard comes up straight away; the keyboard's Done key, the
 * button, or a tap on the scrim commits.
 */
@Composable
private fun TextEntryBar(chrome: FreestyleChrome, entry: TextEntry, onTextChange: (String) -> Unit, onCommit: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val commit = {
        focusManager.clearFocus()
        onCommit()
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) { detectTapGestures(onTap = { commit() }) },
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(chrome.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                // Swallows taps on the bar itself so they don't reach the scrim.
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(chrome.field)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (entry.text.isEmpty()) {
                    Text(text = "Your Text", color = chrome.muted, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = entry.text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = chrome.content),
                    cursorBrush = SolidColor(chrome.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = commit,
                enabled = entry.layerId != null || entry.text.isNotBlank(),
                shape = RoundedCornerShape(50),
            ) {
                Text(if (entry.layerId == null) "Add" else "Done", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * The canvas fills the whole area between the top bar and the panels, in two stacked parts: the
 * layers, clipped to the canvas exactly as the bake crops them, and above it an unclipped overlay
 * for the selected layer's outline and handles.
 */
@Composable
private fun FreestyleCanvas(
    state: FreestyleState,
    chrome: FreestyleChrome,
    selectedLayerId: Long?,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onTextLayerDoubleTapped: (FreestyleLayer) -> Unit,
    onLayerSelected: (Long) -> Unit,
    onLayerTransformed: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onLayerScaleRotated: (id: Long, scale: Float, rotationDegrees: Float) -> Unit,
    onLayerDeleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Stickers and text size themselves, so the overlay reads their measured box from here.
    val layerSizes = remember { mutableStateMapOf<Long, IntSize>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasSize = it
                onCanvasSizeChanged(it)
            },
    ) {
        if (canvasSize == IntSize.Zero) return@Box
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            for (layer in state.layers) {
                key(layer.id) {
                    FreestyleLayerView(
                        layer = layer,
                        canvasSize = canvasSize,
                        onMeasured = { layerSizes[layer.id] = it },
                        onSelect = { onLayerSelected(layer.id) },
                        onDoubleTap = if (layer.content is FreestyleContent.TextContent) {
                            { onTextLayerDoubleTapped(layer) }
                        } else {
                            null
                        },
                        onTransform = { pan, zoom, rotation -> onLayerTransformed(layer.id, pan, zoom, rotation) },
                    )
                }
            }
        }

        val selected = state.layers.firstOrNull { it.id == selectedLayerId }
        val selectedSize = selected?.let { layerSizes[it.id] }
        if (selected != null && selectedSize != null && selectedSize != IntSize.Zero) {
            SelectionOverlay(
                layer = selected,
                sizePx = selectedSize,
                canvasSize = canvasSize,
                chrome = chrome,
                onScaleRotate = { scale, rotation -> onLayerScaleRotated(selected.id, scale, rotation) },
                onDelete = { onLayerDeleted(selected.id) },
            )
        }
    }
}

/** The on-screen box of [layer], centered on its offset; photos have a computed size, the rest wrap. */
@Composable
private fun layerBoxModifier(layer: FreestyleLayer, canvasSize: IntSize, measuredPx: IntSize): Modifier {
    val density = LocalDensity.current
    val content = layer.content
    val (widthPx, heightPx) = if (content is FreestyleContent.ImageContent) {
        val w = canvasSize.width * FreestyleImageBaseWidthFraction * layer.scale
        w to w * (content.image.height.toFloat() / content.image.width.toFloat())
    } else {
        measuredPx.width.toFloat() to measuredPx.height.toFloat()
    }
    val centerPx = Offset(layer.offsetFraction.x * canvasSize.width, layer.offsetFraction.y * canvasSize.height)
    return Modifier
        .offset { IntOffset((centerPx.x - widthPx / 2f).roundToInt(), (centerPx.y - heightPx / 2f).roundToInt()) }
        .then(
            if (content is FreestyleContent.ImageContent) {
                Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            } else {
                Modifier
            },
        )
        .graphicsLayer { rotationZ = layer.rotationDegrees }
}

@Composable
private fun BoxScope.FreestyleLayerView(
    layer: FreestyleLayer,
    canvasSize: IntSize,
    onMeasured: (IntSize) -> Unit,
    onSelect: () -> Unit,
    onDoubleTap: (() -> Unit)?,
    onTransform: (panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
) {
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    // The pointerInput blocks outlive recompositions, so read the latest values through state.
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentRotation by rememberUpdatedState(layer.rotationDegrees)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentCanvasSize by rememberUpdatedState(canvasSize)
    val content = layer.content
    val photoShape = RoundedCornerShape(layer.cornerRadius.dp)

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .then(layerBoxModifier(layer, canvasSize, measuredSize))
            .onSizeChanged {
                measuredSize = it
                onMeasured(it)
            }
            // Selects on touch-down rather than on tap: a tap only completes once the double-tap wait
            // runs out, which made selecting a text layer lag — and a second tap in that window read
            // as a double tap. Also consumes the tap so the canvas underneath doesn't deselect.
            .pointerInput(onDoubleTap != null) {
                detectTapGestures(
                    onPress = { currentOnSelect() },
                    onDoubleTap = if (onDoubleTap != null) ({ currentOnDoubleTap?.invoke() }) else null,
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    // The box is rotated, so its pan arrives in rotated coordinates; turn it back.
                    val canvasPan = rotateVector(pan, currentRotation)
                    currentOnTransform(
                        Offset(canvasPan.x / currentCanvasSize.width, canvasPan.y / currentCanvasSize.height),
                        zoom,
                        rotation,
                    )
                }
            }
            .then(
                if (content is FreestyleContent.ImageContent) {
                    // Stroking the clipped outline at double width leaves a frame of exactly
                    // borderWidth inside the photo's edge — the same geometry bakeFreestyle draws.
                    Modifier
                        .clip(photoShape)
                        .drawWithContent {
                            drawContent()
                            if (layer.borderWidth > 0f) {
                                drawOutline(
                                    outline = photoShape.createOutline(size, layoutDirection, this),
                                    color = FreestyleImageBorderColor,
                                    style = Stroke(width = layer.borderWidth.dp.toPx() * 2f),
                                )
                            }
                        }
                } else {
                    Modifier.padding(12.dp)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (content) {
            is FreestyleContent.ImageContent -> Image(
                bitmap = content.image,
                contentDescription = "Layer photo",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            is FreestyleContent.StickerContent -> Text(
                text = content.emoji,
                fontSize = (FreestyleStickerBaseSizeSp * layer.scale).sp,
                maxLines = 1,
                softWrap = false,
            )

            is FreestyleContent.TextContent -> Text(
                text = content.text,
                color = content.color,
                fontFamily = content.font.fontFamily,
                fontWeight = content.font.fontWeight,
                fontStyle = content.font.fontStyle,
                fontSize = (FreestyleTextBaseSizeSp * layer.scale).sp,
                softWrap = false,
            )
        }
    }
}

/**
 * Outline plus handles for the selected layer, laid over it with the same box and rotation: a
 * delete button on the bottom-left and a resize/rotate handle on the bottom-right. The box itself
 * has no pointer input, so drags anywhere else still fall through to the layer underneath.
 */
@Composable
private fun BoxScope.SelectionOverlay(
    layer: FreestyleLayer,
    sizePx: IntSize,
    canvasSize: IntSize,
    chrome: FreestyleChrome,
    onScaleRotate: (scale: Float, rotationDegrees: Float) -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val currentScale by rememberUpdatedState(layer.scale)
    val currentRotation by rememberUpdatedState(layer.rotationDegrees)
    val currentSize by rememberUpdatedState(sizePx)
    val currentOnScaleRotate by rememberUpdatedState(onScaleRotate)
    val outlineShape = if (layer.content is FreestyleContent.ImageContent) {
        RoundedCornerShape(layer.cornerRadius.dp)
    } else {
        RoundedCornerShape(8.dp)
    }

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .then(layerBoxModifier(layer, canvasSize, sizePx))
            .size(with(density) { sizePx.width.toDp() }, with(density) { sizePx.height.toDp() })
            .border(1.5.dp, chrome.accent, outlineShape),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -DeleteHandleSize / 3, y = DeleteHandleSize / 3)
                .size(DeleteHandleSize)
                .clip(CircleShape)
                .background(Color(0xFFEF3B4E))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete layer", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = ResizeHandleSize / 2, y = ResizeHandleSize / 2)
                .size(ResizeHandleSize)
                .clip(CircleShape)
                .background(lerp(Color.White, chrome.accent, 0.22f))
                .pointerInput(Unit) {
                    // Tracks the finger as a vector from the layer's center in canvas coordinates: it
                    // starts at the (rotated) corner and accumulates each drag delta rotated out of the
                    // box's frame. Its length relative to the start sets the scale and its angle the
                    // rotation, so the handle follows the finger like a real corner.
                    var startScale = 1f
                    var startRotation = 0f
                    var startVector = Offset.Zero
                    var fingerVector = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            startScale = currentScale
                            startRotation = currentRotation
                            startVector = rotateVector(
                                Offset(currentSize.width / 2f, currentSize.height / 2f),
                                currentRotation,
                            )
                            fingerVector = startVector
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        fingerVector += rotateVector(dragAmount, currentRotation)
                        val startDistance = startVector.getDistance()
                        if (startDistance > 0f) {
                            currentOnScaleRotate(
                                startScale * fingerVector.getDistance() / startDistance,
                                snapTextRotation(
                                    startRotation + vectorAngleDegrees(fingerVector) - vectorAngleDegrees(startVector),
                                ),
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.OpenInFull,
                contentDescription = "Drag to resize and rotate",
                tint = chrome.accent,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun FreestyleToolRow(
    chrome: FreestyleChrome,
    selected: FreestyleTool,
    enabled: Boolean,
    replacing: Boolean,
    onToolSelected: (FreestyleTool) -> Unit,
    onImageAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        FreestyleTool.entries.forEach { tool ->
            ToolItem(
                chrome = chrome,
                icon = tool.icon,
                label = tool.label,
                selected = tool == selected,
                enabled = enabled,
                onClick = { onToolSelected(tool) },
            )
        }
        ToolItem(
            chrome = chrome,
            icon = if (replacing) Icons.Outlined.SwapHoriz else Icons.Outlined.AddPhotoAlternate,
            label = if (replacing) "Replace" else "Add Image",
            selected = false,
            enabled = enabled,
            onClick = onImageAction,
        )
    }
}

@Composable
private fun ToolItem(
    chrome: FreestyleChrome,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = when {
        selected -> chrome.accent
        enabled -> chrome.content
        else -> chrome.muted
    }
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun PanelHeader(chrome: FreestyleChrome, title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = chrome.content,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(text = trailing, fontSize = 12.sp, color = chrome.muted)
        }
    }
}

@Composable
private fun BackgroundPanel(chrome: FreestyleChrome, selected: Color, onColorChange: (Color) -> Unit) {
    Column {
        PanelHeader(chrome = chrome, title = "Background")
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().height(118.dp),
        ) {
            items(FreestyleBackgroundColors) { color ->
                BackgroundSwatch(chrome = chrome, color = color, selected = color == selected, onClick = { onColorChange(color) })
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(chrome: FreestyleChrome, color: Color, selected: Boolean, onClick: () -> Unit) {
    val outer = RoundedCornerShape(12.dp)
    val inner = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(outer)
            .border(2.dp, if (selected) chrome.accent else Color.Transparent, outer)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .clip(inner)
            .background(color)
            .border(1.dp, chrome.content.copy(alpha = 0.08f), inner),
    )
}

@Composable
private fun StickersPanel(chrome: FreestyleChrome, onStickerTapped: (String) -> Unit) {
    // null selects the Recent tab, matching EmojiCategoryTabs.
    var category by remember { mutableStateOf<EmojiCategory?>(EmojiCategory.Smileys) }
    var recent by remember { mutableStateOf(emptyList<String>()) }

    Column {
        PanelHeader(chrome = chrome, title = "Stickers")
        EmojiCategoryTabs(selected = category, onSelected = { category = it })
        EmojiGrid(
            emojis = category?.let { EmojisByCategory[it].orEmpty() } ?: recent,
            onEmojiTapped = { emoji ->
                onStickerTapped(emoji)
                recent = (listOf(emoji) + (recent - emoji)).take(MaxRecentEmojis)
            },
            modifier = Modifier.fillMaxWidth().height(100.dp),
        )
    }
}

@Composable
private fun BorderPanel(
    chrome: FreestyleChrome,
    state: FreestyleState,
    selectedImageLayer: FreestyleLayer?,
    onWidthChange: (Float) -> Unit,
    onRadiusChange: (Float) -> Unit,
) {
    // With nothing selected the sliders edit every photo, so they start from the first one's values.
    val shown = selectedImageLayer ?: state.layers.firstOrNull { it.content is FreestyleContent.ImageContent }

    Column {
        PanelHeader(
            chrome = chrome,
            title = "Border",
            trailing = if (selectedImageLayer != null) "Selected photo" else "All photos",
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BorderSliderRow(
                chrome = chrome,
                icon = Icons.Outlined.SpaceDashboard,
                contentDescription = "Border width",
                value = shown?.borderWidth ?: FreestyleDefaultBorderWidth,
                range = FreestyleBorderWidthRange,
                onValueChange = onWidthChange,
            )
            BorderSliderRow(
                chrome = chrome,
                icon = Icons.Outlined.RoundedCorner,
                contentDescription = "Corner radius",
                value = shown?.cornerRadius ?: FreestyleDefaultCornerRadius,
                range = FreestyleCornerRadiusRange,
                onValueChange = onRadiusChange,
            )
        }
    }
}

@Composable
private fun BorderSliderRow(
    chrome: FreestyleChrome,
    icon: ImageVector,
    contentDescription: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = chrome.content, modifier = Modifier.size(22.dp))
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            referenceValue = range.start,
            trackColor = chrome.track,
            fillColor = chrome.accent,
            thumbColor = chrome.accent,
            thumbWidth = 26.dp,
            thumbHeight = 14.dp,
            horizontalPadding = 24.dp,
            glassThumb = true,
            glassTint = if (chrome.isLight) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * A tap-to-type field (opening [TextEntryBar] above the keyboard) plus the Text tool's font and
 * color rows. With a text layer selected ([selectedText]) the field shows its text and the rows
 * restyle it; otherwise they set the style of the next label added.
 */
@Composable
private fun TextPanel(
    chrome: FreestyleChrome,
    selectedText: FreestyleContent.TextContent?,
    font: TextFontStyleOption,
    color: Color,
    onFieldClick: () -> Unit,
    onFontChange: (TextFontStyleOption) -> Unit,
    onColorChange: (Color) -> Unit,
) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(chrome.field)
                .clickable(onClick = onFieldClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = selectedText?.text ?: "Tap to add text",
                color = if (selectedText != null) chrome.content else chrome.muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        FontRow(selected = font, onSelected = onFontChange)
        Spacer(modifier = Modifier.height(10.dp))
        ColorRow(selected = color, onSelected = onColorChange)
    }
}

@Preview
@Composable
private fun FreestyleEditorScreenPreview() {
    ThemePreviews {
        FreestyleEditorContent(
            uiState = FreestyleEditorUiState.Ready(FreestyleState()),
            onClose = {},
            onDone = {},
            onTransformLayer = { _, _, _, _ -> },
            onScaleRotateLayer = { _, _, _ -> },
            onSelectLayer = {},
            onDeleteLayer = {},
            onImageAction = {},
            onAddSticker = {},
            onAddText = { _, _, _ -> },
            onUpdateText = { _, _, _, _ -> },
            onRetypeText = { _, _ -> },
            onUndo = {},
            onRedo = {},
            onGestureEnd = {},
            onBackgroundColorChange = {},
            onBorderWidthChange = { _, _ -> },
            onCornerRadiusChange = { _, _ -> },
        )
    }
}
