package org.example.project.ui.text

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.example.project.data.fonts.FontCatalog
import org.example.project.data.fonts.FontEntry
import org.example.project.data.fonts.FontLibrary
import org.example.project.data.fonts.FontLoader
import org.example.project.i18n.tr
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.LocalInlineToolHost
import org.example.project.ui.common.ToolDropMillis
import org.example.project.ui.common.dropSpec
import org.example.project.ui.common.settleSpec
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.compose.koinInject

@Composable
internal fun TextTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val library = koinInject<FontLibrary>()
    // Parsed as the tool opens, so the catalog is ready by the time the Font panel is.
    val catalog by produceState<FontCatalog?>(null) { value = library.catalog() }
    val favorites by library.favorites.collectAsState()
    val recent by library.recent.collectAsState()
    TextContent(
        sourceImage = sourceImage,
        isPremium = isPremium,
        fonts = TextFonts(catalog, favorites, recent, library::toggleFavorite, library::markUsed),
        onBack = onClose,
        onOpenPremium = onOpenPremium,
        onDone = { edit ->
            onApply(
                bakeTextItems(
                    source = sourceImage,
                    textMeasurer = textMeasurer,
                    texts = edit.texts,
                    previewCanvasWidthPx = edit.canvasWidthPx,
                    previewMaxTextWidthPx = edit.maxTextWidthPx,
                ),
            )
        },
        modifier = modifier,
    )
}

/** Everything [bakeTextItems] needs from the preview, handed over on finish. */
private class TextEdit(
    val texts: List<BakeableText>,
    val canvasWidthPx: Float,
    val maxTextWidthPx: Int,
)

/** The padding around the words on the photo; the bake wraps lines at the photo's width less this. */
private val TextLayerPaddingX = 14.dp
private val TextLayerPaddingY = 6.dp

/** The gap between the selected text and its toolbar. */
private val ToolbarGap = 10.dp

/**
 * The last text copied with the ⋯ menu's Copy, for its Paste. Kept for the app's lifetime rather
 * than the tool's, so a text can be copied off one photo and pasted onto the next.
 */
private object TextClipboard {
    var item by mutableStateOf<TextItem?>(null)
}

/** How a text is laid out right now, as the preview measured it (pixels). */
private class TextMetrics(val outer: IntSize, val area: IntSize, val wordsHeight: Int)

/**
 * The text tool: the photo with any number of texts on it ([TextItem]), and under it a floating bar
 * of [TextOption]s whose ✕ finishes the tool, applying the texts. System back still leaves without
 * them.
 *
 * A text is selected with a tap: a rectangle with corner dots (scale) and side bars (resize the text
 * area), and over it a glass toolbar (Duplicate, Delete, and ⋯ for [TextMoreMenu]). A second tap
 * types in it. The options bar, the Font and Font size panels and the keyboard bar all edit the
 * active text: the one selected or being typed in, else the last one.
 *
 * Opening the tool goes straight into typing a first text, the keyboard coming up with
 * [TextKeyboardBar] riding on top of it. The keyboard bar's ⌄, or the Font option, slides the
 * keyboard down and the [TextFontPanel] up; Font size slides up [TextSizePanel]. The panel keeps one
 * height whichever it shows, so the photo never moves between them.
 */
