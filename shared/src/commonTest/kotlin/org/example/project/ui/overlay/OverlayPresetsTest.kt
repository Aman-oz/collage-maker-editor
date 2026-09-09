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
    fun totalPresetCountMatchesAllFiveCategoriesCombined() {
        assertEquals(8 + 5 + 4 + 4 + 4, OverlayPresets.size)
    }
}
