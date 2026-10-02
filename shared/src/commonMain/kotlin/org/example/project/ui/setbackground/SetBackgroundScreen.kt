package org.example.project.ui.setbackground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import org.example.project.i18n.tr
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.StickerFlightOverlay
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.rememberStickerFlights
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.freestyle.FreestyleCanvas
import org.example.project.ui.freestyle.FreestyleContent
import org.example.project.ui.freestyle.FreestyleFill
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.FreestyleStickerBaseSizeSp
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
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_undo

private val SwatchSize = 56.dp
private val SwatchSpacing = 6.dp

/** A gallery tile spans both rows of the picker grid. */
private val TallTileHeight = SwatchSize * 2 + SwatchSpacing

/**
 * Fixed height of the panel above the bottom tabs. Every panel fits it, so switching tabs never
 * resizes the photo — which would move every text/sticker layer, since they sit at fractions of it.
 */
private val ToolPanelHeight = 190.dp

/** The bottom tabs, each opening its panel under the photo. */
private enum class SetBackgroundTool { Background, Text, Stickers }

/** The two pages of the Background panel. */
private enum class BackdropTab(private val englishLabel: String) {
    Colour("Colour"),
    Gradient("Gradient"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * Background remover step 3: put a colour, gradient or photo behind the transparent cut-out, and
 * add text and stickers over it (the freestyle editor's layers). Done flattens it all into the
 * session and opens the photo editor.
 */
@Composable
fun SetBackgroundScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    onPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetBackgroundViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) viewModel.addPhoto(file.path)
    }

    SetBackgroundContent(
        sourceImage = viewModel.cutOut,
        uiState = uiState,
        photos = photos,
        onBack = onBack,
        onDone = { previewSizePx ->
            // A premium backdrop, text or stickers show a non-subscriber the paywall once, as an
            // offer. The edit lives in the ViewModel, so it is still here when they come back, and
            // the next Done goes through whether or not they subscribed.
            if (viewModel.consumePremiumOffer()) {
                onPremium()
            } else {
                viewModel.apply(textMeasurer, previewSizePx.width.toFloat(), density.density)
                onApplied()
            }
        },
        onAddPhoto = { photoPicker.launch() },
        onSelectBackdrop = viewModel::selectBackdrop,
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
        modifier = modifier,
    )
}

