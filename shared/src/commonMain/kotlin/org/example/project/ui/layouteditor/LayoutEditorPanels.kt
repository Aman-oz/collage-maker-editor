package org.example.project.ui.layouteditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import org.example.project.ui.common.DownloadStatus
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.example.project.data.cache.AssetCache
import org.koin.compose.koinInject
import org.example.project.ui.shop.LocalOpenShop
import org.example.project.ui.shop.ShopPicks
import org.example.project.ui.shop.ShopTab
import org.example.project.ui.stickers.LibrarySearchButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.TextButton
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.PanelSegmentedTabs
import org.example.project.ui.text.PanelSlider
import org.example.project.ui.text.PanelSliderRow
import org.example.project.ui.text.TextPanelColumn
import org.example.project.ui.text.TextPanelPlacement
import org.example.project.ui.text.TextSliderPanel
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.crop.AspectRatioStrip
import org.example.project.ui.frame.FrameColorNone
import org.example.project.ui.frame.FrameColors
import org.example.project.ui.freestyle.StickersPanel
import org.example.project.ui.freestyle.freestyleChrome
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_flip_horizontally
import photocollagemaker.shared.generated.resources.ic_flip_vertically
import photocollagemaker.shared.generated.resources.ic_premium_icon

/**
 * Chrome colours for the layout editor, from the app's light/dark [MaterialTheme] like the collage
 * editor's, with a grey stage so the layout's own (often white) background stays visible.
 */
@Immutable
internal data class LayoutChrome(
    val isLight: Boolean,
    val surface: Color,
    val stage: Color,
    val emptySlot: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
)

@Composable
internal fun layoutChrome(): LayoutChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return LayoutChrome(
        isLight = isLight,
        surface = scheme.surface,
        stage = if (isLight) Color(0xFFE6E6EB) else Color(0xFF0E0E12),
        // Translucent, so an empty slot still shows a hint of the background picked for the gaps.
        emptySlot = if (isLight) Color(0xFFF2F2F5).copy(alpha = 0.85f) else Color(0xFF2C2C2E).copy(alpha = 0.85f),
        content = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.55f),
        accent = scheme.primary,
        track = if (isLight) Color(0xFFD9D6E3) else Color(0xFF3A3A40),
    )
}

/**
 * The panel an option of the glass bar opens in its place: ✕ / its name / ✓ over its controls, as
 * the Text and Stickers tools' panels. Its edits show live; [onClose] takes them back, [onDone]
 * keeps them. Transform, Mirror and Rotate work on [selectedPhotoSlot]'s photo. Text, Filter and
 * Ratio open over the screen instead, so they never come here.
 */
