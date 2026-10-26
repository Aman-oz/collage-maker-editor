package org.example.project.ui.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.i18n.tr
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.shimmer
import org.example.project.ui.common.springBounce
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.premium.PremiumScreen
import org.example.project.ui.stickers.PremiumBadge
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

/**
 * The Shop: the server's stickers, fonts, backgrounds and frames (Border) as titled packs, plus the
 * paywall as its first tab. Opened by the tools' search buttons on their own tab ([initialTab]).
 * The items only show what a pack holds: a pack is downloaded whole (a premium one is a subscriber's,
 * so for anyone else Download turns to the Premium tab), which keeps it on the disk for its tool,
 * and then "Use" goes back to the tool, which opens on that pack ([ShopPicks]).
 */
@Composable
fun ShopScreen(
    initialTab: ShopTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel { parametersOf(initialTab) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShopContent(
        uiState = uiState,
        onClose = onBack,
        onTabSelected = viewModel::selectTab,
        onQueryChange = viewModel::search,
        onPillSelected = viewModel::selectPill,
        onDownload = viewModel::download,
        onUse = { pack ->
            viewModel.use(pack)
            onBack()
        },
        onRetry = viewModel::retry,
        premiumTab = {
            PremiumScreen(
                fromSplash = false,
                // Unlocked: back to the library the Shop was opened for, now all of it downloadable.
                onClose = { viewModel.selectTab(initialTab.takeUnless { it == ShopTab.Premium } ?: ShopTab.Stickers) },
                embedded = true,
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ShopContent(
    uiState: ShopUiState,
    onClose: () -> Unit,
    onTabSelected: (ShopTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onPillSelected: (String) -> Unit,
    onDownload: (ShopPack) -> Unit,
    onUse: (ShopPack) -> Unit,
    onRetry: () -> Unit,
    premiumTab: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ShopTopBar(onClose = onClose)
        ShopTabRow(selected = uiState.tab, onSelected = onTabSelected)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val tabState = uiState.current
            if (tabState == null) {
                premiumTab()
            } else {
                LibraryTab(
                    tab = uiState.tab,
                    state = tabState,
                    downloading = uiState.downloading,
                    downloaded = uiState.downloaded,
                    onQueryChange = onQueryChange,
                    onPillSelected = onPillSelected,
                    onDownload = onDownload,
                    onUse = onUse,
                    onRetry = onRetry,
                )
            }
        }
    }
}

/** ✕ in a grey circle, and the title in the middle. */
@Composable
private fun ShopTopBar(onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)) {
        val bounce = rememberSpringBounce()
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .springBounce(bounce)
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = tr("Close"), tint = scheme.onSurface, modifier = Modifier.size(24.dp))
        }
        Text(
            text = tr("Shop"),
            color = scheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** The tabs, Premium (with its crown) first; the picked one sits on a tinted pill, scrolled into view. */
@Composable
private fun ShopTabRow(selected: ShopTab, onSelected: (ShopTab) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    LaunchedEffect(selected) { listState.animateScrollToItem((selected.ordinal - 1).coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(ShopTab.entries) { tab ->
            val isSelected = tab == selected
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onSelected(tab) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (tab == ShopTab.Premium) {
                    Image(
                        painter = painterResource(Res.drawable.ic_premium_icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = tab.label,
                    color = if (isSelected) scheme.primary else scheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A library tab: the search field, the pills (New, then the categories) and the packs. */
@Composable
private fun LibraryTab(
    tab: ShopTab,
    state: ShopTabState,
    downloading: Map<String, Float>,
    downloaded: Set<String>,
    onQueryChange: (String) -> Unit,
    onPillSelected: (String) -> Unit,
    onDownload: (ShopPack) -> Unit,
    onUse: (ShopPack) -> Unit,
    onRetry: () -> Unit,
) {
    if (tab == ShopTab.Fonts) {
        FontsLibraryTab(state, downloading, downloaded, onQueryChange, onPillSelected, onDownload, onUse, onRetry)
        return
    }
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            placeholder = tr("Search"),
            modifier = Modifier.padding(horizontal = SidePadding),
        )
        val categories = (state.categories as? ShopLoad.Ready)?.value
        // A search runs over every category, so the pills step aside while there is one.
        if (categories != null && state.query.isBlank()) {
            PillRow(categories = categories, selectedId = state.selectedPillId, onSelected = onPillSelected)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Four cells and a sliver of the fifth across the screen, so a longer pack shows it scrolls.
            val cellWidth = (maxWidth - SidePadding * 2 - CellSpacing * 3) / 4.15f
            when (val shown = state.shown) {
                ShopLoad.Loading -> PacksPlaceholder(cellWidth)
                ShopLoad.Failed -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = tab.failedMessage(), color = scheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRetry) { Text(tr("Retry")) }
                }
                is ShopLoad.Ready -> if (shown.value.isEmpty()) {
                    Text(
                        text = if (state.query.isNotBlank()) tr("No results") else tab.emptyMessage(),
                        color = scheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp)) {
                        items(shown.value, key = { it.id }) { pack ->
                            PackSection(
                                tab = tab,
                                pack = pack,
                                cellWidth = cellWidth,
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

internal val SidePadding = 16.dp
private val CellSpacing = 10.dp
private val CellShape = RoundedCornerShape(12.dp)

/** Pink, as the design draws "Use": apart from the brand colour of the download buttons. */
internal val UseColor = Color(0xFFE8457C)

internal fun ShopTab.failedMessage(): String = when (this) {
    ShopTab.Fonts -> tr("Couldn't load fonts")
    ShopTab.Backgrounds -> tr("Couldn't load backgrounds")
    ShopTab.Borders -> tr("Couldn't load frames")
    else -> tr("Couldn't load stickers")
}

internal fun ShopTab.emptyMessage(): String = when (this) {
    ShopTab.Stickers -> tr("No stickers here yet")
    ShopTab.Fonts -> tr("No fonts here yet")
    else -> tr("No results")
}

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(scheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(text = placeholder, color = scheme.onSurface.copy(alpha = 0.45f), fontSize = 15.sp, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = tr("Close"),
                tint = scheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQueryChange("") },
            )
        }
    }
}

/** New (with a flame), then the categories; server names pass through untranslated. */
@Composable
private fun PillRow(categories: List<ShopCategory>, selectedId: String?, onSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SidePadding, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = NewPillId) {
            Pill(label = "🔥 " + tr("New"), selected = selectedId == NewPillId, onClick = { onSelected(NewPillId) })
        }
        items(categories, key = { it.id }) { category ->
            Pill(label = category.name, selected = category.id == selectedId, onClick = { onSelected(category.id) })
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.08f))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) scheme.primary else scheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** A pack: its name (crowned if premium) and Download / Use, over a row of its items. */
@Composable
private fun PackSection(
    tab: ShopTab,
    pack: ShopPack,
    cellWidth: Dp,
    progress: Float?,
    downloaded: Boolean,
    onDownload: () -> Unit,
    onUse: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding).height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pack.name,
                color = scheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (pack.isPremium) {
                Spacer(modifier = Modifier.width(6.dp))
                PremiumBadge(size = 20.dp)
            }
            Spacer(modifier = Modifier.weight(1f))
            when {
                progress != null -> CircularProgressIndicator(
                    progress = { progress },
                    color = scheme.primary,
                    trackColor = scheme.primary.copy(alpha = 0.16f),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp),
                )
                downloaded -> UseButton(onClick = onUse)
                else -> DownloadButton(onClick = onDownload)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = SidePadding),
            horizontalArrangement = Arrangement.spacedBy(CellSpacing),
        ) {
            itemsIndexed(pack.items, key = { _, item -> item.id }) { _, item ->
                ItemCell(tab = tab, item = item, width = cellWidth)
            }
        }
    }
}

@Composable
private fun DownloadButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .size(30.dp)
            .clip(CircleShape)
            .background(scheme.primary.copy(alpha = 0.12f))
            .border(1.dp, scheme.primary, CircleShape)
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.FileDownload, contentDescription = tr("Download"), tint = scheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun UseButton(onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Box(
        modifier = Modifier
            .springBounce(bounce)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, UseColor, RoundedCornerShape(50))
            .clickable(interactionSource = bounce.interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = tr("Use"), color = UseColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * One item of a pack: a sticker fitted in a grey tile, a background filling it, or a frame fitted on
 * white (frames are transparent in the middle). The Fonts tab has its own cards ([FontsLibraryTab]).
 */
@Composable
private fun ItemCell(tab: ShopTab, item: ShopItem, width: Dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(width)
            .height(width)
            .clip(CellShape)
            .background(
                when (tab) {
                    ShopTab.Borders -> if (scheme.background.luminance() > 0.5f) Color.White else scheme.onSurface.copy(alpha = 0.08f)
                    else -> scheme.onSurface.copy(alpha = 0.08f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = item.thumbnailUrl,
            contentDescription = item.name,
            contentScale = if (tab == ShopTab.Backgrounds) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (tab == ShopTab.Backgrounds) 0.dp else 6.dp),
        )
        if (item.isPremium) PremiumBadge(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp), size = 18.dp)
    }
}

/** Shimmering sections where the packs will be. */
@Composable
private fun PacksPlaceholder(cellWidth: Dp) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
        repeat(4) {
            Box(modifier = Modifier.padding(horizontal = SidePadding).width(120.dp).height(20.dp).shimmer())
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.padding(horizontal = SidePadding), horizontalArrangement = Arrangement.spacedBy(CellSpacing)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellWidth)
                            .shimmer(CellShape),
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Preview
@Composable
private fun ShopScreenPreview() {
    val items = List(5) { ShopItem("$it", "Sticker $it", "", "", isPremium = it == 0) }
    val packs = listOf(
        ShopPack("a", "1", "Animal Faces", items),
        ShopPack("b", "1", "Axolotl", items.map { it.copy(id = "b${it.id}", isPremium = false) }),
        ShopPack("c", "1", "Bears and Pandas", items.map { it.copy(id = "c${it.id}", isPremium = false) }),
    )
    ThemePreviews {
        ShopContent(
            uiState = ShopUiState(
                tab = ShopTab.Stickers,
                tabs = mapOf(
                    ShopTab.Stickers to ShopTabState(
                        categories = ShopLoad.Ready(listOf(ShopCategory("1", "Animal Duos"), ShopCategory("2", "Fantasy"), ShopCategory("3", "Food"))),
                        packs = mapOf("1" to ShopLoad.Ready(packs)),
                    ),
                ),
                downloaded = setOf("b"),
            ),
            onClose = {},
            onTabSelected = {},
            onQueryChange = {},
            onPillSelected = {},
            onDownload = {},
            onUse = {},
            onRetry = {},
            premiumTab = {},
        )
    }
}
