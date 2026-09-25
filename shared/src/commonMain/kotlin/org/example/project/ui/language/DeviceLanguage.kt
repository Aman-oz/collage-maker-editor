package org.example.project.ui.language

/**
 * BCP 47 tag of the user's preferred device language (for example `en-US`, `pt-BR`, `ur-PK`).
 *
 * This is platform-specific rather than Compose's `Locale.current` because on iOS the current
 * locale is resolved against the app bundle's localizations, so for an app that only ships English
 * it reports `en` whatever the device is set to. `NSLocale.preferredLanguages` has the real value.
 */
internal expect fun deviceLanguageTag(): String
