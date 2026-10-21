package org.example.project.ui.stickers

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.project.ui.adjust.AdjustValues
import org.example.project.ui.adjust.AdjustmentType
import org.example.project.ui.adjust.contrastMatrix
import org.example.project.ui.adjust.multiplyColorMatrices
import org.example.project.ui.adjust.saturationAdjustMatrix
import org.example.project.ui.adjust.toColorMatrix
import org.example.project.ui.filter.PhotoFilters
import org.example.project.ui.filter.blendWithIdentity
import org.example.project.ui.text.TextLayerMove
import org.example.project.ui.text.TextPhotoAlign
import org.example.project.ui.text.normalizeDegrees
import org.example.project.ui.text.rotateVector

/**
 * A sticker placed on the photo, and every edit the Stickers tool's options make to it.
 *
 * Its place and size are fractions of the photo — [centerX] / [centerY] of its width and height,
 * [widthFraction] of its width — so the preview and the full-resolution bake agree without passing
 * sizes. [aspectRatio] is its box's width / height: the source's own shape ([naturalAspect]) as the
 * crop cuts it. [rotationDegrees] turns it clockwise about its centre. Reshape moves the box's four
 * corners on their own ([cornerOffsets]), and the picture is drawn in perspective onto that
 * quadrilateral. The list order is the stacking order, later on top.
 *
 * The crop and the eraser's strokes are in fractions of the *source* picture, so neither moves when
 * the sticker is resized, reshaped, flipped or turned. The shadow's offset and blur use the Text
 * tool's units ([StickerShadowUnit] of the sticker's longer side standing in for its em), so its
 * Shadow panel works on them as they are.
 */
@Serializable
internal data class PlacedSticker(
    val id: Long,
    val assetId: String,
    val imageUrl: String,
    val aspectRatio: Float,
    val isPremium: Boolean,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val widthFraction: Float = DefaultStickerSizeFraction,
    val rotationDegrees: Float = 0f,
    val flippedHorizontally: Boolean = false,
    val flippedVertically: Boolean = false,
    /** The grid's picture, shown in its place until the full-size one has loaded. */
    val thumbnailUrl: String = "",
    /** The source picture's width / height; 0 for a sticker placed before it was kept. */
    val naturalAspect: Float = 0f,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    /**
     * How far Reshape has moved each corner of the box, as x, y pairs in fractions of the box's
     * width and height, in its turned (unflipped) frame: top-left, top-right, bottom-right,
     * bottom-left. Empty for a plain rectangle.
     */
    val cornerOffsets: List<Float> = emptyList(),
    val eraseStrokes: List<StickerStroke> = emptyList(),
    /** A [PhotoFilters] label (the Effects option), and how strongly it applies, 0–1. */
    val filterLabel: String? = null,
    val filterIntensity: Float = 1f,
    val adjust: StickerAdjust = StickerAdjust(),
    /** A colour washed over the whole sticker (the Fill option), [fillIntensity] 0–100 of the way. */
    val fillArgb: Int? = null,
    val fillIntensity: Float = 100f,
    val outlineArgb: Int = DefaultOutlineArgb,
    /** 0–100 of [MaxStickerOutline]; 0 draws no outline. */
    val outlineWidth: Float = 0f,
    val opacity: Float = 1f,
    val shadowArgb: Int = DefaultStickerShadowArgb,
    val shadowOpacity: Float = 0f,
    val shadowDx: Float = DefaultStickerShadowOffset,
    val shadowDy: Float = DefaultStickerShadowOffset,
    /** 0–100 of [MaxStickerShadowBlur]. */
    val shadowBlur: Float = DefaultStickerShadowBlur,
    /** The Transform option's tilts, 0–100, 50 flat. */
    val tiltX: Float = StickerTiltFlat,
    val tiltY: Float = StickerTiltFlat,
    /** The Mirror option: the half kept and mirrored onto the other, or null for none. */
    val mirror: StickerMirror? = null,
) {
    val sourceAspect: Float get() = if (naturalAspect > 0f) naturalAspect else aspectRatio
    val crop: Rect get() = Rect(cropLeft, cropTop, cropRight, cropBottom)
    val isCropped: Boolean get() = cropLeft > 0f || cropTop > 0f || cropRight < 1f || cropBottom < 1f
}

/** Which half of a sticker its Mirror keeps (and mirrors onto the other half). */
@Serializable
internal enum class StickerMirror(val englishLabel: String) {
    Left("Left"),
    Right("Right"),
    Top("Top"),
    Bottom("Bottom"),
}

/**
 * One eraser stroke over a sticker: [points] are x, y pairs in fractions of the source picture,
 * [radius] a fraction of its width. A [restore] stroke paints the sticker back where it was erased.
 */
@Serializable
internal data class StickerStroke(val points: List<Float>, val radius: Float, val restore: Boolean = false)

/**
 * The Adjust panel's tabs: the photo editor's six adjustments ([photo]), then Clarity and Tint,
 * which only stickers have.
 */
internal enum class StickerAdjustment(val photo: AdjustmentType?) {
    Brightness(AdjustmentType.Brightness),
    Contrast(AdjustmentType.Contrast),
    Saturation(AdjustmentType.Saturation),
    Hue(AdjustmentType.Hue),
    Sharpen(AdjustmentType.Sharpen),
    Exposure(AdjustmentType.Exposure),
    Clarity(null),
    Tint(null),
}

/**
 * The Adjust option's values, -100..100 each: the photo editor's Adjust tool's six, plus [clarity]
 * and [tint] (green at -100, magenta at 100).
 */
