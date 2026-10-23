package org.example.project.ui.text

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One text on the photo. Everything is in units that don't depend on the preview's size, so the
 * same item bakes the same at full resolution: [centerX]/[centerY] are fractions of the photo's
 * width/height, and [boxWidth]/[boxHeight] fractions of the photo's *width* (both, so a box keeps its
 * shape however wide the photo is shown).
 *
 * @param fontFamily a family from the font catalog, or null for the default font.
 * @param boxWidth the text area's width, set by the side handles; null fits the words (wrapping only
 * at the photo's edge).
 * @param boxHeight the text area's least height, set by the top and bottom handles; the words are
 * centred in it. Null (or less than the words need) fits the words.
 * @param locked a locked text can be selected (to unlock it) but not moved, resized, turned or typed in.
 * @param backgroundArgb the plate behind the words ([drawTextPlate]); null for none.
 * @param backgroundCorner the plate's roundness on the Color panel's 0–100 slider ([textPlateCornerRadius]).
 * @param paddingLeft/[paddingTop]/[paddingRight]/[paddingBottom] how far the plate reaches past the
 * words on each side, on the Padding panel's 0–100 sliders ([textPaddingEm]: in em of the font size,
 * so the plate keeps its shape as the text is scaled).
 * @param strokeArgb the outline around the letters; null for none.
 * @param bold/[italic]/[underline]/[strikethrough] the Format panel's toggles, over whatever the font is.
 * @param case how the words' letters are cased when shown ([applyTextCase]); [content] keeps them as typed.
 * @param opacity the whole text's 0–1 opacity (the Opacity panel): words, plate and outline faded
 * together as one layer, on top of each colour's own opacity.
 * @param letterSpacing/[lineSpacing] the Spacing panel's 0–100 sliders, [TextSpacingNormal] being the
 * font's own spacing ([letterSpacingEm], [lineHeightEm]).
 * @param shadowOpacity the Shadow panel's 0–1 opacity; 0 is no shadow. [shadowDx]/[shadowDy] are its
 * offset and [shadowBlur] its 0–100 blur ([shadowBlurEm]), in em of the font size so the shadow
 * scales with the words.
 * @param curve the Curve panel's 0–100 slider: [TextCurveStraight] runs the words straight, less
 * arches them and more sags them ([textCurvature]).
 * Each colour has its own 0–1 opacity, kept apart from the colour so a swatch pick doesn't reset it.
 */
@Serializable
internal data class TextItem(
    val id: Long,
    val content: String = "",
    val fontFamily: String? = null,
    val colorArgb: Int = DefaultTextColorArgb,
    val alignment: TextAlignOption = TextAlignOption.Center,
    val sizeSp: Float = TextSizeDefault,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val rotationDegrees: Float = 0f,
    val boxWidth: Float? = null,
    val boxHeight: Float? = null,
    val locked: Boolean = false,
    val textOpacity: Float = 1f,
    val backgroundArgb: Int? = null,
    val backgroundOpacity: Float = 1f,
    val backgroundCorner: Float = DefaultTextPlateCorner,
    val strokeArgb: Int? = null,
    val strokeOpacity: Float = 1f,
    val curve: Float = TextCurveStraight,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val case: TextCase = TextCase.None,
    val opacity: Float = 1f,
    val letterSpacing: Float = TextSpacingNormal,
    val lineSpacing: Float = TextSpacingNormal,
    val shadowArgb: Int = DefaultShadowArgb,
    val shadowOpacity: Float = 0f,
    val shadowDx: Float = DefaultShadowOffsetEm,
    val shadowDy: Float = DefaultShadowOffsetEm,
    val shadowBlur: Float = DefaultShadowBlur,
    val paddingLeft: Float = DefaultTextPaddingX,
    val paddingTop: Float = DefaultTextPaddingY,
    val paddingRight: Float = DefaultTextPaddingX,
    val paddingBottom: Float = DefaultTextPaddingY,
) {
    val center: Offset get() = Offset(centerX, centerY)

    /** The words as shown on the photo and baked: [content] in its [case]. */
    val shownContent: String get() = applyTextCase(content, case)

    fun withCenter(center: Offset): TextItem =
        copy(centerX = center.x.coerceIn(0f, 1f), centerY = center.y.coerceIn(0f, 1f))
}

/** White, the colour new text starts in. */
internal const val DefaultTextColorArgb: Int = -1

/** How far, as a fraction of the photo, a duplicate or a paste lands from the text it copies. */
internal const val DuplicateNudge = 0.04f

