package org.example.project.ui.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CropMathTest {

    private val bounds = Rect(0f, 0f, 1000f, 1000f)

    @Test
    fun hitTest_findsCornerWithinRadius() {
        val rect = Rect(100f, 100f, 300f, 300f)
        assertEquals(CropHandle.Corner(RectCorner.TopLeft), hitTest(Offset(105f, 108f), rect, handleRadius = 20f))
        assertEquals(CropHandle.Corner(RectCorner.BottomRight), hitTest(Offset(295f, 305f), rect, handleRadius = 20f))
    }

    @Test
    fun hitTest_prefersCornerOverMoveWhenBothApply() {
        // A point inside the rect but within the corner's radius should still resolve to Corner.
        val rect = Rect(100f, 100f, 300f, 300f)
        val result = hitTest(Offset(110f, 110f), rect, handleRadius = 20f)
        assertEquals(CropHandle.Corner(RectCorner.TopLeft), result)
    }

    @Test
    fun hitTest_findsMoveWhenInsideButFarFromCorners() {
        val rect = Rect(100f, 100f, 300f, 300f)
        assertEquals(CropHandle.Move, hitTest(Offset(200f, 200f), rect, handleRadius = 20f))
    }

    @Test
    fun hitTest_returnsNullOutsideRect() {
        val rect = Rect(100f, 100f, 300f, 300f)
        assertEquals(null, hitTest(Offset(50f, 50f), rect, handleRadius = 20f))
    }

    @Test
    fun translate_movesRectByDelta() {
        val rect = Rect(100f, 100f, 300f, 300f)
        val moved = translateCropRect(rect, Offset(50f, -30f), bounds)
        assertEquals(Rect(150f, 70f, 350f, 270f), moved)
    }

    @Test
    fun translate_clampsToBounds() {
        val rect = Rect(100f, 100f, 300f, 300f)
        val moved = translateCropRect(rect, Offset(-500f, -500f), bounds)
        assertEquals(0f, moved.left)
        assertEquals(0f, moved.top)
        assertEquals(200f, moved.width)
        assertEquals(200f, moved.height)
    }

    @Test
    fun resize_freeFormGrowsFromOppositeAnchor() {
        val rect = Rect(200f, 200f, 400f, 400f)
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.BottomRight,
            delta = Offset(50f, 20f),
            ratio = null,
            bounds = bounds,
            minSize = 10f,
        )
        // TopLeft anchor stays fixed; BottomRight moves by the drag delta.
        assertEquals(Rect(200f, 200f, 450f, 420f), resized)
    }

    @Test
    fun resize_topLeftCornerKeepsBottomRightAnchored() {
        val rect = Rect(200f, 200f, 400f, 400f)
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.TopLeft,
            delta = Offset(30f, 30f),
            ratio = null,
            bounds = bounds,
            minSize = 10f,
        )
        assertEquals(Rect(230f, 230f, 400f, 400f), resized)
    }

    @Test
    fun resize_enforcesAspectRatioLock() {
        val rect = Rect(200f, 200f, 400f, 400f) // 200x200, 1:1
        // Drag the bottom-right corner mostly horizontally; height must follow to keep ratio.
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.BottomRight,
            delta = Offset(200f, 0f),
            ratio = 2f, // width:height = 2:1
            bounds = bounds,
            minSize = 10f,
        )
        val width = resized.width
        val height = resized.height
        assertTrue((width / height - 2f) < 0.01f, "expected ~2:1 ratio, got ${width}x$height")
        // Anchor (top-left) must not move.
        assertEquals(200f, resized.left)
        assertEquals(200f, resized.top)
    }

    @Test
    fun resize_respectsMinimumSize() {
        val rect = Rect(200f, 200f, 400f, 400f)
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.BottomRight,
            delta = Offset(-190f, -190f), // try to shrink to near-zero
            ratio = null,
            bounds = bounds,
            minSize = 50f,
        )
        assertTrue(resized.width >= 50f, "width ${resized.width} below minSize")
        assertTrue(resized.height >= 50f, "height ${resized.height} below minSize")
    }

    @Test
    fun resize_clampsToImageBounds() {
        val tightBounds = Rect(0f, 0f, 500f, 500f)
        val rect = Rect(200f, 200f, 400f, 400f)
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.BottomRight,
            delta = Offset(1000f, 1000f), // drag way past the image edge
            ratio = null,
            bounds = tightBounds,
            minSize = 10f,
        )
        assertTrue(resized.right <= tightBounds.right, "right ${resized.right} exceeds bounds")
        assertTrue(resized.bottom <= tightBounds.bottom, "bottom ${resized.bottom} exceeds bounds")
    }

    @Test
    fun resize_withRatio_staysWithinBoundsWhenAnchorNearEdge() {
        val tightBounds = Rect(0f, 0f, 500f, 500f)
        // Anchor (top-left, fixed) is near the right edge already; growing right+down with a 1:1
        // lock must not push the rect past tightBounds on either axis.
        val rect = Rect(450f, 100f, 480f, 130f)
        val resized = resizeCropRect(
            rect = rect,
            corner = RectCorner.BottomRight,
            delta = Offset(300f, 10f),
            ratio = 1f,
            bounds = tightBounds,
            minSize = 10f,
        )
        assertTrue(resized.right <= tightBounds.right + 0.01f, "right ${resized.right} exceeds bounds")
        assertTrue(resized.bottom <= tightBounds.bottom + 0.01f, "bottom ${resized.bottom} exceeds bounds")
        assertEquals(450f, resized.left)
        assertEquals(100f, resized.top)
    }
}
