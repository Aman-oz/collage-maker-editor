package org.example.project.ui.auto

/**
 * Share of the darkest and brightest pixels ignored when picking a channel's black and white
 * points, so a few specular highlights or noisy shadows don't pin the range and cancel the stretch.
 */
internal const val AutoClipFraction = 0.005f

/**
 * Upper bound on a channel's stretch. A nearly flat photo (fog, a white wall) would otherwise be
 * amplified into banding and noise.
 */
internal const val AutoMaxGain = 2.5f

/**
 * How much each channel uses its own levels (1f, which neutralizes color casts) versus the shared
 * luminance levels (0f, which only fixes contrast). Halfway removes most of a cast but keeps
 * intentional warmth like a sunset.
 */
internal const val AutoColorBalance = 0.5f

/** Light saturation boost applied after the levels stretch, so the result looks "enhanced". */
internal const val AutoSaturation = 1.1f

/** Black ([low]) and white ([high]) points of one channel, in 0..255. */
internal data class ChannelLevels(val low: Float, val high: Float)

/** Per-channel gain and offset that map [ChannelLevels] onto the full 0..255 range. */
internal data class LevelsTransform(val gain: Float, val offset: Float)

/**
 * Finds the value below which [clipFraction] of the samples fall (black point) and the value above
 * which [clipFraction] fall (white point). [histogram] has 256 buckets.
 */
internal fun channelLevels(histogram: IntArray, clipFraction: Float = AutoClipFraction): ChannelLevels {
    val total = histogram.sum()
    if (total == 0) return ChannelLevels(0f, 255f)
    val clip = (total * clipFraction).toInt()

    var low = 0
    var seen = 0
    while (low < 255) {
        seen += histogram[low]
        if (seen > clip) break
        low++
    }
    var high = 255
    seen = 0
    while (high > 0) {
        seen += histogram[high]
        if (seen > clip) break
        high--
    }
    return ChannelLevels(low.toFloat(), high.toFloat())
}

/**
 * Stretches [levels] to 0..255. When the stretch would exceed [maxGain], the gain is capped and
 * the range stays centered on its midpoint, rather than anchored at black, so the image doesn't
 * drift darker or lighter.
 */
internal fun levelsTransform(levels: ChannelLevels, maxGain: Float = AutoMaxGain): LevelsTransform {
    val range = levels.high - levels.low
    if (range <= 0f) return LevelsTransform(1f, 0f)
    val gain = 255f / range
    if (gain <= maxGain) return LevelsTransform(gain, -levels.low * gain)
    val mid = (levels.low + levels.high) / 2f
    return LevelsTransform(maxGain, 127.5f - mid * maxGain)
}

/**
 * Builds a 4x5 color matrix (the `ColorMatrix` layout) that auto-corrects the image sampled in
 * [argbPixels] (packed `0xAARRGGBB`, as `ImageBitmap.readPixels` returns them). It does an
 * auto-levels stretch per channel, blended with luminance levels by [AutoColorBalance], followed by
 * an [AutoSaturation] boost. Fully transparent pixels are ignored.
 */
internal fun autoColorMatrix(argbPixels: IntArray): FloatArray {
    val red = IntArray(256)
    val green = IntArray(256)
    val blue = IntArray(256)
    val luma = IntArray(256)
    for (pixel in argbPixels) {
        if (pixel ushr 24 == 0) continue
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        red[r]++
        green[g]++
        blue[b]++
        luma[((r * 54 + g * 183 + b * 19) shr 8).coerceIn(0, 255)]++
    }

    val lumaLevels = channelLevels(luma)
    val transforms = listOf(red, green, blue).map { histogram ->
        val own = channelLevels(histogram)
        levelsTransform(
            ChannelLevels(
                low = lerp(lumaLevels.low, own.low, AutoColorBalance),
                high = lerp(lumaLevels.high, own.high, AutoColorBalance),
            ),
        )
    }
    return saturationAfterLevels(transforms, AutoSaturation)
}

/**
 * Composes `saturation ∘ levels` into one matrix: row i of the result is `S[i] · (gain ⊙ in + offset)`,
 * which folds the per-channel gains into the coefficients and the offsets into the constant column.
 */
private fun saturationAfterLevels(levels: List<LevelsTransform>, saturation: Float): FloatArray {
    val lum = floatArrayOf(0.213f, 0.715f, 0.072f)
    val result = FloatArray(20)
    for (row in 0 until 3) {
        var constant = 0f
        for (col in 0 until 3) {
            val s = lum[col] * (1f - saturation) + if (row == col) saturation else 0f
            result[row * 5 + col] = s * levels[col].gain
            constant += s * levels[col].offset
        }
        result[row * 5 + 4] = constant
    }
    result[18] = 1f
    return result
}

private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