/** The narrowest a side handle can make the text area, as a fraction of the photo's width. */
internal const val MinTextBoxWidth = 0.08f

/**
 * A copy of [item] with id [newId], nudged down and to the right so it doesn't hide its original (or
 * back up and left, when that would push it off the photo). A copy of a locked text is unlocked.
 */
internal fun duplicateTextItem(item: TextItem, newId: Long): TextItem {
    fun nudged(value: Float) = if (value + DuplicateNudge <= 1f) value + DuplicateNudge else value - DuplicateNudge
    return item.copy(id = newId, centerX = nudged(item.centerX), centerY = nudged(item.centerY), locked = false)
}

/** Back to the size, turn, curve and text area a new text has; the words, look and position stay. */
internal fun resetTextItem(item: TextItem): TextItem =
    item.copy(sizeSp = TextSizeDefault, rotationDegrees = 0f, boxWidth = null, boxHeight = null, curve = TextCurveStraight)

/** How far one step of the Nudge panel's arrows moves a text, as a fraction of the photo's width/height. */
internal const val TextNudgeStep = 0.005f

/** How far one step of the Nudge panel's turn buttons turns a text. */
internal const val TextNudgeDegrees = 1f

/** [item] moved by [step] (fractions of the photo), kept on the photo. */
internal fun nudgeTextItem(item: TextItem, step: Offset): TextItem = item.withCenter(item.center + step)

/** [item] turned [degrees] clockwise, its rotation kept within (-180°, 180°] so the Nudge title reads sensibly. */
internal fun turnTextItem(item: TextItem, degrees: Float): TextItem =
    item.copy(rotationDegrees = normalizeDegrees(item.rotationDegrees + degrees))

internal fun normalizeDegrees(degrees: Float): Float {
    val turned = degrees % 360f
    return when {
        turned > 180f -> turned - 360f
        turned <= -180f -> turned + 360f
        else -> turned
    }
}

/** The Position panel's Arrange moves: a text one place up or down the stack, or to its top or bottom. */
internal enum class TextLayerMove { Forward, Backward, ToFront, ToBack }

/**
 * [items] with the text [id] moved by [move]. Later texts draw on top, so forward is one place later
 * in the list and to the front is last. Unchanged if [id] isn't there or is already at that end.
 */
internal fun arrangeTextItems(items: List<TextItem>, id: Long, move: TextLayerMove): List<TextItem> {
    val index = items.indexOfFirst { it.id == id }
    if (index < 0) return items
    val target = when (move) {
        TextLayerMove.Forward -> index + 1
        TextLayerMove.Backward -> index - 1
        TextLayerMove.ToFront -> items.lastIndex
        TextLayerMove.ToBack -> 0
    }.coerceIn(0, items.lastIndex)
    if (target == index) return items
    return items.toMutableList().apply { add(target, removeAt(index)) }
}

/** The Position panel's Align targets: an edge or the middle of the photo, across or down. */
internal enum class TextPhotoAlign { Top, Middle, Bottom, Left, Center, Right }

/**
 * [item] moved against [align]'s edge of the photo, or onto its middle line. [halfWidth]/[halfHeight]
 * are half the (turned) text box's upright bounds, as fractions of the photo's width/height, so the
 * box itself touches the edge. Only the one axis changes.
 */
internal fun alignTextItem(item: TextItem, align: TextPhotoAlign, halfWidth: Float, halfHeight: Float): TextItem =
    when (align) {
        TextPhotoAlign.Top -> item.withCenter(Offset(item.centerX, halfHeight))
        TextPhotoAlign.Middle -> item.withCenter(Offset(item.centerX, 0.5f))
        TextPhotoAlign.Bottom -> item.withCenter(Offset(item.centerX, 1f - halfHeight))
        TextPhotoAlign.Left -> item.withCenter(Offset(halfWidth, item.centerY))
        TextPhotoAlign.Center -> item.withCenter(Offset(0.5f, item.centerY))
        TextPhotoAlign.Right -> item.withCenter(Offset(1f - halfWidth, item.centerY))
    }

/** Which edge of the text area a side handle drags. */
internal enum class TextBoxEdge { Left, Top, Right, Bottom }

/**
 * [item] with [edge] dragged by [delta] — measured in the text's own (rotated) frame, in pixels of a
 * photo shown [photoWidthPx] × [photoHeightPx]. The opposite edge stays put: the area grows or
 * shrinks by the drag and its centre moves by half of it, turned back into photo coordinates.
 *
 * [currentWidthPx]/[currentHeightPx] are the area's size as laid out now, which is where a first
 * drag starts from while the item still fits its words. The height can't shrink below
 * [minHeightPx], what the words need.
 */
