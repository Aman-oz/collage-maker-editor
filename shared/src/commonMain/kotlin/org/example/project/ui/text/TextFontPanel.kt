package org.example.project.ui.text

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.fonts.FontCatalog
import org.example.project.data.fonts.FontEntry
import org.example.project.data.fonts.FontFilter
import org.example.project.data.fonts.FontLoader
import org.example.project.data.fonts.applyFontFilter
import org.example.project.i18n.tr
import org.example.project.ui.common.ToolTopBar

/**
 * What the Text tool knows about fonts, handed in from [TextTool] so the tool's content stays free
 * of DI: the catalog (null until parsed), and the user's favourite and recent families, newest first.
 */
@Immutable
internal class TextFonts(
    val catalog: FontCatalog?,
    val favorites: List<String>,
    val recent: List<String>,
    val onToggleFavorite: (family: String) -> Unit,
    val onUsed: (family: String) -> Unit,
) {
    /** The fonts the keyboard bar offers: the recent ones, then the featured. */
    val quickPicks: List<FontEntry>
        get() {
            val catalog = catalog ?: return emptyList()
            return (recent.mapNotNull(catalog::find) + catalog.featured).distinctBy { it.family }.take(QuickPickCount)
        }

    companion object {
        private const val QuickPickCount = 16
        val Empty = TextFonts(null, emptyList(), emptyList(), {}, {})
    }
}

/** What the font grid lists. */
internal sealed interface FontListSource {
    data object Favorites : FontListSource
    data object Recent : FontListSource
    data object Featured : FontListSource
    data class Filtered(val filter: FontFilter) : FontListSource
}

/**
 * What the Font panel is browsing: the list in its grid and the selected main category. Held by the
 * Text tool rather than the panel, because the category menu ([FontCategoryMenu]) is drawn over the
 * whole tool, outside the panel.
 */
@Stable
internal class FontBrowserState {
    var source by mutableStateOf<FontListSource>(FontListSource.Featured)
    var sectionIndex by mutableIntStateOf(0)
        private set

    /** Switches the main category; the grid follows, to its first sub-category. */
    fun selectSection(index: Int, catalog: FontCatalog?) {
        if (index == sectionIndex) return
        sectionIndex = index
        catalog?.sections?.getOrNull(index)?.filters?.firstOrNull()?.let { source = FontListSource.Filtered(it) }
    }
}

/**
 * The Font panel: ✕ / main category / ✓ over a two-column grid of fonts, each named in itself with a
 * heart that favourites it, and under that a bar picking what the grid lists.
 *
 * The title is the selected main category (Feeling, Appearance, …); tapping it reports where it is
 * ([onOpenCategoryMenu], in root coordinates), so the tool can grow [FontCategoryMenu] out of it.
 * The bar is the favourites button, a divider, then Recent, Featured and the main category's sub-categories, which rise in one after
 * another whenever the main category changes. Picking a font applies it to the text at once; ✕ puts
 * back the font the panel opened with.
 */
@Composable
internal fun TextFontPanel(
    fonts: TextFonts,
    browser: FontBrowserState,
    categoryMenuOpen: Boolean,
    onOpenCategoryMenu: (anchorInRoot: Rect) -> Unit,
    selectedFamily: String?,
    onFontSelected: (FontEntry) -> Unit,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val catalog = fonts.catalog
    val source = browser.source
    val sectionIndex = browser.sectionIndex
    val section = catalog?.sections?.getOrNull(sectionIndex)

    val listed: List<FontEntry> = remember(catalog, source, fonts.favorites, fonts.recent) {
        when {
            catalog == null -> emptyList()
            else -> when (val s = source) {
                FontListSource.Favorites -> fonts.favorites.mapNotNull(catalog::find)
                FontListSource.Recent -> fonts.recent.mapNotNull(catalog::find)
                FontListSource.Featured -> catalog.featured
                is FontListSource.Filtered -> applyFontFilter(catalog.fonts, s.filter)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ToolTopBar(
                title = tr("Font"),
                onClose = onClose,
                onDone = onDone,
                titleContent = {
                    CategoryTitle(
                        title = section?.let { tr(it.title) } ?: tr("Font"),
                        open = categoryMenuOpen,
                        enabled = catalog != null,
                        onClick = onOpenCategoryMenu,
                    )
                },
            )
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                when {
                    catalog == null -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    listed.isEmpty() -> Text(
                        text = if (source == FontListSource.Favorites) tr("No favorite fonts yet") else tr("No recent fonts yet"),
                        color = scheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                    )
                    // Keyed, so a fresh list starts at its top rather than wherever the last one was scrolled to.
                    else -> key(source) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = rememberLazyGridState(),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            items(listed, key = { it.family }) { font ->
                                FontGridCell(
                                    font = font,
                                    selected = font.family == selectedFamily,
                                    favorite = font.family in fonts.favorites,
                                    onClick = { onFontSelected(font) },
                                    onToggleFavorite = { fonts.onToggleFavorite(font.family) },
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.1f))
            FontCategoryBar(
                sectionIndex = sectionIndex,
                filters = section?.filters.orEmpty(),
                source = source,
                onSourceSelected = { browser.source = it },
            )
        }

    }
}

/** The selected main category in the title's place, with a ▾ that turns over while its menu is open. */
@Composable
private fun CategoryTitle(title: String, open: Boolean, enabled: Boolean, onClick: (anchorInRoot: Rect) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val arrowTurn by animateFloatAsState(if (open) 180f else 0f, tween(260))
    // Hidden under the menu's selected row while it is open, so the two don't show through each
    // other; back only once the menu has nearly folded into it.
    val alpha by animateFloatAsState(if (open) 0f else 1f, if (open) tween(120) else tween(160, delayMillis = 220))
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Row(
        modifier = Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(50))
            .background(scheme.primary.copy(alpha = 0.1f))
            .clickable(enabled = enabled) { onClick(bounds) }
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = scheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = tr("Categories"),
            tint = scheme.primary,
            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = arrowTurn },
        )
    }
}