@Composable
private fun TextContent(
    sourceImage: ImageBitmap?,
    isPremium: Boolean,
    fonts: TextFonts,
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    onDone: (TextEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // The edit itself is saveable, unlike most tools': the paywall covers the editor, which drops
    // plain `remember` state, and the texts made before it have to still be there after. Typing is
    // not: coming back, the texts are simply there. A fresh tool starts out typing its first text.
    var items by rememberSaveable(stateSaver = TextItemsSaver) {
        mutableStateOf(if (sourceImage != null) listOf(TextItem(id = 0)) else emptyList())
    }
    var nextId by rememberSaveable { mutableLongStateOf(1L) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by remember { mutableStateOf(items.singleOrNull()?.takeIf { it.content.isEmpty() }?.id) }
    // Set once text that was already written is gone back into (a tap on the selected text, or Edit).
    var reEdited by rememberSaveable { mutableStateOf(false) }
    // The paywall is an offer, not a gate: it is shown once per edit, on the first finish with a
    // premium text edit (see isPremiumTextEdit), and the next finish applies either way.
    var paywallShown by rememberSaveable { mutableStateOf(false) }
    var photoSizePx by remember { mutableStateOf(IntSize.Zero) }
    val metrics = remember { mutableStateMapOf<Long, TextMetrics>() }

    val activeId = editingId ?: selectedId ?: items.lastOrNull()?.id
    val active = items.firstOrNull { it.id == activeId }
    fun update(id: Long?, change: (TextItem) -> TextItem) {
        if (id != null) items = items.map { if (it.id == id) change(it) else it }
    }

    // Every family in use, downloaded once (FontLoader caches it); the default font until then.
    val loadedFonts = remember { mutableStateMapOf<String, FontFamily>() }
    val families = items.mapNotNull { it.fontFamily }.toSet()
    LaunchedEffect(families, fonts.catalog) {
        for (family in families) {
            if (family in loadedFonts) continue
            val entry = fonts.catalog?.find(family) ?: continue
            launch { FontLoader.load(entry.fileUrl)?.let { loadedFonts[family] = it } }
        }
    }
    fun fontOf(item: TextItem): TextFontStyleOption =
        item.fontFamily?.let { family -> loadedFonts[family]?.let { TextFontStyleOption(family, it) } } ?: DefaultTextFont

    var panel by remember { mutableStateOf(TextPanel.Options) }
    // Once the panel has switched, the options bar comes back through the two beats, not the tool's opening.
    var panelChanged by remember { mutableStateOf(false) }
    // The text the Font or Font size panel is editing, as it was when the panel opened: its ✕ puts it back.
    var beforePanel by remember { mutableStateOf<TextItem?>(null) }
    val fontBrowser = remember { FontBrowserState() }
    // Where the Font panel's category title / the toolbar's ⋯ is, in root coordinates, while its menu is open.
    var categoryMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    var moreMenuAnchor by remember { mutableStateOf<Rect?>(null) }
    // The menus are glass over the whole tool, so the tool is their liquefiable backdrop; the
    // selection toolbar's glass is over the photo, which is its own.
    val liquidState = rememberLiquidState()
    val photoLiquidState = rememberLiquidState()
    var rootOffset by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val addText = {
        val item = TextItem(id = nextId++)
        items = items + item
        selectedId = item.id
        editingId = item.id
    }
    val removeText = { id: Long ->
        items = items.filterNot { it.id == id }
        metrics.remove(id)
        if (selectedId == id) selectedId = null
        if (editingId == id) editingId = null
        if (items.isEmpty()) reEdited = false
    }
    val startEditing = { id: Long? ->
        val item = items.firstOrNull { it.id == id }
        if (item == null) {
            addText()
        } else if (!item.locked) {
            if (item.content.isNotBlank()) reEdited = true
            selectedId = item.id
            editingId = item.id
        }
    }
    val openPanel = { target: TextPanel ->
        // Puts the keyboard away as the panel rises in its place.
        focusManager.clearFocus()
        beforePanel = active
        panelChanged = true
        panel = target
    }
    val closePanel = { revert: Boolean ->
        val before = beforePanel
        if (revert && before != null) update(before.id) { before }
        categoryMenuAnchor = null
        beforePanel = null
        panel = TextPanel.Options
    }
    val pickFont = { font: FontEntry -> update(activeId) { it.copy(fontFamily = font.family) } }
    // The Color panel's tab, kept while the panel closes and opens again.
    var colorTab by remember { mutableStateOf(TextColorTab.Text) }
    // The keyboard bar's colour wheel always colours the words; the Color panel's, its tab's colour.
    val pickingPanelColor = panel == TextPanel.Color
    val pickColor = rememberColorPickerLauncher(
        initial = Color(
            when {
                !pickingPanelColor || colorTab == TextColorTab.Text -> active?.colorArgb
                colorTab == TextColorTab.Background -> active?.backgroundArgb
                else -> active?.strokeArgb
            } ?: DefaultTextColorArgb,
        ),
        onPicked = { color ->
            val argb = color.toArgb()
            update(activeId) {
                when {
                    !pickingPanelColor || colorTab == TextColorTab.Text -> it.copy(colorArgb = argb)
                    colorTab == TextColorTab.Background -> it.copy(backgroundArgb = argb)
                    else -> it.copy(strokeArgb = argb)
                }
            }
        },
    )
    val duplicate = { id: Long ->
        items.firstOrNull { it.id == id }?.let { item ->
            val copy = duplicateTextItem(item, nextId++)
            items = items + copy
            selectedId = copy.id
        }
    }
    val onMoreAction: (TextMoreAction) -> Unit = { action ->
        moreMenuAnchor = null
        val id = selectedId
        when (action) {
            TextMoreAction.Duplicate -> if (id != null) duplicate(id)
            TextMoreAction.Reset -> update(id, ::resetTextItem)
            TextMoreAction.Copy -> TextClipboard.item = items.firstOrNull { it.id == id }
            TextMoreAction.Paste -> TextClipboard.item?.let { copied ->
                val pasted = duplicateTextItem(copied, nextId++)
                items = items + pasted
                selectedId = pasted.id
            }
            TextMoreAction.Lock -> update(id) { it.copy(locked = !it.locked) }
            TextMoreAction.Delete -> if (id != null) removeText(id)
        }
    }
    val finish = {
        // Ends any typing in progress, so the keyboard isn't left up under the paywall or the editor.
        focusManager.clearFocus()
        val written = items.filter { it.content.isNotBlank() }
        val premiumEdit = written.any { isPremiumTextEdit(fontOf(it), Color(it.colorArgb), it.sizeSp, reEdited) }
        when {
            written.isEmpty() || sourceImage == null -> onBack()
            premiumEdit && !isPremium && !paywallShown -> {
                paywallShown = true
                onOpenPremium()
            }
            else -> onDone(
                TextEdit(
                    texts = written.map { BakeableText(it, fontOf(it)) },
                    canvasWidthPx = photoSizePx.width.toFloat(),
                    maxTextWidthPx = photoSizePx.width - with(density) { (TextLayerPaddingX * 2).roundToPx() },
                ),
            )
        }
    }

    // The keyboard can also be put away without leaving the field (Android's back, iOS's dismiss
    // key); that ends the typing too, or the keyboard bar would be left floating over the options.
    // Watched in a snapshotFlow rather than read in composition, which would recompose the tool on
    // every frame of the keyboard's slide.
    val ime = WindowInsets.ime
    LaunchedEffect(ime, density) {
        var wasVisible = false
        snapshotFlow { ime.getBottom(density) > 0 }.collect { visible ->
            if (visible) {
                wasVisible = true
            } else if (wasVisible) {
                wasVisible = false
                focusManager.clearFocus()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
        ToolScaffold(
            modifier = Modifier.liquefiable(liquidState),
            // The keyboard is left out of the insets on purpose: it slides up over the controls
            // while the photo (and the text being typed on it) stays exactly where it was.
            insets = WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        ) {
            Box(
                modifier = Modifier
                    .toolStage()
                    .background(scheme.onSurface.copy(alpha = 0.08f))
                    // Tapping anywhere off the texts ends typing (the focus loss lands the text in
                    // Selected first) and then deselects it.
                    .pointerInput(Unit) {
                        detectTapGestures {
                            focusManager.clearFocus()
                            selectedId = null
                        }
                    }
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceImage != null) {
                    // Sized to the photo itself, so the texts' coordinate space is exactly the
                    // displayed image — its width is the preview width the bake scales up from. Only
                    // the inner layer is clipped: the handles and the toolbar may hang past its edges.
                    Box(
                        modifier = Modifier
                            .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                            .onSizeChanged { photoSizePx = it },
                    ) {
                        val maxTextWidthPx = photoSizePx.width - with(density) { (TextLayerPaddingX * 2).roundToPx() }
                        fun topLeftOf(item: TextItem): IntOffset {
                            val outer = metrics[item.id]?.outer ?: IntSize.Zero
                            return IntOffset(
                                (item.centerX * photoSizePx.width - outer.width / 2f).roundToInt(),
                                (item.centerY * photoSizePx.height - outer.height / 2f).roundToInt(),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            Image(
                                bitmap = sourceImage,
                                contentDescription = tr("Photo preview"),
                                modifier = Modifier.fillMaxSize().liquefiable(photoLiquidState),
                                contentScale = ContentScale.Fit,
                            )
                            for (item in items) {
                                key(item.id) {
                                    val font = fontOf(item)
                                    TextLayer(
                                        item = item,
                                        style = TextStyle(
                                            fontFamily = font.fontFamily,
                                            fontWeight = font.fontWeight,
                                            fontStyle = font.fontStyle,
                                            fontSize = item.sizeSp.sp,
                                            color = item.textColor,
                                            textAlign = item.alignment.textAlign,
                                        ),
                                        editing = editingId == item.id,
                                        onContentChange = { text -> update(item.id) { it.copy(content = text) } },
                                        onEditingEnd = {
                                            editingId = null
                                            // Leaving an empty text behind would bake nothing.
                                            if (items.firstOrNull { it.id == item.id }?.content.isNullOrBlank()) removeText(item.id)
                                        },
                                        photoWidthPx = photoSizePx.width,
                                        maxTextWidthPx = maxTextWidthPx,
                                        onMeasured = { metrics[item.id] = it },
                                        modifier = Modifier
                                            .offset { topLeftOf(item) }
                                            .graphicsLayer { rotationZ = item.rotationDegrees },
                                    )
                                }
                            }
                        }

                        for (item in items) {
                            if (item.id == editingId) continue
                            key(item.id) {
                                // Snapshotted when a corner drag starts; the drag scales from it.
                                var scaleStart by remember { mutableStateOf(item) }
                                TextSelectionBox(
                                    selected = selectedId == item.id,
                                    locked = item.locked,
                                    boxSizePx = metrics[item.id]?.outer ?: IntSize.Zero,
                                    rotationDegrees = item.rotationDegrees,
                                    onTap = { if (selectedId == item.id) startEditing(item.id) else selectedId = item.id },
                                    onMove = { pan ->
                                        selectedId = item.id
                                        if (photoSizePx.width > 0 && photoSizePx.height > 0) {
                                            update(item.id) {
                                                it.withCenter(it.center + Offset(pan.x / photoSizePx.width, pan.y / photoSizePx.height))
                                            }
                                        }
                                    },
                                    onPinch = { zoom, turn ->
                                        update(item.id) {
                                            scaleTextItem(it, zoom).copy(rotationDegrees = snapTextRotation(it.rotationDegrees + turn))
                                        }
                                    },
                                    onScaleStart = { scaleStart = items.first { it.id == item.id } },
                                    onScale = { factor -> update(item.id) { scaleTextItem(scaleStart, factor).copy(centerX = it.centerX, centerY = it.centerY) } },
                                    onEdgeDrag = { edge, delta ->
                                        val measured = metrics[item.id] ?: return@TextSelectionBox
                                        update(item.id) {
                                            resizeTextBox(
                                                item = it,
                                                edge = edge,
                                                delta = delta,
                                                photoWidthPx = photoSizePx.width.toFloat(),
                                                photoHeightPx = photoSizePx.height.toFloat(),
                                                currentWidthPx = measured.area.width.toFloat(),
                                                currentHeightPx = measured.area.height.toFloat(),
                                                minHeightPx = measured.wordsHeight.toFloat(),
                                            )
                                        }
                                    },
                                    modifier = Modifier.offset { topLeftOf(item) },
                                )
                            }
                        }

                        val selected = items.firstOrNull { it.id == selectedId }
                        val selectedMetrics = selected?.let { metrics[it.id] }
                        if (selected != null && selectedMetrics != null && editingId == null) {
                            val toolbarHeightPx = with(density) { TextToolbarHeight.toPx() }
                            val gapPx = with(density) { ToolbarGap.toPx() }
                            var toolbarWidthPx by remember { mutableStateOf(0) }
                            TextSelectionToolbar(
                                photoLiquidState = photoLiquidState,
                                onDuplicate = { duplicate(selected.id) },
                                onDelete = { removeText(selected.id) },
                                onMore = { moreMenuAnchor = it },
                                modifier = Modifier
                                    .onSizeChanged { toolbarWidthPx = it.width }
                                    .offset {
                                        // Over the turned text's bounding box, or under it when there's
                                        // no room above.
                                        val halfHeight = rotatedHalfHeight(selectedMetrics.outer, selected.rotationDegrees)
                                        val centerX = selected.centerX * photoSizePx.width
                                        val centerY = selected.centerY * photoSizePx.height
                                        val above = centerY - halfHeight - gapPx - toolbarHeightPx
                                        val y = if (above >= -toolbarHeightPx / 2f) above else centerY + halfHeight + gapPx
                                        IntOffset((centerX - toolbarWidthPx / 2f).roundToInt(), y.roundToInt())
                                    },
                            )
                        }

                        if (items.isEmpty()) {
                            AddTextButton(onClick = addText, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                } else {
                    Text(
                        text = tr("No image to add text to"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            ToolPanel {
                AnimatedContent(
                    targetState = panel,
                    // The editor's two beats: the outgoing view drops away first (and so does the
                    // keyboard, when it is what the Font panel replaces), then the incoming one rises.
                    transitionSpec = {
                        (slideInVertically(settleSpec(delayMillis = ToolDropMillis)) { it } +
                            fadeIn(settleSpec(delayMillis = ToolDropMillis))) togetherWith
                            (slideOutVertically(dropSpec()) { it } + fadeOut(dropSpec()))
                    },
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                        .then(if (sideBySide) Modifier.fillMaxHeight() else Modifier.height(TextPanelHeight)),
                ) { shown ->
                    when (shown) {
                        TextPanel.Options -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                            TextOptionsBar(
                                onOptionSelected = { option ->
                                    when (option) {
                                        TextOption.Edit -> startEditing(activeId)
                                        TextOption.Font -> openPanel(TextPanel.Fonts)
                                        TextOption.FontSize -> if (active != null) openPanel(TextPanel.FontSize)
                                        TextOption.Color -> if (active != null) openPanel(TextPanel.Color)
                                        // Their panels come next.
                                        else -> Unit
                                    }
                                },
                                onClose = finish,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                // Back from a panel the bar waits out the first beat before it rises.
                                cascadeDelayMillis = if (panelChanged) ToolDropMillis else 0,
                            )
                        }
                        TextPanel.Fonts -> TextFontPanel(
                            fonts = fonts,
                            browser = fontBrowser,
                            categoryMenuOpen = categoryMenuAnchor != null,
                            onOpenCategoryMenu = { categoryMenuAnchor = it },
                            selectedFamily = active?.fontFamily,
                            onFontSelected = pickFont,
                            onClose = { closePanel(true) },
                            onDone = {
                                active?.fontFamily?.let(fonts.onUsed)
                                closePanel(false)
                            },
                        )
                        TextPanel.FontSize -> TextSizePanel(
                            value = textSizeToSlider(active?.sizeSp ?: TextSizeDefault),
                            onValueChange = { value ->
                                update(activeId) { scaleTextItem(it, sliderToTextSize(value) / it.sizeSp) }
                            },
                            onClose = { closePanel(true) },
                            onDone = { closePanel(false) },
                        )
                        TextPanel.Color -> TextColorPanel(
                            item = active,
                            tab = colorTab,
                            onTabSelected = { colorTab = it },
                            onChange = { change -> update(activeId, change) },
                            onPickCustom = pickColor,
                            onClose = { closePanel(true) },
                            onDone = { closePanel(false) },
                        )
                    }
                }
            }
        }

        // Fades rather than vanishing, so it is seen riding down with the keyboard (its IME padding
        // follows the keyboard's slide) when the Font panel takes over.
        val editing = items.firstOrNull { it.id == editingId }
        AnimatedVisibility(
            visible = editing != null,
            enter = fadeIn(tween(160)),
            exit = fadeOut(dropSpec()),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        ) {
            TextKeyboardBar(
                fonts = fonts.quickPicks,
                selectedFamily = editing?.fontFamily,
                onFontSelected = { font ->
                    pickFont(font)
                    fonts.onUsed(font.family)
                },
                alignment = editing?.alignment ?: TextAlignOption.Center,
                onAlignmentSelected = { alignment -> update(editingId) { it.copy(alignment = alignment) } },
                onPickColor = pickColor,
                onShowAllFonts = { openPanel(TextPanel.Fonts) },
            )
        }

        // Composed after the editor's handler, so with a menu open back folds the menu, not the tool.
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = categoryMenuAnchor != null || moreMenuAnchor != null,
            onBackCompleted = {
                categoryMenuAnchor = null
                moreMenuAnchor = null
            },
        )
        FontCategoryMenu(
            open = categoryMenuAnchor != null,
            anchor = categoryMenuAnchor?.translate(-rootOffset),
            titles = fonts.catalog?.sections.orEmpty().map { tr(it.title) },
            selectedIndex = fontBrowser.sectionIndex,
            liquidState = liquidState,
            onSelected = { index ->
                categoryMenuAnchor = null
                fontBrowser.selectSection(index, fonts.catalog)
            },
            onDismiss = { categoryMenuAnchor = null },
        )
        TextMoreMenu(
            open = moreMenuAnchor != null,
            anchor = moreMenuAnchor?.translate(-rootOffset),
            liquidState = liquidState,
            locked = items.firstOrNull { it.id == selectedId }?.locked == true,
            canPaste = TextClipboard.item != null,
            onAction = onMoreAction,
            onDismiss = { moreMenuAnchor = null },
        )
    }
}

/** Half the height of the upright box around a [size] box turned by [degrees]. */
private fun rotatedHalfHeight(size: IntSize, degrees: Float): Float {
    val radians = degrees * kotlin.math.PI.toFloat() / 180f
    return abs(size.width / 2f * sin(radians)) + abs(size.height / 2f * cos(radians))
}

/** What the tool's panel shows: the floating options bar, or the Font, Font size or Color panel. */
private enum class TextPanel { Options, Fonts, FontSize, Color }

/** Tall enough for the Font panel's title, four rows of fonts and its category bar. */
private val TextPanelHeight = 292.dp

/** The font text starts in, before one is picked from the catalog. */
private val DefaultTextFont = TextFontStyleOption("Default", FontFamily.SansSerif, FontWeight.Medium)

/**
 * One text on the photo: a text field typed into in place while [editing], a plain label otherwise.
 * The words sit centred in their text area, which is as wide as [TextItem.boxWidth] when set (the
 * words then fill it, so their alignment shows) and at least [TextItem.boxHeight] tall. Both modes
 * use the same symmetric padding around that area, so the text's centre — what the bake positions by
 * — doesn't move when switching. Gestures live on [TextSelectionBox] instead, which sits outside the
 * photo's clip so its handles are never cut off.
 */
@Composable
private fun TextLayer(
    item: TextItem,
    style: TextStyle,
    editing: Boolean,
    onContentChange: (String) -> Unit,
    onEditingEnd: () -> Unit,
    photoWidthPx: Int,
    maxTextWidthPx: Int,
    onMeasured: (TextMetrics) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var outer by remember { mutableStateOf(IntSize.Zero) }
    var area by remember { mutableStateOf(IntSize.Zero) }
    var wordsHeight by remember { mutableIntStateOf(0) }
    var wordsLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val currentOnMeasured by rememberUpdatedState(onMeasured)
    LaunchedEffect(outer, area, wordsHeight) { currentOnMeasured(TextMetrics(outer, area, wordsHeight)) }

    val boxWidthDp = item.boxWidth?.let { with(density) { (it * photoWidthPx).toDp() } }
    val boxHeightDp = item.boxHeight?.let { with(density) { (it * photoWidthPx).toDp() } }
    val areaModifier = Modifier
        .then(if (boxWidthDp != null) Modifier.width(boxWidthDp) else Modifier.widthIn(max = with(density) { maxTextWidthPx.coerceAtLeast(0).toDp() }))
        .then(if (boxHeightDp != null) Modifier.heightIn(min = boxHeightDp) else Modifier)
        .onSizeChanged { area = it }
    // Fills a set text area, so the lines' alignment is within it, as the bake measures them.
    val wordsModifier = (if (boxWidthDp != null) Modifier.fillMaxWidth() else Modifier)
        // The plate and outline, from the words' own layout, exactly as the bake draws them.
        .drawBehind { wordsLayout?.takeIf { item.content.isNotBlank() }?.let { drawTextUnderlay(item, it, Offset.Zero) } }
    val onWordsLayout = { layout: TextLayoutResult ->
        wordsHeight = layout.size.height
        wordsLayout = layout
    }

    Box(
        modifier = modifier
            .onSizeChanged { outer = it }
            .then(if (editing) Modifier.dashedPill(color = Color.White.copy(alpha = 0.9f)) else Modifier)
            .padding(horizontal = TextLayerPaddingX, vertical = TextLayerPaddingY)
            .then(areaModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (editing) {
            val host = LocalInlineToolHost.current
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            // Remembered inside this branch, so it resets every typing session: the field reports
            // "unfocused" once before requestFocus lands, which must not count as leaving the field.
            var hadFocus by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                // The tool opens typing, but the keyboard waits until the editor has finished bringing
                // the tool in: rising during that, it would squeeze the editor's photo under the stage.
                if (host != null) snapshotFlow { host.stage.value }.first { it >= 1f }
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            BasicTextField(
                value = item.content,
                onValueChange = onContentChange,
                textStyle = style,
                cursorBrush = SolidColor(style.color),
                onTextLayout = onWordsLayout,
                // Return starts a new line; the keyboard bar's ⌄ or a tap off the text ends typing.
                modifier = wordsModifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            hadFocus = true
                        } else if (hadFocus) {
                            onEditingEnd()
                        }
                    },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (item.content.isEmpty()) {
                            Text(text = tr("Your Text"), style = style.copy(color = Color.White.copy(alpha = 0.7f)), maxLines = 1)
                        }
                        innerTextField()
                    }
                },
            )
        } else {
            Text(
                text = item.content,
                style = style,
                onTextLayout = onWordsLayout,
                modifier = wordsModifier,
            )
        }
    }
}

@Composable
private fun AddTextButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.25f))
            .dashedPill(color = Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = tr("Add Text"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A dashed, fully rounded outline — the "editable text box" affordance from the design. */
private fun Modifier.dashedPill(color: Color): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val inset = strokeWidth / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
        cornerRadius = CornerRadius(size.height / 2f),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

@Composable
internal fun FontRow(selected: TextFontStyleOption, onSelected: (TextFontStyleOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TextFontStyles) { option ->
            FontChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun FontChip(option: TextFontStyleOption, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val tint = if (selected) scheme.primary else scheme.onSurface
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(if (selected) scheme.primary.copy(alpha = 0.08f) else scheme.surface)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.15f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                fontFamily = option.fontFamily,
                fontWeight = option.fontWeight,
                fontStyle = option.fontStyle,
                fontSize = 16.sp,
                color = tint,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(option.label),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * With [onPickCustom] the row leads with a colour-wheel swatch that opens the host's free colour
 * picker; it is the one ringed while [selected] is a colour outside [TextColorOptions]. [gradients]
 * follow the flat colours; [selected] is null while one of them ([selectedGradient]) is in use.
 * With [onClear] a "none" swatch comes first of all, ringed while neither is set.
 */
@Composable
internal fun ColorRow(
    selected: Color?,
    onSelected: (Color) -> Unit,
    onPickCustom: (() -> Unit)? = null,
    gradients: List<List<Color>> = emptyList(),
    selectedGradient: List<Color>? = null,
    onGradientSelected: (List<Color>) -> Unit = {},
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onClear != null) {
            item {
                ColorSwatch(selected = selected == null && selectedGradient == null, onClick = onClear) {
                    Box(modifier = it, contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = tr("None"),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        if (onPickCustom != null) {
            item {
                ColorSwatch(selected = selected != null && selected !in TextColorOptions, onClick = onPickCustom) {
                    ColorPickerFill(it)
                }
            }
        }
        items(TextColorOptions) { color ->
            ColorSwatch(selected = color == selected, onClick = { onSelected(color) }) { Box(modifier = it.background(color)) }
        }
        items(gradients) { colors ->
            ColorSwatch(selected = colors == selectedGradient, onClick = { onGradientSelected(colors) }) {
                Box(modifier = it.background(Brush.linearGradient(colors)))
            }
        }
    }
}

/** A colour swatch's outer size, its selection ring included. */
internal val SwatchSize = 46.dp

/** [fill] paints the swatch's inner square; it is handed the modifier that sizes and clips it. */
@Composable
internal fun ColorSwatch(selected: Boolean, onClick: () -> Unit, fill: @Composable (Modifier) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val outerShape = RoundedCornerShape(12.dp)
    val innerShape = RoundedCornerShape(8.dp)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(SwatchSize)
            .clip(outerShape)
            // Always drawn (transparent when unselected) so selecting doesn't shift the swatch.
            .border(width = 2.dp, color = if (selected) scheme.primary else Color.Transparent, shape = outerShape)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(innerShape)
                // Keeps light swatches (white) visible against a light surface.
                .border(width = 1.dp, color = scheme.onSurface.copy(alpha = 0.1f), shape = innerShape),
        ) {
            fill(Modifier.matchParentSize())
        }
    }
}

@Preview
@Composable
private fun TextScreenPreview() {
    ThemePreviews {
        TextContent(
            sourceImage = ImageBitmap(360, 480),
            isPremium = false,
            fonts = TextFonts.Empty,
            onBack = {},
            onOpenPremium = {},
            onDone = {},
        )
    }
}
