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
 * editor), the Mirror option's [flipX] / [flipY], the Rotate option's [rotationDegrees] and the
 * Transform option's lean, [tiltX] / [tiltY] (0–100 sliders, [LayoutTiltFlat] flat, as the Stickers
 * tool's Transform). However it is turned or leant, it is drawn large enough to cover its slot (see
 * [layoutPhotoSize]).
 *
 * [image] must be a bitmap this app made (`copyBitmap`), since it is redrawn through scaling draws.
 */
internal data class LayoutPhoto(
    val image: ImageBitmap,
    val transform: SlotTransform = SlotTransform(),
    val flipX: Boolean = false,
    val flipY: Boolean = false,
    val rotationDegrees: Float = 0f,
    val tiltX: Float = LayoutTiltFlat,
    val tiltY: Float = LayoutTiltFlat,
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
 * @param filterLabel the photo filter over every photo (a `PhotoFilter`'s label), null for none; kept
 *   live, so a photo placed later is filtered too and undo takes it off again.
 * @param filterIntensity that filter's strength, 0..1.
 * @param background the canvas's colour behind the slots; transparent for none.
 * @param backgroundOpacity the background's opacity, colour or picture alike, 0..100.
 * @param backgroundArt a server background drawn over the whole canvas in place of [background],
 *   cropped to its shape; null for the colour.
 * @param frameArt the server frame drawn over the whole layout, null for none. Its picture is
 *   loaded by URL where it is drawn (the preview's `NetworkImage`, the ViewModel before a bake).
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
    val filterLabel: String? = null,
    val filterIntensity: Float = 1f,
    val frameArt: LayoutArt? = null,
    val backgroundArt: LayoutArt? = null,
    val backgroundOpacity: Float = 100f,
)

/**
 * The Canvas panel's shapes: the photo editor's ratio strip. Its Free is the one shape not fixed: the
 * canvas takes the first photo's shape (see [layoutCanvasAspect]).
 */
internal val LayoutCanvasOptions: List<AspectRatioOption> = AspectRatioOptions

/** Square, which every layout is drawn for. Not Free: the canvas would jump with the first photo. */
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

internal val LayoutBackgroundOpacityRange = 0f..100f

internal val LayoutRotationRange = -180f..180f

/** The Transform sliders' middle, where the photo faces straight on. */
internal const val LayoutTiltFlat = 50f

/**
 * How far Transform leans a photo at the sliders' ends: a perspective of 1 ± this across its slot.
 * Gentler than a sticker's, since a leant photo must be zoomed in to still cover its slot.
 */
internal const val MaxLayoutTilt = 0.2f
