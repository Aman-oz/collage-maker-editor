package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import org.example.project.ui.text.TextFontStyleOption

/** What a [FreestyleLayer] renders — a placed photo, sticker, or text label. */
sealed interface FreestyleContent {
    data class ImageContent(val image: ImageBitmap) : FreestyleContent
    data class StickerContent(val emoji: String) : FreestyleContent
    /** [background] is the plate drawn behind the words (see [org.example.project.ui.text.drawTextPlate]); null for none. */
    data class TextContent(
        val text: String,
        val fill: FreestyleFill,
        val font: TextFontStyleOption,
        val background: FreestyleFill? = null,
    ) : FreestyleContent
}

/**
 * One item freely placed on the freestyle canvas. [offsetFraction] is its center as 0f..1f
 * fractions of the canvas width and height; [scale] is a multiplier of its own natural base size (a
 * bitmap's width, a sticker/text's font size — see [FreestyleImageBaseWidthFraction] and friends), so every
 * layer resizes consistently whether shown in the on-screen preview or baked at full resolution.
 * Layers later in [FreestyleState.layers] draw on top of earlier ones.
 *
 * [borderWidth] (a white frame) and [cornerRadius] only apply to [FreestyleContent.ImageContent]
 * layers. Both are dp in the on-screen preview and deliberately don't grow with [scale], so a
 * photo keeps the same frame however large it's pinched.
 */
data class FreestyleLayer(
    val id: Long,
    val content: FreestyleContent,
    val offsetFraction: Offset = Offset(0.5f, 0.5f),
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val borderWidth: Float = FreestyleDefaultBorderWidth,
    val cornerRadius: Float = FreestyleDefaultCornerRadius,
)

/** A flat colour or a gradient: what fills the canvas behind the layers, and what a text label is drawn in. */
sealed interface FreestyleFill {
    data class Solid(val color: Color) : FreestyleFill

    /** Runs top-left → bottom-right. */
    data class Gradient(val colors: List<Color>) : FreestyleFill

    /**
     * The fill for a `background(...)`, a `drawRect(...)` or a `TextStyle`. The gradient has no fixed
     * end point, so it spans whatever it is drawn into: the preview and the bake get the same
     * diagonal across the canvas, or across a label's own box.
     */
    val brush: Brush
        get() = when (this) {
            is Solid -> SolidColor(color)
            is Gradient -> Brush.linearGradient(colors)
        }
}

/** Everything needed to render (and later bake) the freestyle canvas. */
data class FreestyleState(
    val layers: List<FreestyleLayer> = emptyList(),
    val background: FreestyleFill = FreestyleFill.Solid(FreestyleBackgroundColors[1]),
)

/** An [FreestyleContent.ImageContent] layer's width at [FreestyleLayer.scale] == 1f, as a fraction of the canvas width. */
internal const val FreestyleImageBaseWidthFraction = 0.42f

/** An [FreestyleContent.StickerContent] layer's font size at [FreestyleLayer.scale] == 1f. */
internal const val FreestyleStickerBaseSizeSp = 64f

/** An [FreestyleContent.TextContent] layer's font size at [FreestyleLayer.scale] == 1f. */
internal const val FreestyleTextBaseSizeSp = 40f

internal val FreestyleLayerScaleRange = 0.25f..5f
internal val FreestyleBorderWidthRange = 0f..16f
internal val FreestyleCornerRadiusRange = 0f..40f
internal const val FreestyleDefaultBorderWidth = 3f
internal const val FreestyleDefaultCornerRadius = 10f
internal val FreestyleImageBorderColor = Color.White

/**
 * Background swatches. The Background panel is a two-row horizontal grid that fills column by
 * column, so the list is ordered as (top, bottom) pairs.
 */
internal val FreestyleBackgroundColors = listOf(
    Color.White, Color(0xFFE5E5EA),
    Color(0xFF1F2933), Color.Black,
    Color(0xFF1A8FD1), Color(0xFF136C94),
    Color(0xFF14204F), Color(0xFF050B4A),
    Color(0xFFF25565), Color(0xFFCB1F2E),
    Color(0xFF9747F5), Color(0xFFA35CF0),
    Color(0xFFF6AE45), Color(0xFFC3BB91),
    Color(0xFF4CAF50), Color(0xFF14B8A6),
    Color(0xFFEC4899), Color(0xFFFFB300),
)

/** Gradient presets for the Background panel's Gradient tab, in the same (top, bottom) pair order. */
internal val FreestyleBackgroundGradients = listOf(
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)), listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)), listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)), listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)), listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFFFCCB90), Color(0xFFD57EEB)), listOf(Color(0xFFF83600), Color(0xFFF9D423)),
    listOf(Color(0xFFFFE259), Color(0xFFFFA751)), listOf(Color(0xFFEE0979), Color(0xFFFF6A00)),
    listOf(Color(0xFFE0EAFC), Color(0xFFCFDEF3)), listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
)

/** Gradient presets for a text label, listed after the flat colours in the Text panel's colour row. */
internal val FreestyleTextGradients = listOf(
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFF83600), Color(0xFFF9D423)),
    listOf(Color(0xFFEE0979), Color(0xFFFF6A00)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
)
