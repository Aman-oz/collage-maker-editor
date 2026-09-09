package org.example.project.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope

internal enum class OverlayCategory(val label: String) {
    Effect("Effect"),
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
