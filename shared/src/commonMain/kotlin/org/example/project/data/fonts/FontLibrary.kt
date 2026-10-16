package org.example.project.data.fonts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.example.project.data.KeyValueStore
import photocollagemaker.shared.generated.resources.Res

/** The parsed catalog and what is built from it once. */
class FontCatalog(val fonts: List<FontEntry>) {
    val sections: List<FontSection> = buildFontSections(fonts)
    val featured: List<FontEntry> = featuredFonts(fonts)
    private val byFamily = fonts.associateBy { it.family }

    fun find(family: String): FontEntry? = byFamily[family]
}

/**
 * The Text tool's fonts: the bundled Google Fonts catalog, read and parsed once (it is ~2,000
 * families, so not on the main thread), and the user's favourite and recently used families, kept
 * across launches.
 */
class FontLibrary(private val store: KeyValueStore) {

    private val catalogLock = Mutex()
    private var catalog: FontCatalog? = null

    suspend fun catalog(): FontCatalog = catalogLock.withLock {
        catalog ?: withContext(Dispatchers.Default) {
            FontCatalog(parseFontCatalog(Res.readBytes("files/fonts_catalog.tsv").decodeToString()))
        }.also { catalog = it }
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
    }

    fun markUsed(family: String) {
        val updated = pushRecentFont(_recent.value, family)
        _recent.value = updated
        store.putFamilies(KeyRecent, updated)
    }

    private companion object {
        const val KeyFavorites = "favorite_fonts"
        const val KeyRecent = "recent_fonts"

        // Family names never hold a newline, so one per line is a safe list format.
        fun KeyValueStore.getFamilies(key: String): List<String> =
            getString(key).orEmpty().lines().filter { it.isNotBlank() }

        fun KeyValueStore.putFamilies(key: String, families: List<String>) = putString(key, families.joinToString("\n"))
    }
}
