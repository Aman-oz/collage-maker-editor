package org.example.project.ui.layouts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.resolveUrl
import org.example.project.ui.collage.frames.FrameImageUtils

class CollageLayoutsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = "https://example.com/"

    private fun asset(id: String, file: String, sequence: Int = 1, enabled: Boolean = true, premium: Boolean = false) = """
        {"id":"$id","is_enabled":$enabled,"is_premium":$premium,"sequence":$sequence,
         "thumbnail_url":"/static/light/$file","more_fields":{"image":[{"url":"/static/light/$file","filename":"$file"}]}}
    """.trimIndent()

    /** Joined with the real slot geometry, as the catalog does. */
    private fun parse(vararg assets: String) =
        json.decodeFromString<List<CentralAssetDto>>(assets.joinToString(",", "[", "]"))
            .toCollageLayouts({ resolveUrl(base, it) }) { name -> FrameImageUtils.createTemplateItems("$name.png") }

    @Test
    fun toCollageLayouts_takesThePhotoCountFromTheTemplate() {
        val layout = parse(asset("a", "collage_3_5.png")).single()

        assertEquals("collage_3_5", layout.name)
        assertEquals(3, layout.photoCount)
        assertEquals("https://example.com/static/light/collage_3_5.png", layout.previewUrl)
    }

    @Test
    fun toCollageLayouts_dropsDisabledAndUndrawableLayouts_inServerOrder() {
        val layouts = parse(
            asset("b", "collage_2_1.png", sequence = 2),
            asset("off", "collage_2_2.png", enabled = false),
            asset("unknown", "collage_99_0.png"),
            asset("a", "collage_2_0.png", sequence = 1),
        )

        assertEquals(listOf("a", "b"), layouts.map { it.id })
    }

    @Test
    fun toCollageLayouts_takesPremiumFromTheServerOnly_notFromTheCollageCatalog() {
        val catalogPremium = { name: String ->
            FrameImageUtils.createTemplateItems("$name.png")?.apply { isPremium = true }
        }
        val layouts = json.decodeFromString<List<CentralAssetDto>>(
            listOf(asset("free", "collage_2_0.png"), asset("paid", "collage_2_1.png", premium = true)).joinToString(",", "[", "]"),
        ).toCollageLayouts({ resolveUrl(base, it) }, catalogPremium)

        assertEquals(listOf(false, true), layouts.map { it.isPremium })
    }
}
