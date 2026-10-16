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
 * @param backgroundCorner the plate's roundness on the Color panel's 0–100 slider ([textPlateCornerFraction]).
 * @param strokeArgb the outline around the letters; null for none.
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
) {
    val center: Offset get() = Offset(centerX, centerY)

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

/** Back to the size, turn and text area a new text has; the words, look and position stay. */
internal fun resetTextItem(item: TextItem): TextItem =
    item.copy(sizeSp = TextSizeDefault, rotationDegrees = 0f, boxWidth = null, boxHeight = null)

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
