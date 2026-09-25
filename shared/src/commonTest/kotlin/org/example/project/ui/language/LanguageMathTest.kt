package org.example.project.ui.language

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageMathTest {

    @Test
    fun exactCode_isSelected() {
        assertEquals("ja", resolveDefaultLanguageCode("ja"))
    }

    @Test
    fun regionSubtag_isIgnored() {
        assertEquals("pt", resolveDefaultLanguageCode("pt-BR"))
        assertEquals("ur", resolveDefaultLanguageCode("ur-PK"))
    }

    @Test
    fun underscoreSeparatorAndCase_areTolerated() {
        assertEquals("fr", resolveDefaultLanguageCode("FR_ca"))
    }

    @Test
    fun unsupportedLanguage_fallsBackToEnglish() {
        assertEquals("en", resolveDefaultLanguageCode("sw-KE"))
    }

    @Test
    fun emptyTag_fallsBackToEnglish() {
        assertEquals("en", resolveDefaultLanguageCode(""))
    }
}
