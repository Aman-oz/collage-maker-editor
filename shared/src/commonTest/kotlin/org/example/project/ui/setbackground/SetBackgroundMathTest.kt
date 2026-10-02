package org.example.project.ui.setbackground

import kotlin.test.Test
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.freestyle.FreestyleContent
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

    @Test
    fun isPremium_gradientBackdropOrPremiumLayers() {
        val plain = SetBackgroundEdit()
        assertEquals(false, plain.isPremium())
        assertEquals(true, plain.copy(backdrop = Backdrop.Gradient(BackdropGradients.first())).isPremium())

        val stickers = (1L..3L).map { FreestyleLayer(id = it, content = FreestyleContent.StickerContent("😀")) }
        assertEquals(false, plain.copy(layers = stickers.take(2)).isPremium())
        assertEquals(true, plain.copy(layers = stickers).isPremium())
    }
}
