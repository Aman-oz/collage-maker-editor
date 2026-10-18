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

    @Test
    fun textCurvature_isStraightInTheMiddleAndSignedEitherSide() {
        assertEquals(0f, textCurvature(TextCurveStraight, 200f))
        assertEquals(0f, textCurvature(0f, 0f))
        assertTrue(textCurvature(0f, 200f) > 0f, "the start arches")
        assertTrue(textCurvature(100f, 200f) < 0f, "the end sags")
        // At either end the line wraps the full MaxTextCurveRadians.
        assertTrue(abs(textCurvature(0f, 200f) * 200f - MaxTextCurveRadians) < 1e-4f)
    }

    @Test
    fun bendTextPoint_keepsTheCentreAndBendsTheEnds() {
        val center = Offset(100f, 20f)
        val arch = textCurvature(10f, 200f)
        assertNear(center, bendTextPoint(center, center, arch))
        // A point straight above the centre only stays above it.
        assertNear(Offset(100f, 5f), bendTextPoint(Offset(100f, 5f), center, arch))
        // An arch pulls the line's ends down and in; a sag lifts them.
        val archedEnd = bendTextPoint(Offset(200f, 20f), center, arch)
        assertTrue(archedEnd.y > 20f && archedEnd.x < 200f, "was $archedEnd")
        val saggedEnd = bendTextPoint(Offset(200f, 20f), center, -arch)
        assertTrue(saggedEnd.y < 20f && saggedEnd.x < 200f, "was $saggedEnd")
        // Mirror images.
        assertNear(Offset(archedEnd.x, 40f - archedEnd.y), saggedEnd)
    }

    @Test
    fun bendTextPoint_keepsLengthAlongTheMiddleLine() {
        val center = Offset(0f, 0f)
        val curvature = 0.01f
        // 50px along a radius-100 arc is half a radian.
        assertNear(Offset(100f * kotlin.math.sin(0.5f), 100f - 100f * kotlin.math.cos(0.5f)), bendTextPoint(Offset(50f, 0f), center, curvature))
        assertTrue(abs(bendTextAngleDegrees(50f, 0f, curvature) - 0.5f * 180f / kotlin.math.PI.toFloat()) < 1e-3f)
    }

    @Test
    fun applyTextCase_casesEachModeAndKeepsTheLength() {
        val typed = "hELLO wORLD, don't 2day"
        assertEquals(typed, applyTextCase(typed, TextCase.None))
        assertEquals("HELLO WORLD, DON'T 2DAY", applyTextCase(typed, TextCase.Upper))
        assertEquals("hello world, don't 2day", applyTextCase(typed, TextCase.Lower))
        assertEquals("Hello World, Don't 2day", applyTextCase(typed, TextCase.Title))
        assertEquals("One\nTwo", applyTextCase("one\ntwo", TextCase.Title))
        for (case in TextCase.entries) assertEquals(typed.length, applyTextCase(typed, case).length)
    }

    @Test
    fun spacing_middleIsTheFontsOwnAndEndsReachTheirLimits() {
        assertEquals(0f, letterSpacingEm(TextSpacingNormal))
        assertEquals(MinLetterSpacingEm, letterSpacingEm(0f))
        assertEquals(MaxLetterSpacingEm, letterSpacingEm(100f))
        assertTrue(letterSpacingEm(75f) > 0f && letterSpacingEm(25f) < 0f)

        assertEquals(null, lineHeightEm(TextSpacingNormal))
        assertEquals(MinLineHeightEm, lineHeightEm(0f))
        assertEquals(MaxLineHeightEm, lineHeightEm(100f))
        assertTrue(abs(lineHeightEm(50.01f)!! - NormalLineHeightEm) < 1e-2f)
    }

    @Test
    fun shadowAngle_roundTripsAndKeepsTheDistance() {
        // The first offset, down and to the right, is 45°.
        assertTrue(abs(shadowAngleDegrees(shadowAngleSlider(DefaultShadowOffsetEm, DefaultShadowOffsetEm)) - 45f) < 1e-3f)
        val turned = shadowOffsetAtAngle(0.1f, 0f, shadowAngleSlider(0f, 1f))
        assertNear(Offset(0f, 0.1f), turned)
        // The slider's end doesn't wrap back to its start.
        val end = shadowOffsetAtAngle(0.1f, 0f, 100f)
        assertTrue(shadowAngleSlider(end.x, end.y) > 99f)
        // Without a direction, turning starts from the first offset's distance.
        val fromNothing = shadowOffsetAtAngle(0f, 0f, 0f)
        assertTrue(abs(fromNothing.getDistance() - Offset(DefaultShadowOffsetEm, DefaultShadowOffsetEm).getDistance()) < 1e-4f)
    }

    @Test
    fun nudgeShadowOffset_movesAndStopsAtTheLimit() {
        assertNear(Offset(0.06f, 0.04f), nudgeShadowOffset(0.06f, 0.06f, Offset(0f, -ShadowNudgeEm)))
        assertNear(Offset(MaxShadowOffsetEm, 0f), nudgeShadowOffset(MaxShadowOffsetEm, 0f, Offset(ShadowNudgeEm, 0f)))
    }

    @Test
    fun textPlateRect_reachesPastTheWordsBySide() {
        // 50 on the slider is 1 em; the font is 20px.
        val plate = textPlateRect(50f, 0f, 25f, 100f, androidx.compose.ui.geometry.Size(100f, 30f), emPx = 20f)
        assertEquals(-20f, plate.left)
        assertTrue(abs(plate.top) < 1e-6f)
        assertEquals(110f, plate.right)
        assertEquals(70f, plate.bottom)
        assertTrue(abs(textPlateRect(0f, 0f, 0f, 0f, androidx.compose.ui.geometry.Size(100f, 30f), 20f).left) < 1e-6f)
    }

    @Test
    fun textPlateCornerRadius_fullyRoundsAtTheEnd() {
        assertEquals(20f, textPlateCornerRadius(100f, 40f))
        assertEquals(0f, textPlateCornerRadius(0f, 40f))
        assertEquals(8f, textPlateCornerRadius(DefaultTextPlateCorner, 40f))
    }

    @Test
    fun plateOverflow_isTheWiderSideBeyondTheBoxAndNothingWithoutAPlate() {
        val item = TextItem(id = 1, backgroundArgb = -1, paddingLeft = 50f, paddingRight = 10f, paddingTop = 0f, paddingBottom = 5f)
        // Left is 1 em = 20px against 14px of box padding; top and bottom stay inside 6px.
        val overflow = item.plateOverflow(emPx = 20f, boxPaddingXPx = 14f, boxPaddingYPx = 6f)
        assertTrue(abs(overflow.width - 6f) < 1e-4f)
        assertEquals(0f, overflow.height)
        assertEquals(0f, item.copy(backgroundArgb = null).plateOverflow(20f, 14f, 6f).width)
    }
}
