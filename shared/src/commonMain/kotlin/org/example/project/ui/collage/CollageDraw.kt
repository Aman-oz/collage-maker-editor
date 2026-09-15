package org.example.project.ui.collage

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.example.project.ui.collage.geom.GeometryUtils
import org.example.project.ui.collage.geom.PhotoItem
import org.example.project.ui.collage.geom.PointF
import org.example.project.ui.collage.geom.RectF
import org.example.project.ui.common.drawImageCropped
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The resolved geometry of one collage slot at a concrete canvas size: where it sits ([leftPx],
 * [topPx], [wPx], [hPx]), the [clipPath] to render its photo through (in slot-local pixels), and the
 * [touchPolygon] in canvas pixels for hit-testing. This is the Compose equivalent of what
 * `FrameImageView` computes in `setSpace`/`addPhotoItemView`.
 */
internal class SlotGeometry(
    val item: PhotoItem,
    val leftPx: Float,
    val topPx: Float,
    val wPx: Float,
    val hPx: Float,
    val clipPath: Path,
    val touchPolygon: List<PointF>,
)

/**
 * Resolves every slot in [template] to a [SlotGeometry] for a [canvasW]x[canvasH] canvas, insetting
 * each slot polygon by [spacePx] (the border gap) and rounding its corners by [cornerPx] — a direct
 * port of the LAS `FramePhotoLayout.addPhotoItemView` + `FrameImageView.setSpace` math.
 */
internal fun computeSlotGeometries(
    template: org.example.project.ui.collage.geom.TemplateItem,
    canvasW: Float,
    canvasH: Float,
    spacePx: Float,
    cornerPx: Float,
): List<SlotGeometry> = template.photoItemList.map { item ->
    val leftPx = canvasW * item.bound.left
    val topPx = canvasH * item.bound.top
    val wPx = if (item.bound.right == 1f) canvasW - leftPx else canvasW * item.bound.width() + 0.5f
    val hPx = if (item.bound.bottom == 1f) canvasH - topPx else canvasH * item.bound.height() + 0.5f

    val clipPath = Path()
    val touchPolygon = ArrayList<PointF>()
    buildSlotPath(item, wPx, hPx, spacePx, cornerPx, clipPath, touchPolygon)

    // Move the touch polygon into canvas space for hit-testing.
    val canvasPolygon = touchPolygon.map { PointF(it.x + leftPx, it.y + topPx) }
    SlotGeometry(item, leftPx, topPx, wPx, hPx, clipPath, canvasPolygon)
}

/** Builds the slot-local clip [outPath] and the (slot-local) [outPolygon] used for hit-testing. */
private fun buildSlotPath(
    item: PhotoItem,
    viewWidth: Float,
    viewHeight: Float,
    space: Float,
    corner: Float,
    outPath: Path,
    outPolygon: MutableList<PointF>,
) {
    val convertedPoints = item.pointList.map { PointF(it.x * viewWidth, it.y * viewHeight) }

    // Optional "clear" (subtracted) region.
    var clearPath: Path? = null
    val clearAreaPoints = item.clearAreaPoints
    if (clearAreaPoints != null && clearAreaPoints.isNotEmpty()) {
        val convertedClear = clearAreaPoints.map { PointF(it.x * viewWidth, it.y * viewHeight) }
        clearPath = Path().also { GeometryUtils.createPathWithCircleCorner(it, convertedClear, corner) }
    } else if (item.clearPath != null) {
        clearPath = Path().also { buildRealClearPath(viewWidth, viewHeight, item, it, corner) }
    }

    if (item.path != null) {
        // Custom-shape slot (heart, circle, polygon).
        buildRealPath(viewWidth, viewHeight, item, outPath, space, corner)
        val bounds = outPath.getBounds()
        outPolygon.add(PointF(bounds.left, bounds.top))
        outPolygon.add(PointF(bounds.right, bounds.top))
        outPolygon.add(PointF(bounds.right, bounds.bottom))
        outPolygon.add(PointF(bounds.left, bounds.bottom))
    } else {
        val shrunk = shrinkPointsForItem(item, convertedPoints, space)
        outPolygon.addAll(shrunk)
        GeometryUtils.createPathWithCircleCorner(outPath, shrunk, corner)
    }

    if (clearPath != null) {
        val combined = Path()
        combined.op(outPath, clearPath, PathOperation.Difference)
        outPath.reset()
        outPath.addPath(combined)
    }
}

