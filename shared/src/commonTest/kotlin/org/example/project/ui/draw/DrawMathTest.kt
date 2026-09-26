package org.example.project.ui.draw

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawMathTest {

    @Test
    fun brushSizeRange_containsDefaultValue() {
        assertTrue(BrushSizeDefault in BrushSizeRange)
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
    fun drawColors_defaultIsPresentAndLabelsUnique() {
        assertTrue(DrawColorDefault in DrawColors)
        val labels = DrawColors.map { it.label }
        assertEquals(labels.size, labels.toSet().size, "duplicate draw color label")
    }

    @Test
    fun mosaicPatterns_defaultIsPresentAndLabelsUnique() {
        assertTrue(MosaicPatternDefault in MosaicPatterns)
        val labels = MosaicPatterns.map { it.label }
        assertEquals(labels.size, labels.toSet().size, "duplicate mosaic pattern label")
    }

    @Test
    fun everyMosaicTexture_isOffered() {
        val offered = MosaicPatterns.filterIsInstance<TextureMosaic>().map { it.texture }.toSet()
        assertEquals(MosaicTexture.entries.toSet(), offered)
    }
}
