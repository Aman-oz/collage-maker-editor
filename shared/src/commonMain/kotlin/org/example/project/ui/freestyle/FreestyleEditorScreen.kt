package org.example.project.ui.freestyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BorderClear
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.TextColorOptions
import org.example.project.ui.text.TextFontStyleOption
import org.example.project.ui.text.TextFontStyles
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_undo

/** [icon] is composable so a tool can use a drawable resource, which only loads in composition. */
private enum class FreestyleTool(val label: String, val icon: @Composable () -> ImageVector) {
    Background("Background", { vectorResource(Res.drawable.ic_background) }),
    Stickers("Stickers", { Icons.Outlined.EmojiEmotions }),
    Border("Border", { Icons.Outlined.BorderClear }),
    Text("Text", { Icons.Outlined.TextFields }),
}

/**
 * Fixed height of the tool panel above the tool row. Every panel fits it, so switching tools never
 * resizes the canvas — which would shift every layer, since positions are fractions of its height.
 */
private val ToolPanelHeight = 190.dp


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
                        layers = uiState.freestyle.layers,
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
        modifier = Modifier.topBar(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTopBarButton(
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            onClick = onClose,
            contentColor = chrome.content,
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
            GlassTopBarButton(
                icon = Icons.Filled.Check,
                contentDescription = "Done",
                onClick = onDone,
                enabled = doneEnabled,
                style = GlassButtonStyle.Primary,
                accentColor = chrome.accent,
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
            .size(TopBarButtonSize)
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
                icon = tool.icon(),
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
