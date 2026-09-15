package org.example.project.ui.collage

import org.example.project.ui.collage.frames.FrameImageUtils
import org.example.project.ui.collage.geom.GeometryUtils
import org.example.project.ui.collage.geom.PointF
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the ported collage geometry that needs no platform `Path`: the generator dispatch (slot
 * counts / bounds / polygons) and the pure point math in [GeometryUtils]. The `Path`-clipping render
 * path is exercised on-device rather than here.
 */
class CollageGeometryTest {

    @Test
    fun twoUpGenerator_hasTwoHalfWidthSlots() {
        val item = FrameImageUtils.createTemplateItems("collage_2_0.png")
        assertNotNull(item)
        assertEquals(2, item.photoItemList.size)
        val first = item.photoItemList[0]
        assertEquals(0f, first.bound.left)
        assertEquals(0.5f, first.bound.right)
        assertEquals(4, first.pointList.size, "each rectangular slot is a 4-point polygon")
        assertEquals(0.5f, item.photoItemList[1].bound.left)
        assertEquals(1f, item.photoItemList[1].bound.right)
    }

    @Test
    fun generatorNames_encodePhotoCount() {
        assertEquals(1, FrameImageUtils.createTemplateItems("collage_1_0.png")?.photoItemList?.size)
        assertEquals(3, FrameImageUtils.createTemplateItems("collage_3_0.png")?.photoItemList?.size)
        assertEquals(6, FrameImageUtils.createTemplateItems("collage_6_0.png")?.photoItemList?.size)
    }

    @Test
    fun unknownName_returnsNull() {
        assertNull(FrameImageUtils.createTemplateItems("collage_does_not_exist.png"))
    }

    @Test
    fun contains_isTrueInsideSquareAndFalseOutside() {
        val square = listOf(PointF(0f, 0f), PointF(10f, 0f), PointF(10f, 10f), PointF(0f, 10f))
        assertTrue(GeometryUtils.contains(square, PointF(5f, 5f)))
        assertTrue(!GeometryUtils.contains(square, PointF(15f, 5f)))
    }

    @Test
    fun shrinkPath_insetsAFullBoundSquareOnBothSides() {
        // A full-bound slot (0,0,1,1): shrinkPath doubles the space on edges touching 0/1.
        val square = listOf(PointF(0f, 0f), PointF(100f, 0f), PointF(100f, 100f), PointF(0f, 100f))
        val bound = org.example.project.ui.collage.geom.RectF(0f, 0f, 1f, 1f)
        val shrunk = GeometryUtils.shrinkPath(square, space = 5f, bound = bound)
        assertEquals(4, shrunk.size)
        // Top-left corner moves inward by 2*space on both axes; bottom-right inward likewise.
        assertEquals(10f, shrunk[0].x)
        assertEquals(10f, shrunk[0].y)
        assertEquals(90f, shrunk[2].x)
        assertEquals(90f, shrunk[2].y)
    }

    @Test
    fun shrinkPath_withZeroSpace_returnsSamePoints() {
        val pts = listOf(PointF(0f, 0f), PointF(1f, 0f), PointF(1f, 1f), PointF(0f, 1f))
        assertEquals(pts, GeometryUtils.shrinkPath(pts, 0f, null))
    }
}