@Serializable
internal data class StickerAdjust(
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val hue: Float = 0f,
    val sharpen: Float = 0f,
    val exposure: Float = 0f,
    val tint: Float = 0f,
    val clarity: Float = 0f,
) {
    fun toAdjustValues() = AdjustValues(brightness, contrast, saturation, hue, sharpen, exposure)

    operator fun get(type: StickerAdjustment): Float = when (type) {
        StickerAdjustment.Clarity -> clarity
        StickerAdjustment.Tint -> tint
        else -> toAdjustValues()[checkNotNull(type.photo)]
    }

    fun with(type: StickerAdjustment, value: Float): StickerAdjust =
        when (type) {
            StickerAdjustment.Clarity -> copy(clarity = value)
            StickerAdjustment.Tint -> copy(tint = value)
            else -> from(toAdjustValues().with(checkNotNull(type.photo), value)).copy(tint = tint, clarity = clarity)
        }

    /** The photo editor's adjustments, then the clarity, then the tint. */
    fun toColorMatrix(): FloatArray =
        multiplyColorMatrices(tintMatrix(tint), multiplyColorMatrices(clarityMatrix(clarity), toAdjustValues().toColorMatrix()))

    companion object {
        fun from(values: AdjustValues) = StickerAdjust(
            values.brightness, values.contrast, values.saturation, values.hue, values.sharpen, values.exposure,
        )
    }
}

/**
 * [value] -100..100 -> clarity. Real clarity is contrast in the midtones' local detail, a spatial
 * filter a [androidx.compose.ui.graphics.ColorMatrix] can't express (as with Sharpen); this stand-in
 * is a gentle contrast with a touch of saturation, so 100 gives crisper, punchier colours and -100
 * softer, hazier ones.
 */
internal fun clarityMatrix(value: Float): FloatArray {
    val t = value.coerceIn(-100f, 100f)
    return multiplyColorMatrices(saturationAdjustMatrix(t * 0.15f), contrastMatrix(t * 0.3f))
}

/**
 * [value] -100..100 -> a green–magenta shift: the green channel is scaled against red and blue,
 * so 100 takes green down and lifts red and blue (magenta), and -100 the reverse.
 */
