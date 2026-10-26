package org.example.project.ui.shop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.data.fonts.FontLoader
import org.example.project.i18n.tr
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.shimmer
import org.example.project.ui.common.springBounce
import org.example.project.ui.stickers.PremiumBadge
import org.koin.compose.koinInject

/** Which packs the Fonts tab's filter button lets through. */
private enum class FontPackFilter(val englishLabel: String) {
    All("All"),
    Free("Free"),
    Premium("Premium"),
    ;

    fun accepts(pack: ShopPack): Boolean = when (this) {
        All -> true
        Free -> !pack.isPremium
        Premium -> pack.isPremium
    }
}

/**
 * The Fonts tab, laid out apart from the other libraries: the main categories as text tabs, then a
 * strip of the category's packs (its sub-categories) that jumps to each, over one big specimen card
 * per pack, drawn in its first font. A pack is still downloaded, then used, whole.
 */
@Composable
internal fun FontsLibraryTab(
    state: ShopTabState,
    downloading: Map<String, Float>,
    downloaded: Set<String>,
    onQueryChange: (String) -> Unit,
    onPillSelected: (String) -> Unit,
    onDownload: (ShopPack) -> Unit,
    onUse: (ShopPack) -> Unit,
    onRetry: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // An ordinal, so it saves on every platform without a Saver.
    var filterOrdinal by rememberSaveable { mutableStateOf(0) }
    val filter = FontPackFilter.entries[filterOrdinal]
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding), verticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = tr("Search fonts, styles…"),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(10.dp))
            FilterButton(filter = filter, onFilter = { filterOrdinal = it.ordinal })
        }
        val categories = (state.categories as? ShopLoad.Ready)?.value
        val searching = state.query.isNotBlank()
        if (categories != null && !searching) {
            CategoryTabs(categories = categories, selectedId = state.selectedPillId, onSelected = onPillSelected)
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
        // A fresh list (and strip) per category, so a new category starts at its top.
        key(state.selectedPillId, searching) {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            when (val shown = state.shown) {
                ShopLoad.Loading -> FontCardsPlaceholder()
                ShopLoad.Failed -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = ShopTab.Fonts.failedMessage(), color = scheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry) { Text(tr("Retry")) }
                }
                is ShopLoad.Ready -> {
                    val packs = shown.value.filter(filter::accepts)
                    if (packs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searching || filter != FontPackFilter.All) tr("No results") else ShopTab.Fonts.emptyMessage(),
                                color = scheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        if (!searching) {
                            // The chip of the card at the top of the list.
                            val current by remember { derivedStateOf { listState.firstVisibleItemIndex } }
                            PackStrip(
                                packs = packs,
                                selectedIndex = current.coerceAtMost(packs.lastIndex),
                                onSelected = { index -> scope.launch { listState.animateScrollToItem(index) } },
                            )
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            itemsIndexed(packs, key = { _, pack -> pack.id }) { index, pack ->
                                FontPackCard(
                                    pack = pack,
                                    palette = CardPalettes[index % CardPalettes.size],
                                    progress = downloading[pack.id],
                                    downloaded = pack.id in downloaded,
                                    onDownload = { onDownload(pack) },
                                    onUse = { onUse(pack) },
                                )
                            }
                            item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars).height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

/** The round sliders button beside the search: All / Free / Premium packs. Tinted while it filters. */
@Composable
private fun FilterButton(filter: FontPackFilter, onFilter: (FontPackFilter) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val bounce = rememberSpringBounce()
    Box {
        Box(
            modifier = Modifier
                .springBounce(bounce)
                .size(48.dp)
                .clip(CircleShape)
                .background(if (filter != FontPackFilter.All) scheme.primary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f))
                .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current) { open = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = tr("Filter"),
                tint = if (filter != FontPackFilter.All) scheme.primary else scheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            FontPackFilter.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(tr(option.englishLabel)) },
                    onClick = {
                        onFilter(option)
                        open = false
                    },
                    trailingIcon = if (option == filter) {
                        { Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * The main categories as bold words; the picked one sits in a tinted bubble whose little tail points
 * down at the strip of its packs. Server names pass through untranslated.
 */
@Composable
private fun CategoryTabs(categories: List<ShopCategory>, selectedId: String?, onSelected: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bubble = scheme.primary.copy(alpha = 0.16f)
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index > 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = SidePadding, end = SidePadding, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(categories, key = { _, category -> category.id }) { _, category ->
            val selected = category.id == selectedId
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) bubble else Color.Transparent)
                        .clickable { onSelected(category.id) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = category.name,
                        color = if (selected) scheme.primary else scheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                Canvas(modifier = Modifier.size(width = 14.dp, height = 6.dp)) {
                    if (selected) {
                        val tail = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2, size.height)
                            close()
                        }
                        drawPath(tail, bubble)
                    }
                }
            }
        }
    }
}

/**
 * The category's packs as chips on a grey track: the one at the top of the list is tinted, a tap
 * scrolls the list to its card, and › at the end pages the chips on while there are more.
 */
