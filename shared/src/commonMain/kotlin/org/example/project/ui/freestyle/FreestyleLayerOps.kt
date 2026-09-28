package org.example.project.ui.freestyle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
internal fun List<FreestyleLayer>.withText(id: Long, text: String, color: Color, font: TextFontStyleOption): List<FreestyleLayer> =
    map { layer ->
        if (layer.id == id && layer.content is FreestyleContent.TextContent) {
            layer.copy(content = FreestyleContent.TextContent(text, color, font))
        } else {
            layer
        }
    }

/** Changes only the words of text layer [id], keeping its style and placement. */
internal fun List<FreestyleLayer>.retyped(id: Long, text: String): List<FreestyleLayer> = map { layer ->
    val content = layer.content
    if (layer.id == id && content is FreestyleContent.TextContent) layer.copy(content = content.copy(text = text)) else layer
}