internal fun tintMatrix(value: Float): FloatArray {
    val t = value.coerceIn(-100f, 100f) / 100f
    val redBlue = 1f + 0.15f * t
    val green = 1f - 0.3f * t
    return floatArrayOf(
        redBlue, 0f, 0f, 0f, 0f,
        0f, green, 0f, 0f, 0f,
        0f, 0f, redBlue, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** A new sticker's longer side, as a fraction of the photo's matching side. */
internal const val DefaultStickerSizeFraction = 0.35f
internal val StickerWidthFractionRange = 0.04f..2f

/** How far a duplicate is set off its original, as a fraction of the photo. */
internal const val StickerDuplicateNudge = 0.04f

internal val DefaultOutlineArgb = Color.White.toArgb()
internal val DefaultStickerShadowArgb = Color.Black.toArgb()

/** An outline's widest, as a fraction of the sticker's longer side; the one an Outline panel starts at, 0–100. */
internal const val MaxStickerOutline = 0.06f
internal const val DefaultOutlineWidth = 35f

/** The shadow's "em": this fraction of the sticker's longer side. */
internal const val StickerShadowUnit = 0.5f
internal const val DefaultStickerShadowOffset = 0.06f
internal const val DefaultStickerShadowBlur = 30f
internal const val DefaultStickerShadowOpacity = 0.5f

/** The blur radius (Gaussian sigma) at the Blur slider's end, as a fraction of the sticker's longer side. */
internal const val MaxStickerShadowBlur = 0.1f

internal const val StickerTiltFlat = 50f

/** How far the Transform option leans the sticker back at the slider's ends (a perspective of 1 ± this across it). */
internal const val MaxStickerTilt = 0.35f

/** How far Reshape lets a corner go from its place, in box widths / heights. */
internal const val MaxCornerOffset = 1f

/** The smallest crop, as a fraction of the source on each side. */
internal const val MinStickerCrop = 0.08f

/** The eraser brush at its slider's ends, as a fraction of the sticker's box width. */
internal const val MinEraserRadius = 0.01f
internal const val MaxEraserRadius = 0.15f

/**
 * [asset] placed in the middle of a [photoWidth] × [photoHeight] photo, sized so that neither side
 * takes more than [DefaultStickerSizeFraction] of the photo's: a tall sticker on a wide photo is
 * held by its height, not its width.
 */
internal fun newPlacedSticker(id: Long, asset: StickerAsset, photoWidth: Float, photoHeight: Float): PlacedSticker {
    val aspect = asset.aspectRatio.takeIf { it > 0f } ?: 1f
    val byHeight = if (photoWidth > 0f) DefaultStickerSizeFraction * photoHeight / photoWidth * aspect else DefaultStickerSizeFraction
    return PlacedSticker(
        id = id,
        assetId = asset.id,
        imageUrl = asset.imageUrl,
        thumbnailUrl = asset.thumbnailUrl,
        aspectRatio = aspect,
        naturalAspect = aspect,
        isPremium = asset.isPremium,
        widthFraction = min(DefaultStickerSizeFraction, byHeight).coerceIn(StickerWidthFractionRange),
    )
}

/** [sticker] moved by ([dx], [dy]) photo fractions; its centre stays on the photo. */
internal fun moveSticker(sticker: PlacedSticker, dx: Float, dy: Float): PlacedSticker =
    sticker.copy(centerX = (sticker.centerX + dx).coerceIn(0f, 1f), centerY = (sticker.centerY + dy).coerceIn(0f, 1f))

/** [sticker] [factor] times its size, about its centre, within [StickerWidthFractionRange]. */
internal fun scaleSticker(sticker: PlacedSticker, factor: Float): PlacedSticker =
    sticker.copy(widthFraction = (sticker.widthFraction * factor).coerceIn(StickerWidthFractionRange))

/** [sticker] turned [degrees] clockwise, kept within (-180°, 180°]. */
internal fun turnSticker(sticker: PlacedSticker, degrees: Float): PlacedSticker =
    sticker.copy(rotationDegrees = normalizeDegrees(sticker.rotationDegrees + degrees))

/** A copy of [sticker] as [newId], set down and right of it (or up and left, at the photo's edge). */
internal fun duplicateSticker(sticker: PlacedSticker, newId: Long): PlacedSticker {
    fun nudged(value: Float) = if (value + StickerDuplicateNudge <= 1f) value + StickerDuplicateNudge else value - StickerDuplicateNudge
    return sticker.copy(id = newId, centerX = nudged(sticker.centerX), centerY = nudged(sticker.centerY))
}

/** [stickers] with [id] moved to the top of the stack ([toFront]) or the bottom. */
internal fun restackSticker(stickers: List<PlacedSticker>, id: Long, toFront: Boolean): List<PlacedSticker> =
    arrangeStickers(stickers, id, if (toFront) TextLayerMove.ToFront else TextLayerMove.ToBack)

/** [stickers] with [id] moved by [move]: later draws on top, so forward is one place later. */
internal fun arrangeStickers(stickers: List<PlacedSticker>, id: Long, move: TextLayerMove): List<PlacedSticker> {
    val index = stickers.indexOfFirst { it.id == id }
    if (index < 0) return stickers
    val target = when (move) {
        TextLayerMove.Forward -> index + 1
        TextLayerMove.Backward -> index - 1
        TextLayerMove.ToFront -> stickers.lastIndex
        TextLayerMove.ToBack -> 0
    }.coerceIn(0, stickers.lastIndex)
    if (target == index) return stickers
    return stickers.toMutableList().apply { add(target, removeAt(index)) }
}

/**
 * [sticker] moved against [align]'s edge of the photo, or onto its middle line. [halfWidth] /
 * [halfHeight] are half its turned box's upright bounds, as fractions of the photo, so the box
 * itself touches the edge.
 */
internal fun alignSticker(sticker: PlacedSticker, align: TextPhotoAlign, halfWidth: Float, halfHeight: Float): PlacedSticker =
    when (align) {
        TextPhotoAlign.Top -> sticker.copy(centerY = halfHeight)
        TextPhotoAlign.Middle -> sticker.copy(centerY = 0.5f)
        TextPhotoAlign.Bottom -> sticker.copy(centerY = 1f - halfHeight)
        TextPhotoAlign.Left -> sticker.copy(centerX = halfWidth)
        TextPhotoAlign.Center -> sticker.copy(centerX = 0.5f)
        TextPhotoAlign.Right -> sticker.copy(centerX = 1f - halfWidth)
    }

/** Whether Done needs a subscription: a premium sticker, or a premium effect on one. */
internal fun needsPremium(stickers: List<PlacedSticker>): Boolean = stickers.any { sticker ->
    sticker.isPremium || PhotoFilters.firstOrNull { it.label == sticker.filterLabel }?.isPremium == true
}

// --- Size and tilt, as the panels' 0–100 sliders show them --------------------------------

/** The Size slider runs linearly over this much of the photo's width. */
private val StickerSizeSliderRange = 0.04f..1.2f

internal fun stickerSizeToSlider(widthFraction: Float): Float =
    ((widthFraction - StickerSizeSliderRange.start) / (StickerSizeSliderRange.endInclusive - StickerSizeSliderRange.start) * 100f).coerceIn(0f, 100f)

internal fun sliderToStickerSize(slider: Float): Float =
    StickerSizeSliderRange.start + slider.coerceIn(0f, 100f) / 100f * (StickerSizeSliderRange.endInclusive - StickerSizeSliderRange.start)

/** The Rotate slider: 0–100 over -180°–180°, 50 upright. */
internal fun rotationToSlider(degrees: Float): Float = (normalizeDegrees(degrees) + 180f) / 360f * 100f

internal fun sliderToRotation(slider: Float): Float = slider.coerceIn(0f, 100f) / 100f * 360f - 180f

// --- Reshape -------------------------------------------------------------------------------------

/**
 * The corners of [sticker]'s reshaped box, [width] × [height] px, about its centre in its turned
 * frame: top-left, top-right, bottom-right, bottom-left.
 */
internal fun stickerCorners(sticker: PlacedSticker, width: Float, height: Float): List<Offset> {
    val base = listOf(Offset(-0.5f, -0.5f), Offset(0.5f, -0.5f), Offset(0.5f, 0.5f), Offset(-0.5f, 0.5f))
    return base.mapIndexed { index, corner ->
        val dx = sticker.cornerOffsets.getOrElse(index * 2) { 0f }
        val dy = sticker.cornerOffsets.getOrElse(index * 2 + 1) { 0f }
        Offset((corner.x + dx) * width, (corner.y + dy) * height)
    }
}

/**
 * [sticker] with its corner [index] moved by [delta] (px, in its turned frame) on a [width] ×
 * [height] box. A move that would fold the shape over itself (no longer convex) is refused, so the
 * picture can always be drawn onto it.
 */
internal fun moveStickerCorner(sticker: PlacedSticker, index: Int, delta: Offset, width: Float, height: Float): PlacedSticker {
    if (width <= 0f || height <= 0f) return sticker
    val offsets = (if (sticker.cornerOffsets.size == 8) sticker.cornerOffsets else List(8) { 0f }).toMutableList()
    offsets[index * 2] = (offsets[index * 2] + delta.x / width).coerceIn(-MaxCornerOffset, MaxCornerOffset)
    offsets[index * 2 + 1] = (offsets[index * 2 + 1] + delta.y / height).coerceIn(-MaxCornerOffset, MaxCornerOffset)
    val moved = sticker.copy(cornerOffsets = offsets)
    return if (isConvexQuad(stickerCorners(moved, width, height))) moved else sticker
}

/** Whether the four [corners], in order, make a convex shape turning one way all round. */
internal fun isConvexQuad(corners: List<Offset>): Boolean {
    var sign = 0f
    for (i in 0 until 4) {
        val a = corners[i]
        val b = corners[(i + 1) % 4]
        val c = corners[(i + 2) % 4]
        val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        if (abs(cross) < 1e-6f) return false
        if (sign == 0f) sign = cross else if (cross * sign < 0f) return false
    }
    return true
}

/**
 * The projective map (row-major 3×3, last element 1) taking the unit square's corners (0,0), (1,0),
 * (1,1), (0,1) onto [corners] in that order — Heckbert's square-to-quad. Null for a shape it can't
 * reach (three corners in a line).
 */
internal fun squareToQuad(corners: List<Offset>): FloatArray? {
    val (p0, p1, p2, p3) = corners
    val sx = p0.x - p1.x + p2.x - p3.x
    val sy = p0.y - p1.y + p2.y - p3.y
    if (abs(sx) < 1e-6f && abs(sy) < 1e-6f) {
        // A parallelogram: the map is affine.
        return floatArrayOf(p1.x - p0.x, p3.x - p0.x, p0.x, p1.y - p0.y, p3.y - p0.y, p0.y, 0f, 0f, 1f)
    }
    val dx1 = p1.x - p2.x
    val dx2 = p3.x - p2.x
    val dy1 = p1.y - p2.y
    val dy2 = p3.y - p2.y
    val det = dx1 * dy2 - dx2 * dy1
    if (abs(det) < 1e-9f) return null
    val g = (sx * dy2 - dx2 * sy) / det
    val h = (dx1 * sy - sx * dy1) / det
    return floatArrayOf(
        p1.x - p0.x + g * p1.x, p3.x - p0.x + h * p3.x, p0.x,
        p1.y - p0.y + g * p1.y, p3.y - p0.y + h * p3.y, p0.y,
        g, h, 1f,
    )
}

/** Where the projective [map] (from [squareToQuad]) takes the point ([u], [v]). */
internal fun applyProjective(map: FloatArray, u: Float, v: Float): Offset {
    val w = map[6] * u + map[7] * v + map[8]
    return Offset((map[0] * u + map[1] * v + map[2]) / w, (map[3] * u + map[4] * v + map[5]) / w)
}

/**
 * The map that draws a [width] × [height] box centred on the origin onto [sticker]'s reshaped
 * corners (row-major 3×3): the box to the unit square, then the square onto the shape. Null for a
 * plain rectangle.
 */
internal fun stickerReshapeMap(sticker: PlacedSticker, width: Float, height: Float): FloatArray? {
    if (sticker.cornerOffsets.none { it != 0f } || width <= 0f || height <= 0f) return null
    return boxToQuadMap(width, height, stickerCorners(sticker, width, height))
}

/**
 * The map (row-major 3×3) that takes a [width] × [height] box centred on the origin onto [quad]
 * (top-left, top-right, bottom-right, bottom-left): the box to the unit square, then the square
 * onto the shape.
 */
private fun boxToQuadMap(width: Float, height: Float, quad: List<Offset>): FloatArray? {
    val h = squareToQuad(quad) ?: return null
    // Box → unit square: u = x / width + 0.5, v = y / height + 0.5.
    val box = floatArrayOf(1f / width, 0f, 0.5f, 0f, 1f / height, 0.5f, 0f, 0f, 1f)
    return FloatArray(9) { i ->
        val row = i / 3
        val col = i % 3
        (0 until 3).sumOf { k -> (h[row * 3 + k] * box[k * 3 + col]).toDouble() }.toFloat()
    }
}

/**
 * The bounds, in its turned frame and about its centre, of what [sticker] draws on a [width] ×
 * [height] box: its corners as [org.example.project.ui.stickers.drawSticker] places them, through
 * the Transform tilt and then the Reshape map. Those are projective, so the shape stays a convex
 * quad and its corners bound it. The plain box when neither is set.
 */
internal fun stickerDrawnBounds(sticker: PlacedSticker, width: Float, height: Float): Rect {
    if (width <= 0f || height <= 0f) return Rect(0f, 0f, 0f, 0f)
    val corners = listOf(
        Offset(-width / 2f, -height / 2f),
        Offset(width / 2f, -height / 2f),
        Offset(width / 2f, height / 2f),
        Offset(-width / 2f, height / 2f),
    ).map { stickerShapePoint(sticker, it, width, height) }
    return Rect(corners.minOf { it.x }, corners.minOf { it.y }, corners.maxOf { it.x }, corners.maxOf { it.y })
}

/**
 * Where a [point] of [sticker]'s [width] × [height] box (px about its centre, as seen, so after any
 * flip) is drawn, in its turned frame: through the Transform tilt and then the Reshape map, as
 * [org.example.project.ui.stickers.drawSticker] stacks them.
 */
internal fun stickerShapePoint(sticker: PlacedSticker, point: Offset, width: Float, height: Float): Offset {
    // The tilt as stickerTiltMatrix has it: w = 1 + px·x + py·y.
    val px = tiltAmount(sticker.tiltX) / (width / 2f)
    val py = tiltAmount(sticker.tiltY) / (height / 2f)
    val w = 1f + px * point.x + py * point.y
    val tilted = Offset(point.x / w, point.y / w)
    val reshape = stickerReshapeMap(sticker, width, height) ?: return tilted
    return applyProjective(reshape, tilted)
}

/** The inverse of [stickerShapePoint]: the point of the box drawn at [point] (turned frame, px). */
internal fun stickerShapePointInverse(sticker: PlacedSticker, point: Offset, width: Float, height: Float): Offset {
    val unshaped = stickerReshapeMap(sticker, width, height)?.let(::invert3x3)?.let { applyProjective(it, point) } ?: point
    return untiltPoint(sticker, unshaped, width, height)
}

/** The box point that [sticker]'s Transform tilt (alone) takes to [point]. */
private fun untiltPoint(sticker: PlacedSticker, point: Offset, width: Float, height: Float): Offset {
    // Undoing q = p / (1 + a·p): p = q / (1 - a·q).
    val px = tiltAmount(sticker.tiltX) / (width / 2f)
    val py = tiltAmount(sticker.tiltY) / (height / 2f)
    val w = 1f - px * point.x - py * point.y
    return Offset(point.x / w, point.y / w)
}

/** The unit square's corners, in the order the Reshape corners go. */
private val UnitCorners = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f), Offset(0f, 1f))