@Composable
private fun PackStrip(packs: List<ShopPack>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val chipState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedIndex) {
        val visible = chipState.layoutInfo.visibleItemsInfo
        val fullyVisible = visible.any { it.index == selectedIndex && it.offset >= 0 && it.offset + it.size <= chipState.layoutInfo.viewportEndOffset }
        if (!fullyVisible) chipState.animateScrollToItem(selectedIndex)
    }
    Row(
        modifier = Modifier
            .padding(horizontal = SidePadding, vertical = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.onSurface.copy(alpha = 0.07f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = chipState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            itemsIndexed(packs, key = { _, pack -> pack.id }) { index, pack ->
                val selected = index == selectedIndex
                val bounce = rememberSpringBounce()
                Box(
                    modifier = Modifier
                        .springBounce(bounce)
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.surface)
                        .border(1.dp, if (selected) Color.Transparent else scheme.onSurface.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current) { onSelected(index) }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pack.name,
                        color = if (selected) scheme.primary else scheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
        if (chipState.canScrollForward) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(scheme.surface)
                    .clickable {
                        scope.launch {
                            val last = chipState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@launch
                            chipState.animateScrollToItem(last)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = tr("More"), tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** A card's gradient (top-left to bottom-right) and the colour of the soft shapes over it. */
private class CardPalette(val start: Color, val end: Color, val blob: Color)

private val CardPalettes = listOf(
    CardPalette(Color(0xFF3E9C9E), Color(0xFF79C6D3), Color(0xFF2F8A7F)),
    CardPalette(Color(0xFF15215E), Color(0xFF3A5BD9), Color(0xFF2B47BE)),
    CardPalette(Color(0xFFC8366F), Color(0xFFEC7AAA), Color(0xFFE0507A)),
    CardPalette(Color(0xFF4E2A97), Color(0xFF9B6DE2), Color(0xFF7246C4)),
    CardPalette(Color(0xFFD65A2A), Color(0xFFF4A75B), Color(0xFFE37437)),
)

private val CardShape = RoundedCornerShape(20.dp)

/** The lines every card sets in its pack's font: a specimen, so it isn't translated. */
private const val SpecimenHeadline = "FRIENDS"
private const val SpecimenLine = "Buy you a lunch."
private const val SpecimenCaption = "BEST FRIENDS"
private const val SpecimenClosing = "eat your lunch."

/**
 * One pack: the specimen in its first font, white on a gradient with soft shapes, its name (in that
 * font, crowned if premium) at the top right and Download / Use at the bottom right.
 */
@Composable
private fun FontPackCard(
    pack: ShopPack,
    palette: CardPalette,
    progress: Float?,
    downloaded: Boolean,
    onDownload: () -> Unit,
    onUse: () -> Unit,
) {
    val font = pack.items.firstNotNullOfOrNull { it.font }
    val loader: FontLoader = koinInject()
    // Only the letters the card draws are fetched (see FontLoader.preview), not the whole font.
    val family by produceState<FontFamily?>(null, font) {
        value = font?.let { loader.preview(it, SpecimenHeadline + SpecimenLine + SpecimenCaption + SpecimenClosing + pack.name) }
    }
    Box(
        modifier = Modifier
            .padding(horizontal = SidePadding, vertical = 6.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .aspectRatio(2.1f)
            .clip(CardShape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(palette.start, palette.end), start = Offset.Zero, end = Offset(size.width, size.height)))
                val h = size.height
                drawCircle(palette.blob.copy(alpha = 0.7f), radius = h * 0.62f, center = Offset(size.width * 0.93f, h * 1.02f))
                drawCircle(Color.White.copy(alpha = 0.12f), radius = h * 0.36f, center = Offset(size.width * 0.9f, h * 0.08f))
                drawCircle(palette.blob.copy(alpha = 0.55f), radius = h * 0.26f, center = Offset(-h * 0.1f, h * 0.78f))
            },
    ) {
        val loaded = family
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp, end = 72.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (loaded == null) {
                listOf(0.35f, 0.75f, 0.3f, 0.65f).forEach { fraction ->
                    Box(modifier = Modifier.fillMaxWidth(fraction).height(16.dp).padding(vertical = 2.dp).shimmer())
                }
            } else {
                SpecimenText(SpecimenHeadline, loaded, 24.sp, FontWeight.Bold)
                SpecimenText(SpecimenLine, loaded, 26.sp, FontWeight.Bold)
                SpecimenText(SpecimenCaption, loaded, 13.sp, FontWeight.Normal)
                SpecimenText(SpecimenClosing, loaded, 26.sp, FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pack.isPremium) {
                PremiumBadge(size = 26.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = pack.name,
                    color = Color.White,
                    fontFamily = loaded,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp)) {
            when {
                progress != null -> CircularProgressIndicator(
                    progress = { progress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp),
                )
                downloaded -> CardUseButton(onClick = onUse)
                else -> CardDownloadButton(onClick = onDownload)
            }
        }
    }
}

@Composable
private fun SpecimenText(text: String, family: FontFamily, size: TextUnit, weight: FontWeight) {
    Text(
        text = text,
        color = Color.White,
        fontFamily = family,
        fontSize = size,
        lineHeight = size * 1.15f,
        fontWeight = weight,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

/** A frosted circle ringed in lavender, so it reads on every gradient. */
@Composable
private fun CardDownloadButton(onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(CardButtonSize)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.5.dp, Color(0xFFA99BFF), CircleShape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.FileDownload, contentDescription = tr("Download"), tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CardUseButton(onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = tr("Use"), color = UseColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val CardButtonSize: Dp = 36.dp

/** Shimmering cards where the packs will be. */
@Composable
private fun FontCardsPlaceholder() {
    Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .padding(horizontal = SidePadding, vertical = 6.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .aspectRatio(2.1f)
                    .shimmer(CardShape),
            )
        }
    }
}
