package org.example.project.ui.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralSubcategoryDto
import org.example.project.data.centraldata.resolveUrl

class TemplatesRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base = "https://example.com/"

    private fun asset(id: String, width: Int = 1074, height: Int = 1600, sequence: Int = 1, enabled: Boolean = true) = """
        {"id":"$id","is_enabled":$enabled,"is_premium":false,"sequence":$sequence,
         "thumbnail_url":"/static/$id.png",
         "more_fields":{"frames":{"image_url":"/static/$id.png","width":$width,"height":$height,
           "coordinates":[{"x":125,"y":78,"height":357,"width":358,"rotation":5.48,"elevation":0}]}}}
    """.trimIndent()

    private fun frames(vararg assets: String) =
        json.decodeFromString<List<CentralAssetDto>>(assets.joinToString(",", "[", "]")).toFrames { resolveUrl(base, it) }

    @Test
    fun toFrames_movesSlotFromTopLeftToCentre() {
        val slot = frames(asset("a")).single().slots.single()

        assertEquals(304f, slot.x)
        assertEquals(256.5f, slot.y)
        assertEquals(358f, slot.width)
        assertEquals(357f, slot.height)
        assertEquals(5.48f, slot.rotation)
    }

    @Test
    fun toFrames_resolvesUrlsAndDerivesLayoutFromSize() {
        val all = frames(asset("p", 1074, 1600), asset("s", 1000, 1000), asset("l", 1600, 1074))

        assertEquals("https://example.com/static/p.png", all.first().imageUrl)
        assertEquals(listOf(TemplateLayout.Portrait, TemplateLayout.Square, TemplateLayout.Landscape), all.map { it.layout })
    }

    @Test
    fun toFrames_dropsDisabledAndSortsBySequence() {
        val all = frames(asset("b", sequence = 2), asset("a", sequence = 1), asset("x", enabled = false))

        assertEquals(listOf("a", "b"), all.map { it.id })
    }

    @Test
    fun toCategories_keepsEnabledInSequenceOrder() {
        val categories = json.decodeFromString<List<CentralSubcategoryDto>>(
            """[{"id":"2","name":"Love","sequence":2},{"id":"1","name":"Animal","sequence":1},
               {"id":"3","name":"Off","sequence":0,"is_enabled":false}]""",
        ).toCategories()

        assertEquals(listOf("Animal", "Love"), categories.map { it.name })
    }
}
