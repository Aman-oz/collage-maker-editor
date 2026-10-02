package org.example.project.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.i18n.tr

/** The colour wheel, as stops 60° apart; the last repeats the first so a sweep closes seamlessly. */
private val HueStops = List(7) { Color.hsv(it * 60f, 1f, 1f) }

private val PickerCorner = 12.dp

/**
 * What a swatch row's "custom colour" tile shows in place of a flat colour: the whole colour wheel
 * under an eyedropper. The caller supplies the tile's size, shape and click.
 */
@Composable
internal fun ColorPickerFill(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Brush.sweepGradient(HueStops)), contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Colorize, contentDescription = tr("Custom color"), tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/**
 * A free colour chooser: a saturation/brightness square over a hue strip, opening on [initial].
 * Nothing is applied until Done, so the host gets one [onPicked] (one undo step) per visit rather
 * than one per drag tick; Cancel, back and a tap outside all leave the colour as it was.
 */
@Composable
internal fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPicked: (Color) -> Unit) {
    var hsv by remember { mutableStateOf(initial.toHsv()) }
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = tr("Custom color"), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(PickerCorner))
                        .dragFraction { x, y -> hsv = hsv.copy(saturation = x, value = 1f - y) },
                ) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue, 1f, 1f))))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    drawThumb(Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height), 9.dp.toPx())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .dragFraction { x, _ -> hsv = hsv.copy(hue = x * 360f) },
                ) {
                    drawRoundRect(Brush.horizontalGradient(HueStops), cornerRadius = CornerRadius(size.height / 2f))
                    // Kept a radius inside each end, so the thumb never hangs off the strip.
                    val radius = size.height / 2f
                    drawThumb(Offset(radius + hsv.hue / 360f * (size.width - 2f * radius), radius), radius - 2.dp.toPx())
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(PickerCorner))
                        .background(hsv.toColor())
                        // Keeps a colour close to the dialog's own surface visible.
                        .border(1.dp, scheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(PickerCorner)),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPicked(hsv.toColor()) }) { Text(text = tr("Done"), color = scheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = tr("Cancel"), color = scheme.onSurfaceVariant) }
        },
    )
}

/**
 * The custom colour swatch's other half: hosts a [ColorPickerDialog] and returns the function that
 * opens it. [initial] is the colour in use when it opens; [onPicked] gets the choice on Done.
 */
@Composable
internal fun rememberColorPickerLauncher(initial: Color, onPicked: (Color) -> Unit): () -> Unit {
    var open by remember { mutableStateOf(false) }
    if (open) {
        ColorPickerDialog(
            initial = initial,
            onDismiss = { open = false },
            onPicked = {
                onPicked(it)
                open = false
            },
        )
    }
    return { open = true }
}

/** A white ring with a dark hairline, so the thumb shows over any colour under it. */
private fun DrawScope.drawThumb(center: Offset, radius: Float) {
    drawCircle(Color.Black.copy(alpha = 0.35f), radius + 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    drawCircle(Color.White, radius, center, style = Stroke(2.5.dp.toPx()))
}

/**
 * Reports the finger's position as 0–1 fractions of this box, from the first touch and through the
 * drag, clamped so a finger that slides off an edge pins the value there.
 */
private fun Modifier.dragFraction(onChange: (x: Float, y: Float) -> Unit): Modifier = pointerInput(Unit) {
    fun report(position: Offset) =
        onChange((position.x / size.width).coerceIn(0f, 1f), (position.y / size.height).coerceIn(0f, 1f))
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        report(down.position)
        drag(down.id) { change ->
            change.consume()
            report(change.position)
        }
    }
}