/** The middles of the unit square's sides: left, top, right, bottom. */
private val UnitSideMiddles = listOf(Offset(0f, 0.5f), Offset(0.5f, 0f), Offset(1f, 0.5f), Offset(0.5f, 1f))

/** How far a cut corner may sit outside the Crop frame and still count as in it, in frame fractions. */
private const val CropFrameSlack = 1e-4f

/**
 * The Crop frame's [frame] (fractions of the selection rectangle round the uncut sticker [full],
 * [stickerSelectionRect]) as a crop of [full]'s box, as seen: the cut that keeps nothing outside the
 * frame. On a reshaped or tilted sticker the picture is a slanted quad, so a cut side can't follow a
 * straight frame side all along; it starts where the middle of the frame's side falls on the box,
 * then moves in until both of its corners are drawn inside the frame. A plain sticker's frame is its
 * box, and its cut is exactly the frame.
 */
internal fun cropFrameToBox(full: PlacedSticker, frame: Rect, photoWidth: Float, photoHeight: Float): Rect {
    val selection = stickerSelectionRect(full, photoWidth, photoHeight)
    if (selection.width <= 0f || selection.height <= 0f) return frame
    val (startLeft, startTop, startRight, startBottom) = UnitSideMiddles.map { side ->
        val inFrame = Offset(frame.left + side.x * frame.width, frame.top + side.y * frame.height)
        val onPhoto = selection.center +
            rotateVector(Offset((inFrame.x - 0.5f) * selection.width, (inFrame.y - 0.5f) * selection.height), full.rotationDegrees)
        photoToStickerBoxFraction(full, onPhoto, photoWidth, photoHeight)
    }
    val start = sidesWithin(startLeft.x, startTop.y, startRight.x, startBottom.y)
    var left = start.left
    var top = start.top
    var right = start.right
    var bottom = start.bottom

    fun inFrame(u: Float, v: Float) = boxPointInCropFrame(full, Offset(u, v), selection, photoWidth, photoHeight)
    // Each side moves in only as far as its two corners need; the sides move each other's corners,
    // so a few rounds settle them.
    repeat(3) {
        left = moveIn(left, right - MinStickerCrop) { l -> inFrame(l, top).x >= frame.left - CropFrameSlack && inFrame(l, bottom).x >= frame.left - CropFrameSlack }
        right = moveIn(right, left + MinStickerCrop) { r -> inFrame(r, top).x <= frame.right + CropFrameSlack && inFrame(r, bottom).x <= frame.right + CropFrameSlack }
        top = moveIn(top, bottom - MinStickerCrop) { t -> inFrame(left, t).y >= frame.top - CropFrameSlack && inFrame(right, t).y >= frame.top - CropFrameSlack }
        bottom = moveIn(bottom, top + MinStickerCrop) { b -> inFrame(left, b).y <= frame.bottom + CropFrameSlack && inFrame(right, b).y <= frame.bottom + CropFrameSlack }
    }
    return Rect(left, top, right, bottom)
}

