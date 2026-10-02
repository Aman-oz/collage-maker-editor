package org.example.project.ui.templates

import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.positionInParent
import org.example.project.i18n.tr
import org.example.project.ui.theme.Brand
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import org.example.project.ui.common.TopBarPremiumIconSize
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.TopBarHeight
import org.example.project.ui.common.TopBarHorizontalPadding
import org.example.project.ui.common.navSharedElement
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.templateFrameKey
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

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
    title: String = tr("Templates"),
    emptyMessage: String = tr("No templates in this category"),
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
    // Centered on the screen like the other top bars; the side padding reserves room for the 48dp
    // premium button so a long title ellipsizes instead of running under it.
    Box(
        // A smaller end padding because the 48dp premium IconButton already insets its icon.
        modifier = Modifier.fillMaxWidth().height(TopBarHeight).padding(start = TopBarHorizontalPadding, end = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = tr("Back"),
            onClick = onBack,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 56.dp),
        )
        val proBounce = rememberSpringBounce()
        IconButton(
            onClick = onGoPro,
            interactionSource = proBounce.interactionSource,
            modifier = Modifier.align(Alignment.CenterEnd).springBounce(proBounce),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = tr("Premium"),
                modifier = Modifier.size(TopBarPremiumIconSize),
            )
//            Icon(imageVector = Icons.Filled.WorkspacePremium, contentDescription = "Premium", tint = PremiumGold)
        }
    }
}

/** Height of the selected category's underline. */
private val CategoryIndicatorHeight = 3.dp

/** Space before the first and after the last category tab. */
private val CategoryRowPadding = 8.dp

/** A tab's side padding around its label; the underline spans the label only. */
private val CategoryTabPadding = 14.dp

/**
 * The indicator's spring: slightly underdamped, so it overshoots a touch and settles as it slides
 * (and stretches) from one category to the next.
 */
private val CategoryIndicatorSpring = spring<Float>(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)

/**
 * Text tabs with a rounded underline that slides and resizes to the selected category. The tabs
 * scroll (catalogs can have many categories); the underline lives in the scrolled content, so it
 * moves with them, and selecting a tab scrolls it toward the centre.
 */
@Composable
private fun CategoryRow(categories: List<TemplateCategory>, selectedId: String?, onSelected: (TemplateCategory) -> Unit) {
    val accent = categoryAccent()
    val scrollState = rememberScrollState()
    var viewportWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // Each tab's label bounds in the scrolled content (left, width; px).
    val textBounds = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val selectedBounds = selectedId?.let(textBounds::get)

    val indicatorLeft = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    var indicatorShown by remember { mutableStateOf(false) }
    LaunchedEffect(selectedBounds) {
        val (left, width) = selectedBounds ?: return@LaunchedEffect
        if (!indicatorShown) {
            // The first placement snaps, rather than sliding in from the row's start.
            indicatorLeft.snapTo(left)
            indicatorWidth.snapTo(width)
            indicatorShown = true
        } else {
            launch { indicatorLeft.animateTo(left, CategoryIndicatorSpring) }
            launch { indicatorWidth.animateTo(width, CategoryIndicatorSpring) }
        }
    }
    // Brings the selected tab toward the centre of the row, as far as the scroll allows.
    LaunchedEffect(selectedId, selectedBounds != null, viewportWidth) {
        val (left, width) = selectedBounds ?: return@LaunchedEffect
        if (viewportWidth == 0) return@LaunchedEffect
        val target = (left + width / 2f - viewportWidth / 2f).roundToInt().coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { viewportWidth = it.width }
            .horizontalScroll(scrollState),
    ) {
        Row(modifier = Modifier.padding(horizontal = CategoryRowPadding)) {
            categories.forEach { category ->
                CategoryTab(
                    category = category,
                    selected = category.id == selectedId,
                    accent = accent,
                    onClick = { onSelected(category) },
                    onPlaced = { coordinates ->
                        // The tab's place in the row, plus the row's own padding, is its place in
                        // the scrolled content (which the scroll doesn't change); the label sits
                        // inside the tab's side padding.
                        val inset = with(density) { (CategoryRowPadding + CategoryTabPadding).toPx() }
                        val sidePadding = with(density) { CategoryTabPadding.toPx() }
                        textBounds[category.id] = (coordinates.positionInParent().x + inset) to
                            (coordinates.size.width - sidePadding * 2).coerceAtLeast(0f)
                    },
                )
            }
        }
        if (indicatorShown && selectedBounds != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 4.dp)
                    // Read in the layout/draw phases, so the slide never recomposes the row.
                    .layout { measurable, _ ->
                        val width = indicatorWidth.value.roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(Constraints.fixed(width, CategoryIndicatorHeight.roundToPx()))
                        layout(placeable.width, placeable.height) {
                            placeable.place(indicatorLeft.value.roundToInt(), 0)
                        }
                    }
                    .clip(RoundedCornerShape(percent = 50))
                    .background(accent),
            )
        }
    }
}

@Composable
private fun CategoryTab(
    category: TemplateCategory,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onPlaced: (LayoutCoordinates) -> Unit,
) {
    val color by animateColorAsState(if (selected) accent else MaterialTheme.colorScheme.onBackground)
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            // Before the bounce, so the reported bounds are the tab's resting ones.
            .onPlaced(onPlaced)
            .springBounce(bounce)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(start = CategoryTabPadding, end = CategoryTabPadding, top = 12.dp, bottom = 14.dp),
    ) {
        // One weight for every tab, so selecting one only recolours it and the row never shifts.
        Text(
            text = category.name,
            color = color,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * The selected category's colour: the brand violet, as in the design. On dark backgrounds the
 * scheme's lighter primary instead, which stays readable where the deep violet would not.
 */
@Composable
private fun categoryAccent(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Brand else MaterialTheme.colorScheme.primary

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
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(frame.layout.aspectRatio)
            // Before the clip, so the rounded corners travel with the thumbnail into the editor.
            .navSharedElement(templateFrameKey(frame.id))
            .springBounce(bounce)
            .clip(RoundedCornerShape(14.dp))
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
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {

                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier.size(20.dp)
                )
//                Icon(
//                    imageVector = Icons.Filled.WorkspacePremium,
//                    contentDescription = "Premium",
//                    tint = PremiumGold,
//                    modifier = Modifier.size(15.dp),
//                )
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
        val bounce = rememberSpringBounce()
        Row(
            modifier = Modifier
                .springBounce(bounce)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onRetry)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(6.dp))
            Text(tr("Retry"), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
