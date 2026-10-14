package org.example.project.data.collage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.resolveUrl

class RemoteCollagesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = "https://example.com/"

    private fun asset(id: String, theme: String, file: String, enabled: Boolean = true, premium: Boolean = false) = """
        {"id":"$id","subcategory_name":"$theme","is_enabled":$enabled,"is_premium":$premium,"sequence":1,
         "thumbnail_url":"/static/$theme/$file","more_fields":{"image":[{"url":"/static/$theme/$file","filename":"$file"}]}}
    """.trimIndent()

    private fun parse(vararg assets: String) =
        json.decodeFromString<List<CentralAssetDto>>(assets.joinToString(",", "[", "]")).toRemoteCollages { resolveUrl(base, it) }

    @Test
    fun toRemoteCollages_joinsDarkAndLightByFileNameIgnoringExtension() {
        val collages = parse(asset("d", "Dark", "collage_2_7.webp"), asset("l", "Light", "collage_2_7.png"))

        assertEquals(1, collages.size)
        val collage = collages.single()
        assertEquals("collage_2_7", collage.name)
        assertEquals("l", collage.id)
        assertEquals("https://example.com/static/Light/collage_2_7.png", collage.previewLight)
        assertEquals("https://example.com/static/Dark/collage_2_7.webp", collage.previewDark)
    }

    @Test
    fun toRemoteCollages_usesTheOnlyThemeForBothPreviews() {
        val collage = parse(asset("d", "Dark", "collage_1_0.png")).single()

        assertEquals(collage.previewDark, collage.previewLight)
    }

    @Test
    fun toRemoteCollages_dropsDisabledAndKeepsPremium() {
        val collages = parse(
            asset("a", "Light", "collage_1_0.png", enabled = false),
            asset("b", "Light", "collage_3_5.png", premium = true),
        )

        assertEquals(listOf("collage_3_5"), collages.map { it.name })
        assertEquals(true, collages.single().isPremium)
    }

    @Test
    fun resolveUrl_keepsAbsoluteAndJoinsRelative() {
        assertEquals("https://cdn.test/a.png", resolveUrl(base, "https://cdn.test/a.png"))
        assertEquals("https://example.com/static/a.png", resolveUrl(base, "/static/a.png"))
    }
}
