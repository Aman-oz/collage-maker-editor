package org.example.project.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextStyleOptionsTest {

    @Test
    fun fontStyles_matchAllFiveRequestedOptions() {
        val labels = TextFontStyles.map { it.label }
        assertEquals(listOf("Classic", "Modern", "Bold", "Elegant", "Handwriting"), labels)
    }

    @Test
    fun colorOptions_hasEightSwatches() {
        assertEquals(8, TextColorOptions.size)
    }

    @Test
    fun colorOptions_areAllUnique() {
        assertEquals(TextColorOptions.size, TextColorOptions.toSet().size)
    }

    @Test
    fun sizeDefault_isWithinRange() {
        assertTrue(TextSizeDefault in TextSizeRange)
    }
}
