package org.example.project.ui.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
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
        onDone = { content, font, color, sizeSp, canvasWidthPx, offsetFraction ->
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
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasAddedText by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }
    var selectedFont by remember { mutableStateOf(TextFontStyles[1]) }
    var selectedColor by remember { mutableStateOf(TextColorOptions.first()) }
    var sizeSp by remember { mutableFloatStateOf(TextSizeDefault) }
    var displayedImageWidthPx by remember { mutableFloatStateOf(0f) }
    var textOffsetFraction by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var textSizePx by remember { mutableStateOf(IntSize.Zero) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(hasAddedText) {
        if (hasAddedText) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        TextTopBar(
            onBack = onBack,
            onDone = {
                onDone(content, selectedFont, selectedColor, sizeSp, displayedImageWidthPx, textOffsetFraction)
            },
            doneEnabled = sourceImage != null,
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val bitmapWidth = sourceImage.width.toFloat()
                val bitmapHeight = sourceImage.height.toFloat()
                val fitScale = min(boxWidthPx / bitmapWidth, boxHeightPx / bitmapHeight)
                val imageWidthPx = bitmapWidth * fitScale
                val imageHeightPx = bitmapHeight * fitScale
                val imageOffsetPx = Offset((boxWidthPx - imageWidthPx) / 2f, (boxHeightPx - imageHeightPx) / 2f)
                LaunchedEffect(imageWidthPx) { displayedImageWidthPx = imageWidthPx }

                Image(
                    bitmap = sourceImage,
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )

                if (hasAddedText) {
                    val textCenterPx = Offset(
                        imageOffsetPx.x + textOffsetFraction.x * imageWidthPx,
                        imageOffsetPx.y + textOffsetFraction.y * imageHeightPx,
                    )
                    Text(
                        text = content.ifBlank { "Your Text" },
                        fontFamily = selectedFont.fontFamily,
                        fontWeight = selectedFont.fontWeight,
                        fontStyle = selectedFont.fontStyle,
                        fontSize = sizeSp.sp,
                        color = if (content.isBlank()) EditorLabelTint else selectedColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .onSizeChanged { textSizePx = it }
                            .offset {
                                IntOffset(
                                    (textCenterPx.x - textSizePx.width / 2f).roundToInt(),
                                    (textCenterPx.y - textSizePx.height / 2f).roundToInt(),
                                )
                            }
                            .pointerInput(imageWidthPx, imageHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    textOffsetFraction = Offset(
                                        (textOffsetFraction.x + dragAmount.x / imageWidthPx).coerceIn(0f, 1f),
                                        (textOffsetFraction.y + dragAmount.y / imageHeightPx).coerceIn(0f, 1f),
                                    )
                                }
                            }
                            .padding(12.dp),
                    )
                } else {
                    AddTextButton(onClick = { hasAddedText = true })
                }
            } else {
                Text(
                    text = "No image to add text to",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        TextInputField(
            value = content,
            onValueChange = { content = it; hasAddedText = true },
            focusRequester = focusRequester,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SectionLabel(text = "Font")
        FontRow(selected = selectedFont, onSelected = { selectedFont = it })

        SectionLabel(text = "Color")
        ColorRow(selected = selectedColor, onSelected = { selectedColor = it })

        TextSizeControlRow(value = sizeSp)
        CenterFillSlider(
            value = sizeSp,
            onValueChange = { sizeSp = it },
            range = TextSizeRange,
            referenceValue = TextSizeRange.start,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun TextTopBar(onBack: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Text",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )

        AccentPillButton(text = "Done", onClick = onDone, enabled = doneEnabled)
    }
}

@Composable
private fun AddTextButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(width = 1.5.dp, color = Color.White.copy(alpha = 0.6f), shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            text = "+ Add Text",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TextInputField(value: String, onValueChange: (String) -> Unit, focusRequester: FocusRequester) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(EditorControlBackground)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(text = "Your Text", color = EditorLabelTint, style = MaterialTheme.typography.bodyLarge)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            cursorBrush = SolidColor(EditorAccent),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = EditorLabelTint,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
    )
}

@Composable
private fun FontRow(selected: TextFontStyleOption, onSelected: (TextFontStyleOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(TextFontStyles) { option ->
            FontChip(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun FontChip(option: TextFontStyleOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EditorControlBackground)
                .then(
                    if (selected) {
                        Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aa",
                fontFamily = option.fontFamily,
                fontWeight = option.fontWeight,
                fontStyle = option.fontStyle,
                fontSize = 22.sp,
                color = if (selected) EditorAccent else Color.White,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ColorRow(selected: Color, onSelected: (Color) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(TextColorOptions) { color ->
            ColorSwatch(color = color, selected = color == selected, onClick = { onSelected(color) })
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun TextSizeControlRow(value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Text Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${value.roundToInt()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorAccent,
        )
    }
}

@Preview
@Composable
private fun TextScreenPreview() {
    ThemePreviews {
        TextContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _, _, _, _, _ -> })
    }
}
