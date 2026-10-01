package org.example.project.ui.bgremover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundRemoverMathTest {

    private val white = 0xFFFFFFFF.toInt()
    private val red = 0xFFFF0000.toInt()

    /** A [size]×[size] image of [background] with a [subject]-coloured square from [from] to [to] (exclusive). */
    private fun squareImage(size: Int, from: Int, to: Int, background: Int = white, subject: Int = red) =
        IntArray(size * size) { i ->
            val x = i % size
            val y = i / size
            if (x in from until to && y in from until to) subject else background
        }

    @Test
    fun autoBackgroundMask_keepsSubjectAndCutsPlainBackground() {
        val keep = autoBackgroundMask(squareImage(10, 3, 7), 10, 10)
        assertFalse(keep[0])
        assertFalse(keep[2 * 10 + 2])
        assertTrue(keep[3 * 10 + 3])
        assertTrue(keep[6 * 10 + 6])
        assertEquals(16, keep.count { it })
    }

    @Test
    fun autoBackgroundMask_followsSmoothGradient() {
        // Each column is 10 levels darker than the previous: small steps, big total drift.
        val width = 12
        val pixels = IntArray(width * 3) { i ->
            val level = 255 - (i % width) * 10
            (0xFF shl 24) or (level shl 16) or (level shl 8) or level
        }
        val keep = autoBackgroundMask(pixels, width, 3, stepTolerance = 18, seedTolerance = 255)
        assertTrue(keep.none { it })
    }

    @Test
    fun autoBackgroundMask_seedToleranceStopsCreepingIntoSubject() {
        // White border; the middle row darkens by 10 per column from its white left edge.
        val width = 12
        val pixels = IntArray(width * 3) { i ->
            val x = i % width
            val y = i / width
            val level = if (y == 1 && x in 1 until width - 1) 255 - x * 10 else 255
            (0xFF shl 24) or (level shl 16) or (level shl 8) or level
        }
        val keep = autoBackgroundMask(pixels, width, 3, stepTolerance = 18, seedTolerance = 30)
        assertFalse(keep[width + 1])
        assertFalse(keep[width + 3])
        assertTrue(keep[width + 4])
        assertTrue(keep[width + 10])
    }

    @Test
    fun autoBackgroundMask_treatsTransparentPixelsAsBackground() {
        val pixels = squareImage(6, 1, 5, background = red, subject = 0)
        val keep = autoBackgroundMask(pixels, 6, 6)
        assertTrue(keep.none { it })
    }

    @Test
    fun autoMaskWorkingSize_downscalesLongerSideOnly() {
        assertEquals(512 to 256, autoMaskWorkingSize(2048, 1024))
        assertEquals(300 to 200, autoMaskWorkingSize(300, 200))
    }

    @Test
    fun brushRadiusFraction_shrinksWithZoomAndHasFloor() {
        assertEquals(0.1f, brushRadiusFraction(0.2f, 2f))
        assertEquals(MinBrushRadiusFraction, brushRadiusFraction(0f, 1f))
        assertEquals(0.2f / MaxZoom, brushRadiusFraction(0.2f, 50f))
    }

    @Test
    fun colorDistance_isLargestChannelDifference() {
        assertEquals(255, colorDistance(white, red))
        assertEquals(0, colorDistance(red, red))
    }

    @Test
    fun colorRegionMask_selectsOnlyTheTappedColourRegion() {
        val pixels = squareImage(10, 3, 7)
        val subject = colorRegionMask(pixels, 10, 10, seedX = 5, seedY = 5, tolerance = 20)
        assertEquals(16, subject.count { it })
        assertTrue(subject[4 * 10 + 4])
        assertFalse(subject[0])

        val background = colorRegionMask(pixels, 10, 10, seedX = 0, seedY = 0, tolerance = 20)
        assertEquals(84, background.count { it })
    }

    @Test
    fun colorRegionMask_staysContiguous() {
        // Two red squares separated by white: tapping one must not grab the other.
        val width = 9
        val pixels = IntArray(width * 3) { i -> if (i % width in 0..2 || i % width in 6..8) red else white }
        val region = colorRegionMask(pixels, width, 3, seedX = 0, seedY = 1, tolerance = 20)
        assertEquals(9, region.count { it })
        assertFalse(region[7])
    }

    @Test
    fun colorRegionMask_toleranceWidensTheRegion() {
        val width = 6
        // Brightness steps of 20 per column.
        val pixels = IntArray(width) { x -> val v = 255 - x * 20; (0xFF shl 24) or (v shl 16) or (v shl 8) or v }
        assertEquals(1, colorRegionMask(pixels, width, 1, 0, 0, tolerance = 10).count { it })
        assertEquals(3, colorRegionMask(pixels, width, 1, 0, 0, tolerance = 45).count { it })
    }

    @Test
    fun magicTolerance_mapsSliderEndsAndClamps() {
        assertEquals(MinMagicTolerance, magicTolerance(0f))
        assertEquals(MaxMagicTolerance, magicTolerance(1f))
        assertEquals(MaxMagicTolerance, magicTolerance(2f))
        assertTrue(magicTolerance(0.5f) in MinMagicTolerance..MaxMagicTolerance)
    }

    @Test
    fun zoomSlider_centreIsFitAndEndsAreLimits() {
        assertEquals(FitZoom, sliderToZoom(0f))
        assertEquals(MinZoom, sliderToZoom(-1f))
        assertEquals(MaxZoom, sliderToZoom(1f))
        assertEquals(MaxZoom, sliderToZoom(3f))
        assertTrue(sliderToZoom(-0.5f) < FitZoom)
        assertTrue(sliderToZoom(0.5f) > FitZoom)
    }

    @Test
    fun zoomToSlider_invertsSliderToZoom() {
        for (value in listOf(-1f, -0.4f, 0f, 0.25f, 1f)) {
            assertEquals(value, zoomToSlider(sliderToZoom(value)), 1e-5f)
        }
        assertEquals(1f, zoomToSlider(50f))
    }
}
