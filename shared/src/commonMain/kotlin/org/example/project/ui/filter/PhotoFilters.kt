package org.example.project.ui.filter

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

/**
 * A selectable filter. [matrix] is `null` for the "None" option, which the filmstrip shows as an
 * icon rather than a photo thumbnail.
 *
 * An [isPremium] filter can be tried on the photo by anyone, but only a subscriber can apply it:
 * for everyone else Done opens the paywall instead.
 */
internal data class PhotoFilter(
    val label: String,
    val matrix: FloatArray?,
    val isNoneOption: Boolean = false,
    val isPremium: Boolean = false,
)

/** The filters only subscribers can apply, by label. */
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

internal val PhotoFilters: List<PhotoFilter> = listOf(
    PhotoFilter("None", matrix = null, isNoneOption = true),
    PhotoFilter("Vivid", saturationMatrix(1.45f)),
    // Warm highlights over teal-lifted shadows, the classic film-grade look.
    PhotoFilter(
        "Cinema",
        concatColorMatrices(
            contrastMatrix(1.12f),
            tintMatrix(1.06f, 1.0f, 0.88f),
            offsetMatrix(-4f, 2f, 14f),
        ),
    ),
    PhotoFilter(
        "Golden",
        concatColorMatrices(
            saturationMatrix(1.1f),
            tintMatrix(1.12f, 1.04f, 0.86f),
            offsetMatrix(8f, 4f, -6f),
        ),
    ),
    PhotoFilter(
        "Blush",
        concatColorMatrices(
            saturationMatrix(0.95f),
            tintMatrix(1.06f, 0.97f, 1.0f),
            offsetMatrix(14f, 4f, 10f),
        ),
    ),
    PhotoFilter(
        "Lush",
        concatColorMatrices(
            saturationMatrix(1.2f),
            tintMatrix(0.96f, 1.1f, 0.94f),
            contrastMatrix(1.05f),
        ),
    ),
    PhotoFilter(
        "Dusk",
        concatColorMatrices(
            tintMatrix(1.04f, 0.9f, 1.12f),
            contrastMatrix(1.05f),
            offsetMatrix(8f, 0f, 14f),
        ),
    ),
    PhotoFilter(
        "Arctic",
        concatColorMatrices(
            saturationMatrix(0.7f),
            tintMatrix(0.92f, 1.0f, 1.1f),
            offsetMatrix(0f, 6f, 14f),
        ),
    ),
    PhotoFilter("Warm", tintMatrix(1.18f, 1.02f, 0.82f)),
    PhotoFilter("Cool", tintMatrix(0.85f, 1.0f, 1.2f)),
    PhotoFilter("Chrome", concatColorMatrices(saturationMatrix(1.2f), contrastMatrix(1.2f))),
    PhotoFilter(
        "Lomo",
        concatColorMatrices(
            saturationMatrix(1.35f),
            contrastMatrix(1.3f),
            tintMatrix(1.05f, 1.02f, 0.9f),
        ),
    ),
    // Contrast below 1 lifts blacks and dims whites on its own, giving the flat matte finish.
    PhotoFilter("Matte", concatColorMatrices(saturationMatrix(0.85f), contrastMatrix(0.8f))),
    PhotoFilter(
        "Retro",
        concatColorMatrices(
            saturationMatrix(0.85f),
            tintMatrix(1.02f, 1.05f, 0.82f),
            offsetMatrix(16f, 14f, 8f),
        ),
    ),
    PhotoFilter(
        "Vintage",
        concatColorMatrices(
            blendWithIdentity(SepiaMatrix, 0.6f),
            contrastMatrix(0.9f),
            offsetMatrix(10f, 6f, 0f),
        ),
    ),
    PhotoFilter("Fade", fadeMatrix()),
    PhotoFilter("Sepia", SepiaMatrix),
    PhotoFilter("Mono", saturationMatrix(0f)),
    PhotoFilter(
        "Noir",
        concatColorMatrices(saturationMatrix(0f), contrastMatrix(1.4f), offsetMatrix(-8f, -8f, -8f)),
    ),
    // Duotones: collapse to gray first so the tint maps luminance onto a single hue.
    PhotoFilter(
        "Bronze",
        concatColorMatrices(
            saturationMatrix(0f),
            tintMatrix(1.12f, 0.96f, 0.78f),
            contrastMatrix(1.1f),
        ),
    ),
    PhotoFilter(
        "Cyano",
        concatColorMatrices(
            saturationMatrix(0f),
            tintMatrix(0.55f, 0.8f, 1.15f),
            offsetMatrix(0f, 10f, 30f),
        ),
    ),
).map { it.copy(isPremium = it.label in PremiumFilterLabels) }

/** The filter's color filter at [intensity] (0..1), or `null` when it would change nothing. */
internal fun PhotoFilter.toColorFilter(intensity: Float = 1f): ColorFilter? {
    val matrix = matrix ?: return null
    if (intensity <= 0f) return null
    return ColorFilter.colorMatrix(ColorMatrix(blendWithIdentity(matrix, intensity)))
}

/** Renders [source] through [filter] into a new bitmap, so the effect survives past this screen. */
internal fun bakeFilter(source: ImageBitmap, filter: PhotoFilter, intensity: Float): ImageBitmap {
    val colorFilter = filter.toColorFilter(intensity) ?: return source
    val output = ImageBitmap(source.width, source.height)
    Canvas(output).drawImage(source, Offset.Zero, Paint().apply { this.colorFilter = colorFilter })
    return output
}
