package org.example.project.ui.filter

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * A CPU port of the parts of CGE (wysaid's android-gpuimage-plus, which the LAS app's filters and
 * overlays run on) that those filters use. The filters are kept as CGE's own config strings
 * ("@adjust lut filters/filter_3.webp", "@curve R(0, 28)(23, 45)…", "@pixblend screen …"), so the
 * list reads like the LAS app's and a new one can be pasted straight in. Each command's maths follows
 * CGE's fragment shader for it. It runs on the CPU rather than as a shader because common Compose has
 * no shader API that works on both Android (before 13) and iOS.
 *
 * CGE renders every command into an 8-bit texture before the next one reads it, so each step here
 * clamps its result to 0..1 too: some chains (an inverting curve, then levels) depend on it.
 */

/** Packed `0xAARRGGBB` pixels, the layout `ImageBitmap.readPixels` gives. */
internal class PixelImage(val pixels: IntArray, val width: Int, val height: Int) {
    val longSide: Int get() = max(width, height)
}

/** One command of a filter. */
internal sealed interface FilterStep

/** A command that maps a colour to a colour, whatever the pixel's place: these are fused (see [runFilterSteps]). */
internal sealed interface ColorStep : FilterStep {
    /** Maps [c] (r, g, b in 0..1) in place. */
    fun apply(c: FloatArray, assets: FilterAssetLookup)
}

/** A command that needs the pixel's place or its neighbours. */
internal sealed interface ImageStep : FilterStep {
    fun apply(image: PixelImage, assets: FilterAssetLookup)
}

/** The LUT pictures and overlay textures a step reads, by their path under `files/`. */
internal fun interface FilterAssetLookup {
    fun image(path: String): PixelImage?
}

/** The pictures [steps] read, which the caller loads before [runFilterSteps]. */
internal fun filterAssetPaths(steps: List<FilterStep>): Set<String> = steps.mapNotNullTo(mutableSetOf()) {
    when (it) {
        is LutStep -> it.path
        is TextureBlendStep -> it.path
        else -> null
    }
}

/**
 * CGE's spatial measures (an edge's stride, a halftone dot) are in pixels of whatever it renders.
 * Here they are taken as pixels of a picture this long, and scaled to the real one, so the small
 * preview, the thumbnails and the full-size bake look alike.
 */
private const val ReferenceLongSide = 1000f

/** The colour-to-colour run is folded into a cube this many steps a side, then looked up per pixel. */
private const val CubeSize = 64

// region Parsing

/**
 * Parses a CGE filter string into its steps. Commands it doesn't know are skipped, so an unknown
 * one leaves the photo as it was rather than failing, as CGE does.
 */
internal fun parseFilterRecipe(recipe: String): List<FilterStep> =
    recipe.replace("#unpack", "")
        .split('@')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap(::parseCommand)

private fun parseCommand(command: String): List<FilterStep> {
    val name = command.substringBefore(' ')
    val rest = command.substringAfter(' ', "").trim()
    return when (name) {
        "adjust" -> listOfNotNull(parseAdjust(rest))
        "curve" -> listOf(parseCurve(rest))
        "pixblend" -> listOfNotNull(parsePixBlend(rest))
        "vigblend" -> listOfNotNull(parseVignetteBlend(rest))
        "colormul" -> listOfNotNull(parseColorMul(rest))
        "style" -> listOfNotNull(parseStyle(rest))
        "beautify" -> listOfNotNull(parseBeautify(rest))
        "krblend" -> listOfNotNull(parseTextureBlend(rest))
        else -> emptyList()
    }
}

private fun String.floats(): List<Float> = split(' ', ',').mapNotNull { it.trim().toFloatOrNull() }

private fun parseAdjust(text: String): FilterStep? {
    val kind = text.substringBefore(' ')
    val args = text.substringAfter(' ', "")
    val v = args.floats()
    return when (kind) {
        "lut" -> LutStep(args.trim())
        "saturation" -> v.getOrNull(0)?.let(::SaturationStep)
        "brightness" -> v.getOrNull(0)?.let(::BrightnessStep)
        "exposure" -> v.getOrNull(0)?.let(::ExposureStep)
        "level" -> if (v.size >= 3) LevelStep(v[0], v[1], v[2]) else null
        "hsl" -> if (v.size >= 3) HslStep(v[0], v[1], v[2]) else null
        "hsv" -> if (v.size >= 6) HsvStep(v.take(6).toFloatArray()) else null
        "colorbalance" -> if (v.size >= 3) ColorBalanceStep(v[0], v[1], v[2]) else null
        "shadowhighlight" -> if (v.size >= 2) ShadowHighlightStep(v[0], v[1]) else null
        "sharpen" -> v.getOrNull(0)?.let { SharpenStep(it, v.getOrElse(1) { 1f }) }
        else -> null
    }
}

