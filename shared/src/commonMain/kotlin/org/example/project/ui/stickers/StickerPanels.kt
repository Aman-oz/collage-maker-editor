package org.example.project.ui.stickers

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.adjust.DrawableAdjustIconScale
import org.example.project.ui.adjust.signedAdjustLabel
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.crop.AspectRatioGrid
import org.example.project.ui.crop.AspectRatioOption
import org.example.project.ui.crop.AspectRatioOptions
import org.example.project.ui.crop.AspectRatioStrip
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.filter.PhotoFilters
import org.example.project.ui.filter.toColorFilter
import org.example.project.ui.text.GlassBarOption
import org.example.project.ui.text.GlassOptionsBar
import org.example.project.ui.text.LocalTextPanelSideBySide
import org.example.project.ui.text.PanelSlider
import org.example.project.ui.text.PanelSliderRow
import org.example.project.ui.text.TextPanelColumn
import org.example.project.ui.text.TextPanelPlacement
import org.example.project.ui.text.TextSwatchGrid
import org.example.project.ui.text.normalizeDegrees
import org.example.project.ui.text.snapTextRotation
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_duplicate
import photocollagemaker.shared.generated.resources.ic_premium_icon
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_reset
import photocollagemaker.shared.generated.resources.ic_undo
import photocollagemaker.shared.generated.resources.icon_adjust
import photocollagemaker.shared.generated.resources.icon_clearity
import photocollagemaker.shared.generated.resources.icon_crop
import photocollagemaker.shared.generated.resources.icon_effect
import photocollagemaker.shared.generated.resources.icon_eraser
import photocollagemaker.shared.generated.resources.icon_fill
import photocollagemaker.shared.generated.resources.icon_mirro
import photocollagemaker.shared.generated.resources.icon_mirror
import photocollagemaker.shared.generated.resources.icon_nudge
import photocollagemaker.shared.generated.resources.icon_opaciy
import photocollagemaker.shared.generated.resources.icon_outline
import photocollagemaker.shared.generated.resources.icon_position
import photocollagemaker.shared.generated.resources.icon_reshape
import photocollagemaker.shared.generated.resources.icon_rotate_90
import photocollagemaker.shared.generated.resources.icon_rotate
import photocollagemaker.shared.generated.resources.icon_shadow
import photocollagemaker.shared.generated.resources.icon_size
import photocollagemaker.shared.generated.resources.icon_tint
import photocollagemaker.shared.generated.resources.icon_transfom

/** The Stickers tool's options for the selected sticker, in the order of its floating bar. */
internal enum class StickerOption(private val englishLabel: String, val icon: DrawableResource) {
    Duplicate("Duplicate", Res.drawable.ic_duplicate),
    Reshape("Reshape", Res.drawable.icon_reshape),
    Eraser("Eraser", Res.drawable.icon_eraser),
    Crop("Crop", Res.drawable.icon_crop),
    Effects("Effects", Res.drawable.icon_effect),
    Fill("Fill", Res.drawable.icon_fill),
    Outline("Outline", Res.drawable.icon_outline),
    Adjust("Adjust", Res.drawable.icon_adjust),
    Opacity("Opacity", Res.drawable.icon_opaciy),
    Shadow("Shadow", Res.drawable.icon_shadow),
    Position("Position", Res.drawable.icon_position),
    Transform("Transform", Res.drawable.icon_transfom),
    Size("Size", Res.drawable.icon_size),
    Rotate("Rotate", Res.drawable.icon_rotate),
    Nudge("Nudge", Res.drawable.icon_nudge),
    Mirror("Mirror", Res.drawable.icon_mirro),
    ;

    /** In the app's current language; read it where it is shown, never keep it. */
    val label: String get() = tr(englishLabel)
}

/** The sticker options in the Text tool's floating glass bar, under its ✕. */
@Composable
internal fun StickerOptionsBar(
    onOptionSelected: (StickerOption) -> Unit,
    onClose: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    cascadeDelayMillis: Int = 0,
) {
    val options = StickerOption.entries
    GlassOptionsBar(
        options = options.map { GlassBarOption(it.label, it.icon) },
        onOptionSelected = { onOptionSelected(options[it]) },
        onClose = onClose,
        listState = listState,
        modifier = modifier,
        cascadeDelayMillis = cascadeDelayMillis,
    )
}

/**
 * The Reshape panel: the sticker itself, small, under ✕ / Reshape / ✓. The reshaping is done on the
 * stage, by dragging the frame's corners.
 */
