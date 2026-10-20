package org.example.project.data.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgRenderingTest {

    @Test
    fun isSvgUrl_ignoresQueryAndCase() {
        assertTrue(isSvgUrl("https://host/a/Asset_1.SVG?v=2"))
        assertFalse(isSvgUrl("https://host/a/Asset_1.png"))
    }

    @Test
    fun inlineSvgClassStyles_copiesClassRulesOntoShapes() {
        val svg = """
            <svg viewBox="0 0 10 10"><defs><style>
              .cls-1, .cls-2 { fill: #f00; }
              /* a comment */
              .cls-2 { opacity: .5 }
              svg .ignored { fill: blue }
            </style></defs>
            <path class="cls-1" d="M0 0"/>
            <circle class="cls-2" style="stroke: #000" r="1"></circle>
            <rect class="other" width="1"/>
            </svg>
        """.trimIndent()
        val out = inlineSvgClassStyles(svg)
        assertTrue("""<path class="cls-1" d="M0 0" style="fill: #f00"/>""" in out, out)
        // Later rules after earlier ones, the element's own style last, so it still wins.
        assertTrue("""<circle class="cls-2" style="fill: #f00;opacity: .5;stroke: #000" r="1">""" in out, out)
        assertTrue("""<rect class="other" width="1"/>""" in out, out)
    }

    @Test
    fun inlineSvgClassStyles_leavesSvgWithoutStylesheetAlone() {
        val svg = """<svg><path class="a" d="M0 0"/></svg>"""
        assertEquals(svg, inlineSvgClassStyles(svg))
    }
}
