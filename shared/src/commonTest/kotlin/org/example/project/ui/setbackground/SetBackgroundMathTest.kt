package org.example.project.ui.setbackground

import kotlin.test.Test
import kotlin.test.assertEquals

class SetBackgroundMathTest {

    @Test
    fun solidColors_formFullColumnPairs() {
        assertEquals(0, BackdropSolidColors.size % 2)
        assertEquals(BackdropSolidColors.size, BackdropSolidColors.toSet().size)
    }

    @Test
    fun gradients_haveAtLeastTwoStops() {
        BackdropGradients.forEach { assertEquals(true, it.size >= 2) }
    }
}
