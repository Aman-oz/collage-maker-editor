package org.example.project.ui.rotate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotateMathTest {

    @Test
    fun rotationStops_containsDefaultAndIsSorted() {
        assertTrue(RotationDefault in RotationStops)
        assertEquals(RotationStops.sorted(), RotationStops)
    }

    @Test
    fun nearestRotationStop_snapsToClosestStop() {
        assertEquals(0f, nearestRotationStop(20f))
        assertEquals(45f, nearestRotationStop(30f))
        assertEquals(-90f, nearestRotationStop(-80f))
        assertEquals(180f, nearestRotationStop(150f))
        assertEquals(-180f, nearestRotationStop(-400f))
    }

    @Test
    fun rotationStops_centerIsDefaultAndSidesMirror() {
        assertEquals(RotationDefault, RotationStops[RotationStops.size / 2])
        assertEquals(RotationStops.map { 0f - it }.reversed(), RotationStops)
    }

    @Test
    fun rotationStopAtPosition_roundsToNearestIndexAndClamps() {
        assertEquals(-180f, rotationStopAtPosition(-2f))
        assertEquals(-45f, rotationStopAtPosition(2.4f))
        assertEquals(0f, rotationStopAtPosition(2.6f))
        assertEquals(180f, rotationStopAtPosition(99f))
    }

    @Test
    fun coverScaleForRotation_isOneWhenUpsideDown() {
        assertEquals(1f, coverScaleForRotation(400f, 300f, 180f), 1e-4f)
    }

    @Test
    fun nearestRotationStop_exactStopValueIsUnchanged() {
        for (stop in RotationStops) {
            assertEquals(stop, nearestRotationStop(stop))
        }
    }

    @Test
    fun normalizeQuarterTurns_wrapsIntoZeroToThreeRange() {
        assertEquals(0, normalizeQuarterTurns(0))
        assertEquals(1, normalizeQuarterTurns(1))
        assertEquals(3, normalizeQuarterTurns(-1))
        assertEquals(0, normalizeQuarterTurns(4))
        assertEquals(2, normalizeQuarterTurns(-2))
        assertEquals(1, normalizeQuarterTurns(5))
    }

    @Test
    fun coverScaleForRotation_isOneWhenUnrotated() {
        assertEquals(1f, coverScaleForRotation(400f, 300f, 0f))
    }

    @Test
    fun coverScaleForRotation_growsWithAngleAndIsAtLeastOne() {
        val at15 = coverScaleForRotation(400f, 300f, 15f)
        val at30 = coverScaleForRotation(400f, 300f, 30f)
        val at45 = coverScaleForRotation(400f, 300f, 45f)
        assertTrue(at15 >= 1f)
        assertTrue(at15 < at30)
        assertTrue(at30 < at45)
    }

    @Test
    fun coverScaleForRotation_isSymmetricForNegativeAngles() {
        assertEquals(coverScaleForRotation(400f, 300f, 30f), coverScaleForRotation(400f, 300f, -30f))
    }

    @Test
    fun sliderAngle_splitsIntoWholeTurnsAndFineRemainder() {
        assertEquals(0, sliderQuarterTurns(45f)); assertEquals(45f, sliderFineDegrees(45f))
        assertEquals(0, sliderQuarterTurns(-45f)); assertEquals(-45f, sliderFineDegrees(-45f))
        assertEquals(1, sliderQuarterTurns(90f)); assertEquals(0f, sliderFineDegrees(90f))
        assertEquals(-1, sliderQuarterTurns(-90f)); assertEquals(0f, sliderFineDegrees(-90f))
        assertEquals(2, sliderQuarterTurns(180f)); assertEquals(0f, sliderFineDegrees(180f))
        assertEquals(-2, sliderQuarterTurns(-180f)); assertEquals(0f, sliderFineDegrees(-180f))
    }
}
