package org.example.project.ui.draw

import androidx.compose.ui.graphics.Color

/**
 * One selectable mosaic style: the actual pixelation is driven by [cellFractionX]/[cellFractionY]
 * (each cell's width/height as a fraction of the image's own width — see [computeMosaicBitmap]),
 * while [previewColors] is a small, deliberately vibrant palette used only to draw the swatch
 * icon, so the picker itself reads as colorful rather than a flat gray checkerboard.
 */
internal data class MosaicPattern(
    val label: String,
    val cellFractionX: Float,
    val cellFractionY: Float,
    val previewColors: List<Color>,
)

private val RainbowPalette = listOf(
    Color(0xFFFF5A5F),
    Color(0xFFFF9F45),
    Color(0xFFFFD23F),
    Color(0xFF3DDC97),
    Color(0xFF3AB0FF),
    Color(0xFFB983FF),
)

private val SunsetPalette = listOf(
    Color(0xFFFF6B6B),
    Color(0xFFFFA36B),
    Color(0xFFFFD56B),
    Color(0xFFFF8FA3),
)

private val OceanPalette = listOf(
    Color(0xFF0EA5E9),
    Color(0xFF22D3EE),
    Color(0xFF14B8A6),
    Color(0xFF6366F1),
)

internal val MosaicPatterns: List<MosaicPattern> = listOf(
    MosaicPattern("Fine", cellFractionX = 0.018f, cellFractionY = 0.018f, previewColors = RainbowPalette),
    MosaicPattern("Blocks", cellFractionX = 0.045f, cellFractionY = 0.045f, previewColors = SunsetPalette),
    MosaicPattern("Large", cellFractionX = 0.085f, cellFractionY = 0.085f, previewColors = OceanPalette),
    MosaicPattern("H-Stripes", cellFractionX = 0.01f, cellFractionY = 0.06f, previewColors = RainbowPalette),
    MosaicPattern("V-Stripes", cellFractionX = 0.06f, cellFractionY = 0.01f, previewColors = SunsetPalette),
    MosaicPattern("Bricks", cellFractionX = 0.03f, cellFractionY = 0.06f, previewColors = OceanPalette),
)

internal val MosaicPatternDefault: MosaicPattern = MosaicPatterns.first()