@Composable
private fun FontGridCell(
    font: FontEntry,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val family by produceState<FontFamily?>(null, font.fileUrl) { value = FontLoader.load(font.fileUrl) }
    // The name waits, faint, in the default font until its own has downloaded.
    val loaded by animateFloatAsState(if (family != null) 1f else 0.35f, tween(220))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = font.family,
            fontFamily = family ?: FontFamily.Default,
            fontSize = 18.sp,
            color = if (selected) scheme.primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).graphicsLayer { alpha = loaded },
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) tr("Remove from favorites") else tr("Add to favorites"),
                tint = if (favorite) Color(0xFFE53935) else scheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FontCategoryBar(
    sectionIndex: Int,
    filters: List<FontFilter>,
    source: FontListSource,
    onSourceSelected: (FontListSource) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // Restarted for each main category, so its sub-categories rise in one after another.
    val cascade = remember(sectionIndex) { Animatable(0f) }
    LaunchedEffect(cascade) { cascade.animateTo(1f, cascadeSpec(filters.size)) }
    val listState = rememberLazyListState()
    LaunchedEffect(sectionIndex) { listState.scrollToItem(0) }
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FavoritesButton(
            selected = source == FontListSource.Favorites,
            onClick = { onSourceSelected(FontListSource.Favorites) },
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(1.dp)
                .height(28.dp)
                .background(scheme.onSurface.copy(alpha = 0.35f)),
        )
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item(key = "recent") {
                SubCategoryChip(tr("Recent"), source == FontListSource.Recent, { onSourceSelected(FontListSource.Recent) })
            }
            item(key = "featured") {
                SubCategoryChip(tr("Featured"), source == FontListSource.Featured, { onSourceSelected(FontListSource.Featured) })
            }
            itemsIndexed(filters, key = { _, filter -> "$sectionIndex/$filter" }) { index, filter ->
                SubCategoryChip(
                    // "All" and "Variable" are the app's own labels; the rest are the catalog's tag names.
                    text = if (filter is FontFilter.Tag) filter.label else tr(filter.label),
                    selected = (source as? FontListSource.Filtered)?.filter == filter,
                    onClick = { onSourceSelected(FontListSource.Filtered(filter)) },
                    modifier = Modifier.cascadeIn(cascade, index, filters.size),
                )
            }
        }
    }
}

/** The bar's favourites button: an outlined circle, primary while the grid shows the favourites. */
@Composable
private fun FavoritesButton(selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (selected) scheme.primary else scheme.onSurface
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.5.dp, tint, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = tr("Favorites"),
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SubCategoryChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .background(if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurface,
            maxLines = 1,
        )
    }
}

/** A font's name drawn in the font itself, for the keyboard bar's pills; the default font until it loads. */
@Composable
internal fun rememberLoadedFontFamily(font: FontEntry?): FontFamily? {
    val family by produceState<FontFamily?>(null, font?.fileUrl) { value = font?.let { FontLoader.load(it.fileUrl) } }
    return family
}
