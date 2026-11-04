package org.example.project.ui.filter

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * A colour-matrix look: the Stickers tool's Effects, which redraw live with every sticker and so
 * need a `ColorFilter`. (The Filter tool's filters are [PhotoFilter]s, which can't be one.) [matrix]
 * is `null` for the "None" option, which the filmstrip shows as an icon rather than a thumbnail.
 *
 * An [isPremium] effect can be tried by anyone, but only a subscriber can apply it.
 */
internal data class ColorMatrixFilter(
    val label: String,
    val matrix: FloatArray?,
    val isNoneOption: Boolean = false,
    val isPremium: Boolean = false,
)

/** The effects only subscribers can apply, by label. */
private val PremiumFilterLabels = setOf("Cinema", "Lush", "Cool", "Lomo", "Sepia")

private fun saturationMatrix(saturation: Float): FloatArray {
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    val invSat = 1f - saturation
    val r = lumR * invSat
    val g = lumG * invSat
    val b = lumB * invSat
    return floatArrayOf(
        r + saturation, g, b, 0f, 0f,
        r, g + saturation, b, 0f, 0f,
        r, g, b + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun tintMatrix(redMul: Float, greenMul: Float, blueMul: Float): FloatArray = floatArrayOf(
    redMul, 0f, 0f, 0f, 0f,
    0f, greenMul, 0f, 0f, 0f,
    0f, 0f, blueMul, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private val SepiaMatrix = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** Washed-out look: desaturate a little and lift the shadows. */
private fun fadeMatrix(): FloatArray = saturationMatrix(0.75f).also { m ->
    m[4] += 18f
    m[9] += 18f
    m[14] += 18f
}

/** Scales each channel around mid-gray, so `> 1` deepens blacks and whites and `< 1` flattens. */
private fun contrastMatrix(contrast: Float): FloatArray {
    val offset = 127.5f * (1f - contrast)
    return floatArrayOf(
        contrast, 0f, 0f, 0f, offset,
        0f, contrast, 0f, 0f, offset,
        0f, 0f, contrast, 0f, offset,
        0f, 0f, 0f, 1f, 0f,
    )
}

/**
 * Adds a constant (0..255 scale) to each channel. Used after the other steps to tint the shadows,
 * since an offset shows most where the channel is otherwise near zero.
 */
private fun offsetMatrix(red: Float, green: Float, blue: Float): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f, red,
    0f, 1f, 0f, 0f, green,
    0f, 0f, 1f, 0f, blue,
    0f, 0f, 0f, 1f, 0f,
)

internal val ColorMatrixFilters: List<ColorMatrixFilter> = listOf(
    ColorMatrixFilter("None", matrix = null, isNoneOption = true),
    ColorMatrixFilter("Vivid", saturationMatrix(1.45f)),
    // Warm highlights over teal-lifted shadows, the classic film-grade look.
    ColorMatrixFilter(
        "Cinema",
        concatColorMatrices(
            contrastMatrix(1.12f),
            tintMatrix(1.06f, 1.0f, 0.88f),
            offsetMatrix(-4f, 2f, 14f),
        ),
    ),
    ColorMatrixFilter(
        "Golden",
        concatColorMatrices(
            saturationMatrix(1.1f),
            tintMatrix(1.12f, 1.04f, 0.86f),
            offsetMatrix(8f, 4f, -6f),
        ),
    ),
    ColorMatrixFilter(
        "Blush",
        concatColorMatrices(
            saturationMatrix(0.95f),
            tintMatrix(1.06f, 0.97f, 1.0f),
            offsetMatrix(14f, 4f, 10f),
        ),
    ),
    ColorMatrixFilter(
        "Lush",
        concatColorMatrices(
            saturationMatrix(1.2f),
            tintMatrix(0.96f, 1.1f, 0.94f),
            contrastMatrix(1.05f),
        ),
    ),
    ColorMatrixFilter(
        "Dusk",
        concatColorMatrices(
            tintMatrix(1.04f, 0.9f, 1.12f),
            contrastMatrix(1.05f),
            offsetMatrix(8f, 0f, 14f),
        ),
    ),
    ColorMatrixFilter(
        "Arctic",
        concatColorMatrices(
            saturationMatrix(0.7f),
            tintMatrix(0.92f, 1.0f, 1.1f),
            offsetMatrix(0f, 6f, 14f),
        ),
    ),
    ColorMatrixFilter("Warm", tintMatrix(1.18f, 1.02f, 0.82f)),
    ColorMatrixFilter("Cool", tintMatrix(0.85f, 1.0f, 1.2f)),
    ColorMatrixFilter("Chrome", concatColorMatrices(saturationMatrix(1.2f), contrastMatrix(1.2f))),
    ColorMatrixFilter(
        "Lomo",
        concatColorMatrices(
            saturationMatrix(1.35f),
            contrastMatrix(1.3f),
            tintMatrix(1.05f, 1.02f, 0.9f),
        ),
    ),
    // Contrast below 1 lifts blacks and dims whites on its own, giving the flat matte finish.
    ColorMatrixFilter("Matte", concatColorMatrices(saturationMatrix(0.85f), contrastMatrix(0.8f))),
    ColorMatrixFilter(
        "Retro",
        concatColorMatrices(
            saturationMatrix(0.85f),
            tintMatrix(1.02f, 1.05f, 0.82f),
            offsetMatrix(16f, 14f, 8f),
        ),
    ),
    ColorMatrixFilter(
        "Vintage",
        concatColorMatrices(
            blendWithIdentity(SepiaMatrix, 0.6f),
            contrastMatrix(0.9f),
            offsetMatrix(10f, 6f, 0f),
        ),
    ),
    ColorMatrixFilter("Fade", fadeMatrix()),
    ColorMatrixFilter("Sepia", SepiaMatrix),
    ColorMatrixFilter("Mono", saturationMatrix(0f)),
    ColorMatrixFilter(
        "Noir",
        concatColorMatrices(saturationMatrix(0f), contrastMatrix(1.4f), offsetMatrix(-8f, -8f, -8f)),
    ),
    // Duotones: collapse to gray first so the tint maps luminance onto a single hue.
    ColorMatrixFilter(
        "Bronze",
        concatColorMatrices(
            saturationMatrix(0f),
            tintMatrix(1.12f, 0.96f, 0.78f),
            contrastMatrix(1.1f),
        ),
    ),
    ColorMatrixFilter(
        "Cyano",
        concatColorMatrices(
            saturationMatrix(0f),
            tintMatrix(0.55f, 0.8f, 1.15f),
            offsetMatrix(0f, 10f, 30f),
        ),
    ),
).map { it.copy(isPremium = it.label in PremiumFilterLabels) }

/** The filter's color filter at [intensity] (0..1), or `null` when it would change nothing. */
internal fun ColorMatrixFilter.toColorFilter(intensity: Float = 1f): ColorFilter? {
    val matrix = matrix ?: return null
    if (intensity <= 0f) return null
    return ColorFilter.colorMatrix(ColorMatrix(blendWithIdentity(matrix, intensity)))
}