private fun shrinkPointsForItem(item: PhotoItem, convertedPoints: List<PointF>, space: Float): List<PointF> = when {
    item.shrinkMethod == PhotoItem.SHRINK_METHOD_3_3 -> {
        val centerIdx = findCenterPointIndex(item)
        GeometryUtils.shrinkPathCollage_3_3(convertedPoints, centerIdx, space, item.bound)
    }

    item.shrinkMethod == PhotoItem.SHRINK_METHOD_USING_MAP && item.shrinkMap != null -> {
        val map = convertedShrinkMap(item, convertedPoints)
        GeometryUtils.shrinkPathCollageUsingMap(convertedPoints, space, map)
    }

    item.shrinkMethod == PhotoItem.SHRINK_METHOD_COMMON && item.shrinkMap != null -> {
        val map = convertedShrinkMap(item, convertedPoints)
        GeometryUtils.commonShrinkPath(convertedPoints, space, map)
    }

    else -> {
        val effectiveSpace = if (item.disableShrink) 0f else space
        GeometryUtils.shrinkPath(convertedPoints, effectiveSpace, item.bound)
    }
}

/** Re-keys the item's normalized shrink map onto the pixel-space [convertedPoints] (as LAS does). */
private fun convertedShrinkMap(item: PhotoItem, convertedPoints: List<PointF>): HashMap<PointF, PointF> {
    val map = HashMap<PointF, PointF>()
    val shrinkMap = item.shrinkMap!!
    item.pointList.forEachIndexed { index, original ->
        shrinkMap[original]?.let { map[convertedPoints[index]] = it }
    }
    return map
}

private fun findCenterPointIndex(item: PhotoItem): Int {
    var centerPointIdx = 0
    if (item.bound.left == 0f && item.bound.top == 0f) {
        var minX = 1f
        item.pointList.forEachIndexed { idx, p ->
            if (p.x > 0 && p.x < 1 && p.y > 0 && p.y < 1 && p.x < minX) {
                centerPointIdx = idx
                minX = p.x
            }
        }
    } else {
        var maxX = 0f
        item.pointList.forEachIndexed { idx, p ->
            if (p.x > 0 && p.x < 1 && p.y > 0 && p.y < 1 && p.x > maxX) {
                centerPointIdx = idx
                maxX = p.x
            }
        }
    }
    return centerPointIdx
}

/** Port of `FrameImageView.buildRealPath` — scales/positions a custom shape path into the slot. */
private fun buildRealPath(viewWidth: Float, viewHeight: Float, item: PhotoItem, outPath: Path, space: Float, corner: Float) {
    val src = item.path ?: return
    var sp = space
    val rect = src.getBounds()
    val pathWidthPixels = rect.width
    val pathHeightPixels = rect.height
    sp = 2 * sp
    outPath.reset()
    outPath.addPath(src)

    var ratioX: Float
    var ratioY: Float
    if (item.fitBound) {
        ratioX = item.pathScaleRatio * (viewWidth * item.pathRatioBound!!.width() - 2 * sp) / pathWidthPixels
        ratioY = item.pathScaleRatio * (viewHeight * item.pathRatioBound!!.height() - 2 * sp) / pathHeightPixels
    } else {
        val ratio = min(
            item.pathScaleRatio * (viewHeight - 2 * sp) / pathHeightPixels,
            item.pathScaleRatio * (viewWidth - 2 * sp) / pathWidthPixels,
        )
        ratioX = ratio
        ratioY = ratio
    }
    outPath.transform(Matrix().apply { scale(ratioX, ratioY) })

    var bound = outPath.getBounds()
    when (item.cornerMethod) {
        PhotoItem.CORNER_METHOD_3_6 -> {
            GeometryUtils.createRegularPolygonPath(outPath, min(bound.width, bound.height), 6, corner)
            bound = outPath.getBounds()
        }
        PhotoItem.CORNER_METHOD_3_13 -> {
            GeometryUtils.createRectanglePath(outPath, bound.width, bound.height, corner)
            bound = outPath.getBounds()
        }
    }

    var x: Float
    var y: Float
    if (item.shrinkMethod == PhotoItem.SHRINK_METHOD_3_6 || item.shrinkMethod == PhotoItem.SHRINK_METHOD_3_8) {
        x = viewWidth / 2 - bound.width / 2
        y = viewHeight / 2 - bound.height / 2
    } else {
        if (item.pathAlignParentRight) {
            x = item.pathRatioBound!!.right * viewWidth - bound.width - sp / ratioX
            y = item.pathRatioBound!!.top * viewHeight + sp / ratioY
        } else {
            x = item.pathRatioBound!!.left * viewWidth + sp / ratioX
            y = item.pathRatioBound!!.top * viewHeight + sp / ratioY
        }
        if (item.pathInCenterHorizontal) x = viewWidth / 2f - bound.width / 2f
        if (item.pathInCenterVertical) y = viewHeight / 2f - bound.height / 2f
    }
    outPath.transform(Matrix().apply { translate(x, y) })
}

