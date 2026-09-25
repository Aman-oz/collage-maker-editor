package org.example.project.ui.language

/** A language offered on the language screen. [code] is an ISO 639-1 primary language subtag. */
data class AppLanguage(
    val code: String,
    val nativeName: String,
    val flag: String,
)

/** Fallback when the device language is not one of [SupportedLanguages]. */
internal const val DefaultLanguageCode = "en"

/** Languages the app offers, in display order. Names are shown in their own script. */
val SupportedLanguages: List<AppLanguage> = listOf(
    AppLanguage("en", "English", "🇺🇸"),
    AppLanguage("pt", "Português", "🇵🇹"),
    AppLanguage("it", "Italiano", "🇮🇹"),
    AppLanguage("ja", "日本語", "🇯🇵"),
    AppLanguage("ko", "한국어", "🇰🇷"),
    AppLanguage("fr", "Français", "🇫🇷"),
    AppLanguage("es", "Español", "🇪🇸"),
    AppLanguage("ar", "العربية", "🇦🇪"),
    AppLanguage("hi", "हिन्दी", "🇮🇳"),
    AppLanguage("vi", "Tiếng Việt", "🇻🇳"),
    AppLanguage("ur", "اردو", "🇵🇰"),
    AppLanguage("de", "Deutsch", "🇩🇪"),
    AppLanguage("tr", "Türkçe", "🇹🇷"),
    AppLanguage("ru", "Русский", "🇷🇺"),
    AppLanguage("id", "Bahasa Indonesia", "🇮🇩"),
)

/**
 * Picks the language code to preselect for a device [languageTag]. Only the primary subtag is
 * compared, so `pt-BR` selects Portuguese and `zh-Hans-CN` would select Chinese; anything not in
 * [languages] falls back to [DefaultLanguageCode].
 */
fun resolveDefaultLanguageCode(
    languageTag: String,
    languages: List<AppLanguage> = SupportedLanguages,
): String {
    val primary = languageTag.substringBefore('-').substringBefore('_').lowercase()
    return languages.firstOrNull { it.code == primary }?.code ?: DefaultLanguageCode
}