/** `R(0, 28)(23, 45)G(…)B(…)RGB(…)`: a tone curve per channel and one over all three. */
private fun parseCurve(text: String): CurveStep {
    val channels = mutableMapOf<String, FloatArray>()
    Regex("""(RGB|R|G|B)\s*((?:\(\s*-?[\d.]+\s*,\s*-?[\d.]+\s*\)\s*)+)""").findAll(text).forEach { match ->
        val points = Regex("""\(\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\)""").findAll(match.groupValues[2])
            .map { it.groupValues[1].toFloat() to it.groupValues[2].toFloat() }
            .toList()
        channels[match.groupValues[1]] = toneCurve(points)
    }
    val all = channels["RGB"]
    fun merged(channel: String): FloatArray {
        val own = channels[channel] ?: IdentityCurve
        return if (all == null) own else FloatArray(256) { sampleCurve(all, own[it]) }
    }
    return CurveStep(merged("R"), merged("G"), merged("B"))
}

/** CGE's colours are 0..1, or 0..255 when any of them is past 1. */
private fun normalizedColor(values: List<Float>): FloatArray {
    val scale = if (values.any { it > 1f }) 1f / 255f else 1f
    return FloatArray(4) { values[it] * scale }
}

/** `<mode> <r> <g> <b> <a> <intensity %>`. */
private fun parsePixBlend(text: String): FilterStep? {
    val mode = BlendKind.of(text.substringBefore(' ')) ?: return null
    val v = text.substringAfter(' ', "").floats()
    if (v.size < 5) return null
    val color = normalizedColor(v.take(4))
    return PixBlendStep(mode, color[0], color[1], color[2], color[3] * v[4] / 100f)
}

/** `<mode> <r> <g> <b> <a> <intensity %> <low> <range> <centre x> <centre y> [kind]`. */
private fun parseVignetteBlend(text: String): FilterStep? {
    val mode = BlendKind.of(text.substringBefore(' ')) ?: return null
    val v = text.substringAfter(' ', "").floats()
    if (v.size < 9) return null
    val color = normalizedColor(v.take(4))
    return VignetteBlendStep(mode, color, color[3] * v[4] / 100f, low = v[5], range = v[6], centerX = v[7], centerY = v[8])
}

/** `mat <9 values>`: the colour times a 3 × 3 matrix, each row giving one channel. */
private fun parseColorMul(text: String): FilterStep? {
    if (text.substringBefore(' ') != "mat") return null
    val v = text.substringAfter(' ', "").floats()
    return if (v.size >= 9) ColorMulStep(v.take(9).toFloatArray()) else null
}

private fun parseStyle(text: String): FilterStep? {
    val v = text.substringAfter(' ', "").floats()
    return when (text.substringBefore(' ')) {
        "edge" -> EdgeStep(v.getOrElse(0) { 1f }, v.getOrElse(1) { 2f })
        "emboss" -> EmbossStep(v.getOrElse(0) { 1f }, v.getOrElse(1) { 2f }, v.getOrElse(2) { 0f })
        "halftone" -> HalftoneStep(v.getOrElse(0) { 1f })
        "haze" -> if (v.size >= 5) HazeStep(v[0], v[1], v[2], v[3], v[4]) else null
        "crosshatch" -> CrosshatchStep(v.getOrElse(0) { 0.03f }, v.getOrElse(1) { 0.003f })
        "sketch" -> SketchStep(v.getOrElse(0) { 1f })
        else -> null
    }
}

/** `bilateral <blur scale> <distance factor> <repeat>`. */
private fun parseBeautify(text: String): FilterStep? {
    if (text.substringBefore(' ') != "bilateral") return null
    val v = text.substringAfter(' ', "").floats()
    return BilateralStep(v.getOrElse(0) { 10f }, v.getOrElse(1) { 4f }, v.getOrElse(2) { 1f }.toInt().coerceIn(1, 4))
}

/** `<mode> <texture path> <intensity %>`: the texture stretched to cover the photo, blended over it. */
private fun parseTextureBlend(text: String): FilterStep? {
    val parts = text.split(' ').filter { it.isNotBlank() }
    if (parts.size < 2) return null
    val mode = BlendKind.of(parts[0]) ?: return null
    return TextureBlendStep(mode, parts[1], parts.getOrNull(2)?.toFloatOrNull()?.div(100f) ?: 1f)
}

// endregion

// region Running

/**
 * Runs [steps] over [image], in place. Each run of [ColorStep]s is folded into a colour cube when
 * the picture has more pixels than the cube has entries, which costs one lookup per pixel however
 * long the run is; a smaller picture (a thumbnail) is mapped pixel by pixel instead.
 */
internal fun runFilterSteps(image: PixelImage, steps: List<FilterStep>, assets: FilterAssetLookup) {
    var index = 0
    while (index < steps.size) {
        val step = steps[index]
        if (step is ImageStep) {
            step.apply(image, assets)
            index++
            continue
        }
        val run = mutableListOf<ColorStep>()
        while (index < steps.size && steps[index] is ColorStep) run += steps[index++] as ColorStep
        applyColorRun(image, run, assets)
    }
}

private fun mapColor(c: FloatArray, run: List<ColorStep>, assets: FilterAssetLookup) {
    for (step in run) {
        step.apply(c, assets)
        c[0] = c[0].clamp01()
        c[1] = c[1].clamp01()
        c[2] = c[2].clamp01()
    }
}

