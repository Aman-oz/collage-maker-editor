package org.example.project.ui.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.ToolTopBar
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TextScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TextViewModel = koinViewModel(),
) {
    val textMeasurer = rememberTextMeasurer()
    TextContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { content, font, color, sizeSp, canvasWidthPx, offsetFraction, rotationDegrees ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyText(
                    bakeText(
                        source = image,
                        textMeasurer = textMeasurer,
                        content = content,
                        fontStyleOption = font,
                        color = color,
                        sizeSp = sizeSp,
                        previewCanvasWidthPx = canvasWidthPx,
                        offsetFraction = offsetFraction,
                        rotationDegrees = rotationDegrees,
                    ),
                )
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun TextContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (
        content: String,
        font: TextFontStyleOption,
        color: Color,
        sizeSp: Float,
        canvasWidthPx: Float,
        offsetFraction: Offset,
        rotationDegrees: Float,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var hasAddedText by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(TextLayerMode.Idle) }
    var content by remember { mutableStateOf("") }
    var selectedFont by remember { mutableStateOf(TextFontStyles[1]) }
    var selectedColor by remember { mutableStateOf(TextColorOptions.first()) }
    var sizeSp by remember { mutableFloatStateOf(TextSizeDefault) }
    var photoSizePx by remember { mutableStateOf(IntSize.Zero) }
    var textOffsetFraction by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var textBoxSizePx by remember { mutableStateOf(IntSize.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }

    val focusManager = LocalFocusManager.current
    val removeText = {
        hasAddedText = false
        content = ""
        mode = TextLayerMode.Idle
        textOffsetFraction = Offset(0.5f, 0.5f)
        rotationDegrees = 0f
    }
    val moveText = { pan: Offset ->
        if (photoSizePx.width > 0 && photoSizePx.height > 0) {
            textOffsetFraction = Offset(
                (textOffsetFraction.x + pan.x / photoSizePx.width).coerceIn(0f, 1f),
                (textOffsetFraction.y + pan.y / photoSizePx.height).coerceIn(0f, 1f),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            // The keyboard is left out of the insets on purpose: it slides up over the controls
            // while the photo (and the text being typed on it) stays exactly where it was.
            .windowInsetsPadding(WindowInsets.safeDrawing.exclude(WindowInsets.ime)),
    ) {
        ToolTopBar(
            title = "Text",
            onClose = onBack,
            onDone = {
                onDone(content, selectedFont, selectedColor, sizeSp, photoSizePx.width.toFloat(), textOffsetFraction, rotationDegrees)
            },
            doneEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                // Tapping anywhere off the text ends editing (the focus loss lands it in Selected
                // first) and then deselects it.
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        mode = TextLayerMode.Idle
                    }
                }
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                // Sized to the photo itself, so the text overlay's coordinate space is exactly the
                // displayed image — its width is the preview width bakeText scales up from. Only the
                // inner layer is clipped: the selection handles may hang past the photo's edges.
                Box(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .onSizeChanged { photoSizePx = it },
                ) {
                    val textStyle = TextStyle(
                        fontFamily = selectedFont.fontFamily,
                        fontWeight = selectedFont.fontWeight,
                        fontStyle = selectedFont.fontStyle,
                        fontSize = sizeSp.sp,
                        color = selectedColor,
                        textAlign = TextAlign.Center,
                    )
                    val textBoxOffset = IntOffset(
                        (textOffsetFraction.x * photoSizePx.width - textBoxSizePx.width / 2f).roundToInt(),
                        (textOffsetFraction.y * photoSizePx.height - textBoxSizePx.height / 2f).roundToInt(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                    ) {
                        Image(
                            bitmap = sourceImage,
                            contentDescription = "Photo preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        if (hasAddedText) {
                            TextLayer(
                                content = content,
                                onContentChange = { content = it },
                                editing = mode == TextLayerMode.Editing,
                                onEditingEnd = {
                                    // Leaving an empty label behind would bake nothing; go back to "+ Add Text".
                                    if (content.isBlank()) removeText() else mode = TextLayerMode.Selected
                                },
                                style = textStyle,
                                modifier = Modifier
                                    .onSizeChanged { textBoxSizePx = it }
                                    .offset { textBoxOffset }
                                    .graphicsLayer { rotationZ = rotationDegrees },
                            )
                        }
                    }

                    if (hasAddedText && mode != TextLayerMode.Editing) {
                        TextGestureBox(
                            selected = mode == TextLayerMode.Selected,
                            boxSizePx = textBoxSizePx,
                            onTap = {
                                mode = if (mode == TextLayerMode.Selected) TextLayerMode.Editing else TextLayerMode.Selected
                            },
                            onMove = { pan ->
                                mode = TextLayerMode.Selected
                                moveText(pan)
                            },
                            onZoom = { zoom -> sizeSp = (sizeSp * zoom).coerceIn(TextSizeRange) },
                            onRotate = { delta -> rotationDegrees = snapTextRotation(rotationDegrees + delta) },
                            onScaleRotate = { newSizeSp, newRotation ->
                                sizeSp = newSizeSp.coerceIn(TextSizeRange)
                                rotationDegrees = snapTextRotation(newRotation)
                            },
                            currentSizeSp = { sizeSp },
                            rotationDegrees = rotationDegrees,
                            onDelete = removeText,
                            modifier = Modifier.offset { textBoxOffset },
                        )
                    } else if (!hasAddedText) {
                        AddTextButton(
                            onClick = {
                                hasAddedText = true
                                mode = TextLayerMode.Editing
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            } else {
                Text(
                    text = "No image to add text to",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        SectionLabel(text = "Font")
        FontRow(selected = selectedFont, onSelected = { selectedFont = it })

        SectionLabel(text = "Color")
        ColorRow(selected = selectedColor, onSelected = { selectedColor = it })

        TextSizeLabelRow(value = sizeSp)
        CenterFillSlider(
            value = sizeSp,
            onValueChange = { sizeSp = it },
            range = TextSizeRange,
            referenceValue = TextSizeRange.start,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 26.dp,
            thumbHeight = 14.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private enum class TextLayerMode { Idle, Selected, Editing }

/**
 * The visible text on the photo: a text field typed into in place while [editing], a plain label
 * otherwise. Both use the same symmetric padding, so the text's center — what bakeText positions
 * by — doesn't move when switching modes. Gestures live on [TextGestureBox] instead, which sits
 * outside the photo's clip so its corner handles are never cut off.
 */
@Composable
private fun TextLayer(
    content: String,
    onContentChange: (String) -> Unit,
    editing: Boolean,
    onEditingEnd: () -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val padded = modifier.padding(horizontal = 14.dp, vertical = 6.dp)
    if (editing) {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        // Remembered inside this branch, so it resets every editing session: the field reports
        // "unfocused" once before requestFocus lands, which must not count as leaving the field.
        var hadFocus by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            textStyle = style,
            cursorBrush = SolidColor(style.color),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = modifier
                .dashedPill(color = Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        onEditingEnd()
                    }
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (content.isEmpty()) {
                        Text(text = "Your Text", style = style.copy(color = Color.White.copy(alpha = 0.7f)), maxLines = 1)
                    }
                    innerTextField()
                }
            },
        )
    } else {
        Text(text = content, style = style, maxLines = 1, modifier = padded)
    }
}

/**
 * An invisible box laid exactly over the text (rotated with it) that takes its gestures: tap
 * selects (and a tap on the selected text edits it), one finger drags, two fingers pinch-zoom and
 * twist. When [selected] it draws the dashed outline, a delete button on the top-left corner and a
 * scale/rotate handle on the bottom-right.
 *
 * Because the box itself is rotated, pointer deltas arrive in its rotated coordinates; they are
 * turned back by [rotationDegrees] into photo coordinates before moving the text or measuring the
 * handle's angle.
 */
@Composable
private fun TextGestureBox(
    selected: Boolean,
    boxSizePx: IntSize,
    onTap: () -> Unit,
    onMove: (pan: Offset) -> Unit,
    onZoom: (zoom: Float) -> Unit,
    onRotate: (deltaDegrees: Float) -> Unit,
    onScaleRotate: (sizeSp: Float, rotationDegrees: Float) -> Unit,
    currentSizeSp: () -> Float,
    rotationDegrees: Float,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnZoom by rememberUpdatedState(onZoom)
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentBoxSize by rememberUpdatedState(boxSizePx)
    val currentRotation by rememberUpdatedState(rotationDegrees)

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotationDegrees }
            .size(with(density) { boxSizePx.width.toDp() }, with(density) { boxSizePx.height.toDp() })
            .then(if (selected) Modifier.dashedPill(color = Color.White) else Modifier)
            .pointerInput(Unit) { detectTapGestures { currentOnTap() } }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    currentOnMove(rotateVector(pan, currentRotation))
                    if (zoom != 1f) currentOnZoom(zoom)
                    if (rotation != 0f) currentOnRotate(rotation)
                }
            },
    ) {
        if (selected) {
            TextHandle(
                icon = Icons.Filled.Close,
                contentDescription = "Delete text",
                background = Color(0xFFE53935),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -TextHandleSize / 2, y = -TextHandleSize / 2)
                    .clickable(onClick = onDelete),
            )
            TextHandle(
                icon = Icons.Filled.OpenInFull,
                contentDescription = "Drag to resize and rotate text",
                background = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = TextHandleSize / 2, y = TextHandleSize / 2)
                    .pointerInput(Unit) {
                        // Tracks the finger as a vector from the text's center, in photo coordinates:
                        // it starts at the (rotated) corner and accumulates each drag delta, rotated
                        // out of the box's frame. Its length relative to the start sets the size, its
                        // angle relative to the start sets the rotation — so the handle follows the
                        // finger like a real corner even as the box resizes and turns under it.
                        var startSizeSp = 0f
                        var startRotation = 0f
                        var startVector = Offset.Zero
                        var fingerVector = Offset.Zero
                        detectDragGestures(
                            onDragStart = {
                                startSizeSp = currentSizeSp()
                                startRotation = currentRotation
                                startVector = rotateVector(
                                    Offset(currentBoxSize.width / 2f, currentBoxSize.height / 2f),
                                    currentRotation,
                                )
                                fingerVector = startVector
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            fingerVector += rotateVector(dragAmount, currentRotation)
                            val startDistance = startVector.getDistance()
                            if (startDistance > 0f) {
                                onScaleRotate(
                                    startSizeSp * fingerVector.getDistance() / startDistance,
                                    startRotation + vectorAngleDegrees(fingerVector) - vectorAngleDegrees(startVector),
                                )
                            }
                        }
                    },
            )
        }
    }
}

private val TextHandleSize = 26.dp

@Composable
private fun TextHandle(icon: ImageVector, contentDescription: String, background: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TextHandleSize)
            .clip(CircleShape)
            .background(background)
            .border(width = 1.5.dp, color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AddTextButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.25f))
            .dashedPill(color = Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Add Text", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** A dashed, fully rounded outline — the "editable text box" affordance from the design. */
private fun Modifier.dashedPill(color: Color): Modifier = drawBehind {
    val strokeWidth = 1.5.dp.toPx()
    val inset = strokeWidth / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
        cornerRadius = CornerRadius(size.height / 2f),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
internal fun FontRow(selected: TextFontStyleOption, onSelected: (TextFontStyleOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(TextFontStyles) { option ->
            FontChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun FontChip(option: TextFontStyleOption, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val tint = if (selected) scheme.primary else scheme.onSurface
    Column(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(if (selected) scheme.primary.copy(alpha = 0.08f) else scheme.surface)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.15f),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                fontFamily = option.fontFamily,
                fontWeight = option.fontWeight,
                fontStyle = option.fontStyle,
                fontSize = 16.sp,
                color = tint,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = option.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.primary else scheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ColorRow(selected: Color, onSelected: (Color) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(TextColorOptions) { color ->
            ColorSwatch(color = color, selected = color == selected, onClick = { onSelected(color) })
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val outerShape = RoundedCornerShape(12.dp)
    val innerShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(outerShape)
            .then(
                if (selected) Modifier.border(width = 1.5.dp, color = scheme.onSurface.copy(alpha = 0.35f), shape = outerShape)
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(innerShape)
                .background(color)
                // Keeps light swatches (white) visible against a light surface.
                .border(width = 1.dp, color = scheme.onSurface.copy(alpha = 0.1f), shape = innerShape),
        )
    }
}

@Composable
private fun TextSizeLabelRow(value: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Text Size",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = scheme.primary,
        )
    }
}

@Preview
@Composable
private fun TextScreenPreview() {
    ThemePreviews {
        TextContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _, _, _, _, _, _ -> })
    }
}
