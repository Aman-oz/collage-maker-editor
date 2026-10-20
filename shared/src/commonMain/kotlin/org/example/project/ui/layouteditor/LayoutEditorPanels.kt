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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ColorPickerFill
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.rememberColorPickerLauncher
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.wholeNumberLabel
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

/** The panel over the tabs for [tool]; Text, Filter and Ratio have none and show the last one's. */
@Composable
internal fun LayoutToolPanel(
    tool: LayoutTool,
    editOption: LayoutEditOption,
    edit: LayoutEdit,
    templates: List<TemplateItem>,
    premiumLayouts: Set<String>,
    chrome: LayoutChrome,
    selectedPhotoSlot: Int?,
    actions: LayoutEditorActions,
    onStickerTapped: (emoji: String, cellBoundsInRoot: Rect) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (tool) {
            LayoutTool.Canvas -> AspectRatioStrip(
                options = LayoutCanvasOptions,
                selected = edit.canvas,
                onSelected = actions::setCanvas,
            )
            LayoutTool.Layout -> LayoutPickerPanel(
                templates = templates,
                premiumLayouts = premiumLayouts,
                selectedId = edit.template.id,
                chrome = chrome,
                onSelected = actions::selectTemplate,
            )
            LayoutTool.Edit -> when (editOption) {
                LayoutEditOption.Margin -> MarginPanel(edit, chrome, actions)
                LayoutEditOption.Border -> BorderPanel(edit, chrome, actions)
                LayoutEditOption.Frame -> FramePanel(edit, chrome, actions)
                LayoutEditOption.Transform -> TransformPanel(edit, chrome, selectedPhotoSlot, actions)
            }
            LayoutTool.Background -> BackgroundPanel(selected = edit.background, chrome = chrome, onSelected = actions::setBackground)
            LayoutTool.Sticker -> StickersPanel(chrome = freestyleChrome(), onStickerTapped = onStickerTapped)
            // These open over the screen instead of a panel, so they never become the panel's tool.
            LayoutTool.Text, LayoutTool.Filter, LayoutTool.Ratio -> Unit
        }
    }
}

private val LayoutThumbnailSize = 64.dp

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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
            .padding(7.dp),
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

private enum class MarginProperty(private val englishLabel: String) {
    Corner("Corner"),
    Shadow("Shadow"),
    Margin("Margin"),
    Space("Space"),
    ;

    val label: String get() = tr(englishLabel)
}

/** Corner, shadow, margin and space: one slider, switched by the chips above it. */
@Composable
private fun MarginPanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions) {
    var property by rememberSaveable { mutableStateOf(MarginProperty.Margin) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PropertyChips(MarginProperty.entries, property, { it.label }, chrome) { property = it }
        when (property) {
            MarginProperty.Corner -> PanelSlider(edit.corner, LayoutCornerRange, chrome, onValueChange = actions::setCorner)
            MarginProperty.Shadow -> PanelSlider(edit.shadow, LayoutShadowRange, chrome, onValueChange = actions::setShadow)
            MarginProperty.Margin -> PanelSlider(edit.margin, LayoutMarginRange, chrome, onValueChange = actions::setMargin)
            MarginProperty.Space -> PanelSlider(edit.space, LayoutSpaceRange, chrome, onValueChange = actions::setSpace)
        }
    }
}

private enum class BorderProperty(private val englishLabel: String) {
    Thickness("Thickness"),
    Opacity("Opacity"),
    ;

    val label: String get() = tr(englishLabel)
}

/** The outline round every slot: its thickness or opacity on the slider, and its colour. */
@Composable
private fun BorderPanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions) {
    var property by rememberSaveable { mutableStateOf(BorderProperty.Thickness) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PropertyChips(BorderProperty.entries, property, { it.label }, chrome) { property = it }
        when (property) {
            BorderProperty.Thickness -> PanelSlider(edit.borderWidth, LayoutBorderWidthRange, chrome, onValueChange = actions::setBorderWidth)
            BorderProperty.Opacity -> PanelSlider(edit.borderOpacity, LayoutBorderOpacityRange, chrome, onValueChange = actions::setBorderOpacity)
        }
        ColorRow(
            colors = LineColors,
            selected = edit.borderColor,
            onSelected = actions::setBorderColor,
        )
    }
}

/** The frame colours without None: a border is turned off by its thickness. */
private val LineColors: List<Color> = FrameColors.map { it.color }.filter { it != FrameColorNone }

