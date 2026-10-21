package org.example.project.data.fonts

import org.example.project.data.centraldata.CentralFontAssetDto

/**
 * One font family from the central-data server: its name, the TTF to download, and whether it is
 * premium (a crown in the lists; the Text tool won't apply it for a non-subscriber).
 */
data class FontEntry(
    val family: String,
    val fileUrl: String,
    val isPremium: Boolean = false,
)

/** A sub-category of a [FontSection] (Active, Artistic, …): one chip of the Font panel's bar. */
data class FontSubcategory(val id: String, val categoryId: String, val name: String)

/** A main font category (Feeling, Appearance, …), as the server names it, with its sub-categories. */
data class FontSection(val title: String, val subcategories: List<FontSubcategory>)

/** The main categories, and the Featured list. A sub-category's fonts are fetched only when it is opened. */
class FontCatalog(val sections: List<FontSection>, val featured: List<FontEntry>)

/**
 * Enabled fonts with a file, in server order. A family listed twice keeps its first entry, since the
 * grid is keyed by family.
 */
internal fun List<CentralFontAssetDto>.toFonts(): List<FontEntry> =
    filter { it.isEnabled && it.name.isNotBlank() }
        .sortedBy { it.sequence }
        .mapNotNull { asset ->
            val url = asset.moreFields.url?.file?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            FontEntry(family = asset.name, fileUrl = url, isPremium = asset.isPremium)
        }
        .distinctBy { it.family }

/**
 * The favourite and recent fonts as kept on the device, so they can be shown and drawn (and their
 * crown known) without first browsing to the sub-category that lists them. Family names and URLs
 * hold no tab or newline, so one `family<TAB>url<TAB>premium` per line is a safe format.
 */
fun encodeKnownFonts(fonts: Collection<FontEntry>): String =
    fonts.joinToString("\n") { "${it.family}\t${it.fileUrl}\t${if (it.isPremium) 1 else 0}" }

/** The fonts of [encodeKnownFonts]'s text, by family; a line without the premium flag reads as free. */
fun decodeKnownFonts(text: String): Map<String, FontEntry> = text.lineSequence()
    .mapNotNull { line ->
        val fields = line.split('\t')
        val family = fields[0]
        val url = fields.getOrNull(1).orEmpty()
        if (family.isEmpty() || url.isEmpty()) null else FontEntry(family, url, isPremium = fields.getOrNull(2) == "1")
    }
    .associateBy { it.family }

/** [family] moved (or added) to the front of [recent], which keeps at most [max] families. */
fun pushRecentFont(recent: List<String>, family: String, max: Int = MaxRecentFonts): List<String> =
    (listOf(family) + recent.filter { it != family }).take(max)

const val MaxRecentFonts = 20

const val FeaturedFontCount = 40
