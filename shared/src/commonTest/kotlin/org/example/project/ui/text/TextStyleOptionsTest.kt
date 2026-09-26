package org.example.project.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextStyleOptionsTest {

    @Test
    fun fontStyles_matchTheDesignsSixOptions() {
        val labels = TextFontStyles.map { it.label }
        assertEquals(listOf("Classic", "Modern", "Bold", "Elegant", "Stylish", "Simple"), labels)
    }

    @Test
    fun colorOptions_hasNineSwatches() {
        assertEquals(9, TextColorOptions.size)
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
