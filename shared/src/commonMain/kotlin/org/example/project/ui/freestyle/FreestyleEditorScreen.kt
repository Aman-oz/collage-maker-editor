package org.example.project.ui.freestyle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.emoji.EmojiCategory
import org.example.project.ui.emoji.EmojisByCategory
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.text.TextColorOptions
import org.example.project.ui.text.TextFontStyleOption
import org.example.project.ui.text.TextFontStyles
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class FreestyleTool(val label: String, val icon: ImageVector) {
    Background("Background", Icons.Filled.Image),
    Sticker("Sticker", Icons.Filled.EmojiEmotions),
    Border("Border", Icons.Filled.CropSquare),
    Text("Text", Icons.Filled.TextFields),
}

private enum class BorderSubTool(val label: String, val icon: ImageVector) {
    Size("Size", Icons.Filled.OpenInFull),
    Round("Round", Icons.Filled.RoundedCorner),
    Color("Color", Icons.Filled.Palette),
}

@Composable
fun FreestyleEditorScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FreestyleEditorViewModel = koinViewModel { parametersOf(imagePaths) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()

    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        file?.let { viewModel.addImage(it.path) }
    }

    FreestyleEditorContent(
        uiState = uiState,
        onClose = onBack,
        onDone = { canvasWidthPx ->
            viewModel.applyFreestyle(textMeasurer, canvasWidthPx)
            onBack()
        },
        onTransformLayer = viewModel::transformLayer,
        onSelectLayer = viewModel::bringToFront,
        onDeleteLayer = viewModel::removeLayer,
        onAddImage = imagePicker::launch,
        onAddSticker = viewModel::addSticker,
        onAddText = viewModel::addText,
        onBackgroundColorChange = viewModel::updateBackgroundColor,
        onBorderWidthChange = viewModel::updateBorderWidth,
        onBorderColorChange = viewModel::updateBorderColor,
        onCornerRadiusChange = viewModel::updateCornerRadius,
        modifier = modifier,
    )
}

@Composable
private fun FreestyleEditorContent(
    uiState: FreestyleEditorUiState,
    onClose: () -> Unit,
    onDone: (canvasWidthPx: Float) -> Unit,
    onTransformLayer: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onSelectLayer: (id: Long) -> Unit,
    onDeleteLayer: (id: Long) -> Unit,
    onAddImage: () -> Unit,
    onAddSticker: (String) -> Unit,
    onAddText: (String, Color, TextFontStyleOption) -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Color) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTool by remember { mutableStateOf<FreestyleTool?>(null) }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        FreestyleTopBar(
            onClose = onClose,
            onDone = { onDone(canvasWidthPx) },
            doneEnabled = uiState is FreestyleEditorUiState.Ready,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                FreestyleEditorUiState.Loading -> CircularProgressIndicator(color = EditorAccent)

                is FreestyleEditorUiState.Ready -> FreestyleCanvas(
                    state = uiState.freestyle,
                    selectedLayerId = selectedLayerId,
                    onCanvasWidthPxChanged = { canvasWidthPx = it },
                    onCanvasTapped = { selectedLayerId = null },
                    onLayerSelected = { id -> selectedLayerId = id; onSelectLayer(id) },
                    onLayerTransformed = onTransformLayer,
                    onLayerDeleted = { id -> onDeleteLayer(id); selectedLayerId = null },
                )

                is FreestyleEditorUiState.Error -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        if (uiState is FreestyleEditorUiState.Ready) {
            FreestyleToolRow(
                selected = selectedTool,
                onToolSelected = { tool -> selectedTool = if (selectedTool == tool) null else tool },
                onAddImage = onAddImage,
            )

            when (selectedTool) {
                FreestyleTool.Background -> BackgroundToolPanel(
                    selected = uiState.freestyle.backgroundColor,
                    onSelected = onBackgroundColorChange,
                )
                FreestyleTool.Sticker -> StickerToolPanel(onStickerTapped = onAddSticker)
                FreestyleTool.Border -> BorderToolPanel(
                    state = uiState.freestyle,
                    onWidthChange = onBorderWidthChange,
                    onColorChange = onBorderColorChange,
                    onRadiusChange = onCornerRadiusChange,
                )
                FreestyleTool.Text -> TextToolPanel(onAdd = onAddText)
                null -> Unit
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FreestyleCanvas(
    state: FreestyleState,
    selectedLayerId: Long?,
    onCanvasWidthPxChanged: (Float) -> Unit,
    onCanvasTapped: () -> Unit,
    onLayerSelected: (Long) -> Unit,
    onLayerTransformed: (id: Long, panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onLayerDeleted: (Long) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(state.cornerRadius.dp))
            .background(state.backgroundColor)
            .border(width = state.borderWidth.dp, color = state.borderColor, shape = RoundedCornerShape(state.cornerRadius.dp)),
    ) {
        val density = LocalDensity.current
        val canvasPx = with(density) { maxWidth.toPx() }
        LaunchedEffect(canvasPx) { onCanvasWidthPxChanged(canvasPx) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onCanvasTapped() }) },
        )

        for (layer in state.layers) {
            FreestyleLayerView(
                layer = layer,
                isSelected = layer.id == selectedLayerId,
                canvasPx = canvasPx,
                onSelect = { onLayerSelected(layer.id) },
                onTransform = { pan, zoom, rotation -> onLayerTransformed(layer.id, pan, zoom, rotation) },
                onDelete = { onLayerDeleted(layer.id) },
            )
        }
    }
}