/**
 * The value nearest [from], on the way to [limit], that [fits]: [from] itself when it does, [limit]
 * when nothing does, else found by halving. Fitting is taken to hold from some point on to [limit].
 */
private inline fun moveIn(from: Float, limit: Float, fits: (Float) -> Boolean): Float {
    if (fits(from)) return from
    if (!fits(limit)) return limit
    var bad = from
    var good = limit
    repeat(24) {
        val mid = (bad + good) / 2f
        if (fits(mid)) good = mid else bad = mid
    }
    return good
}

/** Where the point [boxPoint] of [full]'s box (fractions, as seen) is drawn, in fractions of its Crop frame's [selection]. */
private fun boxPointInCropFrame(full: PlacedSticker, boxPoint: Offset, selection: Rect, photoWidth: Float, photoHeight: Float): Offset {
    val onPhoto = stickerBoxFractionToPhoto(full, boxPoint, photoWidth, photoHeight)
    val local = rotateVector(onPhoto - selection.center, -full.rotationDegrees)
    return Offset(local.x / selection.width + 0.5f, local.y / selection.height + 0.5f)
}

/**
 * Where a crop of [full]'s box (as seen) shows in the Crop frame: round the four corners it is drawn
 * at, as the sticker's selection goes round it once cut. [cropFrameToBox] takes that frame back to
 * (very nearly) the same cut.
 */
