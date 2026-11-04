package org.example.project.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.example.project.ui.filter.BlendKind
import org.example.project.ui.filter.TextureBlendStep

class OverlayPresetsTest {

    @Test
    fun everyTab_hasOnePresetPerTexture() {
        for (category in OverlayCategory.entries) {
            assertEquals(category.count, OverlayPresets.count { it.category == category }, "$category")
        }
        assertEquals(23 + 36 * 4, OverlayPresets.size)
    }

    @Test
    fun presets_blendTheirTabsTexturesTheLasAppsWay() {
        val effect = OverlayPresets.first { it.category == OverlayCategory.Effect }
        assertEquals("overlay/scr1.jpg", effect.texturePath)
        assertEquals(BlendKind.Screen, (effect.steps.single() as TextureBlendStep).mode)
        val burn = OverlayPresets.last { it.category == OverlayCategory.Burn }
        assertEquals("effect/scr36.jpg", burn.texturePath)
        assertEquals(BlendKind.Color, (burn.steps.single() as TextureBlendStep).mode)
    }

    @Test
    fun keys_areUnique() {
        assertEquals(OverlayPresets.size, OverlayPresets.map { it.key }.toSet().size)
    }

    @Test
    fun premiumTabs_haveEveryPresetPremium() {
        val premiumTabs = setOf(OverlayCategory.Effect, OverlayCategory.Dodge, OverlayCategory.Burn)
        assertEquals(premiumTabs, OverlayCategory.entries.filter { it.isPremium }.toSet())
        for (preset in OverlayPresets.filter { it.category in premiumTabs }) assertTrue(preset.isPremium, preset.key)
    }

    @Test
    fun premiumPresets_inFreeTabs_areTheRequestedPositions() {
        val premium = OverlayPresets.filter { it.isPremium && !it.category.isPremium }.groupBy({ it.category }, { it.number })
        // Hardmix's 2nd.
        assertEquals(mapOf(OverlayCategory.Hardmix to listOf(2)), premium)
    }
}
