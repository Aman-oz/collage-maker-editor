package org.example.project.ui.layouts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.templates.CategoryRow
import org.example.project.ui.templates.ErrorState
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplatesTopBar
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

/** Home's View All for the Layouts row: the layouts by photo count, one tab per count. */
@Composable
fun LayoutsScreen(
    onBack: () -> Unit,
    onOpenLayout: (CollageLayout) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LayoutsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LayoutsContent(
        uiState = uiState,
        onBack = onBack,
        onCategorySelected = viewModel::selectCategory,
        onLayoutClick = onOpenLayout,
        onGoPro = onOpenPremium,
        onRetry = viewModel::loadCategories,
        modifier = modifier,
    )
}

@Composable
private fun LayoutsContent(
    uiState: LayoutsUiState,
    onBack: () -> Unit,
    onCategorySelected: (TemplateCategory) -> Unit,
    onLayoutClick: (CollageLayout) -> Unit,
    onGoPro: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        TemplatesTopBar(title = tr("Layouts"), onBack = onBack, onGoPro = onGoPro)

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

                uiState.error != null && uiState.layouts.isEmpty() ->
                    ErrorState(message = uiState.error, onRetry = onRetry)

                uiState.isLoadingLayouts ->
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)

                uiState.layouts.isEmpty() ->
                    Text(
                        text = tr("No collage layouts available"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else -> LazyVerticalGrid(
                    // Four columns on a phone; as many 84dp+ columns as fit on a tablet.
                    columns = GridCells.Adaptive(84.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(uiState.layouts, key = { it.id }) { layout ->
                        CollageLayoutCell(
                            layout = layout,
                            onClick = { onLayoutClick(layout) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * One layout's lines, tinted to the theme's text colour (dark on light, white on dark), with the
 * crown when it is premium. No ground or rounded clip: the picture is the outline itself, and a
 * clip would cut its corners. Shared by the Layouts browser and Home's Layouts row.
 */
@Composable
internal fun CollageLayoutCell(
    layout: CollageLayout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .springBounce(bounce)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        NetworkImage(
            url = layout.previewUrl,
            contentDescription = tr("Layouts"),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            modifier = Modifier.fillMaxSize(),
        )
        if (layout.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun LayoutsScreenPreview() {
    ThemePreviews {
        LayoutsContent(
            uiState = LayoutsUiState(
                categories = listOf(TemplateCategory("1", "1"), TemplateCategory("2", "2"), TemplateCategory("3", "3")),
                selectedCategoryId = "2",
                layouts = List(10) { CollageLayout("$it", "collage_2_$it", 2, "", isPremium = it == 3) },
            ),
            onBack = {},
            onCategorySelected = {},
            onLayoutClick = {},
            onGoPro = {},
            onRetry = {},
        )
    }
}
