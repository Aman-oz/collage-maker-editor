package org.example.project.ui.ratio

import kotlin.test.Test
import kotlin.test.assertEquals

class RatioMathTest {

    @Test
    fun paddingPx_usesShorterSide() {
        assertEquals(40f, paddingPx(width = 400, height = 800, percent = 10f))
        assertEquals(40f, paddingPx(width = 800, height = 400, percent = 10f))
    }

    @Test
    fun paddingPx_zeroPercentIsNoPadding() {
        assertEquals(0f, paddingPx(width = 400, height = 400, percent = 0f))
    }

    @Test
    fun paddingPx_clampsToRange() {
        assertEquals(80f, paddingPx(width = 400, height = 400, percent = 90f))
        assertEquals(0f, paddingPx(width = 400, height = 400, percent = -5f))
    }
}
