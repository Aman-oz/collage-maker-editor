package org.example.project.i18n

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.example.project.ui.language.DefaultLanguageCode
import org.example.project.ui.language.SupportedLanguages

class LocalizationTest {

    private val placeholder = Regex("""\{\d}""")

    @AfterTest
    fun resetLanguage() {
        AppLocale.code = DefaultLanguageCode
    }

    @Test
    fun everyLanguageOnTheLanguageScreen_hasATranslationTable() {
        val codes = SupportedLanguages.map { it.code } - DefaultLanguageCode
        assertEquals(codes.toSet(), Translations.keys)
    }

    @Test
    fun everyTable_translatesEveryStringAndNothingElse() {
        val strings = AppStrings.toSet()
        assertEquals(AppStrings.size, strings.size, "AppStrings has duplicates")
        for ((code, table) in Translations) {
            assertEquals(emptySet(), strings - table.keys, "$code is missing translations")
            assertEquals(emptySet(), table.keys - strings, "$code translates strings the app doesn't have")
            assertTrue(table.values.none { it.isBlank() }, "$code has a blank translation")
        }
    }

    @Test
    fun everyTranslation_keepsItsPlaceholdersAndLineBreaks() {
        for ((code, table) in Translations) {
            for ((english, translated) in table) {
                assertEquals(
                    placeholder.findAll(english).map { it.value }.sorted().toList(),
                    placeholder.findAll(translated).map { it.value }.sorted().toList(),
                    "$code: placeholders of \"$english\"",
                )
                assertEquals(english.count { it == '\n' }, translated.count { it == '\n' }, "$code: line breaks of \"$english\"")
            }
        }
    }

    @Test
    fun translate_fillsPlaceholdersAfterTranslating() {
        assertEquals("3 Select Photos", translate("en", "{0} Select Photos", 3))
        assertEquals("3 Seleccionar fotos", translate("es", "{0} Select Photos", 3))
        // Japanese puts the total first, so the arguments are matched by number, not by position.
        assertEquals("5段階中4", translate("ja", "{0} of {1}", 4, 5))
    }

    @Test
    fun translate_returnsTextItDoesNotKnowUnchanged() {
        assertEquals("Server said no", translate("fr", "Server said no"))
        assertEquals("Done", translate("xx", "Done"))
    }

    @Test
    fun tr_followsTheAppLanguage() {
        AppLocale.code = "de"
        assertEquals("Fertig", tr("Done"))
        AppLocale.code = "en"
        assertEquals("Done", tr("Done"))
    }
}
