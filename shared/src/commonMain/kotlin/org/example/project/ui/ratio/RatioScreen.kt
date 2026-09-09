package org.example.project.ui.ratio

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.crop.centeredRectForRatio
import org.example.project.ui.crop.cropImageBitmap
import org.example.project.ui.crop.toIntRectClamped
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

private data class RatioOption(val label: String, val ratio: Float)

private val RatioOptions = listOf(
    RatioOption("1:1", 1f / 1f),
    RatioOption("4:3", 4f / 3f),
    RatioOption("3:4", 3f / 4f),
    RatioOption("5:4", 5f / 4f),
    RatioOption("4:5", 4f / 5f),
    RatioOption("3:2", 3f / 2f),
    RatioOption("2:3", 2f / 3f),
    RatioOption("9:16", 9f / 16f),
    RatioOption("16:9", 16f / 9f),
)

@Composable
fun RatioScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RatioViewModel = koinViewModel(),
) {
    RatioContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { bitmap ->
            viewModel.applyRatio(bitmap)
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun RatioContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(RatioOptions.first()) }

    // Recomputed whenever the ratio changes, so the preview always shows exactly what Apply
    // would produce — reuses the same crop-baking pipeline the Crop screen uses.
    val preview = remember(sourceImage, selected) {
        sourceImage?.let { image -> cropImageBitmap(image, centeredRectForRatio(image, selected.ratio).toIntRectClamped(image)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        RatioTopBar(
            onBack = onBack,
            onApply = { preview?.let(onApply) },
            applyEnabled = preview != null,
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
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "Ratio preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = "No image to reframe",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        RatioOptionRow(selected = selected, onSelected = { selected = it })
    }
}

@Composable
private fun RatioTopBar(onBack: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Ratio",
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
private fun RatioOptionRow(selected: RatioOption, onSelected: (RatioOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(RatioOptions) { option ->
            RatioChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun RatioChip(option: RatioOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = if (selected) EditorAccent else EditorControlBackground,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) EditorAccent else Color.White,
        )
    }
}

@Preview
@Composable
private fun RatioScreenPreview() {
    ThemePreviews {
        RatioContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = {})
    }
}
