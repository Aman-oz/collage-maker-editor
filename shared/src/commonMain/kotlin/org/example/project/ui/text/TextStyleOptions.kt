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
data class TextFontStyleOption(
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
    TextFontStyleOption("Stylish", FontFamily.Serif, FontWeight.Bold, FontStyle.Italic),
    TextFontStyleOption("Simple", FontFamily.SansSerif, FontWeight.Light),
)

internal val TextColorOptions: List<Color> = listOf(
    Color(0xFF1F2430),
    Color(0xFF3A8FD6),
    Color(0xFF151A4A),
    Color(0xFFEF4A5E),
    Color(0xFF9147E8),
    Color(0xFFF2A93B),
    Color.White,
    Color.Black,
    Color(0xFF2E7D32),
)

internal val TextSizeRange = 12f..120f
internal const val TextSizeDefault = 48f

/** The font, colour and size band that make a text edit a premium one (see [isPremiumTextEdit]). */
internal const val PremiumTextFontLabel = "Stylish"
internal val PremiumTextColor = Color(0xFFEF4A5E)
internal val PremiumTextSizeRange = 30f..50f

/**
 * Whether a text edit uses something premium: the Stylish font, the red colour, a size the user set
 * inside [PremiumTextSizeRange], or text that was written and then gone back into ([reEdited]).
 *
 * [TextSizeDefault] itself sits inside the premium band, so the untouched default does not count:
 * the size only matters once the user has moved it.
 */
internal fun isPremiumTextEdit(font: TextFontStyleOption, color: Color, sizeSp: Float, reEdited: Boolean): Boolean =
    font.label == PremiumTextFontLabel ||
        color == PremiumTextColor ||
        (sizeSp in PremiumTextSizeRange && sizeSp != TextSizeDefault) ||
        reEdited
