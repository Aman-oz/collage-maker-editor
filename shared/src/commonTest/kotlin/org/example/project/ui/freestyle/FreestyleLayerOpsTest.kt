package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.example.project.ui.text.PremiumTextColor
import org.example.project.ui.text.PremiumTextFontLabel
import org.example.project.ui.text.TextColorOptions
import org.example.project.ui.text.TextFontStyles

class FreestyleLayerOpsTest {

    private val sticker = FreestyleLayer(id = 1, content = FreestyleContent.StickerContent("😀"))
    private val text = FreestyleLayer(id = 2, content = FreestyleContent.TextContent("Hi", FreestyleFill.Solid(Color.Red), TextFontStyles.first()))
    private val layers = listOf(sticker, text)

    @Test
    fun transformed_movesScalesAndRotatesOnlyTheTarget() {
        val result = layers.transformed(1, Offset(0.1f, -0.2f), zoomDelta = 2f, rotationDeltaDegrees = 15f)
        val moved = result.first()
        assertEquals(0.6f, moved.offsetFraction.x, 1e-5f)
        assertEquals(0.3f, moved.offsetFraction.y, 1e-5f)
        assertEquals(2f, moved.scale)
        assertEquals(15f, moved.rotationDegrees)
        assertEquals(text, result[1])
    }

    @Test
    fun transformed_keepsCenterOnCanvasAndClampsScale() {
        val moved = layers.transformed(1, Offset(5f, -5f), zoomDelta = 100f, rotationDeltaDegrees = 0f).first()
        assertEquals(Offset(1f, 0f), moved.offsetFraction)
        assertEquals(FreestyleLayerScaleRange.endInclusive, moved.scale)
    }

    @Test
    fun withScaleRotation_clampsScale() {
        val result = layers.withScaleRotation(2, scale = 0.01f, rotationDegrees = 90f)
        assertEquals(FreestyleLayerScaleRange.start, result[1].scale)
        assertEquals(90f, result[1].rotationDegrees)
    }

    @Test
    fun broughtToFront_movesLayerLastAndIsNoOpWhenAlreadyThere() {
        assertEquals(listOf(text, sticker), layers.broughtToFront(1))
        assertSame(layers, layers.broughtToFront(2))
        assertSame(layers, layers.broughtToFront(99))
    }

    @Test
    fun textEdits_onlyTouchTextLayers() {
        val font = TextFontStyles.last()
        val restyled = layers.withText(2, "Hello", FreestyleFill.Solid(Color.Blue), font)
        assertEquals(FreestyleContent.TextContent("Hello", FreestyleFill.Solid(Color.Blue), font), restyled[1].content)
        assertEquals(layers, layers.withText(1, "Nope", FreestyleFill.Solid(Color.Blue), font))

        val retyped = layers.retyped(2, "Bye")
        assertEquals(FreestyleContent.TextContent("Bye", FreestyleFill.Solid(Color.Red), TextFontStyles.first()), retyped[1].content)
    }

    @Test
    fun isPremiumFreestyle_countsPhotosAndStickersPastTheFreeAmount() {
        assertFalse(isPremiumFreestyle(imageCount = 4, stickerCount = 2, texts = emptyList()))
        assertTrue(isPremiumFreestyle(imageCount = 5, stickerCount = 0, texts = emptyList()))
        assertTrue(isPremiumFreestyle(imageCount = 1, stickerCount = 3, texts = emptyList()))
    }

    @Test
    fun isPremiumFreestyle_premiumTextColorOrFont() {
        val plain = FreestyleContent.TextContent("Hi", FreestyleFill.Solid(TextColorOptions.first()), TextFontStyles[1])
        assertFalse(isPremiumFreestyle(1, 0, listOf(plain)))
        assertTrue(isPremiumFreestyle(1, 0, listOf(plain, plain.copy(fill = FreestyleFill.Solid(PremiumTextColor)))))
        assertTrue(isPremiumFreestyle(1, 0, listOf(plain.copy(font = TextFontStyles.first { it.label == PremiumTextFontLabel }))))
        assertFalse(isPremiumFreestyle(1, 0, listOf(plain.copy(fill = FreestyleFill.Gradient(FreestyleTextGradients.first())))))
    }

    @Test
    fun isPremiumFreestyle_readsTheLayersOnTheCanvas() {
        val stickers = (1L..3L).map { FreestyleLayer(id = it, content = FreestyleContent.StickerContent("😀")) }
        assertFalse(stickers.take(2).isPremiumFreestyle())
        assertTrue(stickers.isPremiumFreestyle())
    }

    @Test
    fun scatterPlacements_aSinglePhotoSitsCentredAtFullSize() {
        val placement = scatterPlacements(listOf(1f)).single()
        assertEquals(Offset(0.5f, 0.5f), placement.offsetFraction)
        assertEquals(1f, placement.scale)
    }

    @Test
    fun scatterPlacements_everyPhotoGetsItsOwnSpotOnTheCanvas() {
        for (count in 2..12) {
            val placements = scatterPlacements(List(count) { 4f / 3f })
            assertEquals(count, placements.size)
            assertEquals(count, placements.map { it.offsetFraction }.toSet().size, "count=$count")
            placements.forEach {
                assertTrue(it.offsetFraction.x in 0f..1f && it.offsetFraction.y in 0f..1f, "count=$count")
                assertTrue(it.scale in FreestyleLayerScaleRange && it.scale <= 1f, "count=$count")
            }
        }
    }

    @Test
    fun scatterPlacements_neighboursInARowDoNotOverlapSideways() {
        for (count in 2..12) {
            val placements = scatterPlacements(List(count) { 1f })
            placements.zipWithNext().filter { (a, b) -> b.offsetFraction.x > a.offsetFraction.x }.forEach { (a, b) ->
                val gap = b.offsetFraction.x - a.offsetFraction.x
                val halfWidths = FreestyleImageBaseWidthFraction * (a.scale + b.scale) / 2f
                assertTrue(gap >= halfWidths, "count=$count")
            }
        }
    }

    @Test
    fun scatterPlacements_shrinksATallPhotoToFitItsRow() {
        val square = scatterPlacements(List(4) { 1f }).first()
        val tall = scatterPlacements(List(4) { 3f }).first()
        assertTrue(tall.scale < square.scale)
    }

    @Test
    fun withText_setsAndClearsTheBackground_andRetypingKeepsIt() {
        val plate = FreestyleFill.Solid(Color.Yellow)
        val font = TextFontStyles.first()
        val backed = layers.withText(2, "Hi", FreestyleFill.Solid(Color.Red), font, plate)
        assertEquals(plate, (backed[1].content as FreestyleContent.TextContent).background)
        assertEquals(plate, (backed.retyped(2, "Yo")[1].content as FreestyleContent.TextContent).background)
        val cleared = backed.withText(2, "Hi", FreestyleFill.Solid(Color.Red), font, null)
        assertEquals(null, (cleared[1].content as FreestyleContent.TextContent).background)
    }
}
