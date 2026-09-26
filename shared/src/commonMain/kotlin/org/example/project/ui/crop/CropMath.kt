package org.example.project.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A ratio chip in the bottom strip. [ratio] is width / height, `null` for free-form. [platform]
 * picks the social-network glyph drawn inside the chip's frame for sizes that are a platform's
 * standard (Instagram post/portrait/story, YouTube video).
 */
internal data class AspectRatioOption(val label: String, val ratio: Float?, val platform: RatioPlatform? = null)

internal enum class RatioPlatform { Instagram, YouTube }

internal val AspectRatioOptions = listOf(
    AspectRatioOption("Free", null),
    AspectRatioOption("1:1", 1f / 1f, RatioPlatform.Instagram),
    AspectRatioOption("9:16", 9f / 16f, RatioPlatform.Instagram),
    AspectRatioOption("4:5", 4f / 5f, RatioPlatform.Instagram),
    AspectRatioOption("16:9", 16f / 9f, RatioPlatform.YouTube),
    AspectRatioOption("4:3", 4f / 3f),
    AspectRatioOption("3:4", 3f / 4f),
    AspectRatioOption("5:4", 5f / 4f),
    AspectRatioOption("3:2", 3f / 2f),
    AspectRatioOption("2:3", 2f / 3f),
)

internal sealed interface CropHandle {
    data class Corner(val corner: RectCorner) : CropHandle
    data class Edge(val edge: RectEdge) : CropHandle
    data object Move : CropHandle
}

internal enum class RectCorner { TopLeft, TopRight, BottomLeft, BottomRight }

internal enum class RectEdge { Left, Top, Right, Bottom }

internal fun hitTest(pos: Offset, displayRect: Rect, handleRadius: Float): CropHandle? {
    val corners = mapOf(
        RectCorner.TopLeft to displayRect.topLeft,
        RectCorner.TopRight to Offset(displayRect.right, displayRect.top),
        RectCorner.BottomLeft to Offset(displayRect.left, displayRect.bottom),
        RectCorner.BottomRight to displayRect.bottomRight,
    )
    corners.forEach { (corner, point) ->
        if ((pos - point).getDistance() <= handleRadius) return CropHandle.Corner(corner)
    }
    // Edges are hit anywhere along their length (not just at the drawn midpoint bar), within the
    // same radius; corners were checked first so they win where the two overlap.
    val withinX = pos.x in displayRect.left..displayRect.right
    val withinY = pos.y in displayRect.top..displayRect.bottom
    when {
        withinY && abs(pos.x - displayRect.left) <= handleRadius -> return CropHandle.Edge(RectEdge.Left)
        withinY && abs(pos.x - displayRect.right) <= handleRadius -> return CropHandle.Edge(RectEdge.Right)
        withinX && abs(pos.y - displayRect.top) <= handleRadius -> return CropHandle.Edge(RectEdge.Top)
        withinX && abs(pos.y - displayRect.bottom) <= handleRadius -> return CropHandle.Edge(RectEdge.Bottom)
    }
    return if (displayRect.contains(pos)) CropHandle.Move else null
}

internal fun translateCropRect(rect: Rect, delta: Offset, bounds: Rect): Rect {
    val newLeft = (rect.left + delta.x).coerceIn(bounds.left, (bounds.right - rect.width).coerceAtLeast(bounds.left))
    val newTop = (rect.top + delta.y).coerceIn(bounds.top, (bounds.bottom - rect.height).coerceAtLeast(bounds.top))
    return Rect(newLeft, newTop, newLeft + rect.width, newTop + rect.height)
}

