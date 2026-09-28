package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.example.project.ui.text.TextFontStyles

class FreestyleLayerOpsTest {

    private val sticker = FreestyleLayer(id = 1, content = FreestyleContent.StickerContent("😀"))
    private val text = FreestyleLayer(id = 2, content = FreestyleContent.TextContent("Hi", Color.Red, TextFontStyles.first()))
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
        val restyled = layers.withText(2, "Hello", Color.Blue, font)
        assertEquals(FreestyleContent.TextContent("Hello", Color.Blue, font), restyled[1].content)
        assertEquals(layers, layers.withText(1, "Nope", Color.Blue, font))

        val retyped = layers.retyped(2, "Bye")
        assertEquals(FreestyleContent.TextContent("Bye", Color.Red, TextFontStyles.first()), retyped[1].content)
    }
}
