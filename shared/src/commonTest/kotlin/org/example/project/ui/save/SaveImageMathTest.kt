package org.example.project.ui.save

import androidx.compose.ui.unit.IntRect
import kotlin.test.Test
import kotlin.test.assertEquals

class SaveImageMathTest {

    @Test
    fun watermarkRect_sitsInBottomRightCornerScaledOffShortSide() {
        // Short side 1000 → side 120, margin 35.
        assertEquals(IntRect(left = 845, top = 1845, right = 965, bottom = 1965), watermarkRect(1000, 2000))
    }

    @Test
    fun watermarkRect_isAlwaysSquare() {
        val rect = watermarkRect(3000, 1200)
        assertEquals(rect.width, rect.height)
    }

    @Test
    fun watermarkRect_neverCollapsesOnTinyImages() {
        assertEquals(1, watermarkRect(2, 2).width)
    }
}
