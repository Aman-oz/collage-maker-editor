package org.example.project.data.fonts

import kotlin.test.Test
import kotlin.test.assertEquals
import org.example.project.data.centraldata.CentralFontAssetDto
import org.example.project.data.centraldata.CentralFontFieldsDto
import org.example.project.data.centraldata.CentralFontFileDto

class FontCatalogTest {

    private fun asset(
        name: String,
        sequence: Int,
        file: String? = "https://f/$name.ttf",
        enabled: Boolean = true,
        premium: Boolean = false,
    ) =
        CentralFontAssetDto(
            name = name,
            isEnabled = enabled,
            isPremium = premium,
            sequence = sequence,
            moreFields = CentralFontFieldsDto(file?.let { CentralFontFileDto(it) }),
        )

    @Test
    fun toFonts_keepsEnabledFontsWithAFileInServerOrder() {
        val fonts = listOf(
            asset("Lobster", 2, premium = true),
            asset("Roboto", 1),
            asset("Hidden", 0, enabled = false),
            asset("NoFile", 3, file = null),
            asset("Blank", 4, file = ""),
        ).toFonts()
        assertEquals(listOf(FontEntry("Roboto", "https://f/Roboto.ttf"), FontEntry("Lobster", "https://f/Lobster.ttf", isPremium = true)), fonts)
    }

    @Test
    fun toFonts_keepsTheFirstOfAFamilyListedTwice() {
        val fonts = listOf(asset("Caveat", 1, "https://f/a.ttf"), asset("Caveat", 2, "https://f/b.ttf")).toFonts()
        assertEquals(listOf(FontEntry("Caveat", "https://f/a.ttf")), fonts)
    }

    @Test
    fun knownFonts_roundTripWithTheirCrown() {
        val fonts = listOf(FontEntry("Open Sans", "https://f/open.ttf"), FontEntry("Lobster", "https://f/lobster.ttf", isPremium = true))
        assertEquals(fonts.associateBy { it.family }, decodeKnownFonts(encodeKnownFonts(fonts)))
    }

    @Test
    fun knownFonts_skipBrokenLinesAndReadAMissingFlagAsFree() {
        val decoded = decodeKnownFonts("broken\n\tno-family\nNoUrl\t\nLora\thttps://f/lora.ttf")
        assertEquals(mapOf("Lora" to FontEntry("Lora", "https://f/lora.ttf")), decoded)
    }

    @Test
    fun pushRecentFont_movesToFrontAndCaps() {
        assertEquals(listOf("B", "A", "C"), pushRecentFont(listOf("A", "B", "C"), "B"))
        assertEquals(listOf("D", "A"), pushRecentFont(listOf("A", "B"), "D", max = 2))
    }

    @Test
    fun withPremiumFlags_marksAFamilyPremiumAnywhereAsPremium() {
        // The server lists Kaushan Script premium in Active and free in Artistic.
        val artistic = listOf(FontEntry("Kaushan Script", "https://f/k.ttf"), FontEntry("Lora", "https://f/lora.ttf"))
        assertEquals(
            listOf(FontEntry("Kaushan Script", "https://f/k.ttf", isPremium = true), FontEntry("Lora", "https://f/lora.ttf")),
            withPremiumFlags(artistic, setOf("Kaushan Script")),
        )
    }
}
