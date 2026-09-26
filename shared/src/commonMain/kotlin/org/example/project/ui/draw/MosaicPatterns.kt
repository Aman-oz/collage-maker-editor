package org.example.project.ui.draw

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import org.example.project.ui.common.imageBitmapFromArgb

/**
 * One selectable mosaic brush. A mosaic stroke reveals [renderMosaic]'s bitmap for the pattern,
 * which always covers the whole photo — so what the brush paints is the matching part of the
 * pattern, exactly like the swatch shows it.
 */
internal sealed interface MosaicPattern {
    val label: String
}

/** Reveals a pixelated copy of the photo itself — see [computeMosaicBitmap]. */
internal data class PixelateMosaic(
    override val label: String,
    val cellFractionX: Float,
    val cellFractionY: Float,
) : MosaicPattern

/** Paints a generated texture (grain, pastel blocks, swirl, ...) over the photo. */
internal data class TextureMosaic(
    override val label: String,
    val texture: MosaicTexture,
) : MosaicPattern

internal enum class MosaicTexture { Grain, Pastel, DarkGrain, Swirl, Bokeh, Mist, Rainbow, Ocean, Galaxy }

internal val MosaicPatterns: List<MosaicPattern> = listOf(
    TextureMosaic("Grain", MosaicTexture.Grain),
    TextureMosaic("Pastel", MosaicTexture.Pastel),
    TextureMosaic("Charcoal", MosaicTexture.DarkGrain),
    TextureMosaic("Swirl", MosaicTexture.Swirl),
    TextureMosaic("Bokeh", MosaicTexture.Bokeh),
    TextureMosaic("Mist", MosaicTexture.Mist),
    TextureMosaic("Rainbow", MosaicTexture.Rainbow),
    TextureMosaic("Ocean", MosaicTexture.Ocean),
    TextureMosaic("Galaxy", MosaicTexture.Galaxy),
    PixelateMosaic("Pixel S", cellFractionX = 0.018f, cellFractionY = 0.018f),
    PixelateMosaic("Pixel M", cellFractionX = 0.045f, cellFractionY = 0.045f),
    PixelateMosaic("Pixel L", cellFractionX = 0.085f, cellFractionY = 0.085f),
)

internal val MosaicPatternDefault: MosaicPattern = MosaicPatterns.first()

/**
 * Textures are generated at most this many pixels on their long side and scaled up onto the
 * photo. They are defined in normalized coordinates, so the preview and the full-resolution bake
 * produce the identical texture — rendering one at 12 MP would only cost time, not add detail.
 */
private const val MaxTextureSidePx = 1024

/**
 * The bitmap a [pattern] stroke reveals over [source]. Pixelate patterns come back at [source]'s
 * size; textures at [source]'s aspect ratio but capped to [MaxTextureSidePx] — callers always
 * draw the result scaled into the photo's bounds, so the two are interchangeable.
 */
internal fun renderMosaic(source: ImageBitmap, pattern: MosaicPattern): ImageBitmap = when (pattern) {
    is PixelateMosaic -> computeMosaicBitmap(source, pattern.cellFractionX, pattern.cellFractionY)
    is TextureMosaic -> {
        val scale = minOf(1f, MaxTextureSidePx.toFloat() / max(source.width, source.height))
        renderMosaicTexture(
            texture = pattern.texture,
            width = (source.width * scale).roundToInt().coerceAtLeast(1),
            height = (source.height * scale).roundToInt().coerceAtLeast(1),
        )
    }
}

/**
 * A [sidePx]-square picker thumbnail for [pattern]: textures render directly, pixelate patterns
 * pixelate a center-cropped thumbnail of [source] so the swatch previews the actual photo, with
 * cells doubled — at thumbnail size the finest pattern would otherwise barely look pixelated.
 */
internal fun renderMosaicSwatch(source: ImageBitmap, pattern: MosaicPattern, sidePx: Int = 128): ImageBitmap = when (pattern) {
    is TextureMosaic -> renderMosaicTexture(pattern.texture, sidePx, sidePx)
    is PixelateMosaic -> {
        val cropSide = min(source.width, source.height)
        val thumb = ImageBitmap(sidePx, sidePx)
        Canvas(thumb).drawImageRect(
            image = source,
            srcOffset = IntOffset((source.width - cropSide) / 2, (source.height - cropSide) / 2),
            srcSize = IntSize(cropSide, cropSide),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(sidePx, sidePx),
            paint = Paint().apply { filterQuality = FilterQuality.High },
        )
        computeMosaicBitmap(thumb, pattern.cellFractionX * 2f, pattern.cellFractionY * 2f)
    }
}

