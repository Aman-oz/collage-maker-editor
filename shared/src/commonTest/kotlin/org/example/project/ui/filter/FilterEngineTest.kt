package org.example.project.ui.filter

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterEngineTest {

    private val noAssets = FilterAssetLookup { null }

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun Int.r() = (this shr 16) and 0xFF
    private fun Int.g() = (this shr 8) and 0xFF
    private fun Int.b() = this and 0xFF

    private fun run(recipe: String, vararg pixels: Int, assets: FilterAssetLookup = noAssets): IntArray {
        val image = PixelImage(pixels.copyOf(), pixels.size, 1)
        runFilterSteps(image, parseFilterRecipe(recipe), assets)
        return image.pixels
    }

    @Test
    fun toneCurve_throughTheCornersIsTheIdentity() {
        val curve = toneCurve(listOf(0f to 0f, 255f to 255f))
        for (x in 0..255) assertTrue(abs(curve[x] * 255f - x) < 0.01f, "at $x")
    }

    @Test
    fun toneCurve_isFlatOutsideItsPointsAndClamped() {
        val curve = toneCurve(listOf(40f to 10f, 128f to 200f, 182f to 284f))
        assertEquals(10f / 255f, curve[0])
        assertEquals(10f / 255f, curve[40])
        assertEquals(1f, curve[255])
    }

    @Test
    fun toneCurve_sortsPointsOutOfOrder() {
        val sorted = toneCurve(listOf(0f to 17f, 58f to 80f, 127f to 131f, 132f to 131f, 255f to 225f))
        val unsorted = toneCurve(listOf(0f to 17f, 58f to 80f, 132f to 131f, 127f to 131f, 255f to 225f))
        assertTrue(sorted.contentEquals(unsorted))
    }

    @Test
    fun parse_readsEachCommandAndSkipsUnknownOnes() {
        val steps = parseFilterRecipe("#unpack @adjust saturation 0 @curve R(0, 0)(255, 255) @dynamic wave 0.5 @pixblend screen 1 0.243 0.69 1 30")
        assertEquals(3, steps.size)
        assertTrue(steps[0] is SaturationStep)
        assertTrue(steps[1] is CurveStep)
        assertTrue(steps[2] is PixBlendStep)
    }

    @Test
    fun everyFilterRecipe_isFullyUnderstood() {
        for (filter in PhotoFiltersByCategory.values.flatten()) {
            val commands = filter.recipe.count { it == '@' }
            assertEquals(commands, parseFilterRecipe(filter.recipe).size, "${filter.id}: ${filter.recipe}")
        }
    }

    @Test
    fun saturationZero_greys() {
        val out = run("@adjust saturation 0", argb(200, 100, 50))
        assertEquals(out[0].r(), out[0].g())
        assertEquals(out[0].g(), out[0].b())
    }

    @Test
    fun invertingCurve_inverts() {
        val out = run("@curve RGB(0, 255)(255, 0)", argb(0, 128, 255))
        assertEquals(255, out[0].r())
        assertTrue(abs(out[0].g() - 127) <= 1)
        assertEquals(0, out[0].b())
    }

    @Test
    fun identityLut_leavesColoursAlone() {
        // The standard 8 × 8 tiles of 64 × 64: red across each tile, green down, blue by tile.
        val lut = PixelImage(IntArray(512 * 512), 512, 512)
        for (y in 0 until 512) for (x in 0 until 512) {
            val blue = (y / 64) * 8 + x / 64
            lut.pixels[y * 512 + x] = argb((x % 64) * 255 / 63, (y % 64) * 255 / 63, blue * 255 / 63)
        }
        val out = run("@adjust lut test.webp", argb(10, 130, 250), argb(255, 0, 64), assets = { lut })
        assertTrue(abs(out[0].r() - 10) <= 3 && abs(out[0].g() - 130) <= 3 && abs(out[0].b() - 250) <= 3)
        assertTrue(abs(out[1].r() - 255) <= 3 && abs(out[1].g() - 0) <= 3 && abs(out[1].b() - 64) <= 3)
    }

    @Test
    fun blends_followCge() {
        val c = floatArrayOf(0.2f, 0.5f, 0.8f)
        blend(BlendKind.Screen, c, floatArrayOf(0f, 0f, 0f), 1f)
        assertTrue(abs(c[0] - 0.2f) < 1e-5f && abs(c[2] - 0.8f) < 1e-5f)
        val h = floatArrayOf(0.2f, 0.5f, 0.8f)
        blend(BlendKind.HardMix, h, floatArrayOf(0.5f, 0.5f, 0.5f), 1f)
        assertEquals(listOf(0f, 1f, 1f), h.toList())
        val half = floatArrayOf(0.2f, 0.2f, 0.2f)
        blend(BlendKind.Mix, half, floatArrayOf(1f, 1f, 1f), 0.5f)
        assertTrue(abs(half[0] - 0.6f) < 1e-5f)
    }

    @Test
    fun colourCube_matchesMappingPixelByPixel() {
        // Past the cube's size the run is folded into the cube, below it each pixel is mapped.
        val count = 300_000
        val pixels = IntArray(count) { argb((it * 7) % 256, (it * 13) % 256, (it * 29) % 256) }
        val recipe = "@adjust hsv -0.7 0.5 -0.7 -0.7 0 0 @curve R(0, 0)(149, 145)(255, 255) @pixblend ol 0.29 0.55 0.07 1 25"
        val big = PixelImage(pixels.copyOf(), count, 1)
        runFilterSteps(big, parseFilterRecipe(recipe), noAssets)
        val small = PixelImage(pixels.copyOf(1000), 1000, 1)
        runFilterSteps(small, parseFilterRecipe(recipe), noAssets)
        for (i in 0 until 1000) {
            val a = big.pixels[i]
            val b = small.pixels[i]
            assertTrue(abs(a.r() - b.r()) <= 3 && abs(a.g() - b.g()) <= 3 && abs(a.b() - b.b()) <= 3, "pixel $i")
        }
    }

    @Test
    fun catalogue_allTabListsEveryFilterOnceWithNoneFirst() {
        val all = filtersOf(FilterCategory.All)
        assertEquals(PhotoFilter.None, all.first())
        assertEquals(PhotoFiltersByCategory.values.sumOf { it.size } + 1, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (filter in all) assertEquals(filter, photoFilterById(filter.id))
        assertEquals(null, photoFilterById("Vivid"))
    }

    @Test
    fun catalogue_premiumTabsAreWhollyPremium() {
        val premiumTabs = setOf(FilterCategory.BlackWhite, FilterCategory.Cold, FilterCategory.Warm)
        assertEquals(premiumTabs, FilterCategory.entries.filter { it.isPremium }.toSet())
        for (filter in premiumTabs.flatMap { PhotoFiltersByCategory.getValue(it) }) assertTrue(filter.isPremium, filter.id)
    }

    @Test
    fun catalogue_theSecondOfVintageAndSmoothIsPremium() {
        assertEquals(
            listOf("Vintage/2", "Smooth/2"),
            PhotoFiltersByCategory.values.flatten().filter { it.isPremium && !it.category.isPremium }.map { it.id },
        )
    }
}
