package org.example.project.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.example.project.data.AppSettings
import org.example.project.i18n.tr
import org.koin.compose.koinInject
import kotlin.math.roundToInt

/** The colour wheel, as stops 60° apart; the last repeats the first so a sweep closes seamlessly. */
private val HueStops = List(7) { Color.hsv(it * 60f, 1f, 1f) }

private val PickerCorner = 10.dp
private val TrackHeight = 30.dp
private val CheckerLight = Color(0xFFFFFFFF)
private val CheckerDark = Color(0xFFD5D5DA)

/** What the saved colours start as, until the user saves one of their own. */
private val DefaultSavedColors = listOf(
    0xFF12B76A, 0xFFFFC21A, 0xFFFF7F2A, 0xFFF0443A, 0xFFFF3B5C,
    0xFF0F5F8F, 0xFF7D4FE0, 0xFFE81E63, 0xFFA8705A,
).map { it.toInt() }

private enum class PickerTab(val englishLabel: String) {
    Grid("Grid"),
    Spectrum("Spectrum"),
    Sliders("Sliders"),
    ;

    val label get() = tr(englishLabel)
}

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
 * A free colour chooser opening on [initial]: an eyedropper (see [EyedropperHost]), a Grid,
 * Spectrum or RGB Sliders tab, an Opacity slider (when [showOpacity]; without it the colour is
 * always opaque) and the saved colours, which ＋ adds to and which are kept across launches in
 * [AppSettings.savedColors].
 *
 * There is no Done: like the system pickers this one copies, closing it (✕, back, a tap outside)
 * keeps the colour. The host still gets one [onPicked] (one undo step) per visit rather than one per
 * drag tick, and none at all ([onDismiss] instead) when the colour wasn't changed.
 */
