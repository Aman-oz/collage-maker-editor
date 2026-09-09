package org.example.project.ui.adjust

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdjustMathTest {

    private fun assertMatrixEquals(expected: FloatArray, actual: FloatArray, tolerance: Float = 0.01f) {
        assertEquals(expected.size, actual.size, "matrix size mismatch")
        for (i in expected.indices) {
            assertTrue(
                kotlin.math.abs(expected[i] - actual[i]) <= tolerance,
                "index $i: expected ${expected[i]} but was ${actual[i]}",
            )
        }
    }

    @Test
    fun zeroValues_produceIdentityMatrix() {
        assertMatrixEquals(IdentityColorMatrix, brightnessMatrix(0f))
        assertMatrixEquals(IdentityColorMatrix, exposureMatrix(0f))
        assertMatrixEquals(IdentityColorMatrix, contrastMatrix(0f))
        assertMatrixEquals(IdentityColorMatrix, saturationAdjustMatrix(0f))
        assertMatrixEquals(IdentityColorMatrix, hueRotateMatrix(0f))
        assertMatrixEquals(IdentityColorMatrix, sharpenApproxMatrix(0f))
    }

    @Test
    fun defaultAdjustValues_produceIdentityMatrix() {
        assertMatrixEquals(IdentityColorMatrix, AdjustValues().toColorMatrix())
    }

    @Test
    fun brightness_addsConstantOffsetToEachChannel() {
        val matrix = brightnessMatrix(20f)
        // Translation columns (index 4, 9, 14) should all equal 20 * 2.55.
        assertEquals(51f, matrix[4], 0.01f)
        assertEquals(51f, matrix[9], 0.01f)
        assertEquals(51f, matrix[14], 0.01f)
        // Diagonal scale stays 1 — brightness is purely additive.
        assertEquals(1f, matrix[0], 0.01f)
        assertEquals(1f, matrix[6], 0.01f)
        assertEquals(1f, matrix[12], 0.01f)
    }

    @Test
    fun exposure_scalesEachChannelMultiplicatively() {
        val matrix = exposureMatrix(100f) // full stop up -> 2x
        assertEquals(2f, matrix[0], 0.01f)
        assertEquals(2f, matrix[6], 0.01f)
        assertEquals(2f, matrix[12], 0.01f)
        // No translation — exposure is purely multiplicative.
        assertEquals(0f, matrix[4], 0.01f)
        assertEquals(0f, matrix[9], 0.01f)
        assertEquals(0f, matrix[14], 0.01f)
    }

    @Test
    fun exposure_negativeValueDarkens() {
        val matrix = exposureMatrix(-100f) // -> 0.5x
        assertEquals(0.5f, matrix[0], 0.01f)
    }

    @Test
    fun hueRotate_fullCircleReturnsToIdentity() {
        // value=100 -> 180 degrees; two of those (value applied twice via composition) is 360.
        val halfTurn = hueRotateMatrix(100f)
        val fullTurn = multiplyColorMatrices(halfTurn, halfTurn)
        assertMatrixEquals(IdentityColorMatrix, fullTurn, tolerance = 0.02f)
    }

    @Test
    fun saturationZero_desaturatesToLuminanceWeights() {
        val matrix = saturationAdjustMatrix(-100f) // saturation factor 0
        // Every row should equal the luminance weights repeated across R,G,B.
        assertEquals(0.213f, matrix[0], 0.01f)
        assertEquals(0.715f, matrix[1], 0.01f)
        assertEquals(0.072f, matrix[2], 0.01f)
        assertEquals(0.213f, matrix[5], 0.01f)
        assertEquals(0.715f, matrix[6], 0.01f)
    }

    @Test
    fun multiplyColorMatrices_composesTranslationsAdditively() {
        // Two brightness boosts of +10 should compose into +20 worth of offset (identity linear parts).
        val a = brightnessMatrix(10f)
        val b = brightnessMatrix(10f)
        val composed = multiplyColorMatrices(a, b)
        assertEquals(brightnessMatrix(20f)[4], composed[4], 0.05f)
    }

    @Test
    fun multiplyColorMatrices_isNotCommutativeForScaleThenOffset() {
        // contrast(scale) then brightness(offset) should differ from the reverse order in general,
        // proving composition order actually matters (outer applied after inner).
        val contrastThenBrightness = multiplyColorMatrices(brightnessMatrix(20f), contrastMatrix(50f))
        val brightnessThenContrast = multiplyColorMatrices(contrastMatrix(50f), brightnessMatrix(20f))
        var same = true
        for (i in contrastThenBrightness.indices) {
            if (kotlin.math.abs(contrastThenBrightness[i] - brightnessThenContrast[i]) > 0.01f) same = false
        }
        assertTrue(!same, "expected composition order to matter")
    }

    @Test
    fun adjustValues_getAndWith_roundTrip() {
        val values = AdjustValues()
        for (type in AdjustmentType.entries) {
            assertEquals(0f, values[type])
        }
        val updated = values.with(AdjustmentType.Hue, 42f)
        assertEquals(42f, updated[AdjustmentType.Hue])
        assertEquals(0f, updated[AdjustmentType.Brightness])
    }

    @Test
    fun adjustValues_toColorMatrix_combinesAllNonZeroAdjustments() {
        val values = AdjustValues(brightness = 10f, contrast = 0f, saturation = 0f, hue = 0f, sharpen = 0f, exposure = 0f)
        val matrix = values.toColorMatrix()
        assertMatrixEquals(brightnessMatrix(10f), matrix)
    }
}
