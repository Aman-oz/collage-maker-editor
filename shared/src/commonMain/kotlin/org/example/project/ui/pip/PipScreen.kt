package org.example.project.ui.pip

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.TopBarButtonSize
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

/**
 * Every tile has the same shape, so the grid reads as even rows; previews of the taller frames
 * are center-cropped into it.
 */
private const val TileAspectRatio = 0.82f

/**
 * Pip picker: a grid of the bundled PIP templates. Picking one asks the gallery for as many photos
 * as it has windows ([onTemplateSelected]), which then opens the Pip editor.
 */
@Composable
fun PipScreen(
    onBack: () -> Unit,
    onTemplateSelected: (PipTemplate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PipViewModel = koinViewModel(),
) {
    val previews by viewModel.previews.collectAsStateWithLifecycle()
    PipContent(
        templates = viewModel.templates,
        previews = previews,
        onBack = onBack,
        onTemplateSelected = onTemplateSelected,
        modifier = modifier,
    )
}

@Composable
private fun PipContent(
    templates: List<PipTemplate>,
    previews: Map<String, ImageBitmap>,
    onBack: () -> Unit,
    onTemplateSelected: (PipTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        PipTopBar(onBack = onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(templates, key = { it.name }) { template ->
                PipTile(preview = previews[template.name], onClick = { onTemplateSelected(template) })
            }
        }
    }
}

@Composable
private fun PipTopBar(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.topBar(), contentAlignment = Alignment.Center) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = tr("Back"),
            onClick = onBack,
            contentColor = scheme.onBackground,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = tr("Pip Templates"),
            color = scheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = TopBarButtonSize + 16.dp),
        )
    }
}

@Composable
private fun PipTile(preview: ImageBitmap?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TileAspectRatio)
            .clip(shape)
            // Stands in for the thumbnail while it decodes.
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = tr("PIP template"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun PipPreview() {
    ThemePreviews {
        PipContent(
            templates = PipTemplates,
            previews = emptyMap(),
            onBack = {},
            onTemplateSelected = {},
        )
    }
}
