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
        assertEquals(0f, nearestRotationStop(4f))
        assertEquals(15f, nearestRotationStop(8f))
        assertEquals(-30f, nearestRotationStop(-26f))
        assertEquals(45f, nearestRotationStop(90f))
        assertEquals(-45f, nearestRotationStop(-90f))
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
}