@Composable
private fun SetBackgroundContent(
    sourceImage: ImageBitmap?,
    uiState: SetBackgroundUiState,
    photos: List<ImageBitmap>,
    onBack: () -> Unit,
    onDone: (previewSizePx: IntSize) -> Unit,
    onAddPhoto: () -> Unit,
    onSelectBackdrop: (Backdrop) -> Unit,
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
    var tool by remember { mutableStateOf(SetBackgroundTool.Background) }
    var tab by remember { mutableStateOf(BackdropTab.Colour) }
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
                // The keyboard is left out on purpose: it slides up over the panels while the photo
                // keeps its size, and the text bar docks itself above it instead.
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
        ) {
            SetBackgroundTopBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                doneEnabled = sourceImage != null,
                onClose = onBack,
                onUndo = onUndo,
                onRedo = onRedo,
                onDone = { onDone(previewSizePx) },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Taps that reach here (off every layer) deselect; layers consume their own taps.
                    .pointerInput(Unit) { detectTapGestures(onTap = { selectedLayerId = null }) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceImage != null) {
                    Box(modifier = Modifier.aspectRatio(sourceImage.width.toFloat() / sourceImage.height)) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawComposite(sourceImage, edit.backdrop) }
                        FreestyleCanvas(
                            stickerFlights = stickerFlights,
                            layers = edit.layers,
                            chrome = chrome,
                            selectedLayerId = selectedLayerId,
                            onCanvasSizeChanged = { previewSizePx = it },
                            onTextLayerDoubleTapped = { layer ->
                                selectedLayerId = layer.id
                                onSelectLayer(layer.id)
                                tool = SetBackgroundTool.Text
                                openTextEntry(layer)
                            },
                            onLayerSelected = { id ->
                                selectedLayerId = id
                                onSelectLayer(id)
                                // Selecting a label opens the panel that edits it (font, color, retyping).
                                val isText = edit.layers.any { it.id == id && it.content is FreestyleContent.TextContent }
                                if (isText) tool = SetBackgroundTool.Text
                            },
                            onLayerTransformed = onTransformLayer,
                            onLayerScaleRotated = onScaleRotateLayer,
                            onLayerDeleted = { id ->
                                onDeleteLayer(id)
                                selectedLayerId = null
                            },
                        )
                    }
                } else {
                    Text(tr("No image to edit"), color = chrome.content, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(ToolPanelHeight)) {
                when (tool) {
                    SetBackgroundTool.Background -> BackgroundPanel(
                        tab = tab,
                        onTabSelected = { tab = it },
                        selected = edit.backdrop,
                        photos = photos,
                        onAddPhoto = onAddPhoto,
                        onSelected = onSelectBackdrop,
                    )
                    SetBackgroundTool.Text -> TextPanel(
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
                    SetBackgroundTool.Stickers -> StickersPanel(
                        chrome = chrome,
                        onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { onAddSticker(emoji) } },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_background),
                    label = tr("Background"),
                    selected = tool == SetBackgroundTool.Background,
                    onClick = { tool = SetBackgroundTool.Background },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_text_editor),
                    label = tr("Text"),
                    selected = tool == SetBackgroundTool.Text,
                    enabled = sourceImage != null,
                    onClick = { tool = SetBackgroundTool.Text },
                )
                BottomTab(
                    icon = vectorResource(Res.drawable.ic_stickers_editor),
                    label = tr("Stickers"),
                    selected = tool == SetBackgroundTool.Stickers,
                    enabled = sourceImage != null,
                    onClick = { tool = SetBackgroundTool.Stickers },
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

/** The Colour / Gradient switch over a grid led by the gallery tiles. */
@Composable
private fun BackgroundPanel(
    tab: BackdropTab,
    onTabSelected: (BackdropTab) -> Unit,
    selected: Backdrop,
    photos: List<ImageBitmap>,
    onAddPhoto: () -> Unit,
    onSelected: (Backdrop) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val selectedColor = (selected as? Backdrop.Solid)?.color
    val pickCustomColor = rememberColorPickerLauncher(
        // A gradient or photo backdrop has no one colour to open on, so the picker starts from white.
        initial = selectedColor ?: Color.White,
        onPicked = { onSelected(Backdrop.Solid(it)) },
    )
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        BackdropTabSwitch(
            selected = tab,
            onSelected = onTabSelected,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SwatchSpacing),
            verticalArrangement = Arrangement.spacedBy(SwatchSpacing),
            modifier = Modifier.fillMaxWidth().height(TallTileHeight),
        ) {
            // Both tabs lead with the gallery tiles; each spans both rows so the colour pairs
            // behind them stay aligned (top shade over bottom shade) however many photos there are.
            item(span = { GridItemSpan(maxLineSpan) }) {
                BackdropTile(selected = false, height = TallTileHeight, onClick = onAddPhoto) {
                    Column(
                        modifier = Modifier.fillMaxSize().background(scheme.onSurface.copy(alpha = 0.06f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = scheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tr("Gallery"),
                            color = scheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
            items(photos, span = { GridItemSpan(maxLineSpan) }) { photo ->
                val backdrop = Backdrop.Photo(photo)
                BackdropTile(selected = backdrop == selected, height = TallTileHeight, onClick = { onSelected(backdrop) }) {
                    Image(
                        bitmap = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            when (tab) {
                BackdropTab.Colour -> {
                    // The custom colour tile spans both rows like the gallery tiles, for the same
                    // reason. It is the one ringed while the backdrop is a colour of the user's own.
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BackdropTile(
                            selected = selectedColor != null && selectedColor !in BackdropSolidColors,
                            height = TallTileHeight,
                            onClick = pickCustomColor,
                        ) { ColorPickerFill(Modifier.fillMaxSize()) }
                    }
                    items(BackdropSolidColors) { color ->
                        val backdrop = Backdrop.Solid(color)
                        BackdropTile(selected = backdrop == selected, onClick = { onSelected(backdrop) }) {
                            Box(modifier = Modifier.fillMaxSize().background(color))
                        }
                    }
                }
                BackdropTab.Gradient -> items(BackdropGradients) { colors ->
                    val backdrop = Backdrop.Gradient(colors)
                    BackdropTile(selected = backdrop == selected, onClick = { onSelected(backdrop) }) {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors)))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetBackgroundTopBar(
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
            text = tr("Background"),
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

/** The Colour / Gradient pill switch; the selected half is a soft primary tint. */
@Composable
private fun BackdropTabSwitch(selected: BackdropTab, onSelected: (BackdropTab) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .width(240.dp)
            .clip(CircleShape)
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .padding(4.dp),
    ) {
        for (tab in BackdropTab.entries) {
            val isSelected = tab == selected
            val bounce = rememberSpringBounce()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .springBounce(bounce)
                    .clip(CircleShape)
                    .background(if (isSelected) scheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable(
                        interactionSource = bounce.interactionSource,
                        indication = LocalIndication.current,
                    ) { onSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    color = scheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A picker tile styled like [SelectableSwatch] (accent ring with a gap when selected), but with a
 * free [height] so the gallery tiles can span both grid rows. The hairline edge keeps white (and
 * pale gradients) visible on a light screen.
 */
@Composable
private fun BackdropTile(selected: Boolean, onClick: () -> Unit, height: Dp = SwatchSize, content: @Composable () -> Unit) {
    val ring = RoundedCornerShape(SwatchInnerCorner + 4.dp)
    val inner = RoundedCornerShape(SwatchInnerCorner)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(width = SwatchSize, height = height)
            .clip(ring)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, ring) else Modifier)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(4.dp)
            .clip(inner)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), inner),
    ) { content() }
}

@Composable
private fun BottomTab(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val scheme = MaterialTheme.colorScheme
    val tint = when {
        selected -> scheme.primary
        enabled -> scheme.onSurface
        else -> scheme.onSurface.copy(alpha = 0.35f)
    }
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
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
private fun SetBackgroundPreview() {
    ThemePreviews {
        SetBackgroundContent(
            sourceImage = ImageBitmap(360, 480),
            uiState = SetBackgroundUiState(),
            photos = emptyList(),
            onBack = {},
            onDone = {},
            onAddPhoto = {},
            onSelectBackdrop = {},
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
