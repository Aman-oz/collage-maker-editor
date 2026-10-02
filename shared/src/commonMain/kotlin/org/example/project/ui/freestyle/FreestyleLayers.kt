package org.example.project.ui.freestyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.i18n.tr
import org.example.project.ui.common.ColorPickerDialog
import org.example.project.ui.common.StickerFlights
import org.example.project.ui.common.bubbleClick
import org.example.project.ui.common.rememberBubbleClick
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.stickerFlightTarget
import org.example.project.ui.emoji.EmojiCategory
import org.example.project.ui.emoji.EmojiCategoryTabs
import org.example.project.ui.emoji.EmojiGrid
import org.example.project.ui.emoji.EmojisByCategory
import org.example.project.ui.emoji.MaxRecentEmojis
import org.example.project.ui.text.ColorRow
import org.example.project.ui.text.FontRow
import org.example.project.ui.text.TextBackgroundPadXFraction
import org.example.project.ui.text.TextColorTarget
import org.example.project.ui.text.TextColorTargetSwitch
import org.example.project.ui.text.TextFontStyleOption
import org.example.project.ui.text.drawTextPlate
import org.example.project.ui.text.rotateVector
import org.example.project.ui.text.snapTextRotation
import org.example.project.ui.text.vectorAngleDegrees
import org.example.project.ui.theme.Brand

// The freestyle editor's layer canvas, selection handles, keyboard text bar and Text/Stickers
// panels. Internal so Set Background (text and stickers over a cut-out) reuses the same behavior.

private val DeleteHandleSize = 30.dp
private val ResizeHandleSize = 24.dp

/**
 * Chrome colors for the freestyle editor. Like the collage editor it follows the app's light/dark
 * [MaterialTheme] rather than the always-dark `EditorPalette`.
 */
@Immutable
internal data class FreestyleChrome(
    val isLight: Boolean,
    val surface: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
    val field: Color,
)

@Composable
internal fun freestyleChrome(): FreestyleChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return FreestyleChrome(
        isLight = isLight,
        surface = scheme.surface,
        content = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.55f),
        accent = scheme.primary,
        track = if (isLight) Color(0xFFD9D6E3) else Color(0xFF3A3A40),
        field = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
    )
}

/**
 * Calls [onEnd] each time the last pointer of a gesture lifts. It watches the final pass without
 * consuming anything, so it sees every gesture inside it while the children handle them as usual.
 */
@Composable
internal fun Modifier.onGestureEnd(onEnd: () -> Unit): Modifier {
    val currentOnEnd by rememberUpdatedState(onEnd)
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            do {
                val event = awaitPointerEvent(PointerEventPass.Final)
            } while (event.changes.any { it.pressed })
            currentOnEnd()
        }
    }
}

/** The text being typed in the keyboard-docked bar; [layerId] is the label being edited, or null for a new one. */
internal data class TextEntry(val layerId: Long?, val text: String)

/**
 * A text field docked right above the keyboard, over a scrim covering the rest of the screen.
 * It focuses itself on open so the keyboard comes up straight away; the keyboard's Done key, the
 * button, or a tap on the scrim commits.
 */
