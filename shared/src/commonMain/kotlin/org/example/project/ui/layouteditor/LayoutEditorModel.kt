package org.example.project.ui.layouteditor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.crop.AspectRatioOption
import org.example.project.ui.crop.AspectRatioOptions
import org.example.project.ui.frame.FrameColorNone
import org.example.project.ui.freestyle.FreestyleLayer
import org.example.project.ui.templates.SlotTransform

/**
 * One slot's photo and everything done to it: the pan/zoom of [transform] (as in the collage
 * editor), and the Transform panel's mirror [flipX] / [flipY], [rotationDegrees] and [stretchX] /
 * [stretchY] (the photo's width and height, as multiples of its own). However it is turned or
 * stretched, it is drawn large enough to cover its slot (see [layoutPhotoSize]).
 *
 * [image] must be a bitmap this app made (`copyBitmap`), since it is redrawn through scaling draws.
 */
internal data class LayoutPhoto(
    val image: ImageBitmap,
    val transform: SlotTransform = SlotTransform(),
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val rotationDegrees: Float = 0f,
    val stretchX: Float = 1f,
    val stretchY: Float = 1f,
)

/**
 * Everything the layout editor edits, as one undo step.
 *
 * The measures ([margin], [space], [corner], [borderWidth], [frameWidth], [frameCorner]) are in
 * layout units, 1/[LayoutUnitsPerSide] of the canvas's shorter side (see [layoutUnitPx]), so the
 * preview and the bake draw them in proportion without knowing each other's size. The stickers
 * ([layers]) are the freestyle editor's, placed as fractions of the canvas.
 *
 * @param photos the placed photos by slot index. A slot past the current [template]'s count keeps its
 *   photo, unseen, so switching back to a layout with more slots brings it back.
 * @param canvas the canvas's shape; a `null` ratio is Original, the first photo's own shape.
 * @param shadow the slots' drop shadow, 0..100.
 * @param borderOpacity the slot outline's opacity, 0..100.
 * @param frameColor the frame around the whole canvas, [FrameColorNone] for none.
 */
internal data class LayoutEdit(
    val template: TemplateItem,
    val photos: Map<Int, LayoutPhoto> = emptyMap(),
    val canvas: AspectRatioOption = DefaultLayoutCanvas,
    val margin: Float = 6f,
    val space: Float = 3f,
    val corner: Float = 0f,
    val shadow: Float = 0f,
    val borderWidth: Float = 0f,
    val borderOpacity: Float = 100f,
    val borderColor: Color = Color.White,
    val frameColor: Color = FrameColorNone,
    val frameWidth: Float = 12f,
    val frameCorner: Float = 0f,
    val background: Color = Color.White,
    val layers: List<FreestyleLayer> = emptyList(),
)

/** The Canvas panel's shapes: Original (the first photo's), then the photo editor's ratios but Free. */
internal val LayoutCanvasOptions: List<AspectRatioOption> =
    listOf(AspectRatioOption("Original", null)) + AspectRatioOptions.filter { it.ratio != null }

/** Square, which every layout is drawn for. Not Original: the canvas would jump with the first photo. */
internal val DefaultLayoutCanvas: AspectRatioOption = LayoutCanvasOptions.first { it.ratio == 1f }

internal const val LayoutUnitsPerSide = 360f

internal val LayoutMarginRange = 0f..40f
internal val LayoutSpaceRange = 0f..20f
internal val LayoutCornerRange = 0f..60f
internal val LayoutShadowRange = 0f..100f
internal val LayoutBorderWidthRange = 0f..20f
internal val LayoutBorderOpacityRange = 0f..100f
internal val LayoutFrameWidthRange = 0f..40f
internal val LayoutFrameCornerRange = 0f..60f

/** The Transform panel's width / height, in percent of the photo's own. */
internal val LayoutStretchRange = 50f..200f
internal val LayoutRotationRange = -180f..180f
