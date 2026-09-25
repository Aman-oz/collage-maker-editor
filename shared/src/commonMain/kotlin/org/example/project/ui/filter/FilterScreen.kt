package org.example.project.ui.filter

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HideImage
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FilterScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = koinViewModel(),
) {
    FilterContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { filter ->
            viewModel.sourceImage?.let { image -> viewModel.applyFilter(bakeFilter(image, filter)) }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun FilterContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (PhotoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf(PhotoFilters.first()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        FilterTopBar(
            onBack = onBack,
            onApply = { onApply(selectedFilter) },
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
                // Live, temporary preview — the actual bitmap is only baked when Apply is tapped.
                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = selectedFilter.toColorFilter(),
                )
            } else {
                Text(
                    text = "No image to filter",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        FilterStrip(
            sourceImage = sourceImage,
            selected = selectedFilter,
            onSelected = { selectedFilter = it },
        )
    }
}

@Composable
private fun FilterTopBar(onBack: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Filters",
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
private fun FilterStrip(
    sourceImage: ImageBitmap?,
    selected: PhotoFilter,
    onSelected: (PhotoFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(PhotoFilters) { filter ->
            FilterChip(
                filter = filter,
                sourceImage = sourceImage,
                selected = filter == selected,
                onClick = { onSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    filter: PhotoFilter,
    sourceImage: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EditorControlBackground)
                .then(
                    if (selected) {
                        Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && !filter.isNoneOption) {
                Image(
                    bitmap = sourceImage,
                    contentDescription = filter.label,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                    colorFilter = filter.toColorFilter(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.HideImage,
                    contentDescription = filter.label,
                    tint = if (selected) EditorAccent else EditorIconTint,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun FilterScreenPreview() {
    ThemePreviews {
        FilterContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = {})
    }
}
