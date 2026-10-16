package org.example.project.ui.text

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer

/**
 * The editor's live texts ([TextLayers]), drawn over a photo that this fills exactly, as they will
 * bake. A tap on one reports its id, the topmost where texts overlap (the last drawn), so the editor
 * can open the Text tool on it; a tap anywhere else does nothing.
 */
@Composable
internal fun TextLayersOverlay(layers: TextLayers, onTextTapped: (Long) -> Unit, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val measured = remember(layers, textMeasurer) { measureTextLayers(textMeasurer, layers) }
    val currentOnTextTapped by rememberUpdatedState(onTextTapped)
    Canvas(
        modifier = modifier.pointerInput(measured) {
            detectTapGestures { tap ->
                if (layers.canvasWidthPx <= 0f) return@detectTapGestures
                val scale = size.width / layers.canvasWidthPx
                val hit = measured.lastOrNull { text ->
                    turnedBoxContains(
                        point = tap,
                        center = Offset(text.item.centerX * size.width, text.item.centerY * size.height),
                        width = text.boxSize.width * scale,
                        height = text.boxSize.height * scale,
                        degrees = text.item.rotationDegrees,
                    )
                }
                if (hit != null) currentOnTextTapped(hit.item.id)
            }
        },
    ) {
        drawMeasuredTexts(measured, layers.canvasWidthPx)
    }
}
