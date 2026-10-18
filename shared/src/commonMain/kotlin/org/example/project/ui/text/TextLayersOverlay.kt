package org.example.project.ui.text

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Live texts ([TextLayers]) drawn over a picture that this fills exactly, as they will bake, clipped
 * to it as the bake crops them. A tap on one reports its id, so the screen can open the Text tool on
 * it — the photo editor over its photo, the freestyle editor over its canvas.
 *
 * The taps are caught by an invisible box per text, sized and turned like its selection box in the
 * tool, rather than by the whole overlay: a pointer handler over the whole picture would take every
 * touch from what is underneath, and the freestyle canvas's photos and stickers there still have to
 * be dragged and pinched everywhere a text isn't. Where texts overlap the last drawn (the top one)
 * gets the tap.
 */
@Composable
internal fun TextLayersOverlay(layers: TextLayers, onTextTapped: (Long) -> Unit, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val measured = remember(layers, textMeasurer) { measureTextLayers(textMeasurer, layers) }
    val currentOnTextTapped by rememberUpdatedState(onTextTapped)
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMeasuredTexts(measured, layers.canvasWidthPx)
        }
        if (layers.canvasWidthPx <= 0f) return@BoxWithConstraints
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val scale = widthPx / layers.canvasWidthPx
        for (text in measured) {
            key(text.item.id) {
                val boxWidthPx = text.boxSize.width * scale
                val boxHeightPx = text.boxSize.height * scale
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (text.item.centerX * widthPx - boxWidthPx / 2f).roundToInt(),
                                (text.item.centerY * heightPx - boxHeightPx / 2f).roundToInt(),
                            )
                        }
                        .size(with(density) { boxWidthPx.toDp() }, with(density) { boxHeightPx.toDp() })
                        // Turned like the text, so a tap's hit test follows its turned box.
                        .graphicsLayer { rotationZ = text.item.rotationDegrees }
                        .pointerInput(text.item.id) { detectTapGestures { currentOnTextTapped(text.item.id) } },
                )
            }
        }
    }
}