private fun applyColorRun(image: PixelImage, run: List<ColorStep>, assets: FilterAssetLookup) {
    val pixels = image.pixels
    val c = FloatArray(3)
    if (pixels.size <= CubeSize * CubeSize * CubeSize) {
        for (i in pixels.indices) {
            val p = pixels[i]
            c[0] = ((p shr 16) and 0xFF) / 255f
            c[1] = ((p shr 8) and 0xFF) / 255f
            c[2] = (p and 0xFF) / 255f
            mapColor(c, run, assets)
            pixels[i] = pack(p ushr 24, c[0], c[1], c[2])
        }
        return
    }
    val n = CubeSize
    val last = (n - 1).toFloat()
    val cube = FloatArray(n * n * n * 3)
    var o = 0
    for (b in 0 until n) for (g in 0 until n) for (r in 0 until n) {
        c[0] = r / last
        c[1] = g / last
        c[2] = b / last
        mapColor(c, run, assets)
        cube[o++] = c[0]
        cube[o++] = c[1]
        cube[o++] = c[2]
    }
    for (i in pixels.indices) {
        val p = pixels[i]
        val fr = ((p shr 16) and 0xFF) / 255f * last
        val fg = ((p shr 8) and 0xFF) / 255f * last
        val fb = (p and 0xFF) / 255f * last
        val r0 = fr.toInt().coerceAtMost(n - 2)
        val g0 = fg.toInt().coerceAtMost(n - 2)
        val b0 = fb.toInt().coerceAtMost(n - 2)
        val tr = fr - r0
        val tg = fg - g0
        val tb = fb - b0
        var out = p and 0xFF000000.toInt()
        for (ch in 0 until 3) {
            fun at(r: Int, g: Int, b: Int) = cube[((b * n + g) * n + r) * 3 + ch]
            val c00 = lerp(at(r0, g0, b0), at(r0 + 1, g0, b0), tr)
            val c10 = lerp(at(r0, g0 + 1, b0), at(r0 + 1, g0 + 1, b0), tr)
            val c01 = lerp(at(r0, g0, b0 + 1), at(r0 + 1, g0, b0 + 1), tr)
            val c11 = lerp(at(r0, g0 + 1, b0 + 1), at(r0 + 1, g0 + 1, b0 + 1), tr)
            val v = lerp(lerp(c00, c10, tg), lerp(c01, c11, tg), tb)
            out = out or (to255(v) shl (16 - ch * 8))
        }
        pixels[i] = out
    }
}

// endregion

// region Colour steps

/**
 * CGE's lookup filter: a 512 × 512 picture of 64 tiles (8 × 8), one per blue level, each mapping red
 * across and green down. Blue is interpolated between the two nearest tiles, and red and green within
 * a tile by bilinear sampling, as the GPU does.
 */
internal class LutStep(val path: String) : ColorStep {
    // Scratch for the two tiles' samples; a step is only ever run by the render that parsed it.
    private val r0 = FloatArray(3)
    private val r1 = FloatArray(3)

    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val lut = assets.image(path) ?: return
        val tile = lut.width / 8f
        val blue = c[2] * 63f
        val b0 = floor(blue).toInt()
        val b1 = min(b0 + 1, 63)
        val t = blue - b0
        val x = (tile - 1f) * c[0]
        val y = (tile - 1f) * c[1]
        sampleBilinear(lut, (b0 % 8) * tile + x, (b0 / 8) * tile + y, r0)
        sampleBilinear(lut, (b1 % 8) * tile + x, (b1 / 8) * tile + y, r1)
        c[0] = lerp(r0[0], r1[0], t)
        c[1] = lerp(r0[1], r1[1], t)
        c[2] = lerp(r0[2], r1[2], t)
    }
}

internal class CurveStep(val red: FloatArray, val green: FloatArray, val blue: FloatArray) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        c[0] = sampleCurve(red, c[0])
        c[1] = sampleCurve(green, c[1])
        c[2] = sampleCurve(blue, c[2])
    }
}

/** 0 grey … 1 unchanged, about the HSL lightness. */
internal class SaturationStep(private val saturation: Float) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val lum = (max(c[0], max(c[1], c[2])) + min(c[0], min(c[1], c[2]))) / 2f
        for (i in 0 until 3) c[i] = lerp(lum, c[i], saturation)
    }
}

/** CGE's brightness curve: a negative value darkens, a positive one lightens, both bending rather than shifting. */
internal class BrightnessStep(private val intensity: Float) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        if (intensity == 0f) return
        val fac = 1f / intensity
        val fac2 = 1f - fac
        val sign = if (intensity >= 0f) 1f else -1f
        for (i in 0 until 3) c[i] = fac2 - c[i] + sqrt(max(0f, fac2 * fac2 + 4f * fac * c[i])) * sign
    }
}

internal class ExposureStep(private val exposure: Float) : ColorStep {
    private val factor = 2f.pow(exposure)
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        for (i in 0 until 3) c[i] *= factor
    }
}

