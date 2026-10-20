package org.example.project.data.network

import androidx.compose.ui.graphics.toPixelMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SvgRasterizeIOSTest {

    /** An Illustrator-style SVG: no size of its own, its fills in a stylesheet. */
    private val svg = """
        <?xml version="1.0" encoding="UTF-8"?>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 100">
          <defs><style>.cls-1 { fill: #ff0000; } .cls-2 { fill: #0000ff; }</style></defs>
          <rect class="cls-1" x="0" y="0" width="100" height="100"/>
          <rect class="cls-2" x="100" y="0" width="100" height="100"/>
        </svg>
    """.trimIndent()

    @Test
    fun rasterizeSvg_scalesViewBoxAndAppliesStylesheet() {
        val bitmap = rasterizeSvg(svg.encodeToByteArray(), maxSidePx = 64)
        assertEquals(64, bitmap.width)
        assertEquals(32, bitmap.height)
        val pixels = bitmap.toPixelMap()
        val left = pixels[10, 16]
        val right = pixels[54, 16]
        assertTrue(left.red > 0.9f && left.blue < 0.1f, "left is $left")
        assertTrue(right.blue > 0.9f && right.red < 0.1f, "right is $right")
        assertEquals(1f, left.alpha)
    }
}