internal fun boxToCropFrame(full: PlacedSticker, box: Rect, photoWidth: Float, photoHeight: Float): Rect {
    val selection = stickerSelectionRect(full, photoWidth, photoHeight)
    if (selection.width <= 0f || selection.height <= 0f) return box
    val inFrame = UnitCorners.map { corner ->
        boxPointInCropFrame(full, Offset(box.left + corner.x * box.width, box.top + corner.y * box.height), selection, photoWidth, photoHeight)
    }
    return sidesWithin(inFrame.minOf { it.x }, inFrame.minOf { it.y }, inFrame.maxOf { it.x }, inFrame.maxOf { it.y })
}

/** A crop from its four sides, kept within the unit square and at least [MinStickerCrop] a side. */
private fun sidesWithin(left: Float, top: Float, right: Float, bottom: Float): Rect {
    val l = min(left, right).coerceIn(0f, 1f - MinStickerCrop)
    val t = min(top, bottom).coerceIn(0f, 1f - MinStickerCrop)
    return Rect(l, t, max(left, right).coerceIn(l + MinStickerCrop, 1f), max(top, bottom).coerceIn(t + MinStickerCrop, 1f))
}

/** A fraction of [sticker]'s box, as seen, to where it is drawn on a [photoWidth] × [photoHeight] px photo. */
internal fun stickerBoxFractionToPhoto(sticker: PlacedSticker, fraction: Offset, photoWidth: Float, photoHeight: Float): Offset {
    val width = sticker.widthFraction * photoWidth
    val height = width / sticker.aspectRatio
    val shaped = stickerShapePoint(sticker, Offset((fraction.x - 0.5f) * width, (fraction.y - 0.5f) * height), width, height)
    return Offset(sticker.centerX * photoWidth, sticker.centerY * photoHeight) + rotateVector(shaped, sticker.rotationDegrees)
}

/** The inverse of [stickerBoxFractionToPhoto]: the fraction of the box, as seen, drawn at [point] (photo px). */
internal fun photoToStickerBoxFraction(sticker: PlacedSticker, point: Offset, photoWidth: Float, photoHeight: Float): Offset {
    val width = sticker.widthFraction * photoWidth
    val height = width / sticker.aspectRatio
    val local = rotateVector(point - Offset(sticker.centerX * photoWidth, sticker.centerY * photoHeight), -sticker.rotationDegrees)
    val box = stickerShapePointInverse(sticker, local, width, height)
    return Offset(box.x / width + 0.5f, box.y / height + 0.5f)
}

/** [point] through the row-major projective map [m]. */
private fun applyProjective(m: FloatArray, point: Offset): Offset {
    val d = m[6] * point.x + m[7] * point.y + m[8]
    return Offset((m[0] * point.x + m[1] * point.y + m[2]) / d, (m[3] * point.x + m[4] * point.y + m[5]) / d)
}

/** The inverse of a row-major 3×3 matrix, or null when it has none. */
internal fun invert3x3(m: FloatArray): FloatArray? {
    val c0 = m[4] * m[8] - m[5] * m[7]
    val c1 = m[5] * m[6] - m[3] * m[8]
    val c2 = m[3] * m[7] - m[4] * m[6]
    val det = m[0] * c0 + m[1] * c1 + m[2] * c2
    if (abs(det) < 1e-12f) return null
    val k = 1f / det
    return floatArrayOf(
        c0 * k, (m[2] * m[7] - m[1] * m[8]) * k, (m[1] * m[5] - m[2] * m[4]) * k,
        c1 * k, (m[0] * m[8] - m[2] * m[6]) * k, (m[2] * m[3] - m[0] * m[5]) * k,
        c2 * k, (m[1] * m[6] - m[0] * m[7]) * k, (m[0] * m[4] - m[1] * m[3]) * k,
    )
}

/**
 * Where [sticker]'s selection box goes on a photo [photoWidth] × [photoHeight] px: round what it
 * draws ([stickerDrawnBounds]), unturned, to be turned by its rotation about its own centre. A
 * reshaped or tilted sticker's drawn centre is off its box's, so that centre is turned with it.
 */
internal fun stickerSelectionRect(sticker: PlacedSticker, photoWidth: Float, photoHeight: Float): Rect {
    val width = sticker.widthFraction * photoWidth
    val bounds = stickerDrawnBounds(sticker, width, width / sticker.aspectRatio)
    val center = Offset(sticker.centerX * photoWidth, sticker.centerY * photoHeight) +
        rotateVector(bounds.center, sticker.rotationDegrees)
    return Rect(center - Offset(bounds.width / 2f, bounds.height / 2f), bounds.size)
}

/** The tilt's perspective across the sticker, -[MaxStickerTilt]..[MaxStickerTilt], from its 0–100 slider. */
internal fun tiltAmount(slider: Float): Float = (slider.coerceIn(0f, 100f) - StickerTiltFlat) / StickerTiltFlat * MaxStickerTilt

// --- Crop ----------------------------------------------------------------------------------------

/**
 * [sticker] cropped to [crop] (fractions of the source) with what it shows staying where it is on a
 * photo [photoAspect] (width / height) wide: the box shrinks or grows to the new cut and its centre
 * moves to the cut's centre, turned and flipped as the sticker is.
 */