/** Input levels [dark]..[light] stretched to 0..1, then raised to [gamma]. */
internal class LevelStep(private val dark: Float, private val light: Float, private val gamma: Float) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val span = if (light == dark) 0.0001f else light - dark
        for (i in 0 until 3) c[i] = ((c[i] - dark) / span).clamp01().pow(gamma).clamp01()
    }
}

/** Hue turned by [hue] (of a full turn), saturation and lightness scaled by 1 + the others. */
internal class HslStep(private val hue: Float, private val saturation: Float, private val lightness: Float) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        rgbToHsl(c)
        c[0] += hue
        c[1] *= 1f + saturation
        c[2] *= 1f + lightness
        hslToRgb(c)
    }
}

/**
 * CGE's per-hue saturation: [colors] are red, green, blue, magenta, yellow, cyan, each how much (-1
 * grey … 0 unchanged) that hue's saturation changes; hues between two of them blend their values.
 */
internal class HsvStep(private val colors: FloatArray) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val red = colors[0]
        val green = colors[1]
        val blue = colors[2]
        val magenta = colors[3]
        val yellow = colors[4]
        val cyan = colors[5]
        val r = c[0]
        val g = c[1]
        val b = c[2]
        val fmax = max(r, max(g, b))
        val fmin = min(r, min(g, b))
        val delta = fmax - fmin
        if (delta == 0f) return
        val v = fmax
        var s = delta / fmax
        // The shader's clamp of s discards its result, so s is left unclamped here too.
        when (fmax) {
            r -> if (g >= b) {
                val h = (g - b) / delta
                s *= 1f + ((yellow - red) * h + red)
                c[0] = v; c[2] = v * (1f - s); c[1] = v * (1f - s + s * h)
            } else {
                val h = (r - b) / delta
                s *= 1f + ((red - magenta) * h + magenta)
                c[0] = v; c[1] = v * (1f - s); c[2] = v * (1f - s * h)
            }
            g -> if (r > b) {
                val h = (g - r) / delta
                s *= 1f + ((green - yellow) * h + yellow)
                c[1] = v; c[0] = v * (1f - s * h); c[2] = v * (1f - s)
            } else {
                val h = (b - r) / delta
                s *= 1f + ((cyan - green) * h + green)
                c[1] = v; c[0] = v * (1f - s); c[2] = v * (1f - s + s * h)
            }
            else -> if (g > r) {
                val h = (b - g) / delta
                s *= 1f + ((blue - cyan) * h + cyan)
                c[2] = v; c[0] = v * (1f - s); c[1] = v * (1f - s * h)
            } else {
                val h = (r - g) / delta
                s *= 1f + ((magenta - blue) * h + blue)
                c[2] = v; c[0] = v * (1f - s + s * h); c[1] = v * (1f - s)
            }
        }
    }
}

/** Photoshop-style colour balance on the midtones, keeping each pixel's lightness. */
internal class ColorBalanceStep(private val red: Float, private val green: Float, private val blue: Float) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val lightness = (max(c[0], max(c[1], c[2])) + min(c[0], min(c[1], c[2]))) / 2f
        val midtones = ((lightness - 0.333f) / 0.25f + 0.5f).clamp01() * ((lightness + 0.333f - 1f) / -0.25f + 0.5f).clamp01() * 0.7f
        c[0] = (c[0] + midtones * red).clamp01()
        c[1] = (c[1] + midtones * green).clamp01()
        c[2] = (c[2] + midtones * blue).clamp01()
        rgbToHsl(c)
        c[2] = lightness
        hslToRgb(c)
    }
}

/** Each output channel is a row of [matrix] dotted with the colour. */
internal class ColorMulStep(private val matrix: FloatArray) : ColorStep {
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        val r = c[0]
        val g = c[1]
        val b = c[2]
        for (i in 0 until 3) c[i] = r * matrix[i * 3] + g * matrix[i * 3 + 1] + b * matrix[i * 3 + 2]
    }
}

/**
 * CGE's fast shadows/highlights: a cubic for each half of the range whose slope at black is the
 * shadows' and at white the highlights'. CGE takes shadows in -200..100 and highlights in -100..200;
 * a lower shadow value lifts the shadows and a higher highlight value holds the highlights down.
 */
internal class ShadowHighlightStep(shadow: Float, highlight: Float) : ColorStep {
    private val d = 1f - shadow.coerceIn(-200f, 100f) / 200f
    private val l = 1f - highlight.coerceIn(-100f, 200f) / 200f
    override fun apply(c: FloatArray, assets: FilterAssetLookup) {
        for (i in 0 until 3) {
            val x = c[i]
            val x2 = x * x
            val x3 = x2 * x
            c[i] = if (x < 0.5f) {
                4f * (d - 1f) * x3 + 4f * (1f - d) * x2 + d * x
            } else {
                4f * (l - 1f) * x3 + 8f * (1f - l) * x2 + (5f * l - 4f) * x + 1f - l
            }
        }
    }
}