/**
 * Renders [texture] per pixel. Each shader gets `x` in `0f..1f` across the width and `y` in the
 * same units (so `0f..height/width`), which keeps round features round at any aspect ratio and
 * makes the output resolution-independent.
 */
internal fun renderMosaicTexture(texture: MosaicTexture, width: Int, height: Int): ImageBitmap {
    val w = width.toFloat()
    val aspect = height / w
    val shade = shaderFor(texture, aspect)
    val pixels = IntArray(width * height)
    for (py in 0 until height) {
        val y = (py + 0.5f) / w
        val row = py * width
        for (px in 0 until width) {
            pixels[row + px] = shade((px + 0.5f) / w, y)
        }
    }
    return imageBitmapFromArgb(pixels, width, height)
}

private fun shaderFor(texture: MosaicTexture, aspect: Float): (Float, Float) -> Int = when (texture) {
    MosaicTexture.Grain -> { x, y ->
        val g = 78f + hash((x * 180f).toInt(), (y * 180f).toInt(), 1) * 110f + (fbm(x * 6f, y * 6f, 2) - 0.5f) * 50f
        rgb(g, g, g + 4f)
    }
    MosaicTexture.Pastel -> blocks(16f) { cx, cy ->
        val base = ramp(PastelStops, fbm(cx * 2.5f, cy * 2.5f, 3))
        lighten(base, (hash((cx * 97f).toInt(), (cy * 97f).toInt(), 4) - 0.5f) * 0.12f)
    }
    MosaicTexture.DarkGrain -> { x, y ->
        val g = 22f + fbm(x * 4f, y * 4f, 5) * 75f + (hash((x * 220f).toInt(), (y * 220f).toInt(), 6) - 0.5f) * 55f
        rgb(g, g, g)
    }
    MosaicTexture.Swirl -> { x, y ->
        val dx = x - 0.5f
        val dy = y - aspect / 2f
        val r = sqrt(dx * dx + dy * dy)
        val turn = atan2(dy, dx) / (2f * PI.toFloat()) * 3f + r * 9f
        val band = 0.5f + 0.5f * sin(2f * PI.toFloat() * turn)
        mix(mix(0xFFE83E8C.toInt(), 0xFFFFE3F1.toInt(), band), 0xFF9C1458.toInt(), (r * 0.9f).coerceIn(0f, 0.6f))
    }
    MosaicTexture.Bokeh -> { x, y ->
        var color = ramp(BokehStops, ((y / aspect) * 0.7f + x * 0.3f).coerceIn(0f, 1f))
        for (light in BokehLights) {
            val dx = x - light.x
            val dy = y - light.y * aspect
            val falloff = 1f - sqrt(dx * dx + dy * dy) / light.radius
            if (falloff > 0f) color = mix(color, 0xFFFFF3B0.toInt(), smoothstep(falloff) * 0.45f)
        }
        color
    }
    MosaicTexture.Mist -> { x, y ->
        ramp(MistStops, (fbm(x * 2f, y * 2f, 7) * 0.7f + x * 0.3f).coerceIn(0f, 1f))
    }
    MosaicTexture.Rainbow -> blocks(20f) { cx, cy ->
        hsv(((cx + cy) * 0.8f + fbm(cx * 3f, cy * 3f, 8) * 0.3f) % 1f, 0.55f, 0.97f)
    }
    MosaicTexture.Ocean -> blocks(12f) { cx, cy ->
        ramp(OceanStops, fbm(cx * 2.2f, cy * 2.2f, 9))
    }
    MosaicTexture.Galaxy -> { x, y ->
        if (hash((x * 250f).toInt(), (y * 250f).toInt(), 10) > 0.992f) {
            0xFFFFFFFF.toInt()
        } else {
            ramp(GalaxyStops, smoothstep(fbm(x * 3f, y * 3f, 11)))
        }
    }
}

/** Quantizes a shader to square blocks [perWidth] across, sampling each block at its center. */
private inline fun blocks(perWidth: Float, crossinline shade: (Float, Float) -> Int): (Float, Float) -> Int = { x, y ->
    shade((floor(x * perWidth) + 0.5f) / perWidth, (floor(y * perWidth) + 0.5f) / perWidth)
}

private class BokehLight(val x: Float, val y: Float, val radius: Float)

