package org.example.project.ui.language

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

internal actual fun deviceLanguageTag(): String =
    NSLocale.preferredLanguages.firstOrNull() as? String ?: DefaultLanguageCode