/** A flat colour blended over the picture. */
internal class PixBlendStep(private val mode: BlendKind, red: Float, green: Float, blue: Float, private val alpha: Float) : ColorStep {
    private val color = floatArrayOf(red, green, blue)
    override fun apply(c: FloatArray, assets: FilterAssetLookup) = blend(mode, c, color, alpha)
}

// endregion

// region Image steps

/** A colour blended over the picture by a vignette: none inside [low], all of it past [low] + [range]. */
internal class VignetteBlendStep(
    private val mode: BlendKind,
    private val color: FloatArray,
    private val alpha: Float,
    private val low: Float,
    private val range: Float,
    private val centerX: Float,
    private val centerY: Float,
) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val c = FloatArray(3)
        val span = if (range == 0f) 0.0001f else range
        forEachPixel(image, c) { x, y ->
            val dx = x - centerX
            val dy = y - centerY
            val p = ((sqrt(dx * dx + dy * dy) - low) / span).clamp01()
            blend(mode, c, color, alpha * p * p * (3f - 2f * p))
        }
    }
}

/** A haze thickening (or, negative, thinning) down the picture. */
internal class HazeStep(private val distance: Float, private val slope: Float, r: Float, g: Float, b: Float) : ImageStep {
    private val haze = floatArrayOf(r, g, b)
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val c = FloatArray(3)
        forEachPixel(image, c) { _, y ->
            val d = y * slope + distance
            val keep = if (d == 1f) 0.0001f else 1f - d
            for (i in 0 until 3) c[i] = (c[i] - d * haze[i]) / keep
        }
    }
}

/** Black dots on white, one per cell, each channel's dot as large as that channel is dark. */
internal class HalftoneStep(private val dotSize: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val cell = max(2f, dotSize * 0.01f * image.longSide)
        val source = image.pixels.copyOf()
        val w = image.width
        val h = image.height
        for (y in 0 until h) {
            val cy = (floor(y / cell) + 0.5f) * cell
            for (x in 0 until w) {
                val cx = (floor(x / cell) + 0.5f) * cell
                val p = source[cy.toInt().coerceIn(0, h - 1) * w + cx.toInt().coerceIn(0, w - 1)]
                val dx = x - cx
                val dy = y - cy
                val dis = sqrt(dx * dx + dy * dy)
                var out = image.pixels[y * w + x] and 0xFF000000.toInt()
                for (ch in 0 until 3) {
                    val v = ((p shr (16 - ch * 8)) and 0xFF) / 255f
                    val inDot = dis <= cell * (1f - v) * 0.5f
                    out = out or ((if (inDot) 0 else 255) shl (16 - ch * 8))
                }
                image.pixels[y * w + x] = out
            }
        }
    }
}

/** Pen hatching: more crossing diagonal strokes the darker the pixel, in fractions of the picture. */
internal class CrosshatchStep(private val spacing: Float, private val lineWidth: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val c = FloatArray(3)
        val half = spacing / 2f
        forEachPixel(image, c) { x, y ->
            val lum = c[0] * 0.2125f + c[1] * 0.7154f + c[2] * 0.0721f
            val dark = (lum < 1f && mod(x + y, spacing) <= lineWidth) ||
                (lum < 0.75f && mod(x - y, spacing) <= lineWidth) ||
                (lum < 0.5f && mod(x + y - half, spacing) <= lineWidth) ||
                (lum < 0.3f && mod(x - y - half, spacing) <= lineWidth)
            val v = if (dark) 0f else 1f
            c[0] = v; c[1] = v; c[2] = v
        }
    }
}

/** Sobel edges, [stride] pixels apart. */
internal class EdgeStep(private val intensity: Float, private val stride: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val s = max(1, (stride * image.longSide / ReferenceLongSide).roundToInt())
        val source = image.pixels.copyOf()
        val w = image.width
        val h = image.height
        fun ch(x: Int, y: Int, k: Int) = ((source[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)] shr (16 - k * 8)) and 0xFF) / 255f
        for (y in 0 until h) for (x in 0 until w) {
            var out = image.pixels[y * w + x] and 0xFF000000.toInt()
            for (k in 0 until 3) {
                val hx = -ch(x - s, y - s, k) - 2f * ch(x, y - s, k) - ch(x + s, y - s, k) +
                    ch(x - s, y + s, k) + 2f * ch(x, y + s, k) + ch(x + s, y + s, k)
                val vy = -ch(x - s, y - s, k) + ch(x + s, y - s, k) - 2f * ch(x - s, y, k) +
                    2f * ch(x + s, y, k) - ch(x - s, y + s, k) + ch(x + s, y + s, k)
                out = out or (to255(lerp(ch(x, y, k), sqrt(hx * hx + vy * vy), intensity)) shl (16 - k * 8))
            }
            image.pixels[y * w + x] = out
        }
    }
}

