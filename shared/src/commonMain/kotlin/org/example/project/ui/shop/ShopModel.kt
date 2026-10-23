package org.example.project.ui.shop

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import org.example.project.data.fonts.FontEntry
import org.example.project.i18n.tr

/** The Shop's tabs, left to right. Rides in `Destination.Shop`, so it is [Serializable]. */
@Serializable
enum class ShopTab(private val englishLabel: String) {
    Premium("Premium"),
    Stickers("Sticker"),
    Fonts("Font"),
    Backgrounds("BG"),
    Borders("Border"),
    ;

    val label: String get() = tr(englishLabel)
}

/**
 * Opens the Shop on a tab. Provided by `AppNavDisplay` around the whole app, so a tool's search
 * button (the Stickers tool, the Font panel, the layout editor's Frame and Background libraries)
 * can open it without every editor passing a lambda down to every tool. A no-op in previews.
 */
internal val LocalOpenShop = staticCompositionLocalOf<(ShopTab) -> Unit> { {} }

/** A category pill of a tab (a sticker category, a font main category, a background category, …). */
internal data class ShopCategory(val id: String, val name: String)

/**
 * One thing a pack lists. [fileUrl] is what using it downloads (the sticker's SVG, the picture, the
 * TTF), and the [org.example.project.data.cache.AssetCache] key the tools keep it under, so a pack the
 * Shop downloaded is read from the disk by the tool. [font] is set on the Fonts tab, to draw the name.
 */
internal data class ShopItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val fileUrl: String,
    val isPremium: Boolean,
    val font: FontEntry? = null,
)

/** A sub-category of a [ShopCategory]: one titled row of the Shop, downloaded or used as a whole. */
internal data class ShopPack(
    val id: String,
    val categoryId: String,
    val name: String,
    val items: List<ShopItem>,
) {
    /** A crown by the title: a non-subscriber can't download it. */
    val isPremium: Boolean get() = items.any { it.isPremium }
}

/**
 * The packs of [categories] that match [query]: by the pack's name, its category's, or any of its
 * items' names, ignoring case. Each pack once, in category order. A blank query matches nothing.
 */
internal fun searchShopPacks(categories: List<Pair<ShopCategory, List<ShopPack>>>, query: String): List<ShopPack> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return categories
        .flatMap { (category, packs) ->
            val categoryMatches = category.name.contains(q, ignoreCase = true)
            packs.filter { pack ->
                categoryMatches || pack.name.contains(q, ignoreCase = true) || pack.items.any { it.name.contains(q, ignoreCase = true) }
            }
        }
        .distinctBy { it.id }
}

/** "Use" on a pack: the tool under the Shop opens on [packId] of [categoryId] in [tab]'s library. */
internal data class ShopPick(val tab: ShopTab, val categoryId: String, val packId: String)

/**
 * The hand-off from the Shop to the tool under it. The Shop can't call into a tool on another nav
 * entry, so "Use" [post]s the pick here and pops; the tool, composed again as the Shop leaves,
 * [take]s the pick for its own tab and opens on it. One at a time; a later pick replaces it.
 */
class ShopPicks {
    private val _pick = MutableStateFlow<ShopPick?>(null)
    internal val pick: StateFlow<ShopPick?> = _pick.asStateFlow()

    internal fun post(pick: ShopPick) {
        _pick.value = pick
    }

    /** The pick, if it is for [tab], cleared so it is acted on once. */
    internal fun take(tab: ShopTab): ShopPick? {
        val pick = _pick.value?.takeIf { it.tab == tab } ?: return null
        return if (_pick.compareAndSet(pick, null)) pick else null
    }
}
