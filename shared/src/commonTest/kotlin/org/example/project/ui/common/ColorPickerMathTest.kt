package org.example.project.ui.common

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorPickerMathTest {

    @Test
    fun toHsv_readsThePrimaries() {
        assertEquals(Hsv(0f, 1f, 1f), Color.Red.toHsv())
        assertEquals(Hsv(120f, 1f, 1f), Color.Green.toHsv())
        assertEquals(Hsv(240f, 1f, 1f), Color.Blue.toHsv())
    }

    @Test
    fun toHsv_greysHaveNoHueOrSaturation() {
        assertEquals(Hsv(0f, 0f, 1f), Color.White.toHsv())
        assertEquals(Hsv(0f, 0f, 0f), Color.Black.toHsv())
    }

    @Test
    fun toHsv_roundTripsThroughToColor() {
        listOf(Color(0xFF6C4DF6), Color(0xFFEF4A5E), Color(0xFF14B8A6), Color(0xFFF6AE45), Color(0xFF1F2933)).forEach { color ->
            val back = color.toHsv().toColor()
            assertEquals(color.red, back.red, 0.01f)
            assertEquals(color.green, back.green, 0.01f)
            assertEquals(color.blue, back.blue, 0.01f)
        }
    }

    @Test
    fun colorGrid_topRowIsGreysFromWhiteToBlack() {
        assertTrue(sameRgb(Color.White, colorGridColor(0, 0)))
        assertTrue(sameRgb(Color.Black, colorGridColor(0, ColorGridColumns - 1)))
        (0 until ColorGridColumns).forEach { column ->
            val grey = colorGridColor(0, column)
            assertEquals(grey.red, grey.green, 1e-4f)
            assertEquals(grey.red, grey.blue, 1e-4f)
        }
    }

    @Test
    fun colorGrid_eachColumnRunsDarkToFullToPastel() {
        (0 until ColorGridColumns).forEach { column ->
            assertEquals(1f, colorGridColor(5, column).toHsv().saturation, 0.01f)
            assertEquals(1f, colorGridColor(5, column).toHsv().value, 0.01f)
            assertTrue(colorGridColor(1, column).toHsv().value < 0.5f)
            assertTrue(colorGridColor(ColorGridRows - 1, column).toHsv().saturation < 0.3f)
        }
    }

    @Test
    fun colorGridCellOf_findsEveryCellAndIgnoresAlpha() {
        assertEquals(3 to 7, colorGridCellOf(colorGridColor(3, 7)))
        assertEquals(3 to 7, colorGridCellOf(colorGridColor(3, 7).copy(alpha = 0.4f)))
        assertNull(colorGridCellOf(Color(0xFF123457)))
    }

    @Test
    fun spectrum_runsWhiteToFullToBlackDown() {
        assertTrue(sameRgb(Color.White, spectrumColor(0.3f, 0f).toColor()))
        assertTrue(sameRgb(Color.Red, spectrumColor(0f, 0.5f).toColor()))
        assertTrue(sameRgb(Color.Black, spectrumColor(0.7f, 1f).toColor()))
    }

    @Test
    fun spectrumPosition_isWhereSpectrumColorShowsIt() {
        listOf(0.1f to 0.2f, 0.5f to 0.5f, 0.8f to 0.9f).forEach { (x, y) ->
            val position = spectrumColor(x, y).spectrumPosition()
            assertEquals(x, position.x, 1e-3f)
            assertEquals(y, position.y, 1e-3f)
        }
        assertEquals(1f, Color.Black.toHsv().spectrumPosition().y, 1e-3f)
    }

    @Test
    fun hex_roundTripsAndRejectsOtherText() {
        assertEquals("6C4DF6", Color(0xFF6C4DF6).toHexRgb())
        assertEquals(Color(0xFF6C4DF6), parseHexRgb("#6c4df6"))
        assertNull(parseHexRgb("6C4DF"))
        assertNull(parseHexRgb("6C4DFG"))
    }

    @Test
    fun addSavedColor_putsItFirstWithoutRepeatsAndKeepsTheLimit() {
        assertEquals(listOf(2, 1, 3), addSavedColor(listOf(1, 2, 3), 2))
        val full = List(MaxSavedColors) { it }
        assertEquals(listOf(99) + full.dropLast(1), addSavedColor(full, 99))
    }
}