/** Grey relief: each pixel against its neighbour [stride] pixels away at [angle] radians. */
internal class EmbossStep(private val intensity: Float, private val stride: Float, private val angle: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val s = stride * image.longSide / ReferenceLongSide
        val ox = (cos(angle) * s).roundToInt()
        val oy = (sin(angle) * s).roundToInt()
        val source = image.pixels.copyOf()
        val w = image.width
        val h = image.height
        val c = FloatArray(3)
        forEachPixelIndexed(image, c) { x, y ->
            val q = source[(y + oy).coerceIn(0, h - 1) * w + (x + ox).coerceIn(0, w - 1)]
            val f = ((((q shr 16) and 0xFF) / 255f - c[0]) + (((q shr 8) and 0xFF) / 255f - c[1]) + ((q and 0xFF) / 255f - c[2])) / 3f + 0.5f
            for (i in 0 until 3) c[i] = lerp(c[i], f, intensity)
        }
    }
}

/** Pencil sketch: each pixel's lightness against the lightest pixel around it. */
internal class SketchStep(private val intensity: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val radius = max(1, (image.longSide / ReferenceLongSide).roundToInt())
        val w = image.width
        val h = image.height
        val lum = FloatArray(w * h) { luminance(image.pixels[it]) }
        // The lightest around, as a max filter in two passes (rows, then columns).
        val rows = FloatArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var m = 0f
            for (k in -radius..radius) m = max(m, lum[y * w + (x + k).coerceIn(0, w - 1)])
            rows[y * w + x] = m
        }
        val c = FloatArray(3)
        forEachPixelIndexed(image, c) { x, y ->
            var m = 0f
            for (k in -radius..radius) m = max(m, rows[(y + k).coerceIn(0, h - 1) * w + x])
            val v = min(lum[y * w + x] / (m + 0.001f), 1f)
            for (i in 0 until 3) c[i] = lerp(c[i], v, intensity)
        }
    }
}

/** Unsharp masking against a small box blur [blurScale] pixels wide. */
internal class SharpenStep(private val sharpen: Float, private val blurScale: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val s = max(1, (blurScale * image.longSide / ReferenceLongSide).roundToInt())
        val amount = 1f + sharpen / 4f
        val source = image.pixels.copyOf()
        val w = image.width
        val h = image.height
        fun at(x: Int, y: Int) = source[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]
        for (y in 0 until h) for (x in 0 until w) {
            val p = source[y * w + x]
            var out = p and 0xFF000000.toInt()
            for (k in 0 until 3) {
                val shift = 16 - k * 8
                val blur = (((p shr shift) and 0xFF) + ((at(x - s, y) shr shift) and 0xFF) + ((at(x + s, y) shr shift) and 0xFF) +
                    ((at(x, y - s) shr shift) and 0xFF) + ((at(x, y + s) shr shift) and 0xFF)) / (5f * 255f)
                out = out or (to255(lerp(blur, ((p shr shift) and 0xFF) / 255f, amount)) shl shift)
            }
            image.pixels[y * w + x] = out
        }
    }
}

/** Edge-keeping smoothing: a Gaussian along rows then columns that skips pixels of a different colour. */
internal class BilateralStep(private val blurScale: Float, private val distanceFactor: Float, private val repeat: Int) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val step = max(1f, abs(blurScale) / 10f * image.longSide / ReferenceLongSide)
        repeat(repeat) {
            bilateralPass(image, step, horizontal = true)
            bilateralPass(image, step, horizontal = false)
        }
    }

    private fun bilateralPass(image: PixelImage, step: Float, horizontal: Boolean) {
        val source = image.pixels.copyOf()
        val w = image.width
        val h = image.height
        for (y in 0 until h) for (x in 0 until w) {
            val center = source[y * w + x]
            val cr = (center shr 16) and 0xFF
            val cg = (center shr 8) and 0xFF
            val cb = center and 0xFF
            var total = BilateralWeights[4]
            var sr = cr * total
            var sg = cg * total
            var sb = cb * total
            for (i in 0 until 4) {
                val offset = ((i + 0.5f) * step).roundToInt()
                val weight = BilateralWeights[3 - i]
                for (side in 0 until 2) {
                    val sign = side * 2 - 1
                    val sx = if (horizontal) (x + sign * offset).coerceIn(0, w - 1) else x
                    val sy = if (horizontal) y else (y + sign * offset).coerceIn(0, h - 1)
                    val q = source[sy * w + sx]
                    val qr = (q shr 16) and 0xFF
                    val qg = (q shr 8) and 0xFF
                    val qb = q and 0xFF
                    val dr = (qr - cr) / 255f
                    val dg = (qg - cg) / 255f
                    val db = (qb - cb) / 255f
                    val g = weight * (1f - min(sqrt(dr * dr + dg * dg + db * db) * distanceFactor, 1f))
                    total += g
                    sr += qr * g
                    sg += qg * g
                    sb += qb * g
                }
            }
            image.pixels[y * w + x] = (center and 0xFF000000.toInt()) or
                ((sr / total).roundToInt().coerceIn(0, 255) shl 16) or
                ((sg / total).roundToInt().coerceIn(0, 255) shl 8) or
                (sb / total).roundToInt().coerceIn(0, 255)
        }
    }
}

/** GPUImage's bilateral weights, the centre's in the middle. */
private val BilateralWeights = floatArrayOf(0.05f, 0.09f, 0.12f, 0.15f, 0.18f)