@Composable
private fun BoxScope.FreestyleLayerView(
    layer: FreestyleLayer,
    isSelected: Boolean,
    canvasPx: Float,
    onSelect: () -> Unit,
    onTransform: (panFraction: Offset, zoomDelta: Float, rotationDeltaDegrees: Float) -> Unit,
    onDelete: () -> Unit,
) {
    val density = LocalDensity.current
    var measuredSize by remember(layer.id) { mutableStateOf(IntSize.Zero) }

    val content = layer.content
    val isImage = content is FreestyleContent.ImageContent
    val (widthPx, heightPx) = if (content is FreestyleContent.ImageContent) {
        val w = canvasPx * FreestyleImageBaseWidthFraction * layer.scale
        val h = w * (content.image.height.toFloat() / content.image.width.toFloat())
        w to h
    } else {
        measuredSize.width.toFloat() to measuredSize.height.toFloat()
    }
    val centerPx = Offset(layer.offsetFraction.x * canvasPx, layer.offsetFraction.y * canvasPx)

    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .onSizeChanged { measuredSize = it }
            .offset {
                IntOffset(
                    (centerPx.x - widthPx / 2f).roundToInt(),
                    (centerPx.y - heightPx / 2f).roundToInt(),
                )
            }
            .then(
                if (isImage) {
                    Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
                } else {
                    Modifier
                },
            )
            .graphicsLayer { rotationZ = layer.rotationDegrees }
            .then(
                if (isSelected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(4.dp))
                } else {
                    Modifier
                },
            )
            .pointerInput(layer.id) { detectTapGestures(onTap = { onSelect() }) }
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    onSelect()
                    onTransform(Offset(pan.x / canvasPx, pan.y / canvasPx), zoom, rotation)
                }
            }
            .padding(if (isImage) 0.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (content) {
            is FreestyleContent.ImageContent -> Image(
                bitmap = content.image,
                contentDescription = "Layer photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            is FreestyleContent.StickerContent -> Text(
                text = content.emoji,
                fontSize = (FreestyleStickerBaseSizeSp * layer.scale).sp,
            )

            is FreestyleContent.TextContent -> Text(
                text = content.text,
                color = content.color,
                fontFamily = content.font.fontFamily,
                fontWeight = content.font.fontWeight,
                fontStyle = content.font.fontStyle,
                fontSize = (FreestyleTextBaseSizeSp * layer.scale).sp,
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Delete layer",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FreestyleTopBar(onClose: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onClose)

        Text(
            text = "Freestyle",
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
private fun FreestyleToolRow(selected: FreestyleTool?, onToolSelected: (FreestyleTool) -> Unit, onAddImage: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FreestyleTool.entries.forEach { tool ->
            ToolIconButton(icon = tool.icon, label = tool.label, selected = tool == selected, onClick = { onToolSelected(tool) })
        }
        ToolIconButton(icon = Icons.Filled.AddPhotoAlternate, label = "Add Image", selected = false, onClick = onAddImage)
    }
}

@Composable
private fun ToolIconButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) EditorAccent else EditorLabelTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
        )
    }
}

