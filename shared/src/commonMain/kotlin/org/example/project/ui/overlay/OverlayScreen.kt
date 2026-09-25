package org.example.project.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.CompareButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

private val OverlayIntensityRange = 0f..100f

@Composable
fun OverlayScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverlayViewModel = koinViewModel(),
) {
    OverlayContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { preset, intensity ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyOverlay(bakeOverlay(image, preset, intensity / 100f))
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun OverlayContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (OverlayPreset, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf(OverlayCategory.Effect) }
    var selectedPreset by remember { mutableStateOf(OverlayPresets.first()) }
    var intensity by remember { mutableStateOf(0f) }
    var showOriginal by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        OverlayTopBar(
            onBack = onBack,
            onApply = { onApply(selectedPreset, intensity) },
            applyEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                if (!showOriginal && intensity > 0f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        selectedPreset.draw(this, intensity / 100f, selectedPreset.naturalBlendMode)
                    }
                }
            } else {
                Text(
                    text = "No image to overlay",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        OverlayCategoryTabs(
            selected = selectedCategory,
            onSelected = { category ->
                selectedCategory = category
                selectedPreset = OverlayPresets.first { it.category == category }
            },
        )

        OverlayControlRow(
            label = selectedPreset.label,
            value = intensity,
            onComparePressedChange = { showOriginal = it },
        )

        CenterFillSlider(
            value = intensity,
            onValueChange = { intensity = it },
            range = OverlayIntensityRange,
            referenceValue = OverlayIntensityRange.start,
        )

        OverlayPresetRow(
            category = selectedCategory,
            selected = selectedPreset,
            onSelected = { selectedPreset = it },
        )
    }
}

@Composable
private fun OverlayTopBar(onBack: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Overlay",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        AccentPillButton(text = "Apply", onClick = onApply, enabled = applyEnabled)
    }
}

@Composable
private fun OverlayCategoryTabs(selected: OverlayCategory, onSelected: (OverlayCategory) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OverlayCategory.entries.forEach { category ->
                OverlayCategoryTab(
                    category = category,
                    selected = category == selected,
                    onClick = { onSelected(category) },
                )
            }
        }
        HorizontalDivider(color = EditorControlBackground, thickness = 1.dp)
    }
}

@Composable
private fun OverlayCategoryTab(category: OverlayCategory, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .width(if (selected) 28.dp else 0.dp)
                .background(EditorAccent),
        )
    }
}

@Composable
private fun OverlayControlRow(label: String, value: Float, onComparePressedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorAccent,
            modifier = Modifier.padding(end = 12.dp),
        )
        CompareButton(onPressedChange = onComparePressedChange)
    }
}

@Composable
private fun OverlayPresetRow(
    category: OverlayCategory,
    selected: OverlayPreset,
    onSelected: (OverlayPreset) -> Unit,
) {
    val presets = remember(category) { OverlayPresets.filter { it.category == category } }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(presets) { preset ->
            OverlayPresetChip(
                preset = preset,
                selected = preset == selected,
                onClick = { onSelected(preset) },
            )
        }
    }
}

@Composable
private fun OverlayPresetChip(preset: OverlayPreset, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EditorControlBackground)
                .then(
                    if (selected) {
                        Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                ),
        ) {
            preset.draw(this, 1f, BlendMode.SrcOver)
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun OverlayScreenPreview() {
    ThemePreviews {
        OverlayContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = { _, _ -> })
    }
}
