package org.example.project.ui.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameMathTest {

    @Test
    fun frameWidthRange_containsDefaultValue() {
        assertTrue(FrameWidthDefault in FrameWidthRange)
    }

    @Test
    fun frameCornerRadiusRange_containsDefaultValue() {
        assertTrue(FrameCornerRadiusDefault in FrameCornerRadiusRange)
    }

    @Test
    fun frameColors_startsWithWhiteThenNone() {
        assertEquals("White", FrameColors[0].label)
        assertEquals("None", FrameColors[1].label)
        assertEquals(FrameColorNone, FrameColors[1].color)
    }

    @Test
    fun everyFrameColorLabelIsUnique() {
        val labels = FrameColors.map { it.label }
        assertEquals(labels.size, labels.toSet().size, "duplicate frame color label")
    }
}
