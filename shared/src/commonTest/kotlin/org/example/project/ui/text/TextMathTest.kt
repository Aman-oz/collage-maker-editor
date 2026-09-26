package org.example.project.ui.text

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextMathTest {

    private fun assertNear(expected: Offset, actual: Offset) {
        assertTrue(abs(expected.x - actual.x) < 1e-3f && abs(expected.y - actual.y) < 1e-3f, "expected $expected, was $actual")
    }

    @Test
    fun rotateVector_quarterTurnIsClockwiseOnScreen() {
        // With y pointing down, +x rotated 90° clockwise points down (+y).
        assertNear(Offset(0f, 1f), rotateVector(Offset(1f, 0f), 90f))
        assertNear(Offset(-1f, 0f), rotateVector(Offset(0f, 1f), 90f))
    }

    @Test
    fun rotateVector_roundTripsWithNegativeAngle() {
        val v = Offset(3f, -7f)
        assertNear(v, rotateVector(rotateVector(v, 37f), -37f))
    }

    @Test
    fun vectorAngleDegrees_matchesRotateVector() {
        assertEquals(0f, vectorAngleDegrees(Offset(1f, 0f)))
        assertEquals(90f, vectorAngleDegrees(Offset(0f, 1f)))
        assertTrue(abs(vectorAngleDegrees(rotateVector(Offset(1f, 0f), 30f)) - 30f) < 1e-3f)
    }

    @Test
    fun snapTextRotation_snapsNearQuarterTurnsOnly() {
        assertEquals(0f, snapTextRotation(3f))
        assertEquals(0f, snapTextRotation(-3.5f))
        assertEquals(90f, snapTextRotation(87f))
        assertEquals(-180f, snapTextRotation(-183f))
        assertEquals(10f, snapTextRotation(10f))
        assertEquals(45f, snapTextRotation(45f))
    }
}
