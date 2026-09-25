package org.example.project.ui.collage

import kotlin.test.Test
import kotlin.test.assertEquals

class CollageMathTest {

    @Test
    fun fitAspect_wideRatioInTallBox_isWidthBound() {
        assertEquals(320f to 180f, fitAspect(320f, 500f, 16f / 9f))
    }

    @Test
    fun fitAspect_tallRatioInWideBox_isHeightBound() {
        assertEquals(225f to 400f, fitAspect(500f, 400f, 9f / 16f))
    }

    @Test
    fun fitAspect_squareInSquare_fillsBox() {
        assertEquals(300f to 300f, fitAspect(300f, 300f, 1f))
    }

    @Test
    fun fitAspect_emptyBox_isZero() {
        assertEquals(0f to 0f, fitAspect(0f, 300f, 1f))
    }

    @Test
    fun collageOutputSize_keepsLongSideAndRatio() {
        assertEquals(1080 to 1080, collageOutputSize(CollageRatio.Square.aspect))
        assertEquals(1080 to 608, collageOutputSize(CollageRatio.Landscape.aspect))
        assertEquals(864 to 1080, collageOutputSize(CollageRatio.Portrait.aspect))
        assertEquals(608 to 1080, collageOutputSize(CollageRatio.Story.aspect))
    }
}
