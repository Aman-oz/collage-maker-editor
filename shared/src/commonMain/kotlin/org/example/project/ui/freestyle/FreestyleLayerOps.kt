package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import kotlin.math.ceil
import kotlin.math.sqrt
import org.example.project.ui.text.PremiumTextColor
import org.example.project.ui.text.PremiumTextFontLabel
import org.example.project.ui.text.TextFontStyleOption

// Pure edits of a layer stack, shared by the freestyle editor and Set Background. Each returns a
// new list and leaves every layer other than the targeted one untouched.

/** Applies one drag/pinch/rotate tick to layer [id]; its center stays on the canvas. */
internal fun List<FreestyleLayer>.transformed(
    id: Long,
    panFraction: Offset,
    zoomDelta: Float,
    rotationDeltaDegrees: Float,
): List<FreestyleLayer> = map { layer ->
    if (layer.id != id) {
        layer
    } else {
        layer.copy(
            offsetFraction = Offset(
                (layer.offsetFraction.x + panFraction.x).coerceIn(0f, 1f),
                (layer.offsetFraction.y + panFraction.y).coerceIn(0f, 1f),
            ),
            scale = (layer.scale * zoomDelta).coerceIn(FreestyleLayerScaleRange),
            rotationDegrees = layer.rotationDegrees + rotationDeltaDegrees,
        )
    }
}

/** Sets layer [id]'s absolute size and angle, as dragged by its corner handle. */
internal fun List<FreestyleLayer>.withScaleRotation(id: Long, scale: Float, rotationDegrees: Float): List<FreestyleLayer> =
    map { layer ->
        if (layer.id == id) layer.copy(scale = scale.coerceIn(FreestyleLayerScaleRange), rotationDegrees = rotationDegrees) else layer
    }

/** Moves layer [id] to the top of the stack; the same list if it is already there or missing. */
internal fun List<FreestyleLayer>.broughtToFront(id: Long): List<FreestyleLayer> {
    if (lastOrNull()?.id == id) return this
    val layer = find { it.id == id } ?: return this
    return filterNot { it.id == id } + layer
}

/** Rewrites text layer [id] in place, keeping its placement. */
internal fun List<FreestyleLayer>.withText(
    id: Long,
    text: String,
    fill: FreestyleFill,
    font: TextFontStyleOption,
    background: FreestyleFill? = null,
): List<FreestyleLayer> =
    map { layer ->
        if (layer.id == id && layer.content is FreestyleContent.TextContent) {
            layer.copy(content = FreestyleContent.TextContent(text, fill, font, background))
        } else {
            layer
        }
    }

/** Changes only the words of text layer [id], keeping its style and placement. */
internal fun List<FreestyleLayer>.retyped(id: Long, text: String): List<FreestyleLayer> = map { layer ->
    val content = layer.content
    if (layer.id == id && content is FreestyleContent.TextContent) layer.copy(content = content.copy(text = text)) else layer
}

/** Where [scatterPlacements] puts one of the photos a freestyle canvas opens with. */
internal data class FreestylePlacement(val offsetFraction: Offset, val scale: Float, val rotationDegrees: Float)

/** Tilts dealt out in turn, so neighbouring photos lean different ways. */
private val ScatterRotations = listOf(-8f, 7f, -5f, 9f, -10f, 4f)

/** How much of its grid cell a scattered photo may fill; the rest is the gap between neighbours. */
private const val ScatterCellFill = 0.82f

/** Neighbouring columns sit this far (of a row's height) above and below the row's centre line. */
private const val ScatterStagger = 0.06f

/**
 * The canvas's width over its height, assumed where a photo's height has to be compared with the
 * canvas's: placements are fractions, worked out before the canvas has been measured. The area
 * between the top bar and the panels is a little taller than wide on a phone.
 */
private const val ScatterCanvasAspect = 0.8f

/**
 * Places the photos a freestyle canvas opens with, one per entry of [imageAspects] (each photo's
 * height over its width), so that every one starts in its own spot instead of in a pile at the
 * centre. The canvas is cut into a near-square grid, the photos fill it in reading order (a short
 * last row is centred), and each is scaled down until it fits its cell, never up. A slight tilt and
 * a stagger between columns keep it looking hand-placed rather than like a collage layout.
 */
internal fun scatterPlacements(imageAspects: List<Float>): List<FreestylePlacement> {
    val count = imageAspects.size
    if (count == 0) return emptyList()
    val columns = ceil(sqrt(count.toFloat())).toInt()
    val rows = ceil(count / columns.toFloat()).toInt()

    return imageAspects.mapIndexed { index, aspect ->
        val row = index / columns
        val column = index % columns
        val inRow = if (row == rows - 1) count - row * columns else columns
        val x = (column + 0.5f + (columns - inRow) / 2f) / columns
        // A lone photo has no neighbour to stagger against and stays dead centre.
        val stagger = if (count == 1) 0f else (if (column % 2 == 0) -ScatterStagger else ScatterStagger) / rows
        val y = (row + 0.5f) / rows + stagger

        val fitWidth = ScatterCellFill / columns / FreestyleImageBaseWidthFraction
        val fitHeight = ScatterCellFill / rows / (FreestyleImageBaseWidthFraction * ScatterCanvasAspect * aspect)
        FreestylePlacement(
            offsetFraction = Offset(x, y),
            scale = minOf(1f, fitWidth, fitHeight).coerceIn(FreestyleLayerScaleRange),
            rotationDegrees = ScatterRotations[index % ScatterRotations.size],
        )
    }
}

/** How many photos and stickers anyone can put on a freestyle canvas (see [isPremiumFreestyle]). */
internal const val FreeFreestyleImageCount = 4
internal const val FreeFreestyleStickerCount = 2

/**
 * Whether a freestyle canvas uses something premium: more than [FreeFreestyleImageCount] photos,
 * more than [FreeFreestyleStickerCount] stickers, or a text label in the premium colour or font
 * (the same red and Stylish that are premium in the photo editor's Text tool).
 */
internal fun isPremiumFreestyle(imageCount: Int, stickerCount: Int, texts: List<FreestyleContent.TextContent>): Boolean =
    imageCount > FreeFreestyleImageCount ||
        stickerCount > FreeFreestyleStickerCount ||
        texts.any { (it.fill as? FreestyleFill.Solid)?.color == PremiumTextColor || it.font.label == PremiumTextFontLabel }

/** [isPremiumFreestyle] for the layers on a canvas. */
internal fun List<FreestyleLayer>.isPremiumFreestyle(): Boolean = isPremiumFreestyle(
    imageCount = count { it.content is FreestyleContent.ImageContent },
    stickerCount = count { it.content is FreestyleContent.StickerContent },
    texts = mapNotNull { it.content as? FreestyleContent.TextContent },
)
