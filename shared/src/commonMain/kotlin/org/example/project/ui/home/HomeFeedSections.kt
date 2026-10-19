package org.example.project.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.i18n.tr
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.templateFrameKey
import org.example.project.ui.layouts.CollageLayout
import org.example.project.ui.layouts.CollageLayoutCell
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.templates.TemplateCategory
import org.example.project.ui.templates.TemplateFrame
import org.example.project.ui.templates.TemplateLayout
import org.jetbrains.compose.resources.painterResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

/** A row's height on a phone; every cell is this tall and as wide as its picture's ratio makes it. */
private val HomeFeedRowHeight = 150.dp

/** How many placeholder rows stand in for the rows while they first load. */
private const val HomeFeedPlaceholderRows = 2

/**
 * The template and frame rows under the tools card: per category a title, View All, and a row of
 * thumbnails that scrolls sideways. [scale] grows the rows on a tablet, like the cards above.
 */
@Composable
internal fun HomeFeedSections(
    uiState: HomeFeedUiState,
    scale: Float,
    onViewAll: (HomeSection) -> Unit,
    onFrameClick: (HomeSection, TemplateFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (uiState.isLoading && uiState.sections.isEmpty()) {
            repeat(HomeFeedPlaceholderRows) { HomeFeedPlaceholderRow(scale) }
        }
        uiState.sections.forEach { section ->
            HomeFeedRow(
                section = section,
                scale = scale,
                onViewAll = { onViewAll(section) },
                onFrameClick = { frame -> onFrameClick(section, frame) },
            )
        }
    }
}

@Composable
private fun HomeFeedRow(
    section: HomeSection,
    scale: Float,
    onViewAll: () -> Unit,
    onFrameClick: (TemplateFrame) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        HomeRowHeader(
            title = when (section.kind) {
                HomeSectionKind.Templates -> tr("{0} Templates", section.category.name)
                HomeSectionKind.Frames -> tr("{0} Frames", section.category.name)
            },
            scale = scale,
            onViewAll = onViewAll,
        )
        LazyRow(
            modifier = Modifier.bleedHomeContentPadding().height(HomeFeedRowHeight * scale),
            contentPadding = PaddingValues(horizontal = HomeContentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(section.frames, key = { it.id }) { frame ->
                HomeFeedCell(frame = frame, onClick = { onFrameClick(frame) })
            }
        }
    }
}

/** A Home row's bold title with View All at its end. */
@Composable
private fun HomeRowHeader(title: String, scale: Float, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 17.sp * scale,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        val bounce = rememberSpringBounce()
        Text(
            text = tr("View All"),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp * scale,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .springBounce(bounce)
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = bounce.interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onViewAll,
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun HomeFeedCell(frame: TemplateFrame, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(frame.layout.aspectRatio)
            // Before the clip, so the rounded corners travel with the thumbnail into the editor.
            .navSharedElement(templateFrameKey(frame.id))
            .springBounce(bounce)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        NetworkImage(
            url = frame.thumbnailUrl,
            contentDescription = tr("Template"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (frame.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** A layout cell's side on a phone, and the gap between cells. */
private val HomeLayoutCellSize = 76.dp
private val HomeLayoutSpacing = 12.dp

/**
 * The Layouts row under the tools card: a title, View All, and the collage layouts in two rows
 * that scroll sideways together. Grey squares stand in while they first load; with none (offline,
 * no API key) the row is left out.
 */
@Composable
internal fun HomeLayoutsSection(
    layouts: List<CollageLayout>,
    isLoading: Boolean,
    scale: Float,
    onViewAll: () -> Unit,
    onLayoutClick: (CollageLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layouts.isEmpty() && !isLoading) return
    val cell = HomeLayoutCellSize * scale
    Column(modifier = modifier.fillMaxWidth().padding(top = 20.dp)) {
        HomeRowHeader(title = tr("Layouts"), scale = scale, onViewAll = onViewAll)
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = Modifier.bleedHomeContentPadding().height(cell * 2 + HomeLayoutSpacing),
            contentPadding = PaddingValues(horizontal = HomeContentPadding),
            horizontalArrangement = Arrangement.spacedBy(HomeLayoutSpacing),
            verticalArrangement = Arrangement.spacedBy(HomeLayoutSpacing),
            userScrollEnabled = layouts.isNotEmpty(),
        ) {
            if (layouts.isEmpty()) {
                items(HomeLayoutPlaceholderCount) {
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            } else {
                gridItems(layouts, key = { it.id }) { layout ->
                    CollageLayoutCell(
                        layout = layout,
                        onClick = { onLayoutClick(layout) },
                        modifier = Modifier.size(cell),
                    )
                }
            }
        }
    }
}

/** Enough grey squares to fill both rows on any phone while the layouts load. */
private const val HomeLayoutPlaceholderCount = 16

/** A grey title bar and cells in a row's place while the rows load. */
@Composable
private fun HomeFeedPlaceholderRow(scale: Float) {
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Box(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .width(140.dp * scale)
                .height(18.dp * scale)
                .clip(RoundedCornerShape(6.dp))
                .background(placeholder),
        )
        Row(
            modifier = Modifier.bleedHomeContentPadding().height(HomeFeedRowHeight * scale).padding(start = HomeContentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(TemplateLayout.Portrait.aspectRatio)
                        .clip(RoundedCornerShape(10.dp))
                        .background(placeholder),
                )
            }
        }
    }
}

/**
 * Widens this element past Home's side padding to the screen edges, so a row scrolls from edge to
 * edge while its first cell still lines up with the content (through its own content padding).
 */
private fun Modifier.bleedHomeContentPadding(padding: Dp = HomeContentPadding): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

@Preview
@Composable
private fun HomeFeedSectionsPreview() {
    val frames = List(5) { index ->
        TemplateFrame(
            id = "$index",
            thumbnailUrl = "",
            imageUrl = "",
            layout = if (index == 2) TemplateLayout.Landscape else TemplateLayout.Portrait,
            isPremium = index == 1,
        )
    }
    ThemePreviews {
        HomeFeedSections(
            uiState = HomeFeedUiState(
                sections = listOf(
                    HomeSection(TemplateCategory("1", "Birthday"), HomeSectionKind.Templates, frames),
                    HomeSection(TemplateCategory("2", "Father's Day"), HomeSectionKind.Frames, frames),
                ),
            ),
            scale = 1f,
            onViewAll = {},
            onFrameClick = { _, _ -> },
            modifier = Modifier.padding(horizontal = HomeContentPadding),
        )
    }
}
