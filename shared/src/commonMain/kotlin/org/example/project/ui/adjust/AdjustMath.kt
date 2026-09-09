package org.example.project.ui.adjust

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

internal enum class AdjustmentType(val label: String, val icon: ImageVector) {
    Brightness("Brightness", Icons.Filled.WbSunny),
    Contrast("Contrast", Icons.Filled.Contrast),
    Saturation("Saturation", Icons.Filled.Opacity),
    Hue("Hue", Icons.Filled.Tonality),
    Sharpen("Sharpen", Icons.Filled.AutoAwesome),
    Exposure("Exposure", Icons.Filled.Exposure),
}

/** Each slider is -100..100; 0 is "no change" for every one of them. */
internal data class AdjustValues(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val hue: Float = 0f,
    val sharpen: Float = 0f,
    val exposure: Float = 0f,
) {
    operator fun get(type: AdjustmentType): Float = when (type) {
        AdjustmentType.Brightness -> brightness
        AdjustmentType.Contrast -> contrast
        AdjustmentType.Saturation -> saturation
        AdjustmentType.Hue -> hue
        AdjustmentType.Sharpen -> sharpen
        AdjustmentType.Exposure -> exposure
    }

    fun with(type: AdjustmentType, value: Float): AdjustValues = when (type) {
        AdjustmentType.Brightness -> copy(brightness = value)
        AdjustmentType.Contrast -> copy(contrast = value)
        AdjustmentType.Saturation -> copy(saturation = value)
        AdjustmentType.Hue -> copy(hue = value)
        AdjustmentType.Sharpen -> copy(sharpen = value)
        AdjustmentType.Exposure -> copy(exposure = value)
    }
}

internal val IdentityColorMatrix: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** Composes 4x5 affine color matrices: applies [inner] first, then [outer]. */
internal fun multiplyColorMatrices(outer: FloatArray, inner: FloatArray): FloatArray {
    val result = FloatArray(20)
    for (row in 0 until 4) {
        for (col in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) sum += outer[row * 5 + k] * inner[k * 5 + col]
            result[row * 5 + col] = sum
        }
        var translation = 0f
        for (k in 0 until 4) translation += outer[row * 5 + k] * inner[k * 5 + 4]
        translation += outer[row * 5 + 4]
        result[row * 5 + 4] = translation
    }
    return result
}

/** [value] -100..100 -> an additive RGB offset, roughly -255..255. */
internal fun brightnessMatrix(value: Float): FloatArray {
    val offset = value * 2.55f
    return floatArrayOf(
        1f, 0f, 0f, 0f, offset,
        0f, 1f, 0f, 0f, offset,
        0f, 0f, 1f, 0f, offset,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** [value] -100..100 -> a multiplicative RGB scale (0.5x..2x), simulating exposure stops. */
internal fun exposureMatrix(value: Float): FloatArray {
    val scale = 2f.pow(value / 100f)
    return floatArrayOf(
        scale, 0f, 0f, 0f, 0f,
        0f, scale, 0f, 0f, 0f,
        0f, 0f, scale, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** [value] -100..100 -> contrast factor 0x..2x, pivoted around mid-gray so it doesn't shift brightness. */
internal fun contrastMatrix(value: Float): FloatArray {
    val factor = 1f + value / 100f
    val translation = (1f - factor) * 127.5f
    return floatArrayOf(
        factor, 0f, 0f, 0f, translation,
        0f, factor, 0f, 0f, translation,
        0f, 0f, factor, 0f, translation,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** [value] -100..100 -> saturation factor 0x (grayscale)..2x (double). */
internal fun saturationAdjustMatrix(value: Float): FloatArray {
    val saturation = 1f + value / 100f
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

/** [value] -100..100 -> a -180..180 degree hue rotation (standard SVG/CSS hue-rotate matrix). */
internal fun hueRotateMatrix(value: Float): FloatArray {
    val degrees = value * 1.8f
    val radians = degrees * PI.toFloat() / 180f
    val cosA = cos(radians)
    val sinA = sin(radians)
    return floatArrayOf(
        0.213f + cosA * 0.787f - sinA * 0.213f,
        0.715f - cosA * 0.715f - sinA * 0.715f,
        0.072f - cosA * 0.072f + sinA * 0.928f,
        0f, 0f,

        0.213f - cosA * 0.213f + sinA * 0.143f,
        0.715f + cosA * 0.285f + sinA * 0.140f,
        0.072f - cosA * 0.072f - sinA * 0.283f,
        0f, 0f,

        0.213f - cosA * 0.213f - sinA * 0.787f,
        0.715f - cosA * 0.715f + sinA * 0.715f,
        0.072f + cosA * 0.928f + sinA * 0.072f,
        0f, 0f,

        0f, 0f, 0f, 1f, 0f,
    )
}

/**
 * Approximates "sharpen" as a mild, halved-strength contrast boost. A real unsharp-mask needs a
 * spatial convolution, which a per-pixel [ColorMatrix] can't express; this is a stand-in that at
 * least gives the slider a visible, sensible effect.
 */
internal fun sharpenApproxMatrix(value: Float): FloatArray = contrastMatrix(value * 0.5f)

internal fun AdjustValues.toColorMatrix(): FloatArray {
    var matrix = IdentityColorMatrix
    matrix = multiplyColorMatrices(exposureMatrix(exposure), matrix)
    matrix = multiplyColorMatrices(brightnessMatrix(brightness), matrix)
    matrix = multiplyColorMatrices(contrastMatrix(contrast), matrix)
    matrix = multiplyColorMatrices(saturationAdjustMatrix(saturation), matrix)
    matrix = multiplyColorMatrices(hueRotateMatrix(hue), matrix)
    matrix = multiplyColorMatrices(sharpenApproxMatrix(sharpen), matrix)
    return matrix
}

internal fun bakeAdjustments(source: ImageBitmap, matrix: FloatArray): ImageBitmap {
    val output = ImageBitmap(source.width, source.height)
    Canvas(output).drawImage(source, Offset.Zero, Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix(matrix)) })
    return output
}