/** Fixed, seeded positions (`y` as a fraction of the height) so every render places them identically. */
private val BokehLights: List<BokehLight> = List(14) { i ->
    BokehLight(
        x = hash(i, 0, 12),
        y = hash(i, 1, 12),
        radius = 0.04f + hash(i, 2, 12) * 0.09f,
    )
}

private val PastelStops = intArrayOf(0xFFFFC3A0.toInt(), 0xFFFFAFCC.toInt(), 0xFFCDB4DB.toInt(), 0xFFA2D2FF.toInt(), 0xFFB9FBC0.toInt(), 0xFFFDFFB6.toInt())
private val BokehStops = intArrayOf(0xFF1B4332.toInt(), 0xFF52B788.toInt(), 0xFFFFD166.toInt(), 0xFFF4A261.toInt())
private val MistStops = intArrayOf(0xFF5E5E62.toInt(), 0xFFBDBDC2.toInt(), 0xFF84848A.toInt())
private val OceanStops = intArrayOf(0xFF03045E.toInt(), 0xFF0077B6.toInt(), 0xFF00B4D8.toInt(), 0xFF90E0EF.toInt(), 0xFFCAF0F8.toInt())
private val GalaxyStops = intArrayOf(0xFF0B0B2B.toInt(), 0xFF3A0CA3.toInt(), 0xFF7209B7.toInt(), 0xFFF72585.toInt())

/** Deterministic integer hash → `0f..1f`, so textures don't depend on a random seed per run. */
private fun hash(x: Int, y: Int, seed: Int): Float {
    var h = x * 374761393 + y * 668265263 + seed * 144665
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777216f
}

private fun smoothstep(t: Float): Float = t * t * (3f - 2f * t)

private fun valueNoise(x: Float, y: Float, seed: Int): Float {
    val ix = floor(x).toInt()
    val iy = floor(y).toInt()
    val sx = smoothstep(x - ix)
    val sy = smoothstep(y - iy)
    val top = lerp(hash(ix, iy, seed), hash(ix + 1, iy, seed), sx)
    val bottom = lerp(hash(ix, iy + 1, seed), hash(ix + 1, iy + 1, seed), sx)
    return lerp(top, bottom, sy)
}

/** Three octaves of [valueNoise], still in `0f..1f`. */
private fun fbm(x: Float, y: Float, seed: Int): Float =
    valueNoise(x, y, seed) * 0.6f + valueNoise(x * 2.1f, y * 2.1f, seed + 7) * 0.3f + valueNoise(x * 4.3f, y * 4.3f, seed + 13) * 0.1f

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun rgb(r: Float, g: Float, b: Float): Int =
    (0xFF shl 24) or (r.coerceIn(0f, 255f).toInt() shl 16) or (g.coerceIn(0f, 255f).toInt() shl 8) or b.coerceIn(0f, 255f).toInt()

/** Plain sRGB blend of packed opaque colors — fast enough to run per pixel, unlike `Color.lerp`. */
private fun mix(a: Int, b: Int, t: Float): Int = rgb(
    lerp(((a shr 16) and 0xFF).toFloat(), ((b shr 16) and 0xFF).toFloat(), t),
    lerp(((a shr 8) and 0xFF).toFloat(), ((b shr 8) and 0xFF).toFloat(), t),
    lerp((a and 0xFF).toFloat(), (b and 0xFF).toFloat(), t),
)

private fun lighten(color: Int, amount: Float): Int =
    if (amount >= 0f) mix(color, 0xFFFFFFFF.toInt(), amount) else mix(color, 0xFF000000.toInt(), -amount)

/** Samples evenly spaced [stops] at [t] in `0f..1f`. */
private fun ramp(stops: IntArray, t: Float): Int {
    val scaled = t.coerceIn(0f, 1f) * (stops.size - 1)
    val index = scaled.toInt().coerceAtMost(stops.size - 2)
    return mix(stops[index], stops[index + 1], scaled - index)
}

private fun hsv(h: Float, s: Float, v: Float): Int {
    val sector = h * 6f
    val i = sector.toInt() % 6
    val f = sector - floor(sector)
    val p = v * (1f - s)
    val q = v * (1f - s * f)
    val t = v * (1f - s * (1f - f))
    val (r, g, b) = when (i) {
        0 -> Triple(v, t, p)
        1 -> Triple(q, v, p)
        2 -> Triple(p, v, t)
        3 -> Triple(p, q, v)
        4 -> Triple(t, p, v)
        else -> Triple(v, p, q)
    }
    return rgb(r * 255f, g * 255f, b * 255f)
}
