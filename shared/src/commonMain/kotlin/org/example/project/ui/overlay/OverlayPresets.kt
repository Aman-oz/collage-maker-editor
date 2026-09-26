package org.example.project.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sqrt

internal enum class OverlayCategory(val label: String) {
    Effect("Effect"),
    Colorful("Color"),
    Hardmix("Hardmix"),
    Dodge("Dodge"),
    Burn("Burn"),
    Divide("Divide"),
}

/**
 * A composited overlay effect (as opposed to Filter/Adjust, which are per-pixel color matrices).
 * [draw] renders the effect's shapes/gradients using the [blendMode] supplied at the call site —
 * the real [naturalBlendMode] for the live preview and the baked result, or [BlendMode.SrcOver]
 * for a flat, readable thumbnail swatch.
 */
internal data class OverlayPreset(
    val category: OverlayCategory,
    val label: String,
    val naturalBlendMode: BlendMode,
    val draw: DrawScope.(intensity: Float, blendMode: BlendMode) -> Unit,
)

private fun DrawScope.radialSpot(color: Color, center: Offset, radius: Float, blendMode: BlendMode, intensity: Float) {
    drawCircle(
        brush = Brush.radialGradient(colors = listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
        alpha = intensity,
        blendMode = blendMode,
    )
}

private fun DrawScope.diagonalWash(from: Color, to: Color, blendMode: BlendMode, intensity: Float) {
    drawRect(
        brush = Brush.linearGradient(colors = listOf(from, to), start = Offset.Zero, end = Offset(size.width, size.height)),
        alpha = intensity,
        blendMode = blendMode,
    )
}

private fun DrawScope.stripeWash(color: Color, spacing: Float, diagonal: Boolean, blendMode: BlendMode, intensity: Float) {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(color, Color.Transparent, color, Color.Transparent),
            start = Offset.Zero,
            end = if (diagonal) Offset(spacing, spacing) else Offset(0f, spacing),
            tileMode = TileMode.Repeated,
        ),
        alpha = intensity,
        blendMode = blendMode,
    )
}

private fun DrawScope.vignette(color: Color, strengthFraction: Float, blendMode: BlendMode, intensity: Float) {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, color),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * strengthFraction,
        ),
        alpha = intensity,
        blendMode = blendMode,
    )
}

private fun DrawScope.gradientWash(colors: List<Color>, start: Offset, end: Offset, blendMode: BlendMode, intensity: Float) {
    drawRect(
        brush = Brush.linearGradient(colors = colors, start = start, end = end),
        alpha = intensity,
        blendMode = blendMode,
    )
}

/**
 * Scatters [count] colored dots over the canvas. Positions come from a fixed LCG seed rather than
 * `Random`, so the live preview, the thumbnail and the baked bitmap all get the same layout.
 */
private fun DrawScope.confetti(colors: List<Color>, count: Int, maxRadiusFraction: Float, blendMode: BlendMode, intensity: Float) {
    var seed = 0x2545F491L
    fun next(): Float {
        seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
        return seed / 0x7FFFFFFF.toFloat()
    }
    repeat(count) { index ->
        val center = Offset(next() * size.width, next() * size.height)
        val radius = size.minDimension * maxRadiusFraction * (0.3f + next() * 0.7f)
        radialSpot(colors[index % colors.size], center, radius, blendMode, intensity)
    }
}

/** A grid of dots that grow toward the bottom-left corner — a classic comic halftone. */
private fun DrawScope.halftone(color: Color, cellsAcross: Int, blendMode: BlendMode, intensity: Float) {
    val cell = size.width / cellsAcross
    val diagonal = sqrt(size.width * size.width + size.height * size.height)
    var y = cell / 2f
    while (y < size.height) {
        var x = cell / 2f
        while (x < size.width) {
            // 0 at the top-right corner, 1 at the bottom-left.
            val t = sqrt((size.width - x) * (size.width - x) + y * y) / diagonal
            drawCircle(color = color, radius = cell * 0.5f * t, center = Offset(x, y), alpha = intensity, blendMode = blendMode)
            x += cell
        }
        y += cell
    }
}