@Composable
internal fun LayoutOptionPanel(
    option: LayoutOption,
    edit: LayoutEdit,
    templates: List<TemplateItem>,
    premiumLayouts: Set<String>,
    frameLoad: LayoutArtLoad,
    backgroundLoad: LayoutArtLoad,
    chrome: LayoutChrome,
    selectedPhotoSlot: Int?,
    actions: LayoutEditorActions,
    onStickerTapped: (emoji: String, cellBoundsInRoot: Rect) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val photo = selectedPhotoSlot?.let { edit.photos[it] }
    when (option) {
        LayoutOption.Canvas -> TextPanelColumn(title = option.label, onClose = onClose, onDone = onDone) {
            AspectRatioStrip(
                options = LayoutCanvasOptions,
                selected = edit.canvas,
                onSelected = actions::setCanvas,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        LayoutOption.Layout -> TextPanelColumn(title = option.label, onClose = onClose, onDone = onDone) {
            Box(modifier = Modifier.fillMaxWidth().height(LayoutPickerHeight), contentAlignment = Alignment.Center) {
                LayoutPickerPanel(
                    templates = templates,
                    premiumLayouts = premiumLayouts,
                    selectedId = edit.template.id,
                    chrome = chrome,
                    onSelected = actions::selectTemplate,
                )
            }
        }
        LayoutOption.Margin -> MarginPanel(edit, chrome, actions, onClose, onDone)
        LayoutOption.Border -> BorderPanel(edit, chrome, actions, onClose, onDone)
        LayoutOption.Frame -> FramePanel(edit, frameLoad, chrome, actions, onClose, onDone)
        LayoutOption.Transform -> if (selectedPhotoSlot == null || photo == null) {
            NoPhotoPanel(option.label, chrome, onClose, onDone)
        } else {
            TextSliderPanel(
                title = option.label,
                sliders = listOf(
                    PanelSlider(tr("Horizontal"), photo.tiltX) { actions.setPhotoTiltX(selectedPhotoSlot, it) },
                    PanelSlider(tr("Vertical"), photo.tiltY) { actions.setPhotoTiltY(selectedPhotoSlot, it) },
                ),
                onClose = onClose,
                onDone = onDone,
            )
        }
        LayoutOption.Mirror -> if (selectedPhotoSlot == null || photo == null) {
            NoPhotoPanel(option.label, chrome, onClose, onDone)
        } else {
            TextPanelColumn(title = option.label, onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                ) {
                    ActionChip(
                        icon = vectorResource(Res.drawable.ic_flip_horizontally),
                        label = tr("Horizontal"),
                        active = photo.flipX,
                        chrome = chrome,
                        onClick = { actions.flipPhoto(selectedPhotoSlot, horizontally = true) },
                    )
                    ActionChip(
                        icon = vectorResource(Res.drawable.ic_flip_vertically),
                        label = tr("Vertical"),
                        active = photo.flipY,
                        chrome = chrome,
                        onClick = { actions.flipPhoto(selectedPhotoSlot, horizontally = false) },
                    )
                }
            }
        }
        LayoutOption.Rotate -> if (selectedPhotoSlot == null || photo == null) {
            NoPhotoPanel(option.label, chrome, onClose, onDone)
        } else {
            TextSliderPanel(
                title = option.label,
                sliders = listOf(
                    PanelSlider(
                        label = tr("Rotate"),
                        value = photo.rotationDegrees,
                        range = LayoutRotationRange,
                        referenceValue = 0f,
                    ) { actions.setPhotoRotation(selectedPhotoSlot, it) },
                ),
                onClose = onClose,
                onDone = onDone,
            )
        }
        LayoutOption.Background -> BackgroundPanel(edit, backgroundLoad, chrome, actions, onClose, onDone)
        LayoutOption.Emoji -> TextPanelColumn(title = option.label, onClose = onClose, onDone = onDone) {
            Box(modifier = Modifier.fillMaxWidth().height(EmojiPanelHeight)) {
                StickersPanel(chrome = freestyleChrome(), onStickerTapped = onStickerTapped, showHeader = false)
            }
        }
        // These open over the screen instead of a panel.
        LayoutOption.Text, LayoutOption.Sticker, LayoutOption.Filter, LayoutOption.Ratio -> Unit
    }
}

/** A photo option's panel while no photo is selected: it says to select one. */
@Composable
private fun NoPhotoPanel(title: String, chrome: LayoutChrome, onClose: () -> Unit, onDone: () -> Unit) {
    TextPanelColumn(title = title, onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        Text(
            text = tr("Tap a photo to select it"),
            color = chrome.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

private val LayoutThumbnailSize = 56.dp
private val LayoutThumbnailSpacing = 10.dp
private val LayoutPickerPadding = 8.dp

/** Two rows of layouts and their padding. */
private val LayoutPickerHeight = LayoutThumbnailSize * 2 + LayoutThumbnailSpacing + LayoutPickerPadding * 2

private val EmojiPanelHeight = 150.dp

/** Every layout, two rows scrolling sideways. */
@Composable
private fun LayoutPickerPanel(
    templates: List<TemplateItem>,
    premiumLayouts: Set<String>,
    selectedId: String,
    chrome: LayoutChrome,
    onSelected: (TemplateItem) -> Unit,
) {
    if (templates.isEmpty()) {
        CircularProgressIndicator(color = chrome.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        return
    }
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = LayoutPickerPadding),
        horizontalArrangement = Arrangement.spacedBy(LayoutThumbnailSpacing),
        verticalArrangement = Arrangement.spacedBy(LayoutThumbnailSpacing),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(templates, key = { it.id }) { template ->
            LayoutThumbnail(template = template, premium = template.title in premiumLayouts, selected = template.id == selectedId, chrome = chrome, onClick = { onSelected(template) })
        }
    }
}

@Composable
private fun LayoutThumbnail(template: TemplateItem, premium: Boolean, selected: Boolean, chrome: LayoutChrome, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(LayoutThumbnailSize)
            .clip(shape)
            .background(if (selected) chrome.accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.5.dp, if (selected) chrome.accent else Color.Transparent, shape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = (if (chrome.isLight) template.preview else template.previewDark).orEmpty(),
            contentDescription = template.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (premium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(14.dp),
            )
        }
    }
}

/** A measure as a 0–100 slider over its [range], so every slider reads 0 to 100 as in the design. */
private fun percentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit): PanelSlider {
    val span = range.endInclusive - range.start
    return PanelSlider(label, (value - range.start) / span * 100f) { onValueChange(range.start + it / 100f * span) }
}

/**
 * The frame shared by Margin, Border and Frame: ✕ / [title] / ✓, the selected property's control
 * ([control]: a slider under its name, or a colour row), and under it the chips that pick the property.
 */
@Composable
private fun <T> PropertyPanel(
    title: String,
    properties: List<T>,
    selected: T,
    label: (T) -> String,
    chrome: LayoutChrome,
    onSelected: (T) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    control: @Composable () -> Unit,
) {
    TextPanelColumn(title = title, onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        // One height for every property's control, so the chips under it never move.
        Box(modifier = Modifier.fillMaxWidth().height(PropertyControlHeight), contentAlignment = Alignment.BottomCenter) {
            control()
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            for (property in properties) {
                Chip(label = label(property), selected = property == selected, chrome = chrome, onClick = { onSelected(property) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private val PropertyControlHeight = 86.dp

private enum class MarginProperty(private val englishLabel: String) {
    Corner("Corner"),
    Shadow("Shadow"),
    Margin("Margin"),
    Space("Space"),
    ;

    val label: String get() = tr(englishLabel)
}

/** Corner, shadow, margin and space: one slider, switched by the chips under it. */
@Composable
private fun MarginPanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions, onClose: () -> Unit, onDone: () -> Unit) {
    var property by rememberSaveable { mutableStateOf(MarginProperty.Corner) }
    PropertyPanel(tr("Margin"), MarginProperty.entries, property, { it.label }, chrome, { property = it }, onClose, onDone) {
        val slider = when (property) {
            MarginProperty.Corner -> percentSlider(property.label, edit.corner, LayoutCornerRange, actions::setCorner)
            MarginProperty.Shadow -> percentSlider(property.label, edit.shadow, LayoutShadowRange, actions::setShadow)
            MarginProperty.Margin -> percentSlider(property.label, edit.margin, LayoutMarginRange, actions::setMargin)
            MarginProperty.Space -> percentSlider(property.label, edit.space, LayoutSpaceRange, actions::setSpace)
        }
        // Keyed, so each property's slider starts from its own value rather than the last one's drag.
        key(property) { PanelSliderRow(slider) }
    }
}

private enum class BorderProperty(private val englishLabel: String) {
    Thickness("Thickness"),
    Opacity("Opacity"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The outline round every slot: its thickness or opacity on the slider (picked by the chips at the
 * foot) over a grid of colours, led by the colour picker and a checkerboard that takes it off.
 */
@Composable
private fun BorderPanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions, onClose: () -> Unit, onDone: () -> Unit) {
    var property by rememberSaveable { mutableStateOf(BorderProperty.Thickness) }
    TextPanelColumn(title = tr("Border"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Fill) {
        ColorTabBody(
            slider = when (property) {
                BorderProperty.Thickness -> percentSlider(property.label, edit.borderWidth, LayoutBorderWidthRange, actions::setBorderWidth)
                BorderProperty.Opacity -> percentSlider(property.label, edit.borderOpacity, LayoutBorderOpacityRange, actions::setBorderOpacity)
            },
            sliderKey = property,
            selected = edit.borderColor,
            none = edit.borderWidth <= 0f,
            onSelected = actions::setBorderColor,
            properties = BorderProperty.entries,
            selectedProperty = property,
            propertyLabel = { it.label },
            onPropertySelected = { property = it },
            chrome = chrome,
            modifier = panelBodyModifier(),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private enum class FrameTab(private val englishLabel: String) {
    Color("Color"),
    Frame("Frame"),
    ;

    val label: String get() = tr(englishLabel)
}

private enum class FrameProperty(private val englishLabel: String) {
    Width("Width"),
    CornerRadius("Corner Radius"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Frame option, in two tabs over the whole canvas. Color: the photo editor's plain frame, its
 * width or corner radius on the slider (picked by the chips at the foot) over a grid of colours,
 * whose checkerboard takes it off. Frame: the server's decorative frames ([LayoutArt]), read
 * when the tab first opens, in a grid over the sub-category tiles (after a search button, which
 * opens the Shop's Border tab) and the category pills. Tapping the frame in use again takes it off.
 *
 * Under the canvas the area under the tabs is one fixed height for both, so nothing moves when they
 * switch; beside it the area takes the column's spare height.
 */
@Composable
private fun FramePanel(
    edit: LayoutEdit,
    frameLoad: LayoutArtLoad,
    chrome: LayoutChrome,
    actions: LayoutEditorActions,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(FrameTab.Color) }
    var property by rememberSaveable { mutableStateOf(FrameProperty.Width) }
    LaunchedEffect(tab) { if (tab == FrameTab.Frame) actions.loadFrameArt() }

    TextPanelColumn(title = tr("Frame"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Fill) {
        PanelSegmentedTabs(
            labels = FrameTab.entries.map { it.label },
            selectedIndex = tab.ordinal,
            onSelected = { tab = FrameTab.entries[it] },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (tab) {
            FrameTab.Color -> ColorTabBody(
                slider = when (property) {
                    FrameProperty.Width -> percentSlider(property.label, edit.frameWidth, LayoutFrameWidthRange, actions::setFrameWidth)
                    FrameProperty.CornerRadius -> percentSlider(property.label, edit.frameCorner, LayoutFrameCornerRange, actions::setFrameCorner)
                },
                sliderKey = property,
                selected = edit.frameColor,
                none = edit.frameColor == FrameColorNone,
                onSelected = actions::setFrameColor,
                properties = FrameProperty.entries,
                selectedProperty = property,
                propertyLabel = { it.label },
                onPropertySelected = { property = it },
                chrome = chrome,
                modifier = panelBodyModifier(),
            )
            FrameTab.Frame -> ArtLibraryBody(
                load = frameLoad,
                selected = edit.frameArt,
                cellAspect = FrameArtCellAspect,
                contentScale = ContentScale.Fit,
                failedMessage = tr("Couldn't load frames"),
                shopTab = ShopTab.Borders,
                chrome = chrome,
                onSelected = { art -> actions.setFrameArt(art.takeUnless { it.id == edit.frameArt?.id }) },
                onRetry = actions::loadFrameArt,
                modifier = panelBodyModifier(),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * The area under a panel's bar (and tabs): one fixed height under the canvas, so every tab's
 * controls fill the same room; beside it, the column's spare height.
 */
@Composable
private fun ColumnScope.panelBodyModifier(): Modifier =
    if (LocalTextPanelSideBySide.current) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(PanelBodyHeight)

/** Under the canvas: a slider over two rows of colours and the chips under them. */
private val PanelBodyHeight = 214.dp
private val PanelFootHeight = 36.dp

/**
 * A Color tab: [slider] (keyed by [sliderKey], so each property's slider starts from its own value)
 * over a grid of colours, and at the foot the chips that pick which property the slider sets (none
 * with a single property).
 */
@Composable
private fun <T> ColorTabBody(
    slider: PanelSlider,
    sliderKey: Any,
    selected: Color,
    none: Boolean,
    onSelected: (Color) -> Unit,
    properties: List<T>,
    selectedProperty: T,
    propertyLabel: (T) -> String,
    onPropertySelected: (T) -> Unit,
    chrome: LayoutChrome,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        key(sliderKey) { PanelSliderRow(slider) }
        Spacer(modifier = Modifier.height(6.dp))
        LineColorGrid(selected = selected, none = none, onSelected = onSelected, modifier = Modifier.fillMaxWidth().weight(1f))
        // A single property needs no chips; the colours take their room.
        if (properties.size > 1) Spacer(modifier = Modifier.height(10.dp))
        if (properties.size > 1) ChipRow {
            for (p in properties) {
                Chip(label = propertyLabel(p), selected = p == selectedProperty, chrome = chrome, onClick = { onPropertySelected(p) })
            }
        }
    }
}

/** A row of chips at a panel's foot, scrolling sideways when they don't fit. */
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PanelFootHeight)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** The frame colours without None, which the checkerboard swatch stands for. */
private val LineColors: List<Color> = FrameColors.map { it.color }.filter { it != FrameColorNone }

/**
 * Colours in rows that scroll down: the colour picker (ringed while the colour is one of the user's
 * own), the checkerboard ([FrameColorNone], ringed while [none]), then [LineColors]. [selected]
 * `Color.Unspecified` rings none of them.
 */
@Composable
private fun LineColorGrid(selected: Color, none: Boolean, onSelected: (Color) -> Unit, modifier: Modifier = Modifier) {
    val pickCustomColor = rememberColorPickerLauncher(
        // None is transparent, and Unspecified no colour at all: neither is one to open a picker on.
        initial = if (selected == FrameColorNone || selected == Color.Unspecified) Color.White else selected,
        onPicked = onSelected,
    )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(ColorSwatchSize + 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        item {
            Box(contentAlignment = Alignment.Center) {
                SelectableSwatch(
                    selected = !none && selected != Color.Unspecified && selected !in LineColors,
                    size = ColorSwatchSize,
                    onClick = pickCustomColor,
                ) {
                    ColorPickerFill(Modifier.fillMaxSize())
                }
            }
        }
        item {
            Box(contentAlignment = Alignment.Center) {
                SelectableSwatch(selected = none, size = ColorSwatchSize, onClick = { onSelected(FrameColorNone) }) {
                    Checkerboard(Modifier.fillMaxSize())
                }
            }
        }
        items(LineColors) { color ->
            Box(contentAlignment = Alignment.Center) {
                SelectableSwatch(selected = !none && color == selected, size = ColorSwatchSize, onClick = { onSelected(color) }) {
                    ColorSwatchFill(color)
                }
            }
        }
    }
}

private val ColorSwatchSize = 46.dp

/** The transparency checkerboard, standing for no colour. */
@Composable
private fun Checkerboard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val dark = Color(0xFFD5D5DA)
    Canvas(modifier = modifier.border(1.dp, scheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(SwatchInnerCorner))) {
        drawRect(Color.White)
        val cells = 4
        val cell = size.width / cells
        for (row in 0 until cells) {
            for (col in 0 until cells) {
                if ((row + col) % 2 == 1) drawRect(dark, topLeft = Offset(col * cell, row * cell), size = Size(cell, cell))
            }
        }
    }
}

/**
 * A server library's tab (Frame, Background): a spinner while [load] is in flight, Retry under
 * [failedMessage] if it failed, else the picked sub-category's pictures in a grid of [cellAspect]
 * cells (each drawn with [contentScale]), over a row of the search button and the sub-category
 * tiles, and the category pills at the foot. The search button opens the Shop on [shopTab]; "Use"
 * on a pack there brings the panel back open on it.
 */
@Composable
internal fun ArtLibraryBody(
    load: LayoutArtLoad,
    selected: LayoutArt?,
    cellAspect: Float,
    contentScale: ContentScale,
    failedMessage: String,
    shopTab: ShopTab,
    chrome: LayoutChrome,
    onSelected: (LayoutArt) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var subcategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val openShop = LocalOpenShop.current
    val picks: ShopPicks = koinInject()
    val pick by picks.pick.collectAsState()
    LaunchedEffect(pick) {
        picks.take(shopTab)?.let {
            categoryId = it.categoryId
            subcategoryId = it.packId
        }
    }
    when (load) {
        LayoutArtLoad.Loading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = chrome.accent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
        LayoutArtLoad.Failed -> Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = failedMessage, color = chrome.muted, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text(tr("Retry"), color = chrome.accent) }
        }
        is LayoutArtLoad.Ready -> {
            val categories = load.categories
            val category = categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull()
            val subcategory = category?.subcategories?.let { subs -> subs.firstOrNull { it.id == subcategoryId } ?: subs.firstOrNull() }
            val sideBySide = LocalTextPanelSideBySide.current
            Column(modifier = modifier) {
                LazyVerticalGrid(
                    columns = if (sideBySide) GridCells.Adaptive(96.dp) else GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(ArtSpacing),
                    verticalArrangement = Arrangement.spacedBy(ArtSpacing),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    items(subcategory?.items.orEmpty(), key = { it.id }) { art ->
                        ArtCell(
                            art = art,
                            selected = art.id == selected?.id,
                            contentScale = contentScale,
                            chrome = chrome,
                            onClick = { onSelected(art) },
                            modifier = Modifier.aspectRatio(cellAspect),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(ArtTileHeight),
                ) {
                    item { LibrarySearchButton(onClick = { openShop(shopTab) }, size = 44.dp) }
                    items(category?.subcategories.orEmpty(), key = { it.id }) { sub ->
                        ArtSubcategoryTile(
                            subcategory = sub,
                            selected = sub.id == subcategory?.id,
                            chrome = chrome,
                            onClick = { subcategoryId = sub.id },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ChipRow {
                    for (c in categories) {
                        Chip(
                            label = c.name,
                            selected = c.id == category?.id,
                            chrome = chrome,
                            onClick = {
                                categoryId = c.id
                                subcategoryId = null
                            },
                        )
                    }
                }
            }
        }
    }
}

/** A frame cell is 4:5, the shape of most frames (a taller one is fitted inside); a background's is square. */
internal const val FrameArtCellAspect = 0.8f
private const val BackgroundArtCellAspect = 1f
private val ArtSpacing = 8.dp
private val ArtTileHeight = 58.dp

/** One picture's thumbnail, ringed while [selected], crowned if premium. */
@Composable
private fun ArtCell(
    art: LayoutArt,
    selected: Boolean,
    contentScale: ContentScale,
    chrome: LayoutChrome,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val bounce = rememberSpringBounce()
    // The picture's download into the disk cache, started by a tap on a free one.
    val assetCache = koinInject<AssetCache>()
    val download by remember(art.imageUrl) { assetCache.progressOf(art.imageUrl) }.collectAsState(null)
    val kept by remember(art.imageUrl) { assetCache.keptOf(art.imageUrl) }.collectAsState(null)
    Box(
        modifier = modifier
            .springBounce(bounce)
            .clip(shape)
            .background(if (selected) chrome.accent.copy(alpha = 0.12f) else chrome.content.copy(alpha = 0.05f))
            .border(2.dp, if (selected) chrome.accent else Color.Transparent, shape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = art.thumbnailUrl,
            contentDescription = art.name,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)),
        )
        if (art.isPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier.align(Alignment.TopEnd).size(16.dp),
            )
        }
        DownloadStatus(progress = download, kept = kept, size = 18.dp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
    }
}

/** A sub-category tile: its first picture over its name, tinted while [selected]. */
@Composable
private fun ArtSubcategoryTile(subcategory: LayoutArtSubcategory, selected: Boolean, chrome: LayoutChrome, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(92.dp)
            .fillMaxHeight()
            .clip(shape)
            .background(if (selected) chrome.accent.copy(alpha = 0.12f) else chrome.content.copy(alpha = 0.05f))
            .border(1.5.dp, if (selected) chrome.accent else Color.Transparent, shape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NetworkImage(
            url = subcategory.items.first().thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subcategory.name,
            color = if (selected) chrome.accent else chrome.content,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class BackgroundTab(private val englishLabel: String) {
    Color("Color"),
    Background("Background"),
    ;

    val label: String get() = tr(englishLabel)
}

/** The Color tab's one slider. */
private enum class BackgroundProperty(private val englishLabel: String) {
    Opacity("Opacity"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The Background option, in two tabs behind the slots. Color: the opacity slider over a grid of
 * colours, whose checkerboard leaves the canvas transparent; picking one takes a background
 * picture off. Background: the server's backgrounds ([LayoutArt], cropped to fill the canvas), read
 * when the tab first opens, laid out as the Frame tab's. Tapping the one in use again goes back to
 * the colour. The opacity is the background's, colour or picture.
 */
@Composable
private fun BackgroundPanel(
    edit: LayoutEdit,
    backgroundLoad: LayoutArtLoad,
    chrome: LayoutChrome,
    actions: LayoutEditorActions,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(BackgroundTab.Color) }
    LaunchedEffect(tab) { if (tab == BackgroundTab.Background) actions.loadBackgroundArt() }

    TextPanelColumn(title = tr("Background"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Fill) {
        PanelSegmentedTabs(
            labels = BackgroundTab.entries.map { it.label },
            selectedIndex = tab.ordinal,
            onSelected = { tab = BackgroundTab.entries[it] },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (tab) {
            BackgroundTab.Color -> ColorTabBody(
                slider = percentSlider(
                    BackgroundProperty.Opacity.label,
                    edit.backgroundOpacity,
                    LayoutBackgroundOpacityRange,
                    actions::setBackgroundOpacity,
                ),
                sliderKey = BackgroundProperty.Opacity,
                // A picture in use is no colour of the grid's.
                selected = if (edit.backgroundArt == null) edit.background else Color.Unspecified,
                none = edit.backgroundArt == null && edit.background.alpha == 0f,
                onSelected = actions::setBackground,
                properties = BackgroundProperty.entries,
                selectedProperty = BackgroundProperty.Opacity,
                propertyLabel = { it.label },
                onPropertySelected = {},
                chrome = chrome,
                modifier = panelBodyModifier(),
            )
            BackgroundTab.Background -> ArtLibraryBody(
                load = backgroundLoad,
                selected = edit.backgroundArt,
                cellAspect = BackgroundArtCellAspect,
                contentScale = ContentScale.Crop,
                failedMessage = tr("Couldn't load backgrounds"),
                shopTab = ShopTab.Backgrounds,
                chrome = chrome,
                onSelected = { art -> actions.setBackgroundArt(art.takeUnless { it.id == edit.backgroundArt?.id }) },
                onRetry = actions::loadBackgroundArt,
                modifier = panelBodyModifier(),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ColorSwatchFill(color: Color) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SwatchInnerCorner)
    if (color == FrameColorNone) {
        // None: an empty swatch crossed by a slash, like the photo editor's Frame tool.
        val slash = scheme.onSurface.copy(alpha = 0.35f)
        Canvas(modifier = Modifier.fillMaxSize().border(1.dp, scheme.onSurface.copy(alpha = 0.2f), shape)) {
            drawLine(
                color = slash,
                start = Offset(size.width * 0.2f, size.height * 0.8f),
                end = Offset(size.width * 0.8f, size.height * 0.2f),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
    } else {
        // A white swatch on a light surface would vanish, so every one gets a hairline edge.
        Box(modifier = Modifier.fillMaxSize().background(color).border(1.dp, scheme.onSurface.copy(alpha = 0.12f), shape))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, chrome: LayoutChrome, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .clip(CircleShape)
            .background(if (selected) chrome.accent.copy(alpha = 0.18f) else chrome.content.copy(alpha = 0.06f))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) chrome.accent else chrome.content,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** A chip that does something at once (a flip), tinted while what it toggles is [active]. */
@Composable
private fun ActionChip(icon: ImageVector, label: String, active: Boolean, chrome: LayoutChrome, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    val tint = if (active) chrome.accent else chrome.content
    Row(
        modifier = Modifier
            .springBounce(bounce)
            .clip(CircleShape)
            .background(if (active) chrome.accent.copy(alpha = 0.18f) else chrome.content.copy(alpha = 0.06f))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
