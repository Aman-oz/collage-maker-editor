package org.example.project.ui.freestyle

import org.example.project.i18n.tr
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.rememberDiscardChangesState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerDialog
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.StickerFlightOverlay
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.rememberStickerFlights
import org.example.project.ui.common.springBounce
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
private enum class FreestyleTool(private val englishLabel: String, val icon: @Composable () -> ImageVector) {
    Background("Background", { vectorResource(Res.drawable.ic_background) }),
    Stickers("Stickers", { Icons.Outlined.EmojiEmotions }),
    Border("Border", { Icons.Outlined.BorderClear }),
    Text("Text", { Icons.Outlined.TextFields }),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
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
    onPremium: () -> Unit,
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

    val discard = rememberDiscardChangesState(
        hasChanges = (uiState as? FreestyleEditorUiState.Ready)?.canUndo == true,
        onBack = onBack,
    )
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        FreestyleEditorContent(
            uiState = uiState,
            onClose = discard::requestBack,
            onDone = { canvasSizePx ->
                // A canvas that uses something premium shows a non-subscriber the paywall once, as
                // an offer. The canvas lives in the ViewModel, so it is still here when they come
                // back, and the next Done goes through whether or not they subscribed.
                if (viewModel.consumePremiumOffer()) {
                    onPremium()
                } else {
                    viewModel.applyFreestyle(textMeasurer, canvasSizePx.width.toFloat(), canvasSizePx.height.toFloat(), density.density)
                    onOpenEditor()
                }
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
            onBackgroundChange = viewModel::updateBackground,
            onBorderWidthChange = viewModel::updateImageBorderWidth,
            onCornerRadiusChange = viewModel::updateImageCornerRadius,
            modifier = Modifier.liquefiable(liquidState),
        )
        DiscardChangesPopup(discard, liquidState)
    }
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
    onAddText: (String, FreestyleFill, TextFontStyleOption, FreestyleFill?) -> Unit,
    onUpdateText: (id: Long, text: String, fill: FreestyleFill, font: TextFontStyleOption, background: FreestyleFill?) -> Unit,
    onRetypeText: (id: Long, text: String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onGestureEnd: () -> Unit,
    onBackgroundChange: (FreestyleFill) -> Unit,
    onBorderWidthChange: (layerId: Long?, width: Float) -> Unit,
    onCornerRadiusChange: (layerId: Long?, radius: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = freestyleChrome()
    val stickerFlights = rememberStickerFlights(FreestyleStickerBaseSizeSp)
    var selectedTool by remember { mutableStateOf(FreestyleTool.Background) }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }
    // Style for the next new text layer; a selected text layer shows and edits its own instead.
    var newTextFont by remember { mutableStateOf(TextFontStyles[1]) }
    var newTextFill by remember { mutableStateOf<FreestyleFill>(FreestyleFill.Solid(TextColorOptions.first())) }
    var newTextBackground by remember { mutableStateOf<FreestyleFill?>(null) }
    // Non-null while the keyboard-docked text bar is open.
    var textEntry by remember { mutableStateOf<TextEntry?>(null) }
    var showGuide by remember { mutableStateOf(false) }

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
                onHelp = { showGuide = true },
                onUndo = onUndo,
                onRedo = onRedo,
                onDone = { onDone(canvasSizePx) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background((freestyle ?: FreestyleState()).background.brush)
                    // Taps that reach the canvas (off every layer) deselect; layers consume their own taps.
                    .pointerInput(Unit) { detectTapGestures(onTap = { selectedLayerId = null }) },
                contentAlignment = Alignment.Center,
            ) {
                when (uiState) {
                    FreestyleEditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

                    is FreestyleEditorUiState.Ready -> FreestyleCanvas(
                        stickerFlights = stickerFlights,
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
                            selected = freestyle.background,
                            onSelected = onBackgroundChange,
                        )
                        FreestyleTool.Stickers -> StickersPanel(
                            chrome = chrome,
                            onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { onAddSticker(emoji) } },
                        )
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

        StickerFlightOverlay(stickerFlights)

        // Last, so it covers the whole editor, top bar included.
        if (showGuide) FreestyleGuideOverlay(onDismiss = { showGuide = false })
    }
}


/**
 * ✕, the title, then the guide's "?", undo/redo and the accent ✓, in the theme-aware [ToolTopBar]
 * styling. The title takes the leftover width so the bar never overflows.
 */
@Composable
private fun FreestyleTopBar(
    chrome: FreestyleChrome,
    canUndo: Boolean,
    canRedo: Boolean,
    doneEnabled: Boolean,
    onClose: () -> Unit,
    onHelp: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    // A Row, not ToolTopBar's screen-centred Box: with the guide button there are four controls on
    // the right, too many to mirror with padding on the left without squeezing the title out on a
    // narrow phone. The title is centred in the space between ✕ and the guide button instead.
    Row(modifier = Modifier.topBar(), verticalAlignment = Alignment.CenterVertically) {
        GlassTopBarButton(
            icon = Icons.Filled.Close,
            contentDescription = tr("Close"),
            onClick = onClose,
            contentColor = chrome.content,
        )
        Text(
            text = tr("Freestyle"),
            color = chrome.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        GuideButton(tint = chrome.content, onClick = onHelp)
        Spacer(modifier = Modifier.width(4.dp))
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_undo),
            contentDescription = tr("Undo"),
            enabled = canUndo,
            onClick = onUndo,
            tint = chrome.content,
        )
        Spacer(modifier = Modifier.width(8.dp))
        UndoRedoButton(
            icon = vectorResource(Res.drawable.ic_redo),
            contentDescription = tr("Redo"),
            enabled = canRedo,
            onClick = onRedo,
            tint = chrome.content,
        )
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

/**
 * The "?" that opens the usage guide: a bare glyph like undo/redo beside it, but a size down, so
 * it reads as a quiet extra rather than a fourth editing control.
 */
@Composable
private fun GuideButton(tint: Color, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = tr("Help"),
            tint = tint,
            modifier = Modifier.size(20.dp),
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
            label = if (replacing) tr("Replace") else tr("Add Image"),
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
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            )
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


/** The two swatch sets of the Background panel; Colour is the one led by the custom colour picker. */
private enum class BackgroundTab(private val englishLabel: String) {
    Colour("Colour"),
    Gradient("Gradient"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

private val BackgroundSwatchSize = 52.dp
private val BackgroundSwatchSpacing = 10.dp

@Composable
private fun BackgroundPanel(chrome: FreestyleChrome, selected: FreestyleFill, onSelected: (FreestyleFill) -> Unit) {
    // Opens on the tab holding the fill in use. The panel is recreated each time the tool is picked.
    var tab by remember {
        mutableStateOf(if (selected is FreestyleFill.Gradient) BackgroundTab.Gradient else BackgroundTab.Colour)
    }
    var pickingColor by remember { mutableStateOf(false) }
    val selectedColor = (selected as? FreestyleFill.Solid)?.color

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Background"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = chrome.content,
                modifier = Modifier.weight(1f),
            )
            BackgroundTabSwitch(chrome = chrome, selected = tab, onSelected = { tab = it })
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(BackgroundSwatchSpacing),
            verticalArrangement = Arrangement.spacedBy(BackgroundSwatchSpacing),
            modifier = Modifier.fillMaxWidth().height(BackgroundSwatchSize * 2 + BackgroundSwatchSpacing + 8.dp),
        ) {
            when (tab) {
                BackgroundTab.Colour -> {
                    // Spans both rows, so the colour pairs after it stay aligned (shade over shade).
                    // Ringed while the background is a colour of the user's own.
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BackgroundSwatch(
                            chrome = chrome,
                            selected = selectedColor != null && selectedColor !in FreestyleBackgroundColors,
                            onClick = { pickingColor = true },
                            height = BackgroundSwatchSize * 2 + BackgroundSwatchSpacing,
                        ) { ColorPickerFill(it) }
                    }
                    items(FreestyleBackgroundColors) { color ->
                        BackgroundSwatch(
                            chrome = chrome,
                            selected = color == selectedColor,
                            onClick = { onSelected(FreestyleFill.Solid(color)) },
                        ) { Box(modifier = it.background(color)) }
                    }
                }

                BackgroundTab.Gradient -> items(FreestyleBackgroundGradients) { colors ->
                    val gradient = FreestyleFill.Gradient(colors)
                    BackgroundSwatch(chrome = chrome, selected = gradient == selected, onClick = { onSelected(gradient) }) {
                        Box(modifier = it.background(gradient.brush))
                    }
                }
            }
        }
    }

