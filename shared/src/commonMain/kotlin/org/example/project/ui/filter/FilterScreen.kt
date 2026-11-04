package org.example.project.ui.filter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.i18n.tr
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.PreventScreenCapture
import org.example.project.ui.common.ProFeatureBar
import org.example.project.ui.common.shimmer
import org.example.project.ui.common.wholeNumberLabel
import org.example.project.ui.common.ToolScaffold
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.shop.LocalOpenShop
import org.example.project.ui.shop.ShopPicks
import org.example.project.ui.shop.ShopTab
import org.example.project.ui.stickers.LibrarySearchButton
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_before_after
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val ChipShape = RoundedCornerShape(6.dp)

/**
 * The Filter tool's way to the Shop: [onOpen] opens its Filter tab, and [pickedCategory] is the tab
 * "Use" picked there, which the tool opens on and then reports [onPickTaken]. Empty in previews.
 */
internal class FilterShopLink(
    val onOpen: () -> Unit = {},
    val pickedCategory: FilterCategory? = null,
    val onPickTaken: () -> Unit = {},
)

@Composable
private fun rememberFilterShopLink(): FilterShopLink {
    val openShop = LocalOpenShop.current
    val picks: ShopPicks = koinInject()
    val pick by picks.pick.collectAsState()
    val picked = pick?.takeIf { it.tab == ShopTab.Filters }?.let { p -> FilterCategory.entries.firstOrNull { it.name == p.categoryId } }
    return FilterShopLink(onOpen = { openShop(ShopTab.Filters) }, pickedCategory = picked, onPickTaken = { picks.take(ShopTab.Filters) })
}