internal fun stickerWithCrop(sticker: PlacedSticker, crop: Rect, photoAspect: Float): PlacedSticker {
    val left = crop.left.coerceIn(0f, 1f - MinStickerCrop)
    val top = crop.top.coerceIn(0f, 1f - MinStickerCrop)
    val right = crop.right.coerceIn(left + MinStickerCrop, 1f)
    val bottom = crop.bottom.coerceIn(top + MinStickerCrop, 1f)

    // In photo-width units, so both axes measure alike.
    val width = sticker.widthFraction
    val height = width / sticker.aspectRatio
    val fullWidth = width / (sticker.cropRight - sticker.cropLeft)
    val fullHeight = height / (sticker.cropBottom - sticker.cropTop)

    // A crop's centre relative to the uncut sticker's centre, in its own (turned, flipped) frame.
    fun offsetOf(l: Float, t: Float, r: Float, b: Float): Offset {
        var u = (l + r) / 2f - 0.5f
        var v = (t + b) / 2f - 0.5f
        if (sticker.flippedHorizontally) u = -u
        if (sticker.flippedVertically) v = -v
        return rotateVector(Offset(u * fullWidth, v * fullHeight), sticker.rotationDegrees)
    }

    val center = Offset(sticker.centerX, sticker.centerY / photoAspect)
    val fullCenter = center - offsetOf(sticker.cropLeft, sticker.cropTop, sticker.cropRight, sticker.cropBottom)
    val newCenter = fullCenter + offsetOf(left, top, right, bottom)
    val newWidth = fullWidth * (right - left)
    val newHeight = fullHeight * (bottom - top)
    val cut = sticker.copy(
        cropLeft = left,
        cropTop = top,
        cropRight = right,
        cropBottom = bottom,
        widthFraction = newWidth,
        aspectRatio = newWidth / newHeight,
        centerX = newCenter.x,
        centerY = newCenter.y * photoAspect,
    )
    if (sticker.cornerOffsets.none { it != 0f } && tiltAmount(sticker.tiltX) == 0f && tiltAmount(sticker.tiltY) == 0f) return cut

    // Reshaped or tilted, the sticker is drawn through a projective map, and the cut must stay
    // drawn where it was. Find where the old sticker draws the cut's four corners (in photo-width
    // units)...
    val drawnCorners = UnitCorners.map { seen ->
        val su = if (sticker.flippedHorizontally) 1f - seen.x else seen.x
        val sv = if (sticker.flippedVertically) 1f - seen.y else seen.y
        // That corner of the new cut, in the old cut's fractions, as seen.
        var ou = (left + su * (right - left) - sticker.cropLeft) / (sticker.cropRight - sticker.cropLeft)
        var ov = (top + sv * (bottom - top) - sticker.cropTop) / (sticker.cropBottom - sticker.cropTop)
        if (sticker.flippedHorizontally) ou = 1f - ou
        if (sticker.flippedVertically) ov = 1f - ov
        val shaped = stickerShapePoint(sticker, Offset((ou - 0.5f) * width, (ov - 0.5f) * height), width, height)
        center + rotateVector(shaped, sticker.rotationDegrees)
    }
    // ...centre the new box among them, so it turns and scales about what it shows...
    val pivot = Offset(drawnCorners.sumOf { it.x.toDouble() }.toFloat() / 4f, drawnCorners.sumOf { it.y.toDouble() }.toFloat() / 4f)
    val quad = drawnCorners.map { rotateVector(it - pivot, -sticker.rotationDegrees) }
    // ...and give it the corners that land its box there. Projective maps agree everywhere once
    // they agree on four corners, so the whole cut stays put. The tilt is drawn before the reshape,
    // so the reshape's corners are where that map takes the box corners the tilt would send to them.
    val target = boxToQuadMap(newWidth, newHeight, quad) ?: return cut
    val placed = cut.copy(centerX = pivot.x, centerY = pivot.y * photoAspect)
    val offsets = UnitCorners.flatMap { corner ->
        val base = Offset((corner.x - 0.5f) * newWidth, (corner.y - 0.5f) * newHeight)
        val k = applyProjective(target, untiltPoint(placed, base, newWidth, newHeight))
        listOf((k.x - base.x) / newWidth, (k.y - base.y) / newHeight)
    }
    return placed.copy(cornerOffsets = if (offsets.all { abs(it) < 1e-5f }) emptyList() else offsets)
}

/**
 * A crop as the Crop panel's frame shows it: in the uncut sticker's own unflipped frame, so it
 * follows the picture as drawn. The same mapping turns it back ([displayToSourceCrop]).
 */
internal fun sourceToDisplayCrop(sticker: PlacedSticker, crop: Rect): Rect {
    var (l, r) = crop.left to crop.right
    var (t, b) = crop.top to crop.bottom
    if (sticker.flippedHorizontally) { val nl = 1f - r; r = 1f - l; l = nl }
    if (sticker.flippedVertically) { val nt = 1f - b; b = 1f - t; t = nt }
    return Rect(l, t, r, b)
}

internal fun displayToSourceCrop(sticker: PlacedSticker, crop: Rect): Rect = sourceToDisplayCrop(sticker, crop)

/**
 * The largest crop of [ratio] (width / height, as seen) centred on the uncut sticker, whose
 * displayed shape is [fullAspect]; in display fractions.
 */
internal fun centeredCrop(ratio: Float, fullAspect: Float): Rect {
    val w = min(1f, ratio / fullAspect)
    val h = min(1f, fullAspect / ratio)
    return Rect(0.5f - w / 2f, 0.5f - h / 2f, 0.5f + w / 2f, 0.5f + h / 2f)
}