/** Port of `FrameImageView.buildRealClearPath`. */
private fun buildRealClearPath(viewWidth: Float, viewHeight: Float, item: PhotoItem, clearPath: Path, corner: Float) {
    val src = item.clearPath ?: return
    val rect = src.getBounds()
    val clearPathWidthPixels = rect.width
    val clearPathHeightPixels = rect.height
    clearPath.reset()
    clearPath.addPath(src)

    var ratioX: Float
    var ratioY: Float
    if (item.fitBound) {
        ratioX = item.clearPathScaleRatio * viewWidth * item.clearPathRatioBound!!.width() / clearPathWidthPixels
        ratioY = item.clearPathScaleRatio * viewHeight * item.clearPathRatioBound!!.height() / clearPathHeightPixels
    } else {
        val ratio = min(
            item.clearPathScaleRatio * viewHeight / clearPathHeightPixels,
            item.clearPathScaleRatio * viewWidth / clearPathWidthPixels,
        )
        ratioX = ratio
        ratioY = ratio
    }
    clearPath.transform(Matrix().apply { scale(ratioX, ratioY) })

    var bound = clearPath.getBounds()
    when (item.cornerMethod) {
        PhotoItem.CORNER_METHOD_3_6 -> {
            GeometryUtils.createRegularPolygonPath(clearPath, min(bound.width, bound.height), 6, corner)
            bound = clearPath.getBounds()
        }
        PhotoItem.CORNER_METHOD_3_13 -> {
            GeometryUtils.createRectanglePath(clearPath, bound.width, bound.height, corner)
            bound = clearPath.getBounds()
        }
    }

    var x: Float
    var y: Float
    if (item.shrinkMethod == PhotoItem.SHRINK_METHOD_3_6) {
        x = if (item.clearPathRatioBound!!.left > 0) viewWidth - bound.width / 2 else -bound.width / 2
        y = viewHeight / 2 - bound.height / 2
    } else if (item.centerInClearBound) {
        x = item.clearPathRatioBound!!.left * viewWidth + (viewWidth / 2 - bound.width / 2)
        y = item.clearPathRatioBound!!.top * viewHeight + (viewHeight / 2 - bound.height / 2)
    } else {
        x = item.clearPathRatioBound!!.left * viewWidth
        y = item.clearPathRatioBound!!.top * viewHeight
        if (item.clearPathInCenterHorizontal) x = viewWidth / 2f - bound.width / 2f
        if (item.clearPathInCenterVertical) y = viewHeight / 2f - bound.height / 2f
    }
    clearPath.transform(Matrix().apply { translate(x, y) })
}

/**
 * Draws the whole collage: [background] fill, then each slot's photo center-cropped and clipped to
 * its polygon. [selectedIndex] (if any) is outlined, and empty slots get a faint fill so the user
 * sees where to tap. Shared by the on-screen preview and the final bake.
 */
internal fun DrawScope.drawCollage(
    geometries: List<SlotGeometry>,
    images: Map<Int, ImageBitmap>,
    background: Color,
    canvasW: Float,
    canvasH: Float,
    emptySlotColor: Color,
    selectedIndex: Int? = null,
    selectionColor: Color = Color.Transparent,
) {
    drawRect(color = background, size = Size(canvasW, canvasH))
    for (g in geometries) {
        translate(g.leftPx, g.topPx) {
            clipPath(g.clipPath) {
                val image = images[g.item.index]
                if (image != null) {
                    drawImageCropped(
                        image = image,
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(g.wPx.roundToInt().coerceAtLeast(1), g.hPx.roundToInt().coerceAtLeast(1)),
                    )
                } else {
                    drawRect(color = emptySlotColor, size = Size(g.wPx, g.hPx))
                }
            }
            if (selectedIndex == g.item.index) {
                drawPath(path = g.clipPath, color = selectionColor, style = Stroke(width = 3f * density))
            }
        }
    }
}

/** Which slot (topmost first) contains the canvas-space point, or `null`. */
internal fun hitTestSlot(geometries: List<SlotGeometry>, point: Offset): SlotGeometry? {
    val p = PointF(point.x, point.y)
    for (i in geometries.indices.reversed()) {
        if (GeometryUtils.contains(geometries[i].touchPolygon, p)) return geometries[i]
    }
    return null
}