@Composable
internal fun TextEntryBar(chrome: FreestyleChrome, entry: TextEntry, onTextChange: (String) -> Unit, onCommit: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val commit = {
        focusManager.clearFocus()
        onCommit()
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) { detectTapGestures(onTap = { commit() }) },
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(chrome.surface, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                // Swallows taps on the bar itself so they don't reach the scrim.
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(chrome.field)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (entry.text.isEmpty()) {
                    Text(text = tr("Your Text"), color = chrome.muted, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = entry.text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = chrome.content),
                    cursorBrush = SolidColor(chrome.accent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            val bubble = rememberBubbleClick()
            Button(
                onClick = { bubble.tap(commit) },
                modifier = Modifier.bubbleClick(bubble, Brand),
                interactionSource = bubble.interactionSource,
                enabled = entry.layerId != null || entry.text.isNotBlank(),
                shape = RoundedCornerShape(50),
                // The brand violet, not the dark scheme's pastel primary, like every Done button.
                colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Color.White),
            ) {
                Text(if (entry.layerId == null) tr("Add") else tr("Done"), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * The canvas fills the whole area between the top bar and the panels, in two stacked parts: the
 * layers, clipped to the canvas exactly as the bake crops them, and above it an unclipped overlay
 * for the selected layer's outline and handles.
 */
@Composable
internal fun FreestyleCanvas(
    layers: List<FreestyleLayer>,
    chrome: FreestyleChrome,
    selectedLayerId: Long?,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onTextLayerDoubleTapped: (FreestyleLayer) -> Unit,
    onLayerSelected: (Long) -> Unit,
    onLayerTransformed: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onLayerScaleRotated: (id: Long, scale: Float, rotationDegrees: Float) -> Unit,
    onLayerDeleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
    stickerFlights: StickerFlights? = null,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Stickers and text size themselves, so the overlay reads their measured box from here.
    val layerSizes = remember { mutableStateMapOf<Long, IntSize>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .stickerFlightTarget(stickerFlights)
            .onSizeChanged {
                canvasSize = it
                onCanvasSizeChanged(it)
            },
    ) {
        if (canvasSize == IntSize.Zero) return@Box
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            for (layer in layers) {
                key(layer.id) {
                    FreestyleLayerView(
                        layer = layer,
                        canvasSize = canvasSize,
                        onMeasured = { layerSizes[layer.id] = it },
                        onSelect = { onLayerSelected(layer.id) },
                        onDoubleTap = if (layer.content is FreestyleContent.TextContent) {
                            { onTextLayerDoubleTapped(layer) }
                        } else {
                            null
                        },
                        onTransform = { pan, zoom, rotation -> onLayerTransformed(layer.id, pan, zoom, rotation) },
                    )
                }
            }
        }

        val selected = layers.firstOrNull { it.id == selectedLayerId }
        val selectedSize = selected?.let { layerSizes[it.id] }
        if (selected != null && selectedSize != null && selectedSize != IntSize.Zero) {
            SelectionOverlay(
                layer = selected,
                sizePx = selectedSize,
                canvasSize = canvasSize,
                chrome = chrome,
                onScaleRotate = { scale, rotation -> onLayerScaleRotated(selected.id, scale, rotation) },
                onDelete = { onLayerDeleted(selected.id) },
            )
        }
    }
}

/**
 * The on-screen box of [layer], centered on its offset; photos have a computed size, the rest wrap.
 *
 * It centers on the size measured in this same layout pass rather than one fed back through
 * `onSizeChanged`: a new sticker or label has no measured size on its first frame, so it would
 * draw off-center for a frame and then jump — visible as a jerk as a sticker's flight lands.
 */
@Composable
private fun layerBoxModifier(layer: FreestyleLayer, canvasSize: IntSize): Modifier {
    val density = LocalDensity.current
    val content = layer.content
    val centerPx = Offset(layer.offsetFraction.x * canvasSize.width, layer.offsetFraction.y * canvasSize.height)
    return Modifier
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(
                    (centerPx.x - placeable.width / 2f).roundToInt(),
                    (centerPx.y - placeable.height / 2f).roundToInt(),
                )
            }
        }
        .then(
            if (content is FreestyleContent.ImageContent) {
                val widthPx = canvasSize.width * FreestyleImageBaseWidthFraction * layer.scale
                val heightPx = widthPx * (content.image.height.toFloat() / content.image.width.toFloat())
                Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
            } else {
                Modifier
            },
        )
        .graphicsLayer { rotationZ = layer.rotationDegrees }
}

@Composable
private fun BoxScope.FreestyleLayerView(
    layer: FreestyleLayer,
    canvasSize: IntSize,
    onMeasured: (IntSize) -> Unit,
    onSelect: () -> Unit,
    onDoubleTap: (() -> Unit)?,
    onTransform: (panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
) {
    // The pointerInput blocks outlive recompositions, so read the latest values through state.
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnTransform by rememberUpdatedState(onTransform)
    val currentRotation by rememberUpdatedState(layer.rotationDegrees)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentCanvasSize by rememberUpdatedState(canvasSize)
    val content = layer.content
    val photoShape = RoundedCornerShape(layer.cornerRadius.dp)

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .then(layerBoxModifier(layer, canvasSize))
            .onSizeChanged(onMeasured)
            // Selects on touch-down rather than on tap: a tap only completes once the double-tap wait
            // runs out, which made selecting a text layer lag — and a second tap in that window read
            // as a double tap. Also consumes the tap so the canvas underneath doesn't deselect.
            .pointerInput(onDoubleTap != null) {
                detectTapGestures(
                    onPress = { currentOnSelect() },
                    onDoubleTap = if (onDoubleTap != null) ({ currentOnDoubleTap?.invoke() }) else null,
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    // The box is rotated, so its pan arrives in rotated coordinates; turn it back.
                    val canvasPan = rotateVector(pan, currentRotation)
                    currentOnTransform(
                        Offset(canvasPan.x / currentCanvasSize.width, canvasPan.y / currentCanvasSize.height),
                        zoom,
                        rotation,
                    )
                }
            }
            .then(
                if (content is FreestyleContent.ImageContent) {
                    // Stroking the clipped outline at double width leaves a frame of exactly
                    // borderWidth inside the photo's edge — the same geometry bakeFreestyle draws.
                    Modifier
                        .clip(photoShape)
                        .drawWithContent {
                            drawContent()
                            if (layer.borderWidth > 0f) {
                                drawOutline(
                                    outline = photoShape.createOutline(size, layoutDirection, this),
                                    color = FreestyleImageBorderColor,
                                    style = Stroke(width = layer.borderWidth.dp.toPx() * 2f),
                                )
                            }
                        }
                } else {
                    Modifier.padding(horizontal = textPlateGutter(layer), vertical = 12.dp)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (content) {
            is FreestyleContent.ImageContent -> Image(
                bitmap = content.image,
                contentDescription = tr("Layer photo"),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            is FreestyleContent.StickerContent -> Text(
                text = content.emoji,
                fontSize = (FreestyleStickerBaseSizeSp * layer.scale).sp,
                maxLines = 1,
                softWrap = false,
            )

            // BasicText, not Text: Material's Text resolves a colour of its own and merges it over
            // the style, which would flatten a gradient brush. It also keeps the theme's body line
            // height out of the style, so the box is the words' real height, as the bake measures
            // it, and the background plate sits around them the same way in both.
            is FreestyleContent.TextContent -> BasicText(
                text = content.text,
                style = TextStyle(
                    brush = content.fill.brush,
                    fontFamily = content.font.fontFamily,
                    fontWeight = content.font.fontWeight,
                    fontStyle = content.font.fontStyle,
                    fontSize = (FreestyleTextBaseSizeSp * layer.scale).sp,
                ),
                softWrap = false,
                modifier = Modifier.drawBehind {
                    content.background?.let { drawTextPlate(it.brush, Offset.Zero, size) }
                },
            )
        }
    }
}

/**
 * The side padding of a text or sticker layer's box. A label with a background plate needs enough
 * of it to hold the plate, which grows with the label ([TextBackgroundPadXFraction] of the text's
 * height, roughly 1.2 × its font size), so the selection outline stays outside the plate.
 */
@Composable
private fun textPlateGutter(layer: FreestyleLayer): Dp {
    val content = layer.content
    if (content !is FreestyleContent.TextContent || content.background == null) return 12.dp
    val plateReach = with(LocalDensity.current) {
        (FreestyleTextBaseSizeSp * layer.scale * 1.2f * TextBackgroundPadXFraction).sp.toDp()
    }
    return maxOf(12.dp, plateReach + 4.dp)
}

/**
 * Outline plus handles for the selected layer, laid over it with the same box and rotation: a
 * delete button on the bottom-left and a resize/rotate handle on the bottom-right. The box itself
 * has no pointer input, so drags anywhere else still fall through to the layer underneath.
 */
@Composable
private fun BoxScope.SelectionOverlay(
    layer: FreestyleLayer,
    sizePx: IntSize,
    canvasSize: IntSize,
    chrome: FreestyleChrome,
    onScaleRotate: (scale: Float, rotationDegrees: Float) -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    val currentScale by rememberUpdatedState(layer.scale)
    val currentRotation by rememberUpdatedState(layer.rotationDegrees)
    val currentSize by rememberUpdatedState(sizePx)
    val currentOnScaleRotate by rememberUpdatedState(onScaleRotate)
    val outlineShape = if (layer.content is FreestyleContent.ImageContent) {
        RoundedCornerShape(layer.cornerRadius.dp)
    } else {
        RoundedCornerShape(8.dp)
    }

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .then(layerBoxModifier(layer, canvasSize))
            .size(with(density) { sizePx.width.toDp() }, with(density) { sizePx.height.toDp() }),
    ) {
        // The outline is a child drawn first, not a `border` on this box: a border modifier paints
        // over its box's content, which put the line across the handles sitting on it.
        Box(modifier = Modifier.matchParentSize().border(1.5.dp, chrome.accent, outlineShape))

        val bounce = rememberSpringBounce()
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = -DeleteHandleSize / 3, y = DeleteHandleSize / 3)
                .springBounce(bounce)
                .size(DeleteHandleSize)
                .clip(CircleShape)
                .background(Color(0xFFEF3B4E))
                .clickable(
                    interactionSource = bounce.interactionSource,
                    indication = LocalIndication.current,
                    onClick = onDelete,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Delete, contentDescription = tr("Delete layer"), tint = Color.White, modifier = Modifier.size(16.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = ResizeHandleSize / 2, y = ResizeHandleSize / 2)
                .size(ResizeHandleSize)
                .clip(CircleShape)
                .background(lerp(Color.White, chrome.accent, 0.22f))
                .pointerInput(Unit) {
                    // Tracks the finger as a vector from the layer's center in canvas coordinates: it
                    // starts at the (rotated) corner and accumulates each drag delta rotated out of the
                    // box's frame. Its length relative to the start sets the scale and its angle the
                    // rotation, so the handle follows the finger like a real corner.
                    var startScale = 1f
                    var startRotation = 0f
                    var startVector = Offset.Zero
                    var fingerVector = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            startScale = currentScale
                            startRotation = currentRotation
                            startVector = rotateVector(
                                Offset(currentSize.width / 2f, currentSize.height / 2f),
                                currentRotation,
                            )
                            fingerVector = startVector
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        fingerVector += rotateVector(dragAmount, currentRotation)
                        val startDistance = startVector.getDistance()
                        if (startDistance > 0f) {
                            currentOnScaleRotate(
                                startScale * fingerVector.getDistance() / startDistance,
                                snapTextRotation(
                                    startRotation + vectorAngleDegrees(fingerVector) - vectorAngleDegrees(startVector),
                                ),
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.OpenInFull,
                contentDescription = tr("Drag to resize and rotate"),
                tint = chrome.accent,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
internal fun PanelHeader(chrome: FreestyleChrome, title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = chrome.content,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(text = trailing, fontSize = 12.sp, color = chrome.muted)
        }
    }
}

@Composable
internal fun StickersPanel(chrome: FreestyleChrome, onStickerTapped: (emoji: String, cellBoundsInRoot: Rect) -> Unit) {
    // null selects the Recent tab, matching EmojiCategoryTabs.
    var category by remember { mutableStateOf<EmojiCategory?>(EmojiCategory.Smileys) }
    var recent by remember { mutableStateOf(emptyList<String>()) }

    Column {
        PanelHeader(chrome = chrome, title = tr("Stickers"))
        EmojiCategoryTabs(selected = category, onSelected = { category = it })
        EmojiGrid(
            emojis = category?.let { EmojisByCategory[it].orEmpty() } ?: recent,
            onEmojiTapped = { emoji, cellBounds ->
                onStickerTapped(emoji, cellBounds)
                recent = (listOf(emoji) + (recent - emoji)).take(MaxRecentEmojis)
            },
            modifier = Modifier.fillMaxWidth().height(100.dp),
        )
    }
}

/**
 * A tap-to-type field (opening [TextEntryBar] above the keyboard) plus the Text tool's font and
 * color rows. With a text layer selected ([selectedText]) the field shows its text and the rows
 * restyle it; otherwise they set the style of the next label added. The colour row runs the custom
 * colour picker, the flat colours, then the [FreestyleTextGradients]; the switch at its left picks
 * whether it colours the words ([fill]) or the plate behind them ([background], where it also
 * leads with a "none" swatch).
 */
@Composable
internal fun TextPanel(
    chrome: FreestyleChrome,
    selectedText: FreestyleContent.TextContent?,
    font: TextFontStyleOption,
    fill: FreestyleFill,
    background: FreestyleFill?,
    onFieldClick: () -> Unit,
    onFontChange: (TextFontStyleOption) -> Unit,
    onFillChange: (FreestyleFill) -> Unit,
    onBackgroundChange: (FreestyleFill?) -> Unit,
) {
    var pickingColor by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf(TextColorTarget.Text) }
    // What the colour row shows and edits: the words' fill, or the plate behind them.
    val shown = if (target == TextColorTarget.Text) fill else background
    val onShownChange: (FreestyleFill) -> Unit = if (target == TextColorTarget.Text) onFillChange else onBackgroundChange
    // The hosts give this panel a fixed height, and a Column squeezes whatever doesn't fit out of
    // its last child: with looser spacing the colour row lost height, which cut the top and bottom
    // off the selected swatch's ring. The spacing below leaves the three rows room to spare.
    Column(modifier = Modifier.padding(top = 6.dp)) {
        val bounce = rememberSpringBounce()
        Box(
            modifier = Modifier
                .springBounce(bounce)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(chrome.field)
                .clickable(
                    interactionSource = bounce.interactionSource,
                    indication = LocalIndication.current,
                    onClick = onFieldClick,
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = selectedText?.text ?: tr("Tap to add text"),
                color = if (selectedText != null) chrome.content else chrome.muted,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        FontRow(selected = font, onSelected = onFontChange)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextColorTargetSwitch(
                selected = target,
                onSelected = { target = it },
                modifier = Modifier.padding(start = 16.dp),
            )
            ColorRow(
                selected = (shown as? FreestyleFill.Solid)?.color,
                onSelected = { onShownChange(FreestyleFill.Solid(it)) },
                onPickCustom = { pickingColor = true },
                gradients = FreestyleTextGradients,
                selectedGradient = (shown as? FreestyleFill.Gradient)?.colors,
                onGradientSelected = { onShownChange(FreestyleFill.Gradient(it)) },
                onClear = if (target == TextColorTarget.Background) ({ onBackgroundChange(null) }) else null,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (pickingColor) {
        ColorPickerDialog(
            // A gradient has no one colour to open on, so the picker starts from its first; with no
            // background yet it starts from white.
            initial = when (shown) {
                is FreestyleFill.Solid -> shown.color
                is FreestyleFill.Gradient -> shown.colors.first()
                null -> Color.White
            },
            onDismiss = { pickingColor = false },
            onPicked = {
                onShownChange(FreestyleFill.Solid(it))
                pickingColor = false
            },
        )
    }
}
