package org.example.project.ui.common

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The reveal shapes for the s-Blur / s-Splash tools — the KMP stand-in for the LAS shape-mask PNGs
 * (`square/mask/mN.png`). Each is built procedurally as a Compose [Path] so no binary mask assets
 * are needed and the same shape renders crisply at any size (preview or full-resolution bake).
 */
enum class RevealShape { Circle, Square, Rounded, Triangle, Diamond, Hexagon, Star, Heart }

/** Builds [shape] as a path filling a [size] x [size] box whose top-left is the origin. */
internal fun buildShapePath(shape: RevealShape, size: Float): Path {
    val p = Path()
    when (shape) {
        RevealShape.Circle -> p.addOval(Rect(0f, 0f, size, size))
        RevealShape.Square -> p.addRect(Rect(0f, 0f, size, size))
        RevealShape.Rounded -> p.addRoundRect(
            RoundRect(Rect(0f, 0f, size, size), CornerRadius(size * 0.22f, size * 0.22f)),
        )
        RevealShape.Triangle -> {
            p.moveTo(size * 0.5f, 0f)
            p.lineTo(size, size)
            p.lineTo(0f, size)
            p.close()
        }
        RevealShape.Diamond -> {
            p.moveTo(size * 0.5f, 0f)
            p.lineTo(size, size * 0.5f)
            p.lineTo(size * 0.5f, size)
            p.lineTo(0f, size * 0.5f)
            p.close()
        }
        RevealShape.Hexagon -> regularPolygon(p, size, vertices = 6, rotationDeg = -90f)
        RevealShape.Star -> star(p, size, points = 5, innerRatio = 0.45f)
        RevealShape.Heart -> {
            p.moveTo(size * 0.5f, size * 0.9f)
            p.cubicTo(size * 0.05f, size * 0.55f, size * 0.2f, size * 0.08f, size * 0.5f, size * 0.34f)
            p.cubicTo(size * 0.8f, size * 0.08f, size * 0.95f, size * 0.55f, size * 0.5f, size * 0.9f)
            p.close()
        }
    }
    return p
}

private fun regularPolygon(p: Path, size: Float, vertices: Int, rotationDeg: Float) {
    val r = size / 2f
    val cx = size / 2f
    val cy = size / 2f
    val start = rotationDeg * PI.toFloat() / 180f
    for (i in 0 until vertices) {
        val a = start + i * (2f * PI.toFloat() / vertices)
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
}

private fun star(p: Path, size: Float, points: Int, innerRatio: Float) {
    val outer = size / 2f
    val inner = outer * innerRatio
    val cx = size / 2f
    val cy = size / 2f
    val start = -PI.toFloat() / 2f
    val step = PI.toFloat() / points
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val a = start + i * step
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
}
