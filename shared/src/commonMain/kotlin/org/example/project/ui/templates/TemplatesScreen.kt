package org.example.project.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.TopBarHeight
import org.example.project.ui.common.TopBarHorizontalPadding
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.templateFrameKey
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

private val PremiumGold = Color(0xFFFFB300)

@Composable
fun TemplatesScreen(
    onBack: () -> Unit,
    onOpenEditor: (TemplateFrame) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    TemplatesContent(
        uiState = uiState,
        onBack = onBack,
        onCategorySelected = viewModel::selectCategory,
        // A premium template opens the paywall unless the user already subscribes.
        onFrameClick = { frame ->
            if (frame.isPremium && !isPremium) onOpenPremium() else onOpenEditor(frame)
        },
        onGoPro = onOpenPremium,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * The catalog browser: top bar, category pills and the masonry grid. Also rendered by the Frames
 * screen, which passes its own [title] and [emptyMessage].
 */
@Composable
internal fun TemplatesContent(
    uiState: TemplatesUiState,
    onBack: () -> Unit,
    onCategorySelected: (TemplateCategory) -> Unit,
    onFrameClick: (TemplateFrame) -> Unit,
    onGoPro: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Templates",
    emptyMessage: String = "No templates in this category",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        TemplatesTopBar(title = title, onBack = onBack, onGoPro = onGoPro)

        if (uiState.categories.isNotEmpty()) {
            CategoryRow(
                categories = uiState.categories,
                selectedId = uiState.selectedCategoryId,
                onSelected = onCategorySelected,
            )
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                uiState.isLoadingCategories && uiState.categories.isEmpty() ->
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                uiState.error != null && uiState.frames.isEmpty() && uiState.categories.isEmpty() ->
                    ErrorState(message = uiState.error, onRetry = onRetry)

                uiState.isLoadingFrames ->
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                uiState.frames.isEmpty() ->
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else -> FramesGrid(frames = uiState.frames, onFrameClick = onFrameClick)
            }
        }
    }
}

@Composable
private fun TemplatesTopBar(title: String, onBack: () -> Unit, onGoPro: () -> Unit) {
    Row(
        // A smaller end padding because the 48dp premium IconButton already insets its icon.
        modifier = Modifier.fillMaxWidth().height(TopBarHeight).padding(start = TopBarHorizontalPadding, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        IconButton(onClick = onGoPro) {
            Icon(imageVector = Icons.Filled.WorkspacePremium, contentDescription = "Premium", tint = PremiumGold)
        }
    }
}

@Composable
private fun CategoryRow(categories: List<TemplateCategory>, selectedId: String?, onSelected: (TemplateCategory) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryPill(
                category = category,
                selected = category.id == selectedId,
                onClick = { onSelected(category) },
            )
        }
    }
}

@Composable
private fun CategoryPill(category: TemplateCategory, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
        )
    }
}

@Composable
private fun FramesGrid(frames: List<TemplateFrame>, onFrameClick: (TemplateFrame) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        items(frames, key = { it.id }) { frame ->
            FrameCell(frame = frame, onClick = { onFrameClick(frame) })
        }
    }
}

@Composable
private fun FrameCell(frame: TemplateFrame, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(frame.layout.aspectRatio)
            // Before the clip, so the rounded corners travel with the thumbnail into the editor.
            .navSharedElement(templateFrameKey(frame.id))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        NetworkImage(
            url = frame.thumbnailUrl,
            contentDescription = "Template",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (frame.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text("Retry", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun TemplatesScreenPreview() {
    ThemePreviews {
        TemplatesContent(
            uiState = TemplatesUiState(
                categories = listOf(
                    TemplateCategory("1", "All"),
                    TemplateCategory("2", "Love"),
                    TemplateCategory("3", "Summer"),
                ),
                selectedCategoryId = "1",
                isLoadingFrames = true,
            ),
            onBack = {},
            onCategorySelected = {},
            onFrameClick = {},
            onGoPro = {},
            onRetry = {},
        )
    }
}