    if (pickingColor) {
        ColorPickerDialog(
            initial = selectedColor ?: Color.White,
            onDismiss = { pickingColor = false },
            onPicked = {
                onSelected(FreestyleFill.Solid(it))
                pickingColor = false
            },
        )
    }
}

/** The Colour / Gradient pill switch in the panel's header; the selected half is a soft accent tint. */
@Composable
private fun BackgroundTabSwitch(chrome: FreestyleChrome, selected: BackgroundTab, onSelected: (BackgroundTab) -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(chrome.content.copy(alpha = 0.06f))
            .padding(3.dp),
    ) {
        for (tab in BackgroundTab.entries) {
            val isSelected = tab == selected
            Text(
                text = tab.label,
                color = if (isSelected) chrome.content else chrome.muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) chrome.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** [fill] paints the swatch inside its selection ring; it is handed the modifier that sizes it. */
@Composable
private fun BackgroundSwatch(
    chrome: FreestyleChrome,
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
            title = tr("Border"),
            trailing = if (selectedImageLayer != null) tr("Selected photo") else tr("All photos"),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BorderSliderRow(
                chrome = chrome,
                icon = Icons.Outlined.SpaceDashboard,
                contentDescription = tr("Border width"),
                value = shown?.borderWidth ?: FreestyleDefaultBorderWidth,
                range = FreestyleBorderWidthRange,
                onValueChange = onWidthChange,
            )
            BorderSliderRow(
                chrome = chrome,
                icon = Icons.Outlined.RoundedCorner,
                contentDescription = tr("Corner radius"),
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
            onAddText = { _, _, _, _ -> },
            onUpdateText = { _, _, _, _, _ -> },
            onRetypeText = { _, _ -> },
            onUndo = {},
            onRedo = {},
            onGestureEnd = {},
            onBackgroundChange = {},
            onBorderWidthChange = { _, _ -> },
            onCornerRadiusChange = { _, _ -> },
        )
    }
}
