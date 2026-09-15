package org.example.project.ui.collage.geom

import androidx.compose.ui.graphics.Path

/**
 * One photo slot in a collage layout, ported from the LAS `PhotoItem`. A slot is positioned by
 * [bound] (a `0f..1f` rectangle of the whole collage) and shaped by [pointList] — a polygon whose
 * points are `0f..1f` of the slot's own [bound] box. A few decorative slots instead use a custom
 * [path] (hearts, polygons) or subtract a [clearArea]/[clearPath].
 *
 * [shrinkMethod]/[cornerMethod]/[shrinkMap] tune how the polygon insets for the border `space` and
 * rounds for the `corner` radius; see [GeometryUtils] and the collage renderer.
 */
class PhotoItem {
    var x = 0f
    var y = 0f
    var index = 0
    var imagePath: String? = null
    var maskPath: String? = null

    // Polygon points and bounding box, all in [0,1]. `var` mirrors the original PhotoItem — a few
    // generators replace the whole bound/list rather than mutating it in place.
    var pointList = ArrayList<PointF>()
    var bound = RectF()

    // Custom shape (used by a handful of decorative layouts instead of pointList).
    var path: Path? = null
    var pathRatioBound: RectF? = null
    var pathInCenterHorizontal = false
    var pathInCenterVertical = false
    var pathAlignParentRight = false
    var pathScaleRatio = 1f
    var fitBound = false

    // Other info
    var hasBackground = false
    var shrinkMethod = SHRINK_METHOD_DEFAULT
    var cornerMethod = CORNER_METHOD_DEFAULT
    var disableShrink = false
    var shrinkMap: HashMap<PointF, PointF>? = null

    // Clear (subtract) a polygon or path area from the slot.
    var clearAreaPoints: ArrayList<PointF>? = null
    var clearPath: Path? = null
    var clearPathRatioBound: RectF? = null
    var clearPathInCenterHorizontal = false
    var clearPathInCenterVertical = false
    var clearPathAlignParentRight = false
    var clearPathScaleRatio = 1f
    var centerInClearBound = false

    companion object {
        const val SHRINK_METHOD_DEFAULT = 0
        const val SHRINK_METHOD_3_3 = 1
        const val SHRINK_METHOD_USING_MAP = 2
        const val SHRINK_METHOD_3_6 = 3
        const val SHRINK_METHOD_3_8 = 4
        const val SHRINK_METHOD_COMMON = 5
        const val CORNER_METHOD_DEFAULT = 0
        const val CORNER_METHOD_3_6 = 1
        const val CORNER_METHOD_3_13 = 2
    }
}
