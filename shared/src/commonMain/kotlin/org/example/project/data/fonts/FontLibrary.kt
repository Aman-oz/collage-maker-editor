package org.example.project.data.fonts

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.KeyValueStore
import org.example.project.data.centraldata.CentralDataApi
import org.example.project.data.centraldata.CentralDataConfig

/**
 * The Text tool's fonts, from the central-data server, three levels deep like the stickers:
 *
 * - `categories?superCategoryId=<fonts>` → the main categories (Feeling, Appearance, …).
 * - `subcategories?categoryId=<id>` → each one's sub-categories (the chips).
 * - `assets?categoryId=<id>&subCategoryId=<id>` → that sub-category's fonts (the server wants both).
 *
 * The main and sub-categories are fetched together as the tool opens; a sub-category's fonts (a few
 * hundred KB each) only when it is opened. Answers are kept for the process, failures are not.
 *
 * Also keeps the user's favourite and recently used families across launches, with each one's file
 * URL, so they can be listed and drawn before (or without) the catalog loading.
 */
class FontLibrary(private val api: CentralDataApi, private val store: KeyValueStore) {

    private val mutex = Mutex()
    private var catalog: FontCatalog? = null
    private val fontsBySubcategory = mutableMapOf<String, List<FontEntry>>()

    /** Every font seen this run, and the remembered favourites and recents. Replaced, never mutated. */
    private var known: Map<String, FontEntry> = decodeKnownFonts(store.getString(KeyKnown).orEmpty())

    suspend fun catalog(): FontCatalog {
        mutex.withLock { catalog }?.let { return it }
        val categories = api.categories(CentralDataConfig.FONTS_SUPER_CATEGORY_ID)
            .filter { it.isEnabled && it.id.isNotBlank() }
            .sortedBy { it.sequence }
        val sections = coroutineScope {
            categories.map { category ->
                async {
                    val subcategories = api.subcategories(category.id)
                        .filter { it.isEnabled && it.id.isNotBlank() }
                        .sortedBy { it.sequence }
                        .map { FontSubcategory(id = it.id, categoryId = category.id, name = it.name) }
                    FontSection(category.name, subcategories)
                }
            }.awaitAll()
        }.filter { it.subcategories.isNotEmpty() } // a main category with no chips would open an empty bar
        return FontCatalog(sections, featured(sections)).also { mutex.withLock { catalog = it } }
    }

    /** [subcategory]'s fonts, in server order. */
    suspend fun fonts(subcategory: FontSubcategory): List<FontEntry> {
        mutex.withLock { fontsBySubcategory[subcategory.id] }?.let { return it }
        val fonts = api.fontAssets(subcategory.categoryId, subcategory.id).toFonts()
        mutex.withLock {
            fontsBySubcategory[subcategory.id] = fonts
            // The server's answer wins over what was remembered: a font may have turned premium.
            known = known + fonts.associateBy { it.family }
        }
        return fonts
    }

    /** The font named [family], if it has been listed this run or is a favourite or recent one. */
    fun find(family: String): FontEntry? = known[family]

    /**
     * The first fonts of the first sub-category that has any. The server has no featured list of its
     * own, and its first main category leads with its most eye-catching faces.
     */
    private suspend fun featured(sections: List<FontSection>): List<FontEntry> {
        for (subcategory in sections.firstOrNull()?.subcategories.orEmpty()) {
            val fonts = fonts(subcategory)
            if (fonts.isNotEmpty()) return fonts.take(FeaturedFontCount)
        }
        return emptyList()
    }

    private val _favorites = MutableStateFlow(store.getFamilies(KeyFavorites))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _recent = MutableStateFlow(store.getFamilies(KeyRecent))
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    /** Newest favourite first. */
    fun toggleFavorite(family: String) {
        val current = _favorites.value
        val updated = if (family in current) current - family else listOf(family) + current
        _favorites.value = updated
        store.putFamilies(KeyFavorites, updated)
        rememberUrls()
    }

    fun markUsed(family: String) {
        val updated = pushRecentFont(_recent.value, family)
        _recent.value = updated
        store.putFamilies(KeyRecent, updated)
        rememberUrls()
    }

    /** Stores each favourite and recent font, and nothing else. */
    private fun rememberUrls() {
        val kept = (_favorites.value + _recent.value).toSet()
        store.putString(KeyKnown, encodeKnownFonts(known.filterKeys { it in kept }.values))
    }

    private companion object {
        const val KeyFavorites = "favorite_fonts"
        const val KeyRecent = "recent_fonts"
        const val KeyKnown = "known_fonts"

        // Family names never hold a newline, so one per line is a safe list format.
        fun KeyValueStore.getFamilies(key: String): List<String> =
            getString(key).orEmpty().lines().filter { it.isNotBlank() }

        fun KeyValueStore.putFamilies(key: String, families: List<String>) = putString(key, families.joinToString("\n"))
    }
}