internal fun resizeCropRect(
    rect: Rect,
    corner: RectCorner,
    delta: Offset,
    ratio: Float?,
    bounds: Rect,
    minSize: Float,
): Rect {
    val anchor = when (corner) {
        RectCorner.TopLeft -> rect.bottomRight
        RectCorner.TopRight -> Offset(rect.left, rect.bottom)
        RectCorner.BottomLeft -> Offset(rect.right, rect.top)
        RectCorner.BottomRight -> rect.topLeft
    }
    val currentFree = when (corner) {
        RectCorner.TopLeft -> rect.topLeft
        RectCorner.TopRight -> Offset(rect.right, rect.top)
        RectCorner.BottomLeft -> Offset(rect.left, rect.bottom)
        RectCorner.BottomRight -> rect.bottomRight
    }

    val freeX = (currentFree.x + delta.x).coerceIn(bounds.left, bounds.right)
    val freeY = (currentFree.y + delta.y).coerceIn(bounds.top, bounds.bottom)

    var width = abs(freeX - anchor.x)
    var height = abs(freeY - anchor.y)

    if (ratio != null && ratio > 0f) {
        val safeWidth = width.coerceAtLeast(0.0001f)
        val safeHeight = height.coerceAtLeast(0.0001f)
        if (safeWidth / safeHeight > ratio) width = safeHeight * ratio else height = safeWidth / ratio
    }

    width = width.coerceAtLeast(minSize)
    height = height.coerceAtLeast(minSize)

    val signX = if (freeX >= anchor.x) 1f else -1f
    val signY = if (freeY >= anchor.y) 1f else -1f
    val clampedFreeX = (anchor.x + width * signX).coerceIn(bounds.left, bounds.right)
    val clampedFreeY = (anchor.y + height * signY).coerceIn(bounds.top, bounds.bottom)

    val left = min(anchor.x, clampedFreeX)
    val top = min(anchor.y, clampedFreeY)
    val right = max(anchor.x, clampedFreeX)
    val bottom = max(anchor.y, clampedFreeY)
    return Rect(left, top, right, bottom)
}

/**
 * Drags one [edge] of [rect] by [delta], keeping the opposite edge fixed.
 *
 * Free-form, only that edge moves. With a [ratio] lock, the dragged edge sets one dimension and the
 * other follows, centered on the rect's current midline (shifted back inside [bounds] if needed),
 * so pulling the right edge grows the crop evenly up and down.
 */
internal fun resizeCropEdge(
    rect: Rect,
    edge: RectEdge,
    delta: Offset,
    ratio: Float?,
    bounds: Rect,
    minSize: Float,
): Rect {
    if (ratio == null || ratio <= 0f) {
        return when (edge) {
            RectEdge.Left -> rect.copy(left = (rect.left + delta.x).coerceIn(bounds.left, rect.right - minSize))
            RectEdge.Right -> rect.copy(right = (rect.right + delta.x).coerceIn(rect.left + minSize, bounds.right))
            RectEdge.Top -> rect.copy(top = (rect.top + delta.y).coerceIn(bounds.top, rect.bottom - minSize))
            RectEdge.Bottom -> rect.copy(bottom = (rect.bottom + delta.y).coerceIn(rect.top + minSize, bounds.bottom))
        }
    }

    return when (edge) {
        RectEdge.Left, RectEdge.Right -> {
            val roomToEdge = if (edge == RectEdge.Left) rect.right - bounds.left else bounds.right - rect.left
            val maxWidth = min(roomToEdge, bounds.height * ratio)
            val minWidth = min(max(minSize, minSize * ratio), maxWidth)
            val proposed = if (edge == RectEdge.Left) rect.width - delta.x else rect.width + delta.x
            val width = proposed.coerceIn(minWidth, maxWidth)
            val height = width / ratio
            val left = if (edge == RectEdge.Left) rect.right - width else rect.left
            val top = (rect.center.y - height / 2f).coerceIn(bounds.top, bounds.bottom - height)
            Rect(left, top, left + width, top + height)
        }
        RectEdge.Top, RectEdge.Bottom -> {
            val roomToEdge = if (edge == RectEdge.Top) rect.bottom - bounds.top else bounds.bottom - rect.top
            val maxHeight = min(roomToEdge, bounds.width / ratio)
            val minHeight = min(max(minSize, minSize / ratio), maxHeight)
            val proposed = if (edge == RectEdge.Top) rect.height - delta.y else rect.height + delta.y
            val height = proposed.coerceIn(minHeight, maxHeight)
            val width = height * ratio
            val top = if (edge == RectEdge.Top) rect.bottom - height else rect.top
            val left = (rect.center.x - width / 2f).coerceIn(bounds.left, bounds.right - width)
            Rect(left, top, left + width, top + height)
        }
    }
}
