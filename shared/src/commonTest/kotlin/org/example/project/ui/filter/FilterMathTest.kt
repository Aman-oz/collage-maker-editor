package org.example.project.ui.filter

import kotlin.test.Test
import kotlin.test.assertContentEquals

class FilterMathTest {

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private val filter = floatArrayOf(
        2f, 0f, 0f, 0f, 10f,
        0f, 1f, 0f, 0f, 0f,
        0.5f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    @Test
    fun blendWithIdentity_fullIntensityIsTheFilter() {
        assertContentEquals(filter, blendWithIdentity(filter, 1f))
    }

    @Test
    fun blendWithIdentity_zeroIntensityIsIdentity() {
        assertContentEquals(identity, blendWithIdentity(filter, 0f))
    }

    @Test
    fun blendWithIdentity_halfwayLerpsEachEntry() {
        val half = blendWithIdentity(filter, 0.5f)
        assertContentEquals(floatArrayOf(1.5f, 0f, 0f, 0f, 5f), half.copyOfRange(0, 5))
        assertContentEquals(floatArrayOf(0.25f, 0f, 0.5f, 0f, 0f), half.copyOfRange(10, 15))
    }

    @Test
    fun blendWithIdentity_clampsIntensity() {
        assertContentEquals(filter, blendWithIdentity(filter, 3f))
        assertContentEquals(identity, blendWithIdentity(filter, -1f))
    }

    @Test
    fun concatColorMatrices_identityIsNeutral() {
        assertContentEquals(filter, concatColorMatrices(identity, filter))
        assertContentEquals(filter, concatColorMatrices(filter, identity))
    }

    @Test
    fun concatColorMatrices_appliesFirstThenNext() {
        val doubleRed = floatArrayOf(
            2f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        val addRed = floatArrayOf(
            1f, 0f, 0f, 0f, 10f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        // Offset then scale doubles the offset too: 2 * (r + 10).
        assertContentEquals(
            floatArrayOf(2f, 0f, 0f, 0f, 20f),
            concatColorMatrices(addRed, doubleRed).copyOfRange(0, 5),
        )
        // Scale then offset leaves it alone: 2r + 10.
        assertContentEquals(
            floatArrayOf(2f, 0f, 0f, 0f, 10f),
            concatColorMatrices(doubleRed, addRed).copyOfRange(0, 5),
        )
    }
}