/**
 * The display crop [crop] with its [corner] (0 top-left, 1 top-right, 2 bottom-right, 3 bottom-left)
 * dragged by [dx], [dy] (fractions of the uncut sticker). With a [ratio] (width / height as seen, on
 * an uncut sticker of [fullAspect]) the corner keeps it, led by the horizontal drag. The opposite
 * corner stays, and the crop stays on the sticker and at least [MinStickerCrop] a side.
 */
internal fun dragCropCorner(crop: Rect, corner: Int, dx: Float, dy: Float, ratio: Float?, fullAspect: Float): Rect {
    val anchorX = if (corner == 0 || corner == 3) crop.right else crop.left
    val anchorY = if (corner == 0 || corner == 1) crop.bottom else crop.top
    val signX = if (corner == 0 || corner == 3) -1f else 1f
    val signY = if (corner == 0 || corner == 1) -1f else 1f
    val maxW = if (signX < 0) anchorX else 1f - anchorX
    val maxH = if (signY < 0) anchorY else 1f - anchorY
    var w = (crop.width + signX * dx).coerceIn(MinStickerCrop, maxW)
    var h = (crop.height + signY * dy).coerceIn(MinStickerCrop, maxH)
    if (ratio != null) {
        // Height in fractions for this width, so the cut's real shape is [ratio].
        val k = fullAspect / ratio
        h = w * k
        if (h > maxH) { h = maxH; w = h / k }
        if (h < MinStickerCrop) { h = MinStickerCrop; w = h / k }
        w = w.coerceAtMost(maxW)
    }
    val l = if (signX < 0) anchorX - w else anchorX
    val t = if (signY < 0) anchorY - h else anchorY
    return Rect(l, t, l + w, t + h)
}

/** [crop] slid by [dx], [dy], kept on the sticker. */
internal fun moveCrop(crop: Rect, dx: Float, dy: Float): Rect {
    val l = (crop.left + dx).coerceIn(0f, 1f - crop.width)
    val t = (crop.top + dy).coerceIn(0f, 1f - crop.height)
    return Rect(l, t, l + crop.width, t + crop.height)
}

// --- Eraser --------------------------------------------------------------------------------------

/**
 * Where a point on the photo ([point], px on a [photoWidthPx] × [photoHeightPx] photo) falls on
 * [sticker]'s source picture, in its fractions: undoing the turn, the Reshape and tilt
 * ([photoToStickerBoxFraction]), the flips and the crop.
 */
internal fun photoPointToSticker(sticker: PlacedSticker, point: Offset, photoWidthPx: Float, photoHeightPx: Float): Offset {
    val seen = photoToStickerBoxFraction(sticker, point, photoWidthPx, photoHeightPx)
    var u = seen.x
    var v = seen.y
    if (sticker.flippedHorizontally) u = 1f - u
    if (sticker.flippedVertically) v = 1f - v
    return Offset(
        sticker.cropLeft + u * (sticker.cropRight - sticker.cropLeft),
        sticker.cropTop + v * (sticker.cropBottom - sticker.cropTop),
    )
}

/** A brush [radiusPx] on the photo as a fraction of [sticker]'s source width. */
internal fun brushRadiusOnSticker(sticker: PlacedSticker, radiusPx: Float, photoWidthPx: Float): Float {
    val fullWidthPx = sticker.widthFraction * photoWidthPx / (sticker.cropRight - sticker.cropLeft)
    return if (fullWidthPx > 0f) radiusPx / fullWidthPx else 0f
}

// --- Colour --------------------------------------------------------------------------------------

/**
 * The sticker's colour edits as one colour matrix: its effect, then its adjustments, then its fill.
 * Null when it has none.
 */
internal fun stickerColorMatrix(sticker: PlacedSticker): FloatArray? {
    var matrix: FloatArray? = null
    fun then(next: FloatArray) {
        matrix = matrix?.let { multiplyColorMatrices(next, it) } ?: next
    }
    PhotoFilters.firstOrNull { it.label == sticker.filterLabel }?.matrix?.let { filter ->
        if (sticker.filterIntensity > 0f) then(blendWithIdentity(filter, sticker.filterIntensity))
    }
    val adjust = sticker.adjust
    if (adjust != StickerAdjust()) then(adjust.toColorMatrix())
    val fill = sticker.fillArgb
    if (fill != null && sticker.fillIntensity > 0f) then(fillColorMatrix(Color(fill), sticker.fillIntensity / 100f))
    return matrix
}

/** Moves every colour [amount] (0–1) of the way to [color], keeping each pixel's alpha. */
internal fun fillColorMatrix(color: Color, amount: Float): FloatArray {
    val k = amount.coerceIn(0f, 1f)
    val keep = 1f - k
    return floatArrayOf(
        keep, 0f, 0f, 0f, k * color.red * 255f,
        0f, keep, 0f, 0f, k * color.green * 255f,
        0f, 0f, keep, 0f, k * color.blue * 255f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/** The longer side of a [sticker]'s box on a photo [photoWidthPx] wide. */
internal fun stickerLongSidePx(sticker: PlacedSticker, photoWidthPx: Float): Float {
    val width = sticker.widthFraction * photoWidthPx
    return max(width, width / sticker.aspectRatio)
}

private val StickersJson = Json { ignoreUnknownKeys = true }

/** Saves the placed stickers as JSON, so they survive the paywall covering the editor. */
internal val PlacedStickersSaver: Saver<List<PlacedSticker>, String> = Saver(
    save = { StickersJson.encodeToString(ListSerializer(PlacedSticker.serializer()), it) },
    restore = { runCatching { StickersJson.decodeFromString(ListSerializer(PlacedSticker.serializer()), it) }.getOrNull() },
)
