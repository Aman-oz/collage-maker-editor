package org.example.project.ui.blur

import kotlin.test.Test
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
}
