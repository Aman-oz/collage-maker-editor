package org.example.project.ui.auto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoMathTest {

    private fun histogramOf(vararg values: Int): IntArray = IntArray(256).also { h -> values.forEach { h[it]++ } }

    @Test
    fun channelLevels_findsDarkestAndBrightestUsedValues() {
        assertEquals(ChannelLevels(40f, 200f), channelLevels(histogramOf(40, 100, 200), clipFraction = 0f))
    }

    @Test
    fun channelLevels_ignoresClippedOutliers() {
        // One stray black and one stray white pixel among 1000 mid-gray samples.
        val histogram = IntArray(256).also {
            it[0] = 1
            it[255] = 1
            it[100] = 500
            it[150] = 500
        }
        assertEquals(ChannelLevels(100f, 150f), channelLevels(histogram, clipFraction = 0.005f))
    }

    @Test
    fun channelLevels_emptyHistogramIsFullRange() {
        assertEquals(ChannelLevels(0f, 255f), channelLevels(IntArray(256)))
    }

    @Test
    fun levelsTransform_mapsLevelsOntoFullRange() {
        val t = levelsTransform(ChannelLevels(51f, 204f), maxGain = 10f)
        assertEquals(0f, 51f * t.gain + t.offset, 0.01f)
        assertEquals(255f, 204f * t.gain + t.offset, 0.01f)
    }

    @Test
    fun levelsTransform_capsGainAroundMidpoint() {
        val t = levelsTransform(ChannelLevels(120f, 140f), maxGain = 2f)
        assertEquals(2f, t.gain)
        assertEquals(127.5f, 130f * t.gain + t.offset, 0.01f)
    }

    @Test
    fun levelsTransform_flatChannelIsIdentity() {
        assertEquals(LevelsTransform(1f, 0f), levelsTransform(ChannelLevels(80f, 80f)))
    }

    @Test
    fun autoColorMatrix_keepsAlphaAndBrightensDullGray() {
        // A low-contrast gray image: every pixel either 100 or 150.
        val pixels = IntArray(100) { if (it % 2 == 0) 0xFF646464.toInt() else 0xFF969696.toInt() }
        val m = autoColorMatrix(pixels)
        assertEquals(1f, m[18])
        val darkOut = (m[0] + m[1] + m[2]) * 100f + m[4]
        val lightOut = (m[0] + m[1] + m[2]) * 150f + m[4]
        assertTrue(lightOut - darkOut > 50f, "contrast should be stretched, was ${lightOut - darkOut}")
    }
}
