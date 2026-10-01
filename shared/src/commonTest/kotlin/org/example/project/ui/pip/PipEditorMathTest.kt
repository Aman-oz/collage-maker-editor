package org.example.project.ui.pip

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipEditorMathTest {

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.0001f) {
        assertTrue(kotlin.math.abs(expected - actual) <= tolerance, "expected $expected but was $actual")
    }

    @Test
    fun slotRect_placesMaskAtItsNaturalSizeFromTheTopLeft() {
        // Template 0_0: a 211x439 mask at (344, 120) in a 612x612 frame.
        val rect = PipSlot(344, 120).slotRect(maskWidth = 211, maskHeight = 439, frameWidth = 612, frameHeight = 612)
        assertClose(344f / 612, rect.left)
        assertClose(120f / 612, rect.top)
        assertClose(211f / 612, rect.width)
        assertClose(439f / 612, rect.height)
        assertClose(555f / 612, rect.right)
    }

    @Test
    fun slotRect_normalizesAgainstNonSquareFrames() {
        val rect = PipSlot(89, 691).slotRect(maskWidth = 100, maskHeight = 200, frameWidth = 706, frameHeight = 1000)
        assertClose(89f / 706, rect.left)
        assertClose(0.691f, rect.top)
        assertClose(0.2f, rect.height)
    }

    @Test
    fun toLocal_mapsFramePointIntoRectFractions() {
        val rect = PipSlotRect(left = 0.2f, top = 0.4f, width = 0.5f, height = 0.2f)
        val local = rect.toLocal(Offset(0.45f, 0.5f))
        assertClose(0.5f, local.x)
        assertClose(0.5f, local.y)
    }

    @Test
    fun slotIndexAt_prefersTheLastDrawnSlotWhereRectsOverlap() {
        val rects = listOf(
            PipSlotRect(0f, 0f, 0.6f, 0.6f),
            PipSlotRect(0.4f, 0.4f, 0.6f, 0.6f),
        )
        assertEquals(1, slotIndexAt(Offset(0.5f, 0.5f), rects) { _, _ -> true })
        assertEquals(0, slotIndexAt(Offset(0.1f, 0.1f), rects) { _, _ -> true })
    }

    @Test
    fun slotIndexAt_fallsThroughATransparentMaskToTheSlotBelow() {
        val rects = listOf(
            PipSlotRect(0f, 0f, 0.6f, 0.6f),
            PipSlotRect(0.4f, 0.4f, 0.6f, 0.6f),
        )
        val hit = slotIndexAt(Offset(0.5f, 0.5f), rects) { index, _ -> index == 0 }
        assertEquals(0, hit)
    }

    @Test
    fun slotIndexAt_returnsNullOutsideEverySlot() {
        val rects = listOf(PipSlotRect(0.2f, 0.2f, 0.2f, 0.2f))
        assertNull(slotIndexAt(Offset(0.9f, 0.9f), rects) { _, _ -> true })
        assertNull(slotIndexAt(Offset(0.3f, 0.3f), rects) { _, _ -> false })
    }

    @Test
    fun backdropWorkingSize_downscalesLongSideAndNeverUpscales() {
        assertEquals(1080 to 810, backdropWorkingSize(4000, 3000))
        assertEquals(810 to 1080, backdropWorkingSize(3000, 4000))
        assertEquals(640 to 480, backdropWorkingSize(640, 480))
    }

    @Test
    fun pipOutputSize_keepsTheFrameAspect() {
        assertEquals(1080 to 1080, pipOutputSize(612, 612))
        assertEquals(1080 to 1530, pipOutputSize(706, 1000))
    }

    @Test
    fun catalog_namesAreUniqueAndSlotCountsMatchTheirGroup() {
        assertEquals(PipTemplates.size, PipTemplates.map { it.name }.toSet().size)
        for (template in PipTemplates) {
            val group = template.name.substringBefore('_').toInt()
            assertEquals(group + 1, template.slots.size, "template ${template.name}")
        }
        assertEquals(46, PipTemplates.size)
    }

    @Test
    fun template_assetPathsFollowTheBundledNaming() {
        val template = pipTemplate("1_4")!!
        assertEquals("files/pip/1_4_preview.webp", template.previewPath)
        assertEquals("files/pip/1_4_fg.webp", template.framePath)
        assertEquals("files/pip/1_4_1.png", template.maskPath(1))
        assertNull(pipTemplate("9_9"))
    }
}