internal fun resizeTextBox(
    item: TextItem,
    edge: TextBoxEdge,
    delta: Offset,
    photoWidthPx: Float,
    photoHeightPx: Float,
    currentWidthPx: Float,
    currentHeightPx: Float,
    minHeightPx: Float,
): TextItem {
    if (photoWidthPx <= 0f || photoHeightPx <= 0f) return item
    val minWidthPx = MinTextBoxWidth * photoWidthPx
    return when (edge) {
        TextBoxEdge.Left, TextBoxEdge.Right -> {
            val grow = if (edge == TextBoxEdge.Right) delta.x else -delta.x
            val width = (currentWidthPx + grow).coerceIn(minWidthPx, photoWidthPx)
            val applied = width - currentWidthPx
            val shift = rotateVector(Offset(if (edge == TextBoxEdge.Right) applied / 2f else -applied / 2f, 0f), item.rotationDegrees)
            item.copy(boxWidth = width / photoWidthPx)
                .withCenter(item.center + Offset(shift.x / photoWidthPx, shift.y / photoHeightPx))
        }
        TextBoxEdge.Top, TextBoxEdge.Bottom -> {
            val grow = if (edge == TextBoxEdge.Bottom) delta.y else -delta.y
            val height = (currentHeightPx + grow).coerceIn(minHeightPx, photoHeightPx)
            val applied = height - currentHeightPx
            val shift = rotateVector(Offset(0f, if (edge == TextBoxEdge.Bottom) applied / 2f else -applied / 2f), item.rotationDegrees)
            item.copy(boxHeight = height / photoWidthPx)
                .withCenter(item.center + Offset(shift.x / photoWidthPx, shift.y / photoHeightPx))
        }
    }
}

/**
 * [item] scaled by [factor] around its centre, as a corner handle or a pinch does: the words and, if
 * set, the text area together, so wrapped lines break in the same places. The factor is cut short
 * where the size would leave [TextSizeRange].
 */
internal fun scaleTextItem(item: TextItem, factor: Float): TextItem {
    if (factor <= 0f || item.sizeSp <= 0f) return item
    val sizeSp = (item.sizeSp * factor).coerceIn(TextSizeRange)
    val applied = sizeSp / item.sizeSp
    return item.copy(
        sizeSp = sizeSp,
        boxWidth = item.boxWidth?.let { (it * applied).coerceIn(MinTextBoxWidth, 1f) },
        boxHeight = item.boxHeight?.let { it * applied },
    )
}

/** The Font size panel's 0–100 slider, mapped onto [TextSizeRange]. */
internal fun textSizeToSlider(sizeSp: Float): Float =
    ((sizeSp - TextSizeRange.start) / (TextSizeRange.endInclusive - TextSizeRange.start) * 100f).coerceIn(0f, 100f)

internal fun sliderToTextSize(value: Float): Float =
    TextSizeRange.start + value.coerceIn(0f, 100f) / 100f * (TextSizeRange.endInclusive - TextSizeRange.start)

private val TextItemsJson = Json { ignoreUnknownKeys = true }

/**
 * Saves the texts as JSON: the edit has to survive the paywall covering the editor (see
 * [TextContent]), and a list of data classes can't go in a Bundle as it is.
 */
internal val TextItemsSaver: Saver<List<TextItem>, String> = Saver(
    save = { TextItemsJson.encodeToString(ListSerializer(TextItem.serializer()), it) },
    restore = { runCatching { TextItemsJson.decodeFromString(ListSerializer(TextItem.serializer()), it) }.getOrNull() },
)

/** One text or none, saved like [TextItemsSaver]: what an open panel's ✕ puts back. */
internal val OptionalTextItemSaver: Saver<TextItem?, String> = Saver(
    save = { TextItemsJson.encodeToString(ListSerializer(TextItem.serializer()), listOfNotNull(it)) },
    restore = { runCatching { TextItemsJson.decodeFromString(ListSerializer(TextItem.serializer()), it).firstOrNull() }.getOrNull() },
)

/** Ids saved as one string, which every platform's saved state takes. */
internal val IdListSaver: Saver<List<Long>, String> = Saver(
    save = { it.joinToString(",") },
    restore = { saved -> saved.split(',').mapNotNull { it.toLongOrNull() } },
)
