package org.example.project.ui.blur

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlurMathTest {

    @Test
    fun brushSizeRange_containsDefaultValue() {
        assertTrue(BrushSizeDefault in BrushSizeRange)
    }

    @Test
    fun blurLevelDefault_isWithinValidRange() {
        assertTrue(BlurLevelDefault in BlurLevelMin..BlurLevelMax)
    }

    @Test
    fun brushRadiusFraction_isZero_atMinimumBrushSize() {
        assertEquals(0f, brushRadiusFraction(BrushSizeRange.start))
    }

    @Test
    fun brushRadiusFraction_growsWithBrushSize() {
        val small = brushRadiusFraction(BrushSizeRange.start)
        val mid = brushRadiusFraction(BrushSizeDefault)
        val max = brushRadiusFraction(BrushSizeRange.endInclusive)
        assertTrue(small < mid, "radius should grow as brush size increases")
        assertTrue(mid < max, "radius should grow as brush size increases")
    }

    @Test
    fun brushRadiusFraction_neverExceedsQuarterOfImageWidth() {
        val max = brushRadiusFraction(BrushSizeRange.endInclusive)
        assertTrue(max <= 0.25f, "a full-size brush shouldn't dominate the whole image")
    }

    @Test
    fun boxBlurRadiiForGaussian_matchesGaussianVariance() {
        val sigma = 8f
        val radii = boxBlurRadiiForGaussian(sigma, passes = 3)
        // A box of width w has variance (w² - 1) / 12; the passes' variances add up.
        val variance = radii.sumOf { r -> val w = 2 * r + 1; (w * w - 1) / 12.0 }
        assertTrue(abs(variance - sigma * sigma) < sigma, "variance $variance should be close to ${sigma * sigma}")
    }

    @Test
    fun gaussianBlurArgb_keepsUniformImageUnchanged() {
        val color = 0xFF3366CC.toInt()
        val pixels = IntArray(20 * 10) { color }
        assertContentEquals(pixels, gaussianBlurArgb(pixels, 20, 10, sigma = 4f))
    }

    @Test
    fun gaussianBlurArgb_spreadsAPointSymmetrically() {
        val size = 21
        val pixels = IntArray(size * size) { 0xFF000000.toInt() }
        pixels[10 * size + 10] = 0xFFFFFFFF.toInt()
        val out = gaussianBlurArgb(pixels, size, size, sigma = 2f)
        fun red(x: Int, y: Int) = (out[y * size + x] shr 16) and 0xFF
        assertTrue(red(10, 10) < 255, "the bright point should be spread out")
        assertEquals(red(8, 10), red(12, 10))
        assertEquals(red(10, 8), red(10, 12))
        assertTrue(red(10, 10) >= red(12, 10), "brightness should fall off from the center")
    }

    @Test
    fun gaussianBlurArgb_isNoOp_forTinySigma() {
        val pixels = IntArray(9) { it * 0x010101 or 0xFF000000.toInt() }
        assertContentEquals(pixels, gaussianBlurArgb(pixels, 3, 3, sigma = 0.2f))
    }
}
