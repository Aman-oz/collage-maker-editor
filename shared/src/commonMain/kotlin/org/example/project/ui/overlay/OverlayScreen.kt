package org.example.project.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val OverlayIntensityRange = 0f..100f
private val ChipShape = RoundedCornerShape(6.dp)

/** Thumbnails paint the overlay over this dark ground so light, screen-style effects stay visible. */
private val ThumbnailGround = Color(0xFF16161C)

@Composable
internal fun OverlayTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayContent(
        sourceImage = sourceImage,
        onBack = onClose,
        onApply = { preset, intensity ->
            when {
                // "None", or an overlay faded all the way out, leaves the photo as it was.
                preset == null || intensity <= 0f -> onClose()
                // A premium overlay previews for everyone, but Done sends a non-subscriber to the
                // paywall. The tool stays open underneath, so Done applies it once they come back
                // subscribed.
                preset.isPremium && !isPremium -> onOpenPremium()
                else -> onApply(bakeOverlay(sourceImage, preset, intensity / 100f))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun OverlayContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (OverlayPreset?, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    // Saveable, unlike most tools' edits: the paywall covers the editor, which drops plain
    // `remember` state, and the overlay picked before subscribing has to still be there after.
    var selectedCategory by rememberSaveable { mutableStateOf(OverlayCategory.Effect) }
    // null is the "None" chip: the photo is shown untouched.
    var selectedPresetKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPreset = selectedPresetKey?.let { key -> OverlayPresets.firstOrNull { it.key == key } }
    var intensity by rememberSaveable { mutableFloatStateOf(OverlayIntensityRange.endInclusive) }
    var comparing by remember { mutableStateOf(false) }

    ToolScaffold(modifier = modifier) {
        Box(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                // aspectRatio sizes the stage to the photo itself, so the overlay canvas covers
                // exactly the photo and the rounded clip follows its edges.
                Box(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    Image(
                        bitmap = sourceImage,
                        contentDescription = tr("Photo preview"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    val preset = selectedPreset
                    if (preset != null && !comparing && intensity > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            preset.draw(this, intensity / 100f, preset.naturalBlendMode)
                        }
                    }
                }
            } else {
                Text(
                    text = tr("No image to overlay"),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        ToolPanel(
            title = tr("Overlay"),
            onClose = onBack,
            onDone = { onApply(selectedPreset, intensity) },
            doneEnabled = sourceImage != null,
        ) {
            // The tabs scroll; the compare button stays pinned at the row's end.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverlayCategoryTabs(
                    selected = selectedCategory,
                    onSelected = { selectedCategory = it },
                    modifier = Modifier.weight(1f),
                )
                CompareIconButton(onComparingChange = { comparing = it })
            }

            CenterFillSlider(
                value = intensity,
                onValueChange = { intensity = it },
                range = OverlayIntensityRange,
                referenceValue = OverlayIntensityRange.start,
                trackColor = scheme.onSurface.copy(alpha = 0.12f),
                fillColor = scheme.primary,
                thumbColor = scheme.primary,
                thumbWidth = 32.dp,
                thumbHeight = 18.dp,
                horizontalPadding = 12.dp,
                glassThumb = true,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                valueLabel = ::wholeNumberLabel,
            )

            OverlayPresetStrip(
                category = selectedCategory,
                selected = selectedPreset,
                onSelected = { selectedPresetKey = it?.key },
            )
        }
    }
}

@Composable
private fun OverlayCategoryTabs(
    selected: OverlayCategory,
    onSelected: (OverlayCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        OverlayCategory.entries.forEach { category ->
            val isSelected = category == selected
            // Width(IntrinsicSize.Max) sizes the column to its label, so the underline below can
            // be a fraction of the text width without measuring it.
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(category) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = category.label,
                    color = if (isSelected) scheme.onSurface else scheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) scheme.primary else Color.Transparent),
                )
            }
        }
    }
}

/** Press-and-hold icon that shows the photo without the overlay while held. */
@Composable
private fun CompareIconButton(onComparingChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .pointerInput(onComparingChange) {
                detectTapGestures(
                    onPress = {
                        onComparingChange(true)
                        tryAwaitRelease()
                        onComparingChange(false)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_before_after),
            contentDescription = tr("Press and hold to compare with the original"),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun OverlayPresetStrip(
    category: OverlayCategory,
    selected: OverlayPreset?,
    onSelected: (OverlayPreset?) -> Unit,
) {
    val presets = remember(category) { OverlayPresets.filter { it.category == category } }
    LazyRow(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "none") {
            NoneChip(selected = selected == null, onClick = { onSelected(null) })
        }
        items(presets, key = { it.key }) { preset ->
            OverlayPresetChip(preset = preset, selected = preset == selected, onClick = { onSelected(preset) })
        }
    }
}

/** Same outer size as [OverlayPresetChip], with an icon and label instead of a swatch. */
@Composable
private fun NoneChip(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .chipFrame(selected, scheme.primary, onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr("None"),
            color = scheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** An unlabeled swatch of the overlay itself, drawn flat (SrcOver) so it reads at thumbnail size. */
@Composable
private fun OverlayPresetChip(preset: OverlayPreset, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .chipFrame(selected, scheme.primary, onClick)
            .padding(if (selected) 3.dp else 0.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (selected) RoundedCornerShape(4.dp) else ChipShape)
                .background(ThumbnailGround),
        ) {
            preset.draw(this, 1f, BlendMode.SrcOver)
        }
        if (preset.isPremium) {
            // Inside the swatch rather than hanging off its corner: the chip clips its content.
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(14.dp),
            )
        }
    }
}

private fun Modifier.chipFrame(selected: Boolean, accent: Color, onClick: () -> Unit): Modifier = this
    .width(54.dp)
    .height(74.dp)
    .clip(ChipShape)
    .then(if (selected) Modifier.border(1.5.dp, accent, ChipShape) else Modifier)
    .clickable(onClick = onClick)

@Preview
@Composable
private fun OverlayScreenPreview() {
    ThemePreviews {
        OverlayContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = { _, _ -> })
    }
}