@Composable
internal fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onPicked: (Color) -> Unit,
    showOpacity: Boolean = true,
) {
    val settings: AppSettings = koinInject()
    val scheme = MaterialTheme.colorScheme
    val opening = if (showOpacity) initial else initial.copy(alpha = 1f)
    var hsv by remember { mutableStateOf(opening.toHsv()) }
    var alpha by remember { mutableFloatStateOf(opening.alpha) }
    var tab by remember { mutableIntStateOf(PickerTab.Grid.ordinal) }
    var saved by remember { mutableStateOf(settings.savedColors ?: DefaultSavedColors) }
    val opaque = hsv.toColor()
    val color = opaque.copy(alpha = alpha)
    val close = { if (color.toArgb() != opening.toArgb()) onPicked(color) else onDismiss() }
    val eyedropper = LocalEyedropper.current
    // While the eyedropper is out the dialog steps aside (its state stays here) to show the screen.
    var sampling by remember { mutableStateOf(false) }
    if (sampling) return

    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = scheme.surface,
            modifier = Modifier.padding(16.dp).widthIn(max = 360.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (eyedropper != null) {
                        IconButton(
                            onClick = {
                                sampling = true
                                eyedropper.pick { picked ->
                                    sampling = false
                                    // The eyedropper sees only opaque colour; the opacity set here stays.
                                    if (picked != null) hsv = picked.toHsv()
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterStart),
                        ) {
                            Icon(Icons.Filled.Colorize, contentDescription = tr("Pick a color from the screen"), tint = scheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = tr("Colors"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    IconButton(onClick = close, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Filled.Close, contentDescription = tr("Close"), tint = scheme.onSurfaceVariant)
                    }
                }
                PickerTabs(selected = tab, onSelected = { tab = it })
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(ColorGridColumns.toFloat() / ColorGridRows)) {
                    when (PickerTab.entries[tab]) {
                        PickerTab.Grid -> ColorGrid(selected = opaque, onSelected = { hsv = it.toHsv() })
                        PickerTab.Spectrum -> ColorSpectrum(hsv = hsv, onChange = { hsv = it })
                        PickerTab.Sliders -> RgbSliders(color = opaque, onChange = { hsv = it.toHsv() })
                    }
                }
                if (showOpacity) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = tr("Opacity"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientTrack(
                            brush = Brush.horizontalGradient(listOf(opaque.copy(alpha = 0f), opaque)),
                            fraction = alpha,
                            thumbColor = opaque,
                            checkered = true,
                            onChange = { alpha = it },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(alpha * 100f).roundToInt()}%",
                            fontSize = 17.sp,
                            color = scheme.onSurface,
                            modifier = Modifier.width(60.dp).padding(start = 12.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(color = color, shape = RoundedCornerShape(PickerCorner), modifier = Modifier.size(68.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    SavedColors(
                        saved = saved,
                        current = color,
                        onSelected = { argb ->
                            val picked = Color(argb)
                            hsv = picked.toHsv()
                            if (showOpacity) alpha = picked.alpha
                        },
                        onAdd = {
                            saved = addSavedColor(saved, color.toArgb())
                            settings.savedColors = saved
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * The custom colour swatch's other half: hosts a [ColorPickerDialog] and returns the function that
 * opens it. [initial] is the colour in use when it opens; [onPicked] gets the choice as it closes.
 */
@Composable
internal fun rememberColorPickerLauncher(
    initial: Color,
    showOpacity: Boolean = true,
    onPicked: (Color) -> Unit,
): () -> Unit {
    var open by remember { mutableStateOf(false) }
    if (open) {
        ColorPickerDialog(
            initial = initial,
            showOpacity = showOpacity,
            onDismiss = { open = false },
            onPicked = {
                onPicked(it)
                open = false
            },
        )
    }
    return { open = true }
}

/** Grid / Spectrum / Sliders as a plain segmented control: a grey capsule, the pick raised on it. */
@Composable
private fun PickerTabs(selected: Int, onSelected: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val segment = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.onSurface.copy(alpha = 0.08f))
            .padding(2.dp),
    ) {
        PickerTab.entries.forEach { tab ->
            val on = tab.ordinal == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (on) {
                            Modifier
                                .shadow(1.dp, segment)
                                .background(if (isLight) Color.White else scheme.onSurface.copy(alpha = 0.2f), segment)
                        } else {
                            Modifier
                        },
                    )
                    .clip(segment)
                    .clickable { onSelected(tab.ordinal) },
            ) {
                Text(
                    text = tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = scheme.onSurface,
                )
            }
        }
    }
}

/** The [colorGridColor] grid; the cell showing [selected], if any, gets a white square. */
@Composable
private fun ColorGrid(selected: Color, onSelected: (Color) -> Unit) {
    val cell = colorGridCellOf(selected)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(PickerCorner))
            .dragFraction { x, y ->
                val row = (y * ColorGridRows).toInt().coerceAtMost(ColorGridRows - 1)
                val column = (x * ColorGridColumns).toInt().coerceAtMost(ColorGridColumns - 1)
                onSelected(colorGridColor(row, column))
            },
    ) {
        val width = size.width / ColorGridColumns
        val height = size.height / ColorGridRows
        for (row in 0 until ColorGridRows) {
            for (column in 0 until ColorGridColumns) {
                // Half a pixel over each edge, so no hairline of the dialog shows between cells.
                drawRect(
                    colorGridColor(row, column),
                    topLeft = Offset(column * width, row * height),
                    size = Size(width + 0.5f, height + 0.5f),
                )
            }
        }
        if (cell != null) {
            val stroke = 3.dp.toPx()
            drawRect(
                Color.White,
                topLeft = Offset(cell.second * width + stroke / 2f, cell.first * height + stroke / 2f),
                size = Size(width - stroke, height - stroke),
                style = Stroke(stroke),
            )
        }
    }
}

/** Every hue across, white to full colour to black down: see [spectrumColor]. */
@Composable
private fun ColorSpectrum(hsv: Hsv, onChange: (Hsv) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(PickerCorner))
            .dragFraction { x, y -> onChange(spectrumColor(x, y)) },
    ) {
        drawRect(Brush.horizontalGradient(HueStops))
        // Fading white over the top half and black over the bottom draws exactly spectrumColor.
        drawRect(Brush.verticalGradient(0f to Color.White, 0.5f to Color.White.copy(alpha = 0f)))
        drawRect(Brush.verticalGradient(0.5f to Color.Black.copy(alpha = 0f), 1f to Color.Black))
        val position = hsv.spectrumPosition()
        drawThumb(Offset(position.x * size.width, position.y * size.height), 12.dp.toPx(), hsv.toColor())
    }
}

/** Red, green and blue as 0–255 sliders, each track showing what it would change to, and the hex. */
@Composable
private fun RgbSliders(color: Color, onChange: (Color) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize(),
    ) {
        listOf(
            tr("Red") to color.red,
            tr("Green") to color.green,
            tr("Blue") to color.blue,
        ).forEachIndexed { channel, (label, value) ->
            fun withLevel(level: Float) = when (channel) {
                0 -> color.copy(red = level)
                1 -> color.copy(green = level)
                else -> color.copy(blue = level)
            }
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GradientTrack(
                    brush = Brush.horizontalGradient(listOf(withLevel(0f), withLevel(1f))),
                    fraction = value,
                    thumbColor = color,
                    checkered = false,
                    onChange = { onChange(withLevel(it)) },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = (value * 255f).roundToInt().toString(),
                    fontSize = 15.sp,
                    color = scheme.onSurface,
                    modifier = Modifier.width(48.dp).padding(start = 12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val hex = color.toHexRgb()
        // Re-seeded whenever the colour changes from elsewhere; a half-typed value isn't a colour
        // yet, so it doesn't change the key and stays as typed.
        var text by remember(hex) { mutableStateOf(hex) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tr("Hex Color #"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            BasicTextField(
                value = text,
                onValueChange = { typed ->
                    val digits = typed.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(6).uppercase()
                    text = digits
                    parseHexRgb(digits)?.let(onChange)
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                modifier = Modifier
                    .width(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.onSurface.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/**
 * The saved colours as rows of five round swatches ending in ＋, which saves [current]. The one
 * showing [current] is ringed.
 */
@Composable
private fun SavedColors(
    saved: List<Int>,
    current: Color,
    onSelected: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val currentArgb = current.toArgb()
    val cells: List<Int?> = saved + null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        cells.chunked(SavedColorsPerRow).forEach { row ->
            Row {
                row.forEach { argb ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        if (argb == null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(scheme.onSurface.copy(alpha = 0.1f))
                                    .clickable(onClick = onAdd),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = tr("Save color"), tint = scheme.onSurface, modifier = Modifier.size(20.dp))
                            }
                        } else {
                            val swatch = Color(argb)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .then(if (argb == currentArgb) Modifier.border(2.dp, swatch.copy(alpha = 1f), CircleShape) else Modifier)
                                    .clip(CircleShape)
                                    .clickable { onSelected(argb) },
                            ) {
                                ColorDot(
                                    color = swatch,
                                    shape = CircleShape,
                                    modifier = Modifier.size(if (argb == currentArgb) 22.dp else 30.dp),
                                )
                            }
                        }
                    }
                }
                // Keeps a short last row on the columns of the rows above it.
                repeat(SavedColorsPerRow - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

private const val SavedColorsPerRow = 5

/** [color] over the transparency checkerboard, so its alpha shows, with a hairline edge. */
@Composable
private fun ColorDot(color: Color, shape: Shape, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = modifier.clip(shape).border(1.dp, scheme.onSurface.copy(alpha = 0.12f), shape)) {
        if (color.alpha < 1f) drawCheckerboard(6.dp.toPx())
        drawRect(color)
    }
}

/**
 * A rounded slider track filled with [brush] (over a checkerboard when [checkered]) and a round
 * thumb filled with [thumbColor] at [fraction]; dragging anywhere on it reports the new 0–1 value.
 */
@Composable
private fun GradientTrack(
    brush: Brush,
    fraction: Float,
    thumbColor: Color,
    checkered: Boolean,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .height(TrackHeight)
            // Inset like the thumb below, so the value under the finger is the thumb's.
            .dragFraction(thumbInset = true) { x, _ -> onChange(x) },
    ) {
        val radius = size.height / 2f
        val corner = CornerRadius(radius)
        if (checkered) {
            val track = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) }
            clipPath(track) { drawCheckerboard(radius / 2f) }
        }
        drawRoundRect(brush, cornerRadius = corner)
        // Kept a radius inside each end, so the thumb never hangs off the track.
        drawThumb(Offset(radius + fraction.coerceIn(0f, 1f) * (size.width - 2f * radius), radius), radius - 2.dp.toPx(), thumbColor)
    }
}

/** A disc of [fill] in a white ring with a dark hairline, so it shows over any colour under it. */
private fun DrawScope.drawThumb(center: Offset, radius: Float, fill: Color) {
    drawCircle(fill.copy(alpha = 1f), radius, center)
    drawCircle(Color.Black.copy(alpha = 0.3f), radius + 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
    drawCircle(Color.White, radius - 1.5.dp.toPx(), center, style = Stroke(3.dp.toPx()))
}

private fun DrawScope.drawCheckerboard(cell: Float) {
    drawRect(CheckerLight)
    val columns = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if ((row + column) % 2 == 1) {
                drawRect(CheckerDark, topLeft = Offset(column * cell, row * cell), size = Size(cell, cell))
            }
        }
    }
}

/**
 * Reports the finger's position as 0–1 fractions of this box, from the first touch and through the
 * drag, clamped so a finger that slides off an edge pins the value there. With [thumbInset], x is
 * measured between points half the box's height in from each end, where a slider's round thumb
 * stops. [onChange] is read fresh on every move, so one that reads the colour it edits never sees a
 * stale one.
 */
private fun Modifier.dragFraction(thumbInset: Boolean = false, onChange: (x: Float, y: Float) -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onChange)
    pointerInput(thumbInset) {
        fun report(position: Offset) {
            val inset = if (thumbInset) size.height / 2f else 0f
            latest(
                ((position.x - inset) / (size.width - 2f * inset)).coerceIn(0f, 1f),
                (position.y / size.height).coerceIn(0f, 1f),
            )
        }
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
}
