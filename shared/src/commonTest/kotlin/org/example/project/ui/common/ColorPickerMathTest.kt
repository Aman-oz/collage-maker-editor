package org.example.project.ui.common

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