@Composable
internal fun StickerReshapePanel(preview: ImageBitmap?, onClose: () -> Unit, onDone: () -> Unit) {
    TextPanelColumn(title = tr("Reshape"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
            if (preview != null) {
                Image(bitmap = preview, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(64.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * The Eraser panel: Erase / Restore, undo and redo of the strokes, and the brush size. The strokes
 * themselves are drawn on the stage, over the selected sticker.
 */
@Composable
internal fun StickerEraserPanel(
    restore: Boolean,
    onRestoreChange: (Boolean) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    TextPanelColumn(title = tr("Eraser"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelChip(label = tr("Erase"), selected = !restore, onClick = { onRestoreChange(false) })
            PanelChip(label = tr("Restore"), selected = restore, onClick = { onRestoreChange(true) })
            Spacer(modifier = Modifier.weight(1f))
            PanelIconButton(painterResource(Res.drawable.ic_undo), tr("Undo"), enabled = canUndo, onClick = onUndo)
            PanelIconButton(painterResource(Res.drawable.ic_redo), tr("Redo"), enabled = canRedo, onClick = onRedo)
        }
        PanelSliderRow(PanelSlider(tr("Brush size"), brushSize, onValueChange = onBrushSizeChange))
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** The Crop panel's "keep the sticker's own shape" chip, after Free; worded like it. */
internal val OriginalCropOption = AspectRatioOption("Original", null)

/** The Crop panel's shapes: the photo editor's Crop ratios, with Original after Free. */
internal val StickerCropOptions: List<AspectRatioOption> =
    AspectRatioOptions.take(1) + OriginalCropOption + AspectRatioOptions.drop(1)

/**
 * The Crop panel: the photo editor's ratio strip (a grid beside the photo). The frame itself is
 * dragged on the stage.
 */
@Composable
internal fun StickerCropPanel(
    selected: AspectRatioOption,
    onSelected: (AspectRatioOption) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val sideBySide = LocalTextPanelSideBySide.current
    TextPanelColumn(
        title = tr("Crop"),
        onClose = onClose,
        onDone = onDone,
        placement = if (sideBySide) TextPanelPlacement.Fill else TextPanelPlacement.Bottom,
    ) {
        if (sideBySide) {
            AspectRatioGrid(options = StickerCropOptions, selected = selected, onSelected = onSelected, modifier = Modifier.weight(1f))
        } else {
            AspectRatioStrip(options = StickerCropOptions, selected = selected, onSelected = onSelected)
        }
    }
}

/**
 * The Effects panel: the photo filters, each shown on the sticker ([preview]) with a crown when
 * premium, and the picked one's intensity.
 */
@Composable
internal fun StickerEffectsPanel(
    preview: ImageBitmap?,
    filterLabel: String?,
    intensity: Float,
    onFilterSelected: (String?) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    TextPanelColumn(title = tr("Effects"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        // Right under the ✕ / ✓ row, over the effects. Dimmed with no effect picked, where it would change nothing.
        Box(modifier = Modifier.alpha(if (filterLabel == null) 0.4f else 1f)) {
            PanelSliderRow(PanelSlider(tr("Intensity"), intensity * 100f) { if (filterLabel != null) onIntensityChange(it / 100f) })
        }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(PhotoFilters, key = { it.label }) { filter ->
                val selected = if (filter.matrix == null) filterLabel == null else filter.label == filterLabel
                val bounce = rememberSpringBounce()
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .springBounce(bounce)
                        .width(60.dp)
                        .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current) {
                            onFilterSelected(filter.label.takeIf { filter.matrix != null })
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(scheme.onSurface.copy(alpha = 0.08f))
                            .then(if (selected) Modifier.border(2.dp, scheme.primary, shape) else Modifier),
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                colorFilter = filter.toColorFilter(),
                                modifier = Modifier.padding(6.dp).size(44.dp),
                            )
                        }
                        if (filter.isPremium) PremiumBadge(Modifier.align(Alignment.TopEnd).padding(3.dp), size = 18.dp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(filter.label),
                        fontSize = 11.sp,
                        color = scheme.onSurface,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * A slider over a row of colours, the colour wheel first: the Fill panel (its intensity) and the
 * Outline panel (its thickness).
 */
@Composable
internal fun StickerColorPanel(
    title: String,
    slider: PanelSlider,
    selectedArgb: Int?,
    onColorSelected: (Color) -> Unit,
    onPickCustom: () -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val sideBySide = LocalTextPanelSideBySide.current
    TextPanelColumn(
        title = title,
        onClose = onClose,
        onDone = onDone,
        placement = if (sideBySide) TextPanelPlacement.Fill else TextPanelPlacement.Bottom,
    ) {
        PanelSliderRow(slider)
        Spacer(modifier = Modifier.height(8.dp))
        TextSwatchGrid(
            selectedArgb = selectedArgb,
            onSelected = onColorSelected,
            onPickCustom = onPickCustom,
            vertical = sideBySide,
            modifier = if (sideBySide) Modifier.weight(1f) else Modifier,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * The Adjust panel: the photo editor's six adjustments and Tint, one at a time on a -100..100
 * slider, then Reset, which puts them all back to zero.
 */
@Composable
internal fun StickerAdjustPanel(
    adjust: StickerAdjust,
    type: StickerAdjustment,
    onTypeSelected: (StickerAdjustment) -> Unit,
    onChange: (StickerAdjust) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    TextPanelColumn(title = tr("Adjust"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        CenterFillSlider(
            value = adjust[type],
            onValueChange = { onChange(adjust.with(type, it)) },
            range = -100f..100f,
            referenceValue = 0f,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            valueLabel = ::signedAdjustLabel,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(StickerAdjustment.entries) { entry ->
                val photo = entry.photo
                PanelVectorTab(
                    icon = when {
                        photo != null -> photo.icon
                        entry == StickerAdjustment.Clarity -> vectorResource(Res.drawable.icon_clearity)
                        else -> vectorResource(Res.drawable.icon_tint)
                    },
                    label = when {
                        photo != null -> photo.label
                        entry == StickerAdjustment.Clarity -> tr("Clarity")
                        else -> tr("Tint")
                    },
                    iconScale = photo?.iconScale ?: DrawableAdjustIconScale,
                    selected = entry == type,
                    // A dot under the ones moved off zero.
                    marked = adjust[entry] != 0f,
                    onClick = { onTypeSelected(entry) },
                )
            }
            item {
                PanelVectorTab(
                    icon = vectorResource(Res.drawable.ic_reset),
                    label = tr("Reset"),
                    selected = false,
                    marked = false,
                    onClick = { onChange(StickerAdjust()) },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * The Rotate panel: a 0–100 slider over the sticker's whole turn (50 upright, [rotationToSlider]),
 * then Rotate (a quarter turn to the left, anticlockwise) and Reset (upright again).
 */
@Composable
internal fun StickerRotatePanel(
    rotationDegrees: Float,
    onRotationChange: (Float) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    TextPanelColumn(title = tr("Rotate"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        PanelSliderRow(
            PanelSlider(tr("Rotate"), rotationToSlider(rotationDegrees)) { value ->
                onRotationChange(snapTextRotation(sliderToRotation(value)))
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            PanelVectorTab(
                icon = vectorResource(Res.drawable.icon_rotate_90),
                label = tr("Rotate"),
                selected = false,
                marked = false,
                onClick = { onRotationChange(normalizeDegrees(rotationDegrees - 90f)) },
                iconScale = RotateActionIconScale * DrawableAdjustIconScale,
            )
            PanelVectorTab(
                icon = vectorResource(Res.drawable.ic_reset),
                label = tr("Reset"),
                selected = false,
                marked = false,
                onClick = { onRotationChange(0f) },
                iconScale = RotateActionIconScale,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** The Rotate panel's two buttons are its only controls besides the slider, so they are drawn larger. */
private const val RotateActionIconScale = 1.15f

/**
 * The Mirror panel: Left, Right, Top or Bottom keeps that half of the sticker and mirrors it onto the
 * other ([PlacedSticker.mirror]); tapping the chosen one again, or Reset, takes the mirror off. The
 * buttons share [Res.drawable.icon_mirror] (its filled half on the right), turned to put the filled
 * half on the side they keep.
 */
@Composable
internal fun StickerMirrorPanel(
    mirror: StickerMirror?,
    onMirrorChange: (StickerMirror?) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
) {
    TextPanelColumn(title = tr("Mirror"), onClose = onClose, onDone = onDone, placement = TextPanelPlacement.Center) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            for (entry in StickerMirror.entries) {
                PanelVectorTab(
                    icon = vectorResource(Res.drawable.icon_mirror),
                    label = tr(entry.englishLabel),
                    selected = entry == mirror,
                    marked = false,
                    onClick = { onMirrorChange(if (entry == mirror) null else entry) },
                    iconScale = DrawableAdjustIconScale,
                    iconRotation = entry.iconRotation,
                    modifier = Modifier.weight(1f),
                )
            }
            PanelVectorTab(
                icon = vectorResource(Res.drawable.ic_reset),
                label = tr("Reset"),
                selected = false,
                marked = false,
                onClick = { onMirrorChange(null) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** How far each Mirror button turns [Res.drawable.icon_mirror], whose filled half is on the right. */
private val StickerMirror.iconRotation: Float
    get() = when (this) {
        StickerMirror.Left -> 180f
        StickerMirror.Right -> 0f
        StickerMirror.Top -> -90f
        StickerMirror.Bottom -> 90f
    }

/** A pill to pick one of a few modes or shapes, brand-tinted while [selected]. */
@Composable
private fun PanelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .height(34.dp)
            .clip(shape)
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.07f))
            .then(if (selected) Modifier.border(1.dp, scheme.primary, shape) else Modifier)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun PanelIconButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun PanelVectorTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    marked: Boolean,
    onClick: () -> Unit,
    iconScale: Float = 1f,
    iconRotation: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    val tint = if (selected) scheme.primary else scheme.onSurface
    Column(
        modifier = modifier
            .springBounce(bounce)
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Scaled within its slot, so a larger icon doesn't push its label out of line with the others'.
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp).scale(iconScale).rotate(iconRotation))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(4.dp)
                .clip(RoundedCornerShape(50))
                .background(if (marked) scheme.primary else Color.Transparent),
        )
    }
}
