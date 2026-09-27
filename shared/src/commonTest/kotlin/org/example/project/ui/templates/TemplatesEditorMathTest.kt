package org.example.project.ui.templates

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplatesEditorMathTest {

    @Test
    fun applyGesture_zoomIsClampedToRange() {
        assertEquals(1f, SlotTransform().applyGesture(0f, 0f, 0.5f, 100f, 100f).scale)
        assertEquals(MaxSlotZoom, SlotTransform(scale = 4f).applyGesture(0f, 0f, 2f, 100f, 100f).scale)
    }

    @Test
    fun applyGesture_panAtFitScaleDoesNotMove() {
        val result = SlotTransform().applyGesture(50f, -30f, 1f, 100f, 100f)
        assertEquals(0f, result.offsetX, 1e-6f)
        assertEquals(0f, result.offsetY, 1e-6f)
    }

    @Test
    fun applyGesture_panIsFractionOfSlotSize() {
        val result = SlotTransform(scale = 2f).applyGesture(20f, -10f, 1f, 200f, 100f)
        assertEquals(0.1f, result.offsetX, 1e-6f)
        assertEquals(-0.1f, result.offsetY, 1e-6f)
    }

    @Test
    fun applyGesture_panIsClampedSoPhotoCoversSlot() {
        // At 2x the photo overhangs by half the slot size per side.
        val result = SlotTransform(scale = 2f).applyGesture(1000f, -1000f, 1f, 100f, 100f)
        assertEquals(0.5f, result.offsetX)
        assertEquals(-0.5f, result.offsetY)
    }

    @Test
    fun applyGesture_zoomingOutPullsOffsetBackInside() {
        val result = SlotTransform(scale = 3f, offsetX = 1f, offsetY = -1f).applyGesture(0f, 0f, 0.5f, 100f, 100f)
        assertEquals(1.5f, result.scale)
        assertEquals(0.25f, result.offsetX)
        assertEquals(-0.25f, result.offsetY)
    }
}
