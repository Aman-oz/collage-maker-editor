package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.text.TextFontStyleOption

/** What a [FreestyleLayer] renders — a placed photo, sticker, or text label. */
sealed interface FreestyleContent {
    data class ImageContent(val image: ImageBitmap) : FreestyleContent
    data class StickerContent(val emoji: String) : FreestyleContent
    data class TextContent(val text: String, val color: Color, val font: TextFontStyleOption) : FreestyleContent
}

/**
 * One item freely placed on the freestyle canvas. [offsetFraction] is its center as a 0f..1f
 * fraction of the square canvas; [scale] is a multiplier of its own natural base size (a bitmap's
 * width, a sticker/text's font size — see [FreestyleImageBaseWidthFraction] and friends), so every
 * layer resizes consistently whether shown in the on-screen preview or baked at full resolution.
 * Layers later in [FreestyleState.layers] draw on top of earlier ones.
 */
data class FreestyleLayer(
    val id: Long,
    val content: FreestyleContent,
    val offsetFraction: Offset = Offset(0.5f, 0.5f),
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
)

/** Everything needed to render (and later bake) the freestyle canvas. */
data class FreestyleState(
    val layers: List<FreestyleLayer> = emptyList(),
    val backgroundColor: Color = Color(0xFF1C1C1E),
    val borderWidth: Float = 0f,
    val borderColor: Color = Color.White,
    val cornerRadius: Float = 0f,
)

/** An [FreestyleContent.ImageContent] layer's width at [FreestyleLayer.scale] == 1f, as a fraction of the canvas. */
internal const val FreestyleImageBaseWidthFraction = 0.42f

/** An [FreestyleContent.StickerContent] layer's font size at [FreestyleLayer.scale] == 1f. */
internal const val FreestyleStickerBaseSizeSp = 64f

/** An [FreestyleContent.TextContent] layer's font size at [FreestyleLayer.scale] == 1f. */
internal const val FreestyleTextBaseSizeSp = 40f

internal val FreestyleLayerScaleRange = 0.25f..5f
internal val FreestyleBorderWidthRange = 0f..30f
internal val FreestyleCornerRadiusRange = 0f..48f

internal val FreestyleBackgroundColors = listOf(
    Color(0xFF1C1C1E),
    Color.White,
    Color.Black,
    Color(0xFFEF4444),
    Color(0xFFFF8C42),
    Color(0xFFFFB300),
    Color(0xFF4CAF50),
    Color(0xFF14B8A6),
    Color(0xFF3B82F6),
    Color(0xFF9333EA),
    Color(0xFFEC4899),
)

internal val FreestyleBorderColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFC0C0C8),
    Color(0xFF8E8E93),
    Color(0xFFEF4444),
    Color(0xFFFF8C42),
    Color(0xFFFFB300),
    Color(0xFF4CAF50),
    Color(0xFF14B8A6),
    Color(0xFF3B82F6),
    Color(0xFF9333EA),
    Color(0xFFEC4899),
)
