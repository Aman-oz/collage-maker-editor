package org.example.project.ui.language

import java.util.Locale

// toLanguageTag() rather than Locale.language, which still returns legacy codes ("in", "iw").
internal actual fun deviceLanguageTag(): String = Locale.getDefault().toLanguageTag()
