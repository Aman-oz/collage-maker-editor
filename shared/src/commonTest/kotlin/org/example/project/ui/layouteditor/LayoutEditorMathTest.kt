package org.example.project.ui.layouteditor

import org.example.project.ui.templates.MaxSlotZoom
import org.example.project.ui.templates.SlotTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutEditorMathTest {

    @Test
    fun canvasAspect_fixedRatioWins() {
        assertEquals(0.8f, layoutCanvasAspect(0.8f, 4000, 1000))
    }

    @Test
    fun canvasAspect_originalFollowsFirstPhoto_withinRange() {
        assertEquals(1.5f, layoutCanvasAspect(null, 3000, 2000))
        assertEquals(OriginalCanvasAspectRange.endInclusive, layoutCanvasAspect(null, 5000, 1000))
        assertEquals(OriginalCanvasAspectRange.start, layoutCanvasAspect(null, 1000, 5000))
    }

    @Test
    fun canvasAspect_originalWithoutPhoto_isSquare() {
        assertEquals(1f, layoutCanvasAspect(null, null, null))
    }

    @Test
    fun unit_isShorterSideOver360() {
        assertEquals(2f, layoutUnitPx(1440f, 720f))
    }

    @Test
    fun rotatedSlotBounds_quarterTurnSwapsSides() {
        val (w, h) = rotatedSlotBounds(300f, 100f, 90f)
        assertEquals(100f, w, 0.01f)
        assertEquals(300f, h, 0.01f)
    }

    @Test
    fun rotatedSlotBounds_unturnedIsTheSlot() {
        assertEquals(300f to 100f, rotatedSlotBounds(300f, 100f, 0f))
    }

    @Test
    fun photoSize_coversSlotAtOneX() {
        val (w, h) = layoutPhotoSize(1000f, 500f, 200f, 200f, LayoutPhotoShape(), 1f)
        assertEquals(400f, w, 0.01f)
        assertEquals(200f, h, 0.01f)
    }

    @Test
    fun tilt_flatAtTheMiddleAndFullAtTheEnds() {
        assertEquals(0f, layoutTiltAmount(LayoutTiltFlat))
        assertEquals(MaxLayoutTilt, layoutTiltAmount(100f), 0.0001f)
        assertEquals(-MaxLayoutTilt, layoutTiltAmount(0f), 0.0001f)
        assertEquals(1f, layoutTiltCover(LayoutPhotoShape()))
    }

    @Test
    fun photoSize_leantPhotoIsZoomedToCover() {
        val flat = layoutPhotoSize(1000f, 1000f, 200f, 200f, LayoutPhotoShape(), 1f)
        val leant = layoutPhotoSize(1000f, 1000f, 200f, 200f, LayoutPhotoShape(tiltX = 100f), 1f)
        assertEquals(flat.first / (1f - MaxLayoutTilt), leant.first, 0.01f)
    }

    @Test
    fun clamp_limitsZoom() {
        val t = clampLayoutTransform(SlotTransform(scale = 0.2f), 100f, 100f, 50f, 50f, LayoutPhotoShape())
        assertEquals(1f, t.scale)
        val big = clampLayoutTransform(SlotTransform(scale = 99f), 100f, 100f, 50f, 50f, LayoutPhotoShape())
        assertEquals(MaxSlotZoom, big.scale)
    }

    @Test
    fun clamp_squarePhotoInSquareSlotAtOneX_cannotMove() {
        val t = clampLayoutTransform(SlotTransform(offsetX = 0.4f, offsetY = -0.3f), 100f, 100f, 50f, 50f, LayoutPhotoShape())
        assertEquals(0f, t.offsetX, 0.0001f)
        assertEquals(0f, t.offsetY, 0.0001f)
    }

    @Test
    fun clamp_keepsTurnedZoomedPhotoCoveringSlot() {
        val slotW = 300f
        val slotH = 180f
        for (degrees in listOf(-170f, -45f, 0f, 30f, 90f, 135f)) {
            val shape = LayoutPhotoShape(rotationDegrees = degrees, tiltX = 85f, tiltY = 10f)
            val t = clampLayoutTransform(SlotTransform(scale = 1.6f, offsetX = 5f, offsetY = -5f), 1200f, 900f, slotW, slotH, shape)
            assertCovers(1200f, 900f, slotW, slotH, shape, t)
        }
    }

    @Test
    fun gesture_panMovesByFractionOfSlot() {
        // A wide photo in a square slot overhangs sideways, so a small pan passes through unclamped.
        val t = SlotTransform().applyLayoutGesture(10f, 0f, 1f, 2000f, 1000f, 100f, 100f, LayoutPhotoShape())
        assertEquals(0.1f, t.offsetX, 0.0001f)
        assertEquals(0f, t.offsetY, 0.0001f)
    }

    @Test
    fun loadSize_capsTheLongerSide() {
        assertEquals(2048 to 1536, layoutPhotoLoadSize(4000, 3000))
        assertEquals(800 to 600, layoutPhotoLoadSize(800, 600))
    }

    /**
     * Every corner of the slot shows a point of the drawn photo: each corner is taken back through the
     * lean (`p = q / (1 - a·q)`, about the slot's centre), then measured in the photo's turned frame.
     */
    private fun assertCovers(imageW: Float, imageH: Float, slotW: Float, slotH: Float, shape: LayoutPhotoShape, t: SlotTransform) {
        val (drawnW, drawnH) = layoutPhotoSize(imageW, imageH, slotW, slotH, shape, t.scale)
        val ax = layoutTiltAmount(shape.tiltX) / (slotW / 2f)
        val ay = layoutTiltAmount(shape.tiltY) / (slotH / 2f)
        val radians = shape.rotationDegrees * PI.toFloat() / 180f
        val c = cos(radians)
        val s = sin(radians)
        val centerX = t.offsetX * slotW
        val centerY = t.offsetY * slotH
        for (slotCornerX in listOf(-slotW / 2f, slotW / 2f)) {
            for (slotCornerY in listOf(-slotH / 2f, slotH / 2f)) {
                val w = 1f - ax * slotCornerX - ay * slotCornerY
                val cornerX = slotCornerX / w
                val cornerY = slotCornerY / w
                val dx = cornerX - centerX
                val dy = cornerY - centerY
                val qx = dx * c + dy * s
                val qy = -dx * s + dy * c
                assertTrue(abs(qx) <= drawnW / 2f + 0.5f, "corner x at ${shape.rotationDegrees}°")
                assertTrue(abs(qy) <= drawnH / 2f + 0.5f, "corner y at ${shape.rotationDegrees}°")
            }
        }
    }
}
