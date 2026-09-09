package org.example.project.ui.text

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * A selectable text style. Maps to Compose's generic system font families (no bundled font
 * assets), differentiated further by weight/style so each option reads distinctly even on
 * platforms where the generic families resolve to similar glyphs.
 */
internal data class TextFontStyleOption(
    val label: String,
    val fontFamily: FontFamily,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal,
)

internal val TextFontStyles: List<TextFontStyleOption> = listOf(
    TextFontStyleOption("Classic", FontFamily.Serif, FontWeight.Normal),
    TextFontStyleOption("Modern", FontFamily.SansSerif, FontWeight.Medium),
    TextFontStyleOption("Bold", FontFamily.SansSerif, FontWeight.Bold),
    TextFontStyleOption("Elegant", FontFamily.Cursive, FontWeight.Normal, FontStyle.Italic),
    TextFontStyleOption("Handwriting", FontFamily.Cursive, FontWeight.Light, FontStyle.Italic),
)

internal val TextColorOptions: List<Color> = listOf(
    Color.White,
    Color.Black,
    Color(0xFFE53935),
    Color(0xFFEF6C00),
    Color(0xFFC0A930),
    Color(0xFF2E7D32),
    Color(0xFF1976D2),
    Color(0xFF7B1FA2),
)

internal val TextSizeRange = 12f..120f
internal const val TextSizeDefault = 48f
