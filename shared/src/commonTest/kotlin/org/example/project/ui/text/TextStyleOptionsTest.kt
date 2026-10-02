package org.example.project.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun isPremiumTextEdit_plainEditIsFree() {
        // The default size sits inside the premium band, but untouched it does not count.
        assertFalse(isPremiumTextEdit(TextFontStyles[1], TextColorOptions.first(), TextSizeDefault, reEdited = false))
        assertFalse(isPremiumTextEdit(TextFontStyles[1], TextColorOptions.first(), 80f, reEdited = false))
    }

    @Test
    fun isPremiumTextEdit_eachPremiumChoiceCountsOnItsOwn() {
        val font = TextFontStyles[1]
        val color = TextColorOptions.first()
        assertTrue(isPremiumTextEdit(TextFontStyles.first { it.label == "Stylish" }, color, 80f, reEdited = false))
        assertTrue(isPremiumTextEdit(font, PremiumTextColor, 80f, reEdited = false))
        assertTrue(isPremiumTextEdit(font, color, 30f, reEdited = false))
        assertTrue(isPremiumTextEdit(font, color, 50f, reEdited = false))
        assertFalse(isPremiumTextEdit(font, color, 29f, reEdited = false))
        assertFalse(isPremiumTextEdit(font, color, 51f, reEdited = false))
        assertTrue(isPremiumTextEdit(font, color, 80f, reEdited = true))
    }

    @Test
    fun premiumTextOptions_existInThePickers() {
        assertTrue(TextFontStyles.any { it.label == PremiumTextFontLabel })
        assertTrue(PremiumTextColor in TextColorOptions)
    }
}