/**
 * CGE's `krblend`: a texture scaled, keeping its shape, to cover the whole picture (centred, its
 * overflow cut off), then blended over it with [mode] at [alpha]. The LAS app's overlays.
 */
internal class TextureBlendStep(val mode: BlendKind, val path: String, private val alpha: Float) : ImageStep {
    override fun apply(image: PixelImage, assets: FilterAssetLookup) {
        val texture = assets.image(path) ?: return
        val scale = max(image.width.toFloat() / texture.width, image.height.toFloat() / texture.height)
        val offsetX = (texture.width - image.width / scale) / 2f
        val offsetY = (texture.height - image.height / scale) / 2f
        val c = FloatArray(3)
        val t = FloatArray(3)
        forEachPixelIndexed(image, c) { x, y ->
            sampleBilinear(texture, offsetX + (x + 0.5f) / scale - 0.5f, offsetY + (y + 0.5f) / scale - 0.5f, t)
            blend(mode, c, t, alpha)
        }
    }
}

// endregion

// region Blending

/** CGE's blend modes the filters and overlays use, by CGE's names for them. */
internal enum class BlendKind(vararg val names: String) {
    Mix("mix"),
    Screen("screen", "sr"),
    Overlay("overlay", "ol"),
    ColorDodge("colordodge", "cd"),
    PinLight("pinlight", "pl"),
    HardMix("hardmix", "hm"),
    Divide("divide", "div"),
    Color("color", "cl"),
    ;

    companion object {
        fun of(name: String): BlendKind? = entries.firstOrNull { name in it.names }
    }
}

/** Blends [over] onto [c] in place, then mixes that with [c] by [alpha], as CGE's `blend()` does. */
internal fun blend(mode: BlendKind, c: FloatArray, over: FloatArray, alpha: Float) {
    if (alpha <= 0f) return
    if (mode == BlendKind.Color) {
        val shift = luminance(c[0], c[1], c[2]) - luminance(over[0], over[1], over[2])
        for (i in 0 until 3) c[i] = lerp(c[i], (over[i] + shift).clamp01(), alpha)
        return
    }
    for (i in 0 until 3) {
        val a = c[i]
        val b = over[i]
        val blended = when (mode) {
            BlendKind.Mix -> b
            BlendKind.Screen -> a + b - a * b
            BlendKind.Overlay -> if (a < 0.5f) 2f * a * b else 1f - 2f * (1f - a) * (1f - b)
            BlendKind.ColorDodge -> if (b >= 1f) 1f else min(a / (1f - b), 1f)
            BlendKind.PinLight -> {
                val b2 = b * 2f
                if (b2 > a) max(b2 - 1f, a) else b2
            }
            BlendKind.HardMix -> if (b >= 1f - a) 1f else 0f
            BlendKind.Divide -> if (b <= 0f) 1f else min(a / b, 1f)
            BlendKind.Color -> b
        }
        c[i] = lerp(a, blended, alpha)
    }
}

// endregion

// region Helpers

private val IdentityCurve = FloatArray(256) { it / 255f }

/** [curve] (256 entries, 0..1) at [x] (0..1), linearly between entries. */
private fun sampleCurve(curve: FloatArray, x: Float): Float {
    val f = x.clamp01() * 255f
    val i = f.toInt().coerceAtMost(254)
    return lerp(curve[i], curve[i + 1], f - i)
}

/**
 * A 256-entry tone curve through [points] (0..255 both ways), as a natural cubic spline, the way
 * Photoshop and GPUImage draw curves: flat before the first point and after the last, clamped to
 * 0..255. Points out of order are sorted, and of two at the same input the later one is kept.
 */
internal fun toneCurve(points: List<Pair<Float, Float>>): FloatArray {
    val sorted = points.associate { it }.toList().sortedBy { it.first }
    if (sorted.isEmpty()) return IdentityCurve.copyOf()
    val xs = sorted.map { it.first }.toFloatArray()
    val ys = sorted.map { it.second }.toFloatArray()
    val n = xs.size
    val out = FloatArray(256)
    if (n == 1) {
        out.fill(ys[0].coerceIn(0f, 255f) / 255f)
        return out
    }
    val second = splineSecondDerivatives(xs, ys)
    for (x in 0 until 256) {
        val v = when {
            x <= xs[0] -> ys[0]
            x >= xs[n - 1] -> ys[n - 1]
            else -> {
                var i = 0
                while (i < n - 2 && x > xs[i + 1]) i++
                val h = xs[i + 1] - xs[i]
                val b = (x - xs[i]) / h
                val a = 1f - b
                a * ys[i] + b * ys[i + 1] + (h * h / 6f) * ((a * a * a - a) * second[i] + (b * b * b - b) * second[i + 1])
            }
        }
        out[x] = v.coerceIn(0f, 255f) / 255f
    }
    return out
}

