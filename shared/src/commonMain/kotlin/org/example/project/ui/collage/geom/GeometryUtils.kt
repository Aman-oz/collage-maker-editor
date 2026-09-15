package org.example.project.ui.collage.geom

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Polygon geometry for collage slots, ported from the LAS `GeometryUtils`. Insets each slot polygon
 * for the border `space`, rounds its corners into a Compose [Path] for `corner`, and answers
 * point-in-polygon for touch. All points must be ordered clockwise.
 *
 * The original relied on object identity in a few spots (a point being the *same instance* as a
 * neighbour, or present in the convex hull); those comparisons are kept as `===`/`!==` so behaviour
 * matches even though [PointF] also defines value equality (needed for the shrink-map `HashMap`).
 */
internal object GeometryUtils {

    private fun Double.toDegrees(): Double = this * 180.0 / PI

    /**
     * True if [test] lies inside the polygon [points].
     * See http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
     */
    fun contains(points: List<PointF>, test: PointF): Boolean {
        var result = false
        var i = 0
        var j = points.size - 1
        while (i < points.size) {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                test.x < (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y - points[i].y) + points[i].x
            ) {
                result = !result
            }
            j = i
            i++
        }
        return result
    }

    fun createRectanglePath(outPath: Path, width: Float, height: Float, corner: Float) {
        val pointList = arrayListOf(
            PointF(0f, 0f),
            PointF(width, 0f),
            PointF(width, height),
            PointF(0f, height),
        )
        createPathWithCircleCorner(outPath, pointList, corner)
    }

    fun createRegularPolygonPath(outPath: Path, size: Float, vertexCount: Int, corner: Float) {
        createRegularPolygonPath(outPath, size, size / 2, size / 2, vertexCount, corner)
    }

    fun createRegularPolygonPath(
        outPath: Path,
        size: Float,
        centerX: Float,
        centerY: Float,
        vertexCount: Int,
        corner: Float,
    ) {
        val section = (2.0 * PI / vertexCount).toFloat()
        val radius = size / 2
        val pointList = ArrayList<PointF>()
        pointList.add(PointF(centerX + radius * cos(0f), centerY + radius * sin(0f)))
        for (i in 1 until vertexCount) {
            pointList.add(
                PointF(
                    centerX + radius * cos(section * i),
                    centerY + radius * sin(section * i),
                ),
            )
        }
        createPathWithCircleCorner(outPath, pointList, corner)
    }

    fun shrinkPathCollageUsingMap(pointList: List<PointF>, space: Float, map: HashMap<PointF, PointF>): List<PointF> {
        val result = ArrayList<PointF>()
        for (p in pointList) {
            val add = map[p]!!
            result.add(PointF(p.x + add.x * space, p.y + add.y * space))
        }
        return result
    }

    /** Resolve the special "3_3" collage frame. */
    fun shrinkPathCollage_3_3(pointList: List<PointF>, centerPointIdx: Int, space: Float, bound: RectF?): List<PointF> {
        val result = ArrayList<PointF>()
        val center = pointList[centerPointIdx]
        val left: PointF = if (centerPointIdx > 0) pointList[centerPointIdx - 1] else pointList[pointList.size - 1]
        val right: PointF = if (centerPointIdx < pointList.size - 1) pointList[centerPointIdx + 1] else pointList[0]

        for (p in pointList) {
            val pointF = PointF()
            var spaceX = space
            var spaceY = space
            if (bound != null) {
                if ((bound.left == 0f && p.x < center.x) || (bound.right == 1f && p.x >= center.x)) spaceX = 2 * space
                if ((bound.top == 0f && p.y < center.y) || (bound.bottom == 1f && p.y >= center.y)) spaceY = 2 * space
            }

            if (left.x == right.x) {
                if (left.x < center.x) {
                    pointF.x = if (p.x <= center.x) p.x + spaceX else p.x - spaceX
                } else {
                    pointF.x = if (p.x < center.x) p.x + spaceX else p.x - spaceX
                }

                if (p !== left && p !== right && p !== center) {
                    pointF.y = if (p.y < center.y) p.y + spaceY else p.y - spaceY
                } else if (p === left || p === right) {
                    pointF.y = if (p.y < center.y) p.y - space else p.y + space
                } else {
                    pointF.y = p.y
                }
            }
            result.add(pointF)
        }
        return result
    }

    fun shrinkPath(pointList: List<PointF>, space: Float, bound: RectF?): List<PointF> {
        val result = ArrayList<PointF>()
        if (space == 0f) {
            result.addAll(pointList)
        } else {
            val center = PointF(0f, 0f)
            for (p in pointList) {
                center.x += p.x
                center.y += p.y
            }
            center.x /= pointList.size
            center.y /= pointList.size
            for (p in pointList) {
                val pointF = PointF()
                var spaceX = space
                var spaceY = space
                if (bound != null) {
                    if ((bound.left == 0f && p.x < center.x) || (bound.right == 1f && p.x >= center.x)) spaceX = 2 * space
                    if ((bound.top == 0f && p.y < center.y) || (bound.bottom == 1f && p.y >= center.y)) spaceY = 2 * space
                }

                if (abs(center.x - p.x) >= 1) {
                    if (p.x < center.x) pointF.x = p.x + spaceX else if (p.x > center.x) pointF.x = p.x - spaceX
                } else {
                    pointF.x = p.x
                }

                if (abs(center.y - p.y) >= 1) {
                    if (p.y < center.y) pointF.y = p.y + spaceY else if (p.y > center.y) pointF.y = p.y - spaceY
                } else {
                    pointF.y = p.y
                }
                result.add(pointF)
            }
        }
        return result
    }

    fun commonShrinkPath(pointList: List<PointF>, space: Float, shrunkPointLeftRightDistances: Map<PointF, PointF>): List<PointF> {
        val result = ArrayList<PointF>()
        if (space == 0f) {
            result.addAll(pointList)
        } else {
            val convexHull = jarvis(pointList)
            for (i in pointList.indices) {
                val center = pointList[i]
                var concave = true
                for (point in convexHull) {
                    if (center === point) {
                        concave = false
                        break
                    }
                }
                val left: PointF = if (i == 0) pointList[pointList.size - 1] else pointList[i - 1]
                val right: PointF = if (i == pointList.size - 1) pointList[0] else pointList[i + 1]
                val leftRightDistance = shrunkPointLeftRightDistances[center]!!
                val pointF = shrinkPoint(center, left, right, leftRightDistance.x * space, leftRightDistance.y * space, !concave, !concave)
                result.add(pointF ?: PointF(0f, 0f))
            }
        }
        return result
    }

    fun createPathWithCircleCorner(path: Path, pointList: List<PointF>, corner: Float) {
        path.reset()
        var firstPoints: Array<PointF>? = null
        val convexHull = jarvis(pointList)
        for (i in pointList.indices) {
            if (corner == 0f || pointList.size < 3) {
                if (i == 0) path.moveTo(pointList[i].x, pointList[i].y) else path.lineTo(pointList[i].x, pointList[i].y)
            } else {
                var concave = true
                for (p in convexHull) {
                    if (p === pointList[i]) {
                        concave = false
                        break
                    }
                }
                val center = PointF(pointList[i].x, pointList[i].y)
                val left = PointF()
                val right = PointF()
                if (i == 0) {
                    left.x = pointList[pointList.size - 1].x
                    left.y = pointList[pointList.size - 1].y
                } else {
                    left.x = pointList[i - 1].x
                    left.y = pointList[i - 1].y
                }
                if (i == pointList.size - 1) {
                    right.x = pointList[0].x
                    right.y = pointList[0].y
                } else {
                    right.x = pointList[i + 1].x
                    right.y = pointList[i + 1].y
                }

                val pointFs = arrayOf(PointF(), PointF(), PointF())
                val angles = DoubleArray(2)
                createArc(center, left, right, corner, angles, pointFs, concave)
                if (i == 0) path.moveTo(pointFs[1].x, pointFs[1].y) else path.lineTo(pointFs[1].x, pointFs[1].y)

                val oval = Rect(pointFs[0].x - corner, pointFs[0].y - corner, pointFs[0].x + corner, pointFs[0].y + corner)
                path.arcTo(oval, angles[0].toFloat(), angles[1].toFloat(), false)

                if (i == 0) firstPoints = pointFs
                if (i == pointList.size - 1) path.lineTo(firstPoints!![1].x, firstPoints[1].y)
            }
        }
        path.close()
    }

    private fun findPointOnSegment(a: PointF, b: PointF, dA: Double): PointF {
        if (dA == 0.0) return PointF(a.x, a.y)
        val result = PointF()
        val dAB = sqrt(((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)).toDouble()).toFloat()
        val dx = abs(a.x - b.x) * dA / dAB
        val dy = abs(a.y - b.y) * dA / dAB
        result.x = if (a.x > b.x) (a.x - dx).toFloat() else (a.x + dx).toFloat()
        result.y = if (a.y > b.y) (a.y - dy).toFloat() else (a.y + dy).toFloat()
        return result
    }

    private fun createArc(a: PointF, b: PointF, c: PointF, dA: Float, outAngles: DoubleArray, outPoints: Array<PointF>, isConcave: Boolean) {
        outPoints[0] = findPointOnBisector(a, b, c, dA)
        val d = sqrt(
            (((a.x - outPoints[0].x) * (a.x - outPoints[0].x) + (a.y - outPoints[0].y) * (a.y - outPoints[0].y)) - dA * dA).toDouble(),
        )
        outPoints[1] = findPointOnSegment(a, b, d)
        outPoints[2] = findPointOnSegment(a, c, d)
        val dMA = sqrt(((a.x - outPoints[0].x) * (a.x - outPoints[0].x) + (a.y - outPoints[0].y) * (a.y - outPoints[0].y)).toDouble())
        val halfSweepAngle = acos(dA / dMA)
        val startAngle = atan2((outPoints[1].y - outPoints[0].y).toDouble(), (outPoints[1].x - outPoints[0].x).toDouble())
        val endAngle = atan2((outPoints[2].y - outPoints[0].y).toDouble(), (outPoints[2].x - outPoints[0].x).toDouble())
        var sweepAngle = endAngle - startAngle
        if (!isConcave) sweepAngle = 2 * halfSweepAngle

        outAngles[0] = startAngle.toDegrees()
        outAngles[1] = sweepAngle.toDegrees()
        val tmp = (2 * halfSweepAngle).toDegrees()
        if (abs(tmp - abs(outAngles[1])) > 1) outAngles[1] = -tmp
    }

    private fun findPointOnBisector(a: PointF, b: PointF, c: PointF, dA: Float): PointF {
        val lineAB = getCoefficients(a, b)
        val lineAC = getCoefficients(a, c)
        val vB = lineAC[0] * b.x + lineAC[1] * b.y + lineAC[2]
        val vC = lineAB[0] * c.x + lineAB[1] * c.y + lineAB[2]
        val square1 = sqrt(lineAB[0] * lineAB[0] + lineAB[1] * lineAB[1])
        val square2 = sqrt(lineAC[0] * lineAC[0] + lineAC[1] * lineAC[1])
        return if (vC > 0) {
            if (vB > 0) {
                findIntersectPoint(lineAB[0], lineAB[1], dA * square1 - lineAB[2], lineAC[0], lineAC[1], dA * square2 - lineAC[2])
            } else {
                findIntersectPoint(lineAB[0], lineAB[1], dA * square1 - lineAB[2], -lineAC[0], -lineAC[1], dA * square2 + lineAC[2])
            }
        } else {
            if (vB > 0) {
                findIntersectPoint(-lineAB[0], -lineAB[1], dA * square1 + lineAB[2], lineAC[0], lineAC[1], dA * square2 - lineAC[2])
            } else {
                findIntersectPoint(-lineAB[0], -lineAB[1], dA * square1 + lineAB[2], -lineAC[0], -lineAC[1], dA * square2 + lineAC[2])
            }
        }!!
    }

    /** Shrunk point of A, dAB/dAC from lines AB/AC, b/c whether it stays on the same half-plane. */
    private fun shrinkPoint(a: PointF, b: PointF, c: PointF, dAB: Float, dAC: Float, b2: Boolean, c2: Boolean): PointF? {
        val ab = getCoefficients(a, b)
        val ac = getCoefficients(a, c)
        val m = dAB * sqrt(ab[0] * ab[0] + ab[1] * ab[1]) - ab[2]
        val n = dAC * sqrt(ac[0] * ac[0] + ac[1] * ac[1]) - ac[2]
        val p = -dAB * sqrt(ab[0] * ab[0] + ab[1] * ab[1]) - ab[2]
        val q = -dAC * sqrt(ac[0] * ac[0] + ac[1] * ac[1]) - ac[2]
        val p1 = findIntersectPoint(ab[0], ab[1], m, ac[0], ac[1], n)
        val p2 = findIntersectPoint(ab[0], ab[1], m, ac[0], ac[1], q)
        val p3 = findIntersectPoint(ab[0], ab[1], p, ac[0], ac[1], n)
        val p4 = findIntersectPoint(ab[0], ab[1], p, ac[0], ac[1], q)
        return when {
            testShrunkPoint(ab, ac, b, c, p1, b2, c2) -> p1
            testShrunkPoint(ab, ac, b, c, p2, b2, c2) -> p2
            testShrunkPoint(ab, ac, b, c, p3, b2, c2) -> p3
            testShrunkPoint(ab, ac, b, c, p4, b2, c2) -> p4
            else -> null
        }
    }

    private fun testShrunkPoint(ab: DoubleArray, ac: DoubleArray, b: PointF, c: PointF, p: PointF?, bFlag: Boolean, cFlag: Boolean): Boolean {
        if (p != null && p.x < Float.MAX_VALUE && p.y < Float.MAX_VALUE) {
            val signC = (ab[0] * p.x + ab[1] * p.y + ab[2]) * (ab[0] * c.x + ab[1] * c.y + ab[2])
            val signB = (ac[0] * p.x + ac[1] * p.y + ac[2]) * (ac[0] * b.x + ac[1] * b.y + ac[2])
            val testC = signC > Double.MIN_VALUE
            val testB = signB > Double.MIN_VALUE
            if (testC == cFlag && testB == bFlag) return true
        }
        return false
    }

    /** Solve ax+by=c, dx+ey=f. Null = no solution; (MAX,MAX) = infinite solutions. */
    private fun findIntersectPoint(a: Double, b: Double, c: Double, d: Double, e: Double, f: Double): PointF? {
        val det = a * e - b * d
        val dx = c * e - b * f
        val dy = a * f - c * d
        return when {
            det == 0.0 && dx == 0.0 -> PointF(Float.MAX_VALUE, Float.MAX_VALUE)
            det == 0.0 && dx != 0.0 -> null
            else -> PointF((dx / det).toFloat(), (dy / det).toFloat())
        }
    }

    private fun getCoefficients(a: PointF, b: PointF): DoubleArray {
        val aa = (b.y - a.y).toDouble()
        val bb = (a.x - b.x).toDouble()
        val cc = (b.x * a.y - a.x * b.y).toDouble()
        return doubleArrayOf(aa, bb, cc)
    }

    private fun ccw(p: PointF, q: PointF, r: PointF): Boolean {
        val value = (q.y.toInt() - p.y.toInt()) * (r.x.toInt() - q.x.toInt()) - (q.x.toInt() - p.x.toInt()) * (r.y.toInt() - q.y.toInt())
        return value < 0
    }

    /** Convex hull (gift-wrapping); the returned points are instances from [points]. */
    fun jarvis(points: List<PointF>): ArrayList<PointF> {
        val result = ArrayList<PointF>()
        val n = points.size
        if (n < 3) {
            result.addAll(points)
            return result
        }
        val next = IntArray(n) { -1 }
        var leftMost = 0
        for (i in 1 until n) {
            if (points[i].x.toInt() < points[leftMost].x.toInt()) leftMost = i
        }
        var p = leftMost
        var q: Int
        do {
            q = (p + 1) % n
            for (i in 0 until n) {
                if (ccw(points[p], points[i], points[q])) q = i
            }
            next[p] = q
            p = q
        } while (p != leftMost)

        for (i in next.indices) {
            if (next[i] != -1) result.add(points[i])
        }
        return result
    }
}