private enum class FrameProperty(private val englishLabel: String) {
    Width("Width"),
    CornerRadius("Corner Radius"),
    ;

    val label: String get() = tr(englishLabel)
}

/** The photo editor's frame, round the whole canvas: width or corners on the slider, and its colour (or None). */
@Composable
private fun FramePanel(edit: LayoutEdit, chrome: LayoutChrome, actions: LayoutEditorActions) {
    var property by rememberSaveable { mutableStateOf(FrameProperty.Width) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PropertyChips(FrameProperty.entries, property, { it.label }, chrome) { property = it }
        when (property) {
            FrameProperty.Width -> PanelSlider(edit.frameWidth, LayoutFrameWidthRange, chrome, onValueChange = actions::setFrameWidth)
            FrameProperty.CornerRadius -> PanelSlider(edit.frameCorner, LayoutFrameCornerRange, chrome, onValueChange = actions::setFrameCorner)
        }
        ColorRow(
            colors = FrameColors.map { it.color },
            selected = edit.frameColor,
            onSelected = actions::setFrameColor,
        )
    }
}

private enum class TransformProperty(private val englishLabel: String) {
    Width("Width"),
    Height("Height"),
    Rotate("Rotate"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * The selected photo's mirror (horizontally / vertically, a tap each), and its width, height and
 * turn on the slider. With no photo selected it says to select one.
 */
@Composable
private fun TransformPanel(edit: LayoutEdit, chrome: LayoutChrome, selectedSlot: Int?, actions: LayoutEditorActions) {
    val photo = selectedSlot?.let { edit.photos[it] }
    if (selectedSlot == null || photo == null) {
        Text(
            text = tr("Tap a photo to select it"),
            color = chrome.muted,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    var property by rememberSaveable { mutableStateOf(TransformProperty.Rotate) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                ActionChip(
                    icon = vectorResource(Res.drawable.ic_flip_horizontally),
                    label = tr("Horizontal"),
                    active = photo.flipX,
                    chrome = chrome,
                    onClick = { actions.flipPhoto(selectedSlot, horizontally = true) },
                )
            }
            item {
                ActionChip(
                    icon = vectorResource(Res.drawable.ic_flip_vertically),
                    label = tr("Vertical"),
                    active = photo.flipY,
                    chrome = chrome,
                    onClick = { actions.flipPhoto(selectedSlot, horizontally = false) },
                )
            }
            items(TransformProperty.entries) { p ->
                Chip(label = p.label, selected = p == property, chrome = chrome, onClick = { property = p })
            }
            item {
                Chip(label = tr("Reset"), selected = false, chrome = chrome, onClick = { actions.resetPhotoTransform(selectedSlot) })
            }
        }
        when (property) {
            TransformProperty.Width -> PanelSlider(
                value = photo.stretchX * 100f,
                range = LayoutStretchRange,
                chrome = chrome,
                referenceValue = 100f,
                onValueChange = { actions.setPhotoStretchX(selectedSlot, it / 100f) },
            )
            TransformProperty.Height -> PanelSlider(
                value = photo.stretchY * 100f,
                range = LayoutStretchRange,
                chrome = chrome,
                referenceValue = 100f,
                onValueChange = { actions.setPhotoStretchY(selectedSlot, it / 100f) },
            )
            TransformProperty.Rotate -> PanelSlider(
                value = photo.rotationDegrees,
                range = LayoutRotationRange,
                chrome = chrome,
                referenceValue = 0f,
                onValueChange = { actions.setPhotoRotation(selectedSlot, it) },
            )
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

/** Picks which of [entries] the slider below edits. */
@Composable
private fun <T> PropertyChips(
    entries: List<T>,
    selected: T,
    label: (T) -> String,
    chrome: LayoutChrome,
    onSelected: (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(entries) { entry ->
            Chip(label = label(entry), selected = entry == selected, chrome = chrome, onClick = { onSelected(entry) })
        }
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

@Composable
private fun PanelSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    chrome: LayoutChrome,
    referenceValue: Float = range.start,
    onValueChange: (Float) -> Unit,
) {
    CenterFillSlider(
        value = value,
        onValueChange = onValueChange,
        range = range,
        referenceValue = referenceValue,
        trackColor = chrome.track,
        fillColor = chrome.accent,
        thumbColor = chrome.accent,
        thumbWidth = 32.dp,
        thumbHeight = 18.dp,
        horizontalPadding = 24.dp,
        glassThumb = true,
        glassTint = if (chrome.isLight) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        valueLabel = ::wholeNumberLabel,
    )
}