/** A natural spline's second derivatives (zero at both ends), by the tridiagonal (Thomas) solve. */
private fun splineSecondDerivatives(xs: FloatArray, ys: FloatArray): FloatArray {
    val n = xs.size
    val result = FloatArray(n)
    if (n < 3) return result
    val c = FloatArray(n)
    val d = FloatArray(n)
    for (i in 1 until n - 1) {
        val a = (xs[i] - xs[i - 1]) / 6f
        val b = (xs[i + 1] - xs[i - 1]) / 3f
        val cc = (xs[i + 1] - xs[i]) / 6f
        val r = (ys[i + 1] - ys[i]) / (xs[i + 1] - xs[i]) - (ys[i] - ys[i - 1]) / (xs[i] - xs[i - 1])
        val m = b - a * c[i - 1]
        c[i] = cc / m
        d[i] = (r - a * d[i - 1]) / m
    }
    for (i in n - 2 downTo 1) result[i] = d[i] - c[i] * result[i + 1]
    return result
}

private fun sampleBilinear(image: PixelImage, x: Float, y: Float, out: FloatArray) {
    val w = image.width
    val h = image.height
    val cx = x.coerceIn(0f, (w - 1).toFloat())
    val cy = y.coerceIn(0f, (h - 1).toFloat())
    val x0 = cx.toInt().coerceAtMost(max(w - 2, 0))
    val y0 = cy.toInt().coerceAtMost(max(h - 2, 0))
    val x1 = min(x0 + 1, w - 1)
    val y1 = min(y0 + 1, h - 1)
    val tx = cx - x0
    val ty = cy - y0
    val p00 = image.pixels[y0 * w + x0]
    val p10 = image.pixels[y0 * w + x1]
    val p01 = image.pixels[y1 * w + x0]
    val p11 = image.pixels[y1 * w + x1]
    for (ch in 0 until 3) {
        val shift = 16 - ch * 8
        val top = lerp(((p00 shr shift) and 0xFF).toFloat(), ((p10 shr shift) and 0xFF).toFloat(), tx)
        val bottom = lerp(((p01 shr shift) and 0xFF).toFloat(), ((p11 shr shift) and 0xFF).toFloat(), tx)
        out[ch] = lerp(top, bottom, ty) / 255f
    }
}

/** Maps every pixel's colour in place, given its place as fractions (0..1) of the picture. */
private inline fun forEachPixel(image: PixelImage, c: FloatArray, map: (x: Float, y: Float) -> Unit) {
    val w = image.width
    val h = image.height
    forEachPixelIndexed(image, c) { x, y -> map((x + 0.5f) / w, (y + 0.5f) / h) }
}

/** Maps every pixel's colour in place, given its place in pixels. */
private inline fun forEachPixelIndexed(image: PixelImage, c: FloatArray, map: (x: Int, y: Int) -> Unit) {
    val w = image.width
    val pixels = image.pixels
    for (y in 0 until image.height) for (x in 0 until w) {
        val i = y * w + x
        val p = pixels[i]
        c[0] = ((p shr 16) and 0xFF) / 255f
        c[1] = ((p shr 8) and 0xFF) / 255f
        c[2] = (p and 0xFF) / 255f
        map(x, y)
        pixels[i] = pack(p ushr 24, c[0], c[1], c[2])
    }
}

private fun rgbToHsl(c: FloatArray) {
    val r = c[0]
    val g = c[1]
    val b = c[2]
    val maxc = max(r, max(g, b))
    val minc = min(r, min(g, b))
    val l = (maxc + minc) / 2f
    if (maxc == minc) {
        c[0] = 0f; c[1] = 0f; c[2] = l
        return
    }
    val d = maxc - minc
    val s = if (l <= 0.5f) d / (maxc + minc) else d / (2f - maxc - minc)
    var h = when (maxc) {
        r -> (g - b) / d
        g -> 2f + (b - r) / d
        else -> 4f + (r - g) / d
    } * 60f
    if (h < 0f) h += 360f
    c[0] = h / 360f; c[1] = s; c[2] = l
}

private fun hslToRgb(c: FloatArray) {
    val h = c[0]
    val s = c[1]
    val l = c[2]
    if (s <= 0f) {
        c[0] = l; c[1] = l; c[2] = l
        return
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun channel(t0: Float): Float {
        val t = t0 - floor(t0)
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 0.5f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    c[0] = channel(h + 1f / 3f)
    c[1] = channel(h)
    c[2] = channel(h - 1f / 3f)
}

private fun luminance(r: Float, g: Float, b: Float): Float = 0.299f * r + 0.587f * g + 0.114f * b

private fun luminance(p: Int): Float =
    luminance(((p shr 16) and 0xFF) / 255f, ((p shr 8) and 0xFF) / 255f, (p and 0xFF) / 255f)

private fun mod(x: Float, m: Float): Float = x - m * floor(x / m)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun Float.clamp01(): Float = if (this.isNaN()) 0f else coerceIn(0f, 1f)

private fun to255(v: Float): Int = (v.clamp01() * 255f + 0.5f).toInt()

private fun pack(alpha: Int, r: Float, g: Float, b: Float): Int =
    (alpha shl 24) or (to255(r) shl 16) or (to255(g) shl 8) or to255(b)

// endregion
