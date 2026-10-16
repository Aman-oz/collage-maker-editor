package org.example.project.ui.text

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextItemsTest {

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 1e-4f) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual")

    @Test
    fun duplicate_nudgesDownRightAndUnlocks() {
        val copy = duplicateTextItem(TextItem(id = 1, content = "Hi", centerX = 0.5f, centerY = 0.5f, locked = true), newId = 7)
        assertEquals(7, copy.id)
        assertEquals("Hi", copy.content)
        assertNear(0.5f + DuplicateNudge, copy.centerX)
        assertNear(0.5f + DuplicateNudge, copy.centerY)
        assertFalse(copy.locked)
    }

    @Test
    fun duplicate_nudgesBackAtTheEdge() {
        val copy = duplicateTextItem(TextItem(id = 1, centerX = 0.99f, centerY = 0.2f), newId = 2)
        assertNear(0.99f - DuplicateNudge, copy.centerX)
        assertNear(0.2f + DuplicateNudge, copy.centerY)
    }

    @Test
    fun reset_keepsWordsLookAndPlace() {
        val item = TextItem(id = 1, content = "Hi", colorArgb = 5, sizeSp = 90f, centerX = 0.3f, rotationDegrees = 40f, boxWidth = 0.5f, boxHeight = 0.2f)
        val reset = resetTextItem(item)
        assertEquals(TextSizeDefault, reset.sizeSp)
        assertEquals(0f, reset.rotationDegrees)
        assertNull(reset.boxWidth)
        assertNull(reset.boxHeight)
        assertEquals(item.copy(sizeSp = TextSizeDefault, rotationDegrees = 0f, boxWidth = null, boxHeight = null), reset)
    }

    @Test
    fun resize_rightEdgeGrowsAndShiftsCentreByHalf() {
        val item = TextItem(id = 1, centerX = 0.5f, centerY = 0.5f)
        val resized = resizeTextBox(item, TextBoxEdge.Right, Offset(40f, 0f), 400f, 800f, 100f, 50f, 30f)
        assertNear(140f / 400f, resized.boxWidth!!)
        assertNear(0.5f + 20f / 400f, resized.centerX)
        assertNear(0.5f, resized.centerY)
    }

    @Test
    fun resize_leftEdgeDraggedRightShrinks() {
        val item = TextItem(id = 1, centerX = 0.5f, centerY = 0.5f)
        val resized = resizeTextBox(item, TextBoxEdge.Left, Offset(20f, 0f), 400f, 800f, 100f, 50f, 30f)
        assertNear(80f / 400f, resized.boxWidth!!)
        // The right edge stays: the centre moves right by half the shrink.
        assertNear(0.5f + 10f / 400f, resized.centerX)
    }

    @Test
    fun resize_turnedTextMovesItsCentreAlongItsOwnAxis() {
        val item = TextItem(id = 1, centerX = 0.5f, centerY = 0.5f, rotationDegrees = 90f)
        val resized = resizeTextBox(item, TextBoxEdge.Right, Offset(40f, 0f), 400f, 800f, 100f, 50f, 30f)
        // Turned a quarter clockwise, the text's own +x points down the photo.
        assertNear(0.5f, resized.centerX)
        assertNear(0.5f + 20f / 800f, resized.centerY)
    }

    @Test
    fun resize_heightStopsAtTheWords() {
        val item = TextItem(id = 1)
        val resized = resizeTextBox(item, TextBoxEdge.Bottom, Offset(0f, -100f), 400f, 800f, 100f, 50f, 30f)
        assertNear(30f / 400f, resized.boxHeight!!)
    }

    @Test
    fun resize_widthStopsAtTheMinimum() {
        val resized = resizeTextBox(TextItem(id = 1), TextBoxEdge.Right, Offset(-500f, 0f), 400f, 800f, 100f, 50f, 30f)
        assertNear(MinTextBoxWidth, resized.boxWidth!!)
    }

    @Test
    fun scale_scalesWordsAndAreaTogetherWithinTheSizeRange() {
        val item = TextItem(id = 1, sizeSp = 40f, boxWidth = 0.4f, boxHeight = 0.1f)
        val doubled = scaleTextItem(item, 2f)
        assertNear(80f, doubled.sizeSp)
        assertNear(0.8f, doubled.boxWidth!!)
        assertNear(0.2f, doubled.boxHeight!!)
        val capped = scaleTextItem(item, 10f)
        assertNear(TextSizeRange.endInclusive, capped.sizeSp)
        // Scaled by the factor actually applied (3×), then held to the photo's width.
        assertNear(1f, capped.boxWidth!!)
        assertNear(0.3f, capped.boxHeight!!)
    }

    @Test
    fun slider_mapsOntoTheSizeRangeBothWays() {
        assertNear(TextSizeRange.start, sliderToTextSize(0f))
        assertNear(TextSizeRange.endInclusive, sliderToTextSize(100f))
        assertNear(37.5f, textSizeToSlider(sliderToTextSize(37.5f)))
    }

    @Test
    fun saver_roundTripsTheTexts() {
        val items = listOf(
            TextItem(id = 1, content = "Hello\nWorld", fontFamily = "Lobster", alignment = TextAlignOption.Right, boxWidth = 0.4f, locked = true),
            TextItem(id = 2),
        )
        val scope = SaverScope { true }
        val saved = with(TextItemsSaver) { scope.save(items) }!!
        assertEquals(items, TextItemsSaver.restore(saved))
    }

    @Test
    fun nudgeAndTurn_moveAStepStayOnThePhotoAndKeepTheTurnReadable() {
        val item = TextItem(id = 1, centerX = 0.5f, centerY = 0.998f)
        val moved = nudgeTextItem(item, Offset(TextNudgeStep, TextNudgeStep))
        assertNear(0.5f + TextNudgeStep, moved.centerX)
        assertEquals(1f, moved.centerY)

        assertEquals(-179f, turnTextItem(TextItem(id = 1, rotationDegrees = 180f), TextNudgeDegrees).rotationDegrees)
        assertEquals(180f, turnTextItem(TextItem(id = 1, rotationDegrees = -179f), -TextNudgeDegrees).rotationDegrees)
        assertEquals(10f, normalizeDegrees(370f))
        assertEquals(-90f, normalizeDegrees(270f))
    }

    @Test
    fun arrangeTextItems_movesUpAndDownTheStack() {
        val items = (1L..4L).map { TextItem(id = it) }
        fun ids(list: List<TextItem>) = list.map { it.id }
        assertEquals(listOf(1L, 3L, 2L, 4L), ids(arrangeTextItems(items, 2, TextLayerMove.Forward)))
        assertEquals(listOf(2L, 1L, 3L, 4L), ids(arrangeTextItems(items, 2, TextLayerMove.Backward)))
        assertEquals(listOf(1L, 3L, 4L, 2L), ids(arrangeTextItems(items, 2, TextLayerMove.ToFront)))
        assertEquals(listOf(3L, 1L, 2L, 4L), ids(arrangeTextItems(items, 3, TextLayerMove.ToBack)))
        // Already at that end, or not there at all.
        assertEquals(items, arrangeTextItems(items, 4, TextLayerMove.Forward))
        assertEquals(items, arrangeTextItems(items, 1, TextLayerMove.ToBack))
        assertEquals(items, arrangeTextItems(items, 9, TextLayerMove.ToFront))
    }

    @Test
    fun alignTextItem_putsTheBoxAgainstAnEdgeOrOnTheMiddle() {
        val item = TextItem(id = 1, centerX = 0.3f, centerY = 0.7f)
        assertNear(0.1f, alignTextItem(item, TextPhotoAlign.Top, 0.2f, 0.1f).centerY)
        assertNear(0.9f, alignTextItem(item, TextPhotoAlign.Bottom, 0.2f, 0.1f).centerY)
        assertNear(0.2f, alignTextItem(item, TextPhotoAlign.Left, 0.2f, 0.1f).centerX)
        assertNear(0.8f, alignTextItem(item, TextPhotoAlign.Right, 0.2f, 0.1f).centerX)
        val centered = alignTextItem(item, TextPhotoAlign.Center, 0.2f, 0.1f)
        assertNear(0.5f, centered.centerX)
        assertNear(0.7f, centered.centerY)
        assertNear(0.5f, alignTextItem(item, TextPhotoAlign.Middle, 0.2f, 0.1f).centerY)
    }

    @Test
    fun rotatedHalfSize_swapsOnAQuarterTurn() {
        val turned = rotatedHalfSize(100f, 40f, 90f)
        assertNear(20f, turned.width, 1e-3f)
        assertNear(50f, turned.height, 1e-3f)
    }
}