@Composable
internal fun FilterTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    onClose: () -> Unit,
    onApply: (ImageBitmap) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // Done renders the photo at its own size, which takes a moment; Done waits rather than twice.
    var baking by remember { mutableStateOf(false) }
    val shop = rememberFilterShopLink()
    FilterContent(
        shop = shop,
        sourceImage = sourceImage,
        isSubscribed = isPremium,
        onOpenPremium = onOpenPremium,
        onBack = onClose,
        busy = baking,
        onApply = { filter, intensity ->
            when {
                // A premium filter previews for everyone, but Done sends a non-subscriber to the
                // paywall. The tool stays open underneath, so Done applies it once they come back
                // subscribed.
                filter.isPremium && !isPremium -> onOpenPremium()
                // "None", or a filter faded all the way out, leaves the photo as it was.
                filter.isNoneOption || intensity <= 0f -> onClose()
                !baking -> {
                    baking = true
                    scope.launch {
                        val baked = bakeFilter(sourceImage, filter, intensity)
                        baking = false
                        onApply(baked)
                    }
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * The Filter tool for an editor that keeps the filter live rather than baked (the layout editor,
 * which puts one filter over all its photos): it previews on [sourceImage], opens on
 * [initialFilterId] (a [PhotoFilter.id]) at [initialIntensity], and hands the pick to [onApply]. A
 * premium filter sends a non-subscriber to the paywall instead, as in [FilterTool].
 */
@Composable
internal fun FilterPickerTool(
    sourceImage: ImageBitmap,
    isPremium: Boolean,
    initialFilterId: String?,
    initialIntensity: Float,
    onClose: () -> Unit,
    onApply: (filter: PhotoFilter, intensity: Float) -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shop = rememberFilterShopLink()
    FilterContent(
        shop = shop,
        sourceImage = sourceImage,
        isSubscribed = isPremium,
        onOpenPremium = onOpenPremium,
        onBack = onClose,
        onApply = { filter, intensity ->
            if (filter.isPremium && !isPremium) onOpenPremium() else onApply(filter, intensity)
        },
        initialFilterId = initialFilterId,
        initialIntensity = initialIntensity,
        modifier = modifier,
    )
}

/**
 * The filters run on the CPU (see [runFilterSteps]), so nothing here filters on the fly: the stage
 * shows the photo cut to [FilterPreviewLongSide] with the picked filter's render over it at the
 * slider's strength, and each chip renders its own thumbnail once, all off the main thread.
 */
@Composable
private fun FilterContent(
    sourceImage: ImageBitmap?,
    isSubscribed: Boolean,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit,
    onApply: (filter: PhotoFilter, intensity: Float) -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    initialFilterId: String? = null,
    initialIntensity: Float = 1f,
    shop: FilterShopLink = FilterShopLink(),
) {
    val scheme = MaterialTheme.colorScheme
    // Saveable, unlike the other tools' edits: the paywall covers the editor, which drops plain
    // `remember` state, and the filter picked before subscribing has to still be there after.
    var selectedId by rememberSaveable { mutableStateOf(initialFilterId ?: PhotoFilter.None.id) }
    val selectedFilter = photoFilterById(selectedId) ?: PhotoFilter.None
    // Saved by name: an enum is not saveable on every platform, a String is.
    var categoryName by rememberSaveable {
        mutableStateOf(selectedFilter.category.takeUnless { selectedFilter.isNoneOption }?.name ?: FilterCategory.All.name)
    }
    val category = FilterCategory.entries.firstOrNull { it.name == categoryName } ?: FilterCategory.All
    var intensity by rememberSaveable { mutableFloatStateOf(initialIntensity) }
    var comparing by remember { mutableStateOf(false) }
    LaunchedEffect(shop.pickedCategory) {
        val picked = shop.pickedCategory ?: return@LaunchedEffect
        shop.onPickTaken()
        categoryName = picked.name
    }

    val previewSource = rememberScaledImage(sourceImage, FilterPreviewLongSide)
    val rendered = rememberRendered(previewSource, selectedFilter.id) { renderFilter(it, selectedFilter) }
    val thumbnailSource = rememberScaledImage(sourceImage, FilterThumbnailLongSide)
    val thumbnails = rememberFilterThumbnails(thumbnailSource)
    val onSelected = { filter: PhotoFilter -> selectedId = filter.id }

    ToolScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .toolStage()
                .background(scheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (sourceImage != null) {
                    // Live, temporary preview — the actual bitmap is only baked when Done is tapped.
                    // aspectRatio sizes the stage to the photo itself, so the rounded clip follows the
                    // photo's edges instead of the letterboxed stage.
                    Box(
                        modifier = Modifier
                            .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = previewSource ?: sourceImage,
                            contentDescription = tr("Photo preview"),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        if (rendered != null && !selectedFilter.isNoneOption && !comparing) {
                            Image(
                                bitmap = rendered.image,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                alpha = intensity,
                            )
                        }
                        val rendering = !selectedFilter.isNoneOption && rendered?.key != selectedFilter.id
                        if (rendering || busy) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                } else {
                    Text(
                        text = tr("No image to filter"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            // Under the photo while a non-subscriber previews a premium filter; it is part of the stage
            // so it fades in and out with it, and leads to the paywall like Done does.
            ProFeatureBar(visible = selectedFilter.isPremium && !isSubscribed)
            PreventScreenCapture(enabled = selectedFilter.isPremium && !isSubscribed)
        }

        ToolPanel(
            title = tr("Filters"),
            onClose = onBack,
            onDone = { onApply(selectedFilter, intensity) },
            doneEnabled = sourceImage != null && !busy,
        ) {
            // The tabs scroll; the compare button stays pinned at the row's end.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolCategoryTabs(
                    categories = FilterCategory.entries,
                    selected = category,
                    label = { it.label },
                    onSelected = { categoryName = it.name },
                    modifier = Modifier.weight(1f),
                )
                CompareIconButton(onComparingChange = { comparing = it })
            }
            val filters = remember(category) { filtersOf(category) }
            // Beside the photo the filters fill the column as a grid, the slider under them.
            if (sideBySide) {
                FilterGrid(
                    filters = filters,
                    thumbnails = thumbnails,
                    thumbnailSource = thumbnailSource,
                    selected = selectedFilter,
                    onSelected = onSelected,
                    onSearch = shop.onOpen,
                    modifier = Modifier.weight(1f),
                )
            }

            CenterFillSlider(
                value = intensity,
                onValueChange = { intensity = it },
                range = 0f..1f,
                trackColor = scheme.onSurface.copy(alpha = 0.12f),
                fillColor = scheme.primary,
                thumbColor = scheme.primary,
                thumbWidth = 32.dp,
                thumbHeight = 18.dp,
                horizontalPadding = 12.dp,
                glassThumb = true,
                glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                valueLabel = { wholeNumberLabel(it * 100f) },
                modifier = if (sideBySide) Modifier.padding(bottom = 16.dp) else Modifier,
            )

            if (!sideBySide) {
                FilterStrip(
                    category = category,
                    filters = filters,
                    thumbnails = thumbnails,
                    thumbnailSource = thumbnailSource,
                    selected = selectedFilter,
                    onSelected = onSelected,
                    onSearch = shop.onOpen,
                )
            }
        }
    }
}

/** Press-and-hold icon that shows the unfiltered photo while held. */
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
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FilterStrip(
    category: FilterCategory,
    filters: List<PhotoFilter>,
    thumbnails: FilterThumbnails,
    thumbnailSource: ImageBitmap?,
    selected: PhotoFilter,
    onSelected: (PhotoFilter) -> Unit,
    onSearch: () -> Unit,
) {
    val listState = rememberLazyListState()
    // A tab picked (or opened from the Shop) starts at its first filter.
    LaunchedEffect(category) { listState.scrollToItem(0) }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Opens the Shop's Filter tab, as the Stickers tool's search button opens its own.
        item(key = "search") { LibrarySearchButton(onClick = onSearch, size = 44.dp) }
        items(filters, key = { it.id }) { filter ->
            FilterChip(
                filter = filter,
                thumbnail = thumbnails.thumbnail(filter.id, thumbnailSource) { renderFilter(it, filter, FilterThumbnailLongSide) },
                selected = filter.id == selected.id,
                onClick = { onSelected(filter) },
            )
        }
    }
}

/** [FilterStrip]'s chips as a grid that scrolls vertically, for the side column of a phone on its side. */
@Composable
private fun FilterGrid(
    filters: List<PhotoFilter>,
    thumbnails: FilterThumbnails,
    thumbnailSource: ImageBitmap?,
    selected: PhotoFilter,
    onSelected: (PhotoFilter) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(66.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "search") {
            Box(contentAlignment = Alignment.Center) { LibrarySearchButton(onClick = onSearch, size = 44.dp) }
        }
        gridItems(filters, key = { it.id }) { filter ->
            Box(contentAlignment = Alignment.Center) {
                FilterChip(
                    filter = filter,
                    thumbnail = thumbnails.thumbnail(filter.id, thumbnailSource) { renderFilter(it, filter, FilterThumbnailLongSide) },
                    selected = filter.id == selected.id,
                    onClick = { onSelected(filter) },
                )
            }
        }
    }
}

/**
 * A thumbnail with its label underneath. The selected chip gets an accent outline around both,
 * so the thumbnail and label read as one tile. The thumbnail shimmers until it has rendered.
 */
@Composable
private fun FilterChip(
    filter: PhotoFilter,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .width(60.dp)
            .clip(ChipShape)
            .then(if (selected) Modifier.border(1.5.dp, scheme.primary, ChipShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (filter.isNoneOption || thumbnail != null) {
                        Modifier.background(scheme.primary.copy(alpha = 0.12f))
                    } else {
                        Modifier.shimmer(RoundedCornerShape(4.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (filter.isNoneOption) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = filter.label,
                    tint = scheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp),
                )
            } else if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = filter.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (filter.isPremium) {
                // Inside the thumbnail rather than hanging off its corner: the chip clips its content.
                Image(
                    painter = painterResource(Res.drawable.ic_premium_icon),
                    contentDescription = tr("Premium"),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filter.label,
            color = scheme.onSurface,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun FilterScreenPreview() {
    ThemePreviews {
        FilterContent(sourceImage = ImageBitmap(360, 480), isSubscribed = false, onOpenPremium = {}, onBack = {}, onApply = { _, _ -> })
    }
}