internal val OverlayPresets: List<OverlayPreset> = listOf(
    // Effect — mixed light/texture overlays.
    OverlayPreset(OverlayCategory.Effect, "Light Leak", BlendMode.Screen) { i, bm ->
        radialSpot(Color(0xFFFFB74D), Offset(size.width * 0.8f, size.height * 0.15f), size.minDimension * 0.5f, bm, i)
    },
    OverlayPreset(OverlayCategory.Effect, "Grain", BlendMode.Overlay) { i, bm ->
        stripeWash(Color.White, 6f, diagonal = false, bm, i * 0.6f)
    },
    OverlayPreset(OverlayCategory.Effect, "Dust", BlendMode.Screen) { i, bm ->
        radialSpot(Color.White, Offset(size.width * 0.25f, size.height * 0.3f), size.minDimension * 0.04f, bm, i)
        radialSpot(Color.White, Offset(size.width * 0.55f, size.height * 0.25f), size.minDimension * 0.03f, bm, i)
        radialSpot(Color.White, Offset(size.width * 0.4f, size.height * 0.55f), size.minDimension * 0.025f, bm, i)
    },
    OverlayPreset(OverlayCategory.Effect, "Bokeh", BlendMode.Screen) { i, bm ->
        radialSpot(Color.White.copy(alpha = 0.6f), Offset(size.width * 0.2f, size.height * 0.2f), size.minDimension * 0.18f, bm, i)
        radialSpot(Color.White, Offset(size.width * 0.7f, size.height * 0.65f), size.minDimension * 0.1f, bm, i)
    },
    OverlayPreset(OverlayCategory.Effect, "Glow", BlendMode.Screen) { i, bm ->
        radialSpot(Color.White, Offset(size.width * 0.5f, size.height * 0.4f), size.maxDimension * 0.5f, bm, i)
    },
    OverlayPreset(OverlayCategory.Effect, "Vintage", BlendMode.Softlight) { i, bm ->
        vignette(Color(0xFF4E342E), 0.9f, bm, i * 0.7f)
    },
    OverlayPreset(OverlayCategory.Effect, "Film", BlendMode.Multiply) { i, bm ->
        vignette(Color(0xFF102027), 0.75f, bm, i * 0.6f)
    },
    OverlayPreset(OverlayCategory.Effect, "Texture", BlendMode.Overlay) { i, bm ->
        stripeWash(Color.White, 8f, diagonal = true, bm, i * 0.5f)
    },

    // Color — vivid multi-color washes and patterns.
    OverlayPreset(OverlayCategory.Colorful, "Rainbow", BlendMode.Softlight) { i, bm ->
        gradientWash(
            listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFD500F9)),
            Offset.Zero, Offset(size.width, size.height), bm, i,
        )
    },
    OverlayPreset(OverlayCategory.Colorful, "Sunset", BlendMode.Overlay) { i, bm ->
        gradientWash(listOf(Color(0xFFFFD54F), Color(0xFFFF7043), Color(0xFFD81B60), Color(0xFF4A148C)), Offset.Zero, Offset(0f, size.height), bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Aurora", BlendMode.Screen) { i, bm ->
        gradientWash(
            listOf(Color.Transparent, Color(0xFF00E5FF), Color(0xFF76FF03), Color(0xFFD500F9), Color.Transparent),
            Offset(0f, size.height * 0.1f), Offset(size.width, size.height * 0.6f), bm, i * 0.8f,
        )
    },
    OverlayPreset(OverlayCategory.Colorful, "Neon", BlendMode.Screen) { i, bm ->
        radialSpot(Color(0xFFFF00E5), Offset(0f, 0f), size.maxDimension * 0.7f, bm, i)
        radialSpot(Color(0xFF00E5FF), Offset(size.width, size.height), size.maxDimension * 0.7f, bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Candy", BlendMode.Softlight) { i, bm ->
        gradientWash(listOf(Color(0xFFFF80AB), Color(0xFFB388FF), Color(0xFF80D8FF)), Offset(size.width, 0f), Offset(0f, size.height), bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Tropical", BlendMode.Overlay) { i, bm ->
        gradientWash(listOf(Color(0xFF00BFA5), Color(0xFFFFEB3B), Color(0xFFFF6D00)), Offset.Zero, Offset(size.width, 0f), bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Ocean", BlendMode.Softlight) { i, bm ->
        gradientWash(listOf(Color(0xFF18FFFF), Color(0xFF2962FF), Color(0xFF1A237E)), Offset.Zero, Offset(0f, size.height), bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Holo", BlendMode.Screen) { i, bm ->
        stripeWash(Color(0xFFFF80AB), size.minDimension * 0.5f, diagonal = true, bm, i * 0.5f)
        gradientWash(listOf(Color(0xFF84FFFF), Color.Transparent, Color(0xFFEA80FC)), Offset.Zero, Offset(size.width, size.height), bm, i * 0.6f)
    },
    OverlayPreset(OverlayCategory.Colorful, "Cosmic", BlendMode.Screen) { i, bm ->
        radialSpot(Color(0xFF7C4DFF), Offset(size.width * 0.3f, size.height * 0.3f), size.maxDimension * 0.45f, bm, i)
        radialSpot(Color(0xFFFF4081), Offset(size.width * 0.75f, size.height * 0.7f), size.maxDimension * 0.4f, bm, i)
        confetti(listOf(Color.White), 24, 0.015f, bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Confetti", BlendMode.Screen) { i, bm ->
        confetti(
            listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF9100)),
            40, 0.05f, bm, i,
        )
    },
    OverlayPreset(OverlayCategory.Colorful, "Halftone", BlendMode.Overlay) { i, bm ->
        halftone(Color(0xFFFF5722), 18, bm, i)
    },
    OverlayPreset(OverlayCategory.Colorful, "Prism Leak", BlendMode.Screen) { i, bm ->
        radialSpot(Color(0xFFFF1744), Offset(0f, size.height * 0.2f), size.minDimension * 0.5f, bm, i)
        radialSpot(Color(0xFFFFEA00), Offset(0f, size.height * 0.5f), size.minDimension * 0.45f, bm, i * 0.8f)
        radialSpot(Color(0xFF00B0FF), Offset(0f, size.height * 0.8f), size.minDimension * 0.5f, bm, i)
    },

    // Hardmix — vivid two-color diagonal washes.
    OverlayPreset(OverlayCategory.Hardmix, "Crimson", BlendMode.Hardlight) { i, bm ->
        diagonalWash(Color(0xFFE53935), Color(0xFF1A0000), bm, i)
    },
    OverlayPreset(OverlayCategory.Hardmix, "Cyan Split", BlendMode.Hardlight) { i, bm ->
        diagonalWash(Color(0xFF29B6F6), Color(0xFFD50000), bm, i)
    },
    OverlayPreset(OverlayCategory.Hardmix, "Amber Cut", BlendMode.Hardlight) { i, bm ->
        diagonalWash(Color(0xFFFFA000), Color(0xFF1A0F00), bm, i)
    },
    OverlayPreset(OverlayCategory.Hardmix, "Violet Mix", BlendMode.Hardlight) { i, bm ->
        diagonalWash(Color(0xFF7C4DFF), Color(0xFF1A0033), bm, i)
    },
    OverlayPreset(OverlayCategory.Hardmix, "Toxic", BlendMode.Hardlight) { i, bm ->
        diagonalWash(Color(0xFF8BC34A), Color(0xFF1B2A00), bm, i)
    },

    // Dodge — bright, lightening effects.
    OverlayPreset(OverlayCategory.Dodge, "Sunrise", BlendMode.ColorDodge) { i, bm ->
        radialSpot(Color(0xFFFFEB3B), Offset(size.width * 0.5f, size.height * 0.75f), size.maxDimension * 0.5f, bm, i)
    },
    OverlayPreset(OverlayCategory.Dodge, "Soft Beam", BlendMode.ColorDodge) { i, bm ->
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White, Color.Transparent),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            alpha = i,
            blendMode = bm,
        )
    },
    OverlayPreset(OverlayCategory.Dodge, "Halo", BlendMode.ColorDodge) { i, bm ->
        radialSpot(Color.White, Offset(size.width * 0.4f, size.height * 0.35f), size.minDimension * 0.3f, bm, i)
    },
    OverlayPreset(OverlayCategory.Dodge, "Flare", BlendMode.ColorDodge) { i, bm ->
        radialSpot(Color(0xFFFFF176), Offset(size.width * 0.25f, size.height * 0.25f), size.minDimension * 0.12f, bm, i)
        radialSpot(Color.White, Offset(size.width * 0.7f, size.height * 0.6f), size.minDimension * 0.08f, bm, i)
    },

    // Burn — dark, darkening vignette-based effects.
    OverlayPreset(OverlayCategory.Burn, "Shadow", BlendMode.ColorBurn) { i, bm ->
        vignette(Color.Black, 0.9f, bm, i)
    },
    OverlayPreset(OverlayCategory.Burn, "Vignette", BlendMode.ColorBurn) { i, bm ->
        vignette(Color.Black, 0.65f, bm, i)
    },
    OverlayPreset(OverlayCategory.Burn, "Ember", BlendMode.ColorBurn) { i, bm ->
        vignette(Color.Black, 0.85f, bm, i)
        radialSpot(Color(0xFFFF7043), Offset(size.width * 0.75f, size.height * 0.25f), size.minDimension * 0.15f, BlendMode.Screen, i * 0.6f)
    },
    OverlayPreset(OverlayCategory.Burn, "Charcoal", BlendMode.ColorBurn) { i, bm ->
        vignette(Color(0xFF37474F), 0.8f, bm, i)
    },

    // Divide — surreal, color-clashing effects.
    OverlayPreset(OverlayCategory.Divide, "Split", BlendMode.Difference) { i, bm ->
        diagonalWash(Color(0xFF1E88E5), Color(0xFFFB8C00), bm, i)
    },
    OverlayPreset(OverlayCategory.Divide, "Duotone", BlendMode.Difference) { i, bm ->
        diagonalWash(Color(0xFFD81B60), Color(0xFF4A148C), bm, i)
    },
    OverlayPreset(OverlayCategory.Divide, "Fracture", BlendMode.Difference) { i, bm ->
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00BFA5), Color(0xFFD50000), Color(0xFF00BFA5)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            alpha = i,
            blendMode = bm,
        )
    },
    OverlayPreset(OverlayCategory.Divide, "Prism", BlendMode.Difference) { i, bm ->
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            alpha = i * 0.7f,
            blendMode = bm,
        )
    },
)
