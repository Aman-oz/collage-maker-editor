package org.example.project.ui.layouteditor

import org.example.project.data.centraldata.CentralAssetDto
import org.example.project.data.centraldata.CentralAssetFieldsDto
import org.example.project.data.centraldata.CentralImageDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class LayoutArtTest {

    private fun asset(id: String, sequence: Int, url: String = "/b/$id.png", w: Float = 1254f, h: Float = 1568f, enabled: Boolean = true) =
        CentralAssetDto(
            id = id,
            name = "Frame $id",
            isEnabled = enabled,
            sequence = sequence,
            thumbnailUrl = "/b/thumb_$id.webp",
            moreFields = CentralAssetFieldsDto(image = listOf(CentralImageDto(url = url, width = w, height = h))),
        )

    @Test
    fun toLayoutArts_keepsEnabledWithAPictureInServerOrder() {
        val frames = listOf(
            asset("b", sequence = 2),
            asset("a", sequence = 1),
            asset("off", sequence = 0, enabled = false),
            asset("blank", sequence = 3, url = ""),
        ).toLayoutArts { "https://host$it" }

        assertEquals(listOf("a", "b"), frames.map { it.id })
        assertEquals("https://host/b/a.png", frames[0].imageUrl)
        assertEquals("https://host/b/thumb_a.webp", frames[0].thumbnailUrl)
        assertEquals(0.8f, frames[0].aspectRatio, 0.001f)
    }

    @Test
    fun toLayoutArts_withoutASizeIsSquare() {
        val frame = listOf(asset("a", 1, w = 0f, h = 0f)).toLayoutArts { it }.single()
        assertEquals(1f, frame.aspectRatio)
    }

    @Test
    fun opaqueBounds_cutsTheClearMargin() {
        val w = 5
        val h = 4
        val pixels = IntArray(w * h)
        pixels[1 * w + 1] = 0xFF000000.toInt()
        pixels[2 * w + 3] = 0x80FFFFFF.toInt()
        pixels[3 * w + 4] = 0x05FFFFFF // fainter than the threshold
        assertContentEquals(intArrayOf(1, 1, 4, 3), opaqueBounds(pixels, w, h))
    }

    @Test
    fun opaqueBounds_isNullWhenAllClear() {
        assertNull(opaqueBounds(IntArray(6), 3, 2))
    }
}
