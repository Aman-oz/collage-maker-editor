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
import org.example.project.ui.freestyle.FreestyleBackgroundColors
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
        LayoutOption.Frame -> FramePanel(edit, chrome, actions, onClose, onDone)
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
        LayoutOption.Background -> TextPanelColumn(title = option.label, onClose = onClose, onDone = onDone) {
            BackgroundPanel(selected = edit.background, chrome = chrome, onSelected = actions::setBackground)
            Spacer(modifier = Modifier.height(12.dp))
        }
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
    Color("Color"),
    ;

    val label: String get() = tr(englishLabel)
}

/** The outline round every slot: its thickness, its opacity, or its colour. */
@Composable
private fun BorderPanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions, onClose: () -> Unit, onDone: () -> Unit) {
    var property by rememberSaveable { mutableStateOf(BorderProperty.Thickness) }
    PropertyPanel(tr("Border"), BorderProperty.entries, property, { it.label }, chrome, { property = it }, onClose, onDone) {
        when (property) {
            BorderProperty.Thickness -> key(property) {
                PanelSliderRow(percentSlider(property.label, edit.borderWidth, LayoutBorderWidthRange, actions::setBorderWidth))
            }
            BorderProperty.Opacity -> key(property) {
                PanelSliderRow(percentSlider(property.label, edit.borderOpacity, LayoutBorderOpacityRange, actions::setBorderOpacity))
            }
            BorderProperty.Color -> ColorRow(colors = LineColors, selected = edit.borderColor, onSelected = actions::setBorderColor)
        }
    }
}

/** The frame colours without None: a border is turned off by its thickness. */
private val LineColors: List<Color> = FrameColors.map { it.color }.filter { it != FrameColorNone }

private enum class FrameProperty(private val englishLabel: String) {
    Width("Width"),
    CornerRadius("Corner Radius"),
    Color("Color"),
    ;

    val label: String get() = tr(englishLabel)
}

/** The photo editor's frame, round the whole canvas: its width, its corners, or its colour (or None). */
@Composable
private fun FramePanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions, onClose: () -> Unit, onDone: () -> Unit) {
    var property by rememberSaveable { mutableStateOf(FrameProperty.Width) }
    PropertyPanel(tr("Frame"), FrameProperty.entries, property, { it.label }, chrome, { property = it }, onClose, onDone) {
        when (property) {
            FrameProperty.Width -> key(property) {
                PanelSliderRow(percentSlider(property.label, edit.frameWidth, LayoutFrameWidthRange, actions::setFrameWidth))
            }
            FrameProperty.CornerRadius -> key(property) {
                PanelSliderRow(percentSlider(property.label, edit.frameCorner, LayoutFrameCornerRange, actions::setFrameCorner))
            }
            FrameProperty.Color -> ColorRow(colors = FrameColors.map { it.color }, selected = edit.frameColor, onSelected = actions::setFrameColor)
        }
    }
}

private val BackgroundSwatchSize = 50.dp
private val BackgroundSwatchSpacing = 10.dp

/** The background colours: two rows scrolling sideways, led by the custom colour swatch spanning both. */
@Composable
private fun BackgroundPanel(selected: Color, chrome: LayoutChrome, onSelected: (Color) -> Unit) {
    val pickCustomColor = rememberColorPickerLauncher(initial = selected, onPicked = onSelected)
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(BackgroundSwatchSpacing),
        verticalArrangement = Arrangement.spacedBy(BackgroundSwatchSpacing),
        modifier = Modifier.fillMaxWidth().height(BackgroundSwatchSize * 2 + BackgroundSwatchSpacing),
    ) {
        // It spans both rows, so the colour pairs after it stay aligned. It is the one ringed while
        // the background is a colour of the user's own.
        item(span = { GridItemSpan(maxLineSpan) }) {
            ColorTile(
                selected = selected !in FreestyleBackgroundColors,
                chrome = chrome,
                height = BackgroundSwatchSize * 2 + BackgroundSwatchSpacing,
                onClick = pickCustomColor,
            ) { ColorPickerFill(it) }
        }
        items(FreestyleBackgroundColors) { color ->
            ColorTile(selected = color == selected, chrome = chrome, onClick = { onSelected(color) }) {
                Box(modifier = it.background(color))
            }
        }
    }
}

/** [fill] paints the tile inside its selection ring; it is handed the modifier that sizes it. */
@Composable
private fun ColorTile(
    selected: Boolean,
    chrome: LayoutChrome,
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
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(4.dp)
            .clip(inner)
            .border(1.dp, chrome.content.copy(alpha = 0.08f), inner),
    ) {
        fill(Modifier.matchParentSize())
    }
}

/**
 * A row of colour swatches led by the custom colour one, which is ringed while [selected] is none of
 * [colors]. [FrameColorNone] in [colors] shows as a slashed swatch.
 */
@Composable
private fun ColorRow(colors: List<Color>, selected: Color, onSelected: (Color) -> Unit) {
    val pickCustomColor = rememberColorPickerLauncher(
        // None is transparent, which is no colour to open a picker on.
        initial = if (selected == FrameColorNone) Color.White else selected,
        onPicked = onSelected,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            SelectableSwatch(selected = selected !in colors, size = ColorSwatchSize, onClick = pickCustomColor) {
                ColorPickerFill(Modifier.fillMaxSize())
            }
        }
        items(colors) { color ->
            SelectableSwatch(selected = color == selected, size = ColorSwatchSize, onClick = { onSelected(color) }) {
                ColorSwatchFill(color)
            }
        }
    }
}

private val ColorSwatchSize = 44.dp

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
