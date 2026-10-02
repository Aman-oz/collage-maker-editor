package org.example.project.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.project.i18n.translations.TranslationsAr
import org.example.project.i18n.translations.TranslationsDe
import org.example.project.i18n.translations.TranslationsEs
import org.example.project.i18n.translations.TranslationsFr
import org.example.project.i18n.translations.TranslationsHi
import org.example.project.i18n.translations.TranslationsId
import org.example.project.i18n.translations.TranslationsIt
import org.example.project.i18n.translations.TranslationsJa
import org.example.project.i18n.translations.TranslationsKo
import org.example.project.i18n.translations.TranslationsPt
import org.example.project.i18n.translations.TranslationsRu
import org.example.project.i18n.translations.TranslationsTr
import org.example.project.i18n.translations.TranslationsUr
import org.example.project.i18n.translations.TranslationsVi

/**
 * The language the app is shown in, as the code of one of the language screen's languages.
 *
 * It is the app's own setting rather than the platform locale: the language picked on the language
 * screen has to apply at once, on both platforms, without restarting anything, and neither Android
 * nor iOS lets an app switch its resource locale that way. [code] is snapshot state, so every
 * composable that called [tr] recomposes when it changes. [org.example.project.data.AppSettings]
 * is the only thing that sets it.
 */
object AppLocale {
    var code: String by mutableStateOf("en")
}

/** English is the source text, so it has no table: an untranslated lookup simply returns the key. */
internal val Translations: Map<String, Map<String, String>> = mapOf(
    "pt" to TranslationsPt,
    "it" to TranslationsIt,
    "ja" to TranslationsJa,
    "ko" to TranslationsKo,
    "fr" to TranslationsFr,
    "es" to TranslationsEs,
    "ar" to TranslationsAr,
    "hi" to TranslationsHi,
    "vi" to TranslationsVi,
    "ur" to TranslationsUr,
    "de" to TranslationsDe,
    "tr" to TranslationsTr,
    "ru" to TranslationsRu,
    "id" to TranslationsId,
)

/**
 * [text] in the app's current language. [text] is the English string itself (one of [AppStrings]);
 * `{0}`, `{1}`… in it are replaced by [args] after translating, so a translation can reorder them.
 *
 * A plain function, not a composable: reading [AppLocale.code] is what subscribes a composable to
 * language changes, and it means click handlers and ViewModels can call it too. The one place it
 * must not be called is where the result is kept — an enum constructor, a top-level `val`, a
 * `remember` — because that string would outlive a language change. Keep the English there and
 * translate where it is shown.
 *
 * Text that isn't one of [AppStrings] (a server message, a file name) comes back unchanged.
 */
fun tr(text: String, vararg args: Any?): String = translate(AppLocale.code, text, *args)

internal fun translate(languageCode: String, text: String, vararg args: Any?): String {
    var result = Translations[languageCode]?.get(text) ?: text
    args.forEachIndexed { index, arg -> result = result.replace("{$index}", arg.toString()) }
    return result
}