@Composable
private fun BackgroundToolPanel(selected: Color, onSelected: (Color) -> Unit) {
    LazyRowColorSwatches(colors = FreestyleBackgroundColors, selected = selected, onSelected = onSelected)
}

@Composable
private fun StickerToolPanel(onStickerTapped: (String) -> Unit) {
    var category by remember { mutableStateOf(EmojiCategory.Smileys) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            EmojiCategory.entries.forEach { entry ->
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (entry == category) FontWeight.Bold else FontWeight.Normal,
                    color = if (entry == category) EditorAccent else EditorLabelTint,
                    modifier = Modifier.clickable { category = entry },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().height(150.dp),
        ) {
            items(EmojisByCategory[category].orEmpty()) { emoji ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = { onStickerTapped(emoji) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun BorderToolPanel(
    state: FreestyleState,
    onWidthChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onRadiusChange: (Float) -> Unit,
) {
    var subTool by remember { mutableStateOf(BorderSubTool.Size) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BorderSubTool.entries.forEach { tool ->
                ToolIconButton(icon = tool.icon, label = tool.label, selected = tool == subTool, onClick = { subTool = tool })
            }
        }

        when (subTool) {
            BorderSubTool.Size -> {
                ValueRow(label = "Border", value = state.borderWidth)
                CenterFillSlider(
                    value = state.borderWidth,
                    onValueChange = onWidthChange,
                    range = FreestyleBorderWidthRange,
                    referenceValue = FreestyleBorderWidthRange.start,
                )
            }

            BorderSubTool.Round -> {
                ValueRow(label = "Corners", value = state.cornerRadius)
                CenterFillSlider(
                    value = state.cornerRadius,
                    onValueChange = onRadiusChange,
                    range = FreestyleCornerRadiusRange,
                    referenceValue = FreestyleCornerRadiusRange.start,
                )
            }

            BorderSubTool.Color -> LazyRowColorSwatches(colors = FreestyleBorderColors, selected = state.borderColor, onSelected = onColorChange)
        }
    }
}

@Composable
private fun TextToolPanel(onAdd: (String, Color, TextFontStyleOption) -> Unit) {
    var content by remember { mutableStateOf("") }
    var font by remember { mutableStateOf(TextFontStyles[1]) }
    var color by remember { mutableStateOf(TextColorOptions.first()) }
    val focusRequester = remember { FocusRequester() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(EditorControlBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                if (content.isEmpty()) {
                    Text(text = "Your Text", color = EditorLabelTint, style = MaterialTheme.typography.bodyLarge)
                }
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    cursorBrush = SolidColor(EditorAccent),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            AccentPillButton(
                text = "Add",
                enabled = content.isNotBlank(),
                onClick = {
                    onAdd(content, color, font)
                    content = ""
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextFontStyles.forEach { option ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (option == font) EditorAccent else EditorControlBackground)
                        .clickable { font = option }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (option == font) EditorOnAccent else EditorLabelTint,
                    )
                }
            }
        }

        LazyRowColorSwatches(colors = TextColorOptions, selected = color, onSelected = { color = it })
    }
}

@Composable
private fun LazyRowColorSwatches(colors: List<Color>, selected: Color, onSelected: (Color) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        colors.forEach { color -> ColorSwatch(color = color, selected = color == selected, onClick = { onSelected(color) }) }
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
                .size(if (selected) 32.dp else 36.dp)
                .clip(CircleShape)
                .background(color)
                .border(width = 1.dp, color = EditorLabelTint.copy(alpha = 0.3f), shape = CircleShape),
        )
    }
}

@Composable
private fun ValueRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EditorLabelTint,
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
private fun FreestyleEditorScreenPreview() {
    ThemePreviews {
        FreestyleEditorContent(
            uiState = FreestyleEditorUiState.Loading,
            onClose = {},
            onDone = {},
            onTransformLayer = { _, _, _, _ -> },
            onSelectLayer = {},
            onDeleteLayer = {},
            onAddImage = {},
            onAddSticker = {},
            onAddText = { _, _, _ -> },
            onBackgroundColorChange = {},
            onBorderWidthChange = {},
            onBorderColorChange = {},
            onCornerRadiusChange = {},
        )
    }
}
