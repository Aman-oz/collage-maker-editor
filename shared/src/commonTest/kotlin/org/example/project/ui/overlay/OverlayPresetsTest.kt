package org.example.project.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayPresetsTest {

    @Test
    fun everyCategoryHasAtLeastOnePreset() {
        for (category in OverlayCategory.entries) {
            assertTrue(
                OverlayPresets.any { it.category == category },
                "category $category has no presets",
            )
        }
    }

    @Test
    fun effectCategory_hasAllEightRequestedPresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Effect }.map { it.label }
        assertEquals(
            listOf("Light Leak", "Grain", "Dust", "Bokeh", "Glow", "Vintage", "Film", "Texture"),
            labels,
        )
    }

    @Test
    fun colorCategory_hasAllTwelvePresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Colorful }.map { it.label }
        assertEquals(
            listOf(
                "Rainbow", "Sunset", "Aurora", "Neon", "Candy", "Tropical",
                "Ocean", "Holo", "Cosmic", "Confetti", "Halftone", "Prism Leak",
            ),
            labels,
        )
    }

    @Test
    fun hardmixCategory_hasAllFiveRequestedPresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Hardmix }.map { it.label }
        assertEquals(listOf("Crimson", "Cyan Split", "Amber Cut", "Violet Mix", "Toxic"), labels)
    }

    @Test
    fun dodgeCategory_hasAllFourRequestedPresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Dodge }.map { it.label }
        assertEquals(listOf("Sunrise", "Soft Beam", "Halo", "Flare"), labels)
    }

    @Test
    fun burnCategory_hasAllFourRequestedPresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Burn }.map { it.label }
        assertEquals(listOf("Shadow", "Vignette", "Ember", "Charcoal"), labels)
    }

    @Test
    fun divideCategory_hasAllFourRequestedPresets() {
        val labels = OverlayPresets.filter { it.category == OverlayCategory.Divide }.map { it.label }
        assertEquals(listOf("Split", "Duotone", "Fracture", "Prism"), labels)
    }

    @Test
    fun everyPresetLabelIsUniqueWithinItsCategory() {
        for (category in OverlayCategory.entries) {
            val labels = OverlayPresets.filter { it.category == category }.map { it.label }
            assertEquals(labels.size, labels.toSet().size, "duplicate label within $category")
        }
    }

    @Test
    fun totalPresetCountMatchesAllCategoriesCombined() {
        assertEquals(8 + 12 + 5 + 4 + 4 + 4, OverlayPresets.size)
    }

    @Test
    fun premiumPresets_areTheRequestedPositionsInEachTab() {
        val premium = OverlayPresets.filter { it.isPremium }.groupBy({ it.category }, { it.label })
        assertEquals(
            mapOf(
                // 2nd and second last.
                OverlayCategory.Effect to listOf("Grain", "Film"),
                // 5th, 6th, third last and second last.
                OverlayCategory.Colorful to listOf("Candy", "Tropical", "Confetti", "Halftone"),
                // 2nd.
                OverlayCategory.Hardmix to listOf("Cyan Split"),
            ),
            premium,
        )
    }
}
