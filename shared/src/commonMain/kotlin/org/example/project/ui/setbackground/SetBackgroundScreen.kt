package org.example.project.ui.setbackground

import org.example.project.ui.common.readableWidth
import org.example.project.ui.common.EditorPanelLayout
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
import org.example.project.ui.freestyle.FreestyleStickerBaseSizeSp
import org.example.project.ui.freestyle.StickersPanel
import org.example.project.ui.freestyle.freestyleChrome
import org.example.project.ui.freestyle.onGestureEnd
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.vectorResource
import org.example.project.ui.text.TextLayers
import org.example.project.ui.text.TextLayersOverlay
import org.example.project.ui.text.TextToolOverlay
import org.example.project.ui.stickers.PlacedSticker
import org.example.project.ui.stickers.StickerLayersOverlay
import org.example.project.ui.stickers.StickersToolOverlay
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_background
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_sticker_editor
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

/** The bottom tabs that open a panel under the photo; Text opens the Text tool instead. */
private enum class SetBackgroundTool { Background, Stickers }

/** The two pages of the Background panel. */
private enum class BackdropTab(private val englishLabel: String) {
    Colour("Colour"),
    Gradient("Gradient"),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/**
 * Background remover step 3: put a colour, gradient or photo behind the transparent cut-out, add
 * stickers over it (the freestyle editor's layers) and text over everything with the photo editor's
 * Text tool. Done flattens it all into the session and opens the photo editor.
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
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    val stickers by viewModel.stickers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) viewModel.addPhoto(file.path)
    }

    Box(modifier = modifier.fillMaxSize()) {
        SetBackgroundContent(
            sourceImage = viewModel.cutOut,
            uiState = uiState,
            texts = texts,
            stickers = stickers,
            photos = photos,
            onBack = onBack,
            onDone = { previewSizePx ->
                // A premium backdrop or stickers show a non-subscriber the paywall once, as an
                // offer. The edit lives in the ViewModel, so it is still here when they come back, and
                // the next Done goes through whether or not they subscribed.
                if (viewModel.consumePremiumOffer()) {
                    onPremium()
                } else {
                    scope.launch {
                        if (viewModel.apply(textMeasurer, previewSizePx.width.toFloat(), density.density)) onApplied()
                    }
                }
            },
            onAddPhoto = { photoPicker.launch() },
            onSelectBackdrop = viewModel::selectBackdrop,
            onAddSticker = viewModel::addSticker,
            onOpenTextTool = { previewSizePx, selectedTextId ->
                viewModel.openTextTool(textMeasurer, previewSizePx.width.toFloat(), density.density, selectedTextId)
            },
            onOpenStickersTool = { previewSizePx, selectedStickerId ->
                viewModel.openStickersTool(textMeasurer, previewSizePx.width.toFloat(), density.density, selectedStickerId)
            },
            onTransformLayer = viewModel::transformLayer,
            onScaleRotateLayer = viewModel::setLayerScaleRotation,
            onSelectLayer = viewModel::bringToFront,
            onDeleteLayer = viewModel::removeLayer,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onGestureEnd = viewModel::endGesture,
        )
        // The photo editor's Text and Stickers tools, over the whole screen.
        TextToolOverlay(
            session = viewModel.textSession.value,
            isPremium = isPremium,
            onClose = viewModel::closeTextTool,
            onApply = viewModel::applyTexts,
            onOpenPremium = onPremium,
            // The live stickers sit under the texts, as Done bakes them.
            underlay = { StickerLayersOverlay(stickers, Modifier.fillMaxSize()) },
        )
        StickersToolOverlay(
            session = viewModel.stickersSession.value,
            isPremium = isPremium,
            onClose = viewModel::closeStickersTool,
            onApply = viewModel::applyStickers,
            onOpenPremium = onPremium,
        )
    }
}

@Composable
private fun SetBackgroundContent(
    sourceImage: ImageBitmap?,
    uiState: SetBackgroundUiState,
    texts: TextLayers?,
    stickers: List<PlacedSticker>,
    photos: List<ImageBitmap>,
    onBack: () -> Unit,
    onDone: (previewSizePx: IntSize) -> Unit,
    onAddPhoto: () -> Unit,
    onSelectBackdrop: (Backdrop) -> Unit,
    onAddSticker: (String) -> Unit,
    onOpenTextTool: (previewSizePx: IntSize, selectedTextId: Long?) -> Unit,
    onOpenStickersTool: (previewSizePx: IntSize, selectedStickerId: Long?) -> Unit,
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
    val openTextTool = { selectedTextId: Long? ->
        selectedLayerId = null
        onOpenTextTool(previewSizePx, selectedTextId)
    }
    val openStickersTool = { selectedStickerId: Long? ->
        selectedLayerId = null
        onOpenStickersTool(previewSizePx, selectedStickerId)
    }

    // One observer for the whole screen: any finger lift ends the drag/pinch step in progress.
    Box(modifier = modifier.fillMaxSize().onGestureEnd(onGestureEnd)) {
        EditorPanelLayout(
            panelHeight = ToolPanelHeight,
            modifier = Modifier
                .background(chrome.surface)
                // The keyboard is left out on purpose: the Text tool's slides up over the screen, which
                // keeps its size under it (the photo's size is what its layers are placed against).
                .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
            topBar = {
                SetBackgroundTopBar(
                    canUndo = uiState.canUndo,
                    canRedo = uiState.canRedo,
                    doneEnabled = sourceImage != null,
                    onClose = onBack,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    onDone = { onDone(previewSizePx) },
                )
            },
            stage = { stageModifier ->
                Box(
                    modifier = stageModifier
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
                                // Texts are the Text tool's, over the photo, not its layers.
                                onTextLayerDoubleTapped = {},
                                onLayerSelected = { id ->
                                    selectedLayerId = id
                                    onSelectLayer(id)
                                },
                                onLayerTransformed = onTransformLayer,
                                onLayerScaleRotated = onScaleRotateLayer,
                                onLayerDeleted = { id ->
                                    onDeleteLayer(id)
                                    selectedLayerId = null
                                },
                            )
                            // The Stickers tool's stickers, over the emoji; a tap on one opens the tool on it.
                            StickerLayersOverlay(
                                stickers = stickers,
                                modifier = Modifier.fillMaxSize(),
                                onStickerTapped = { id -> openStickersTool(id) },
                            )
                            // Above the stickers, as Done bakes them; a tap on one opens the Text tool on it.
                            if (texts != null) {
                                TextLayersOverlay(
                                    layers = texts,
                                    onTextTapped = { id -> openTextTool(id) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    } else {
                        Text(tr("No image to edit"), color = chrome.content, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            panel = {
                when (tool) {
                    SetBackgroundTool.Background -> BackgroundPanel(
                        tab = tab,
                        onTabSelected = { tab = it },
                        selected = edit.backdrop,
                        photos = photos,
                        onAddPhoto = onAddPhoto,
                        onSelected = onSelectBackdrop,
                    )
                    SetBackgroundTool.Stickers -> StickersPanel(
                        chrome = chrome,
                        onStickerTapped = { emoji, from -> stickerFlights.launch(emoji, from) { onAddSticker(emoji) } },
                    )
                }
            },
            tabs = {
                Row(
                    modifier = Modifier
                        // Capped, so on a tablet the tabs stay grouped instead of spreading across the screen.
                        .readableWidth()
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
                        selected = false,
                        enabled = sourceImage != null,
                        onClick = { openTextTool(null) },
                    )
                    BottomTab(
                        icon = vectorResource(Res.drawable.ic_sticker_editor),
                        label = tr("Stickers"),
                        selected = false,
                        enabled = sourceImage != null,
                        onClick = { openStickersTool(null) },
                    )
                    BottomTab(
                        icon = vectorResource(Res.drawable.ic_stickers_editor),
                        label = tr("Emoji"),
                        selected = tool == SetBackgroundTool.Stickers,
                        enabled = sourceImage != null,
                        onClick = { tool = SetBackgroundTool.Stickers },
                    )
                }
            },
        )

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
            texts = null,
            stickers = emptyList(),
            photos = emptyList(),
            onBack = {},
            onDone = {},
            onAddPhoto = {},
            onSelectBackdrop = {},
            onAddSticker = {},
            onOpenTextTool = { _, _ -> },
            onOpenStickersTool = { _, _ -> },
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
