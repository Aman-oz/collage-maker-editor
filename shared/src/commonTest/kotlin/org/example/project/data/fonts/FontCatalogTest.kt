package org.example.project.data.fonts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FontCatalogTest {

    private val tsv = listOf(
        "Roboto\tsans-serif\t2\t2\thttps://f/s/roboto.ttf\t/Expressive/Calm=90;/Sans/Neo Grotesque=100",
        "Lobster\tdisplay\t1\t0\thttps://f/s/lobster.ttf\t/Expressive/Playful=80;/Expressive/Calm=10",
        "Caveat\thandwriting\t1\t1\thttps://f/s/caveat.ttf\t/Expressive/Playful=60;/Script/Handwritten=95",
        "Lora\tserif\t2\t1\thttps://f/s/lora.ttf\t/Slab/Humanist=70",
        "broken line",
    ).joinToString("\n")

    private val fonts = parseFontCatalog(tsv)

    @Test
    fun parseFontCatalog_readsColumnsAndSkipsShortLines() {
        assertEquals(listOf("Roboto", "Lobster", "Caveat", "Lora"), fonts.map { it.family })
        assertEquals(mapOf("/Expressive/Calm" to 90, "/Sans/Neo Grotesque" to 100), fonts[0].tags)
        assertEquals(2, fonts[0].axes)
    }

    @Test
    fun buildFontSections_listsTheMainCategoriesInOrder() {
        val titles = buildFontSections(fonts).map { it.title }
        assertEquals(listOf("Feeling", "Appearance", "Calligraphy", "Serif", "Sans Serif", "Seasonal", "Technology"), titles)
    }

    @Test
    fun technology_listsTheVariableFonts() {
        val technology = buildFontSections(fonts).last()
        assertEquals(listOf<FontFilter>(FontFilter.Variable), technology.filters)
        assertEquals(listOf("Roboto", "Caveat", "Lora"), applyFontFilter(fonts, FontFilter.Variable).map { it.family })
    }

    @Test
    fun buildFontSections_dropsTagsNoFontFitsAndLeadsWithAll() {
        val sections = buildFontSections(fonts).associateBy { it.title }
        assertEquals(listOf("Calm", "Playful"), sections.getValue("Feeling").filters.map { it.label })
        assertEquals(listOf("All", "Handwritten"), sections.getValue("Calligraphy").filters.map { it.label })
        assertEquals(listOf("All", "Slab Humanist"), sections.getValue("Serif").filters.map { it.label })
    }

    @Test
    fun applyFontFilter_keepsGoodMatchesBestFirst() {
        val playful = FontFilter.Tag("/Expressive/Playful", "Playful")
        assertEquals(listOf("Lobster", "Caveat"), applyFontFilter(fonts, playful).map { it.family })
    }

    @Test
    fun featuredFonts_takesDisplayAndHandwritingInCatalogOrder() {
        assertEquals(listOf("Lobster", "Caveat"), featuredFonts(fonts).map { it.family })
    }

    @Test
    fun pushRecentFont_movesToFrontWithoutDuplicatesAndCaps() {
        assertEquals(listOf("B", "A", "C"), pushRecentFont(listOf("A", "B", "C"), "B"))
        val capped = pushRecentFont((1..MaxRecentFonts).map { "F$it" }, "New")
        assertEquals(MaxRecentFonts, capped.size)
        assertTrue(capped.first() == "New" && "F$MaxRecentFonts" !in capped)
    }
}
