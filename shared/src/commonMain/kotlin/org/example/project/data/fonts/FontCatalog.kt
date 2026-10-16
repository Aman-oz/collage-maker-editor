package org.example.project.data.fonts

/**
 * One Google Fonts family, as listed in the bundled `files/fonts_catalog.tsv` (ported from the
 * FontsFactory sample, which generates it with `fonts_catalog.py`). The catalog is in Google Fonts'
 * popularity order.
 */
data class FontEntry(
    val family: String,
    /** serif, sans-serif, display, handwriting or monospace. */
    val category: String,
    val styles: Int,
    /** More than 0 for a variable font. */
    val axes: Int,
    val fileUrl: String,
    /** A tag path ("/Expressive/Playful") to how well the family fits it, 0..100. */
    val tags: Map<String, Int>,
)

/** Parses the catalog's TSV: `family, category, styles, axes, fileUrl, tags` (`;`-separated `path=score`). */
fun parseFontCatalog(tsv: String): List<FontEntry> = tsv.lineSequence()
    .filter { it.isNotBlank() }
    .mapNotNull { line ->
        val c = line.split('\t')
        if (c.size < 5) return@mapNotNull null
        FontEntry(
            family = c[0],
            category = c[1],
            styles = c[2].toIntOrNull() ?: 1,
            axes = c[3].toIntOrNull() ?: 0,
            fileUrl = c[4],
            tags = c.getOrNull(5).orEmpty().split(';')
                .filter { '=' in it }
                .associate { it.substringBeforeLast('=') to (it.substringAfterLast('=').toIntOrNull() ?: 0) },
        )
    }
    .toList()

/** A sub-category of a [FontSection]. [score] is how well a font matches it, 0 = not at all. */
sealed interface FontFilter {
    /** English for "All" and "Variable" (translate them where shown); otherwise the catalog's own tag name, shown as is. */
    val label: String
    fun score(font: FontEntry): Int

    data class Tag(val path: String, override val label: String) : FontFilter {
        override fun score(font: FontEntry) = font.tags[path] ?: 0
    }

    /** The "All" sub-category of Calligraphy, Serif and Sans Serif: every font of that category. */
    data class Category(val category: String) : FontFilter {
        override val label get() = "All"
        override fun score(font: FontEntry) = if (font.category == category) 100 else 0
    }

    /** Technology's one sub-category: variable fonts, which have at least one design axis. */
    data object Variable : FontFilter {
        override val label get() = "Variable"
        override fun score(font: FontEntry) = if (font.axes > 0) 100 else 0
    }
}

/** A main font category (Feeling, Appearance, …); [title] is English, translated where shown. */
data class FontSection(val title: String, val filters: List<FontFilter>)

private class SectionSpec(val title: String, val prefixes: List<String>, val allCategory: String? = null)

// Google Fonts' sidebar sections, and the families.csv tag groups behind them.
private val SectionSpecs = listOf(
    SectionSpec("Feeling", listOf("/Expressive/")),
    SectionSpec("Appearance", listOf("/Theme/")),
    SectionSpec("Calligraphy", listOf("/Script/"), allCategory = "handwriting"),
    SectionSpec("Serif", listOf("/Serif/", "/Slab/"), allCategory = "serif"),
    SectionSpec("Sans Serif", listOf("/Sans/"), allCategory = "sans-serif"),
    SectionSpec("Seasonal", listOf("/Seasonal/")),
)

/** A font must score at least this on a tag to be listed under it. */
const val MinFontScore = 50

/**
 * The main categories and their sub-categories, from the tags the catalog actually uses (and, last,
 * Technology, whose one sub-category is variable fonts). A tag no
 * font fits at least [MinFontScore] is dropped: it would open an empty grid.
 */
fun buildFontSections(fonts: List<FontEntry>): List<FontSection> {
    val allTags = fonts.flatMap { it.tags.keys }.distinct().sorted()
    val tagSections = SectionSpecs.map { spec ->
        val tagFilters = allTags
            .filter { tag -> spec.prefixes.any { tag.startsWith(it) } }
            .map { tag ->
                val name = tag.substringAfterLast('/')
                FontFilter.Tag(tag, if (tag.startsWith("/Slab/")) "Slab $name" else name)
            }
            .filter { filter -> fonts.any { filter.score(it) >= MinFontScore } }
        FontSection(spec.title, listOfNotNull(spec.allCategory?.let { FontFilter.Category(it) }) + tagFilters)
    }
    // Not a tag group: Google Fonts' Technology section filters on the font file itself.
    return tagSections + FontSection("Technology", listOf(FontFilter.Variable))
}

/** The fonts of [filter], best match first. */
fun applyFontFilter(fonts: List<FontEntry>, filter: FontFilter): List<FontEntry> =
    fonts.filter { filter.score(it) >= MinFontScore }.sortedByDescending { filter.score(it) }

/**
 * The Featured list: the most popular display and handwriting families, which show off a text edit
 * better than the plain sans and serif faces that top the catalog's overall popularity.
 */
fun featuredFonts(fonts: List<FontEntry>, count: Int = FeaturedFontCount): List<FontEntry> =
    fonts.filter { it.category == "display" || it.category == "handwriting" }.take(count)

const val FeaturedFontCount = 40

/** [family] moved (or added) to the front of [recent], which keeps at most [max] families. */
fun pushRecentFont(recent: List<String>, family: String, max: Int = MaxRecentFonts): List<String> =
    (listOf(family) + recent.filter { it != family }).take(max)

const val MaxRecentFonts = 20
