package org.example.project.ui.collage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlin.math.roundToInt
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val BorderWidthRange = 0f..30f
private val CornerRadiusRange = 0f..48f

private val CollageBorderColors = listOf(
    Color.White,
    Color.Black,
    Color(0xFFC0C0C8),
    Color(0xFF8E8E93),
    Color(0xFFEF4444),
    Color(0xFFFF8C42),
    Color(0xFFFFB300),
    Color(0xFF4CAF50),
    Color(0xFF14B8A6),
    Color(0xFF3B82F6),
    Color(0xFF9333EA),
    Color(0xFFEC4899),
)

private val TemplateThumbnailPalette = listOf(
    EditorAccent.copy(alpha = 0.55f),
    Color(0xFF6366F1).copy(alpha = 0.55f),
    Color(0xFFEC4899).copy(alpha = 0.55f),
    Color(0xFF22D3EE).copy(alpha = 0.55f),
    Color(0xFF4CAF50).copy(alpha = 0.55f),
    Color(0xFFFF8C42).copy(alpha = 0.55f),
)

private enum class CollageTool(val label: String, val icon: ImageVector) {
    Frame("Frame", Icons.Filled.GridView),
    Border("Border", Icons.Filled.ViewColumn),
    Corners("Corners", Icons.Filled.RoundedCorner),
}

@Composable
fun CollageEditorScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollageEditorViewModel = koinViewModel { parametersOf(imagePaths) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }

    val slotImagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slotIndex = pendingSlotIndex
        if (file != null && slotIndex != null) {
            viewModel.setSlotImage(slotIndex, file.path)
        }
        pendingSlotIndex = null
    }

    CollageEditorContent(
        uiState = uiState,
        onClose = onBack,
        onDone = { canvasWidthPx ->
            viewModel.applyCollage(canvasWidthPx)
            onBack()
        },
        onTemplateSelected = viewModel::changeTemplate,
        onSwapImages = viewModel::swapImages,
        onRequestSlotImage = { slotIndex ->
            pendingSlotIndex = slotIndex
            slotImagePicker.launch()
        },
        onBorderWidthChange = viewModel::updateBorderWidth,
        onBorderColorChange = viewModel::updateBorderColor,
        onCornerRadiusChange = viewModel::updateCornerRadius,
        modifier = modifier,
    )
}

@Composable
private fun CollageEditorContent(
    uiState: CollageEditorUiState,
    onClose: () -> Unit,
    onDone: (canvasWidthPx: Float) -> Unit,
    onTemplateSelected: (CollageTemplate) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onRequestSlotImage: (Int) -> Unit,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Color) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTool by remember { mutableStateOf(CollageTool.Frame) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        CollageTopBar(
            onClose = onClose,
            onDone = { onDone(canvasWidthPx) },
            doneEnabled = uiState is CollageEditorUiState.Ready,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                CollageEditorUiState.Loading -> CircularProgressIndicator(color = EditorAccent)

                is CollageEditorUiState.Ready -> CollagePreview(
                    state = uiState.collage,
                    selectedSlot = selectedSlot,
                    onCanvasWidthPxChanged = { canvasWidthPx = it },
                    onSlotTap = { index ->
                        val hasImage = uiState.collage.images.any { it.slotIndex == index }
                        selectedSlot = when {
                            !hasImage -> {
                                onRequestSlotImage(index)
                                null
                            }
                            selectedSlot == null -> index
                            selectedSlot == index -> null
                            else -> {
                                onSwapImages(selectedSlot!!, index)
                                null
                            }
                        }
                    },
                    onSlotLongPress = onRequestSlotImage,
                )

                is CollageEditorUiState.Error -> Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        CollageToolTabs(selected = selectedTool, onToolSelected = { selectedTool = it; selectedSlot = null })

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState is CollageEditorUiState.Ready) {
            when (selectedTool) {
                CollageTool.Frame -> FrameTab(
                    currentTemplate = uiState.collage.template,
                    onTemplateSelected = onTemplateSelected,
                )
                CollageTool.Border -> BorderTab(
                    state = uiState.collage,
                    onWidthChange = onBorderWidthChange,
                    onColorChange = onBorderColorChange,
                )
                CollageTool.Corners -> CornersTab(state = uiState.collage, onChange = onCornerRadiusChange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollagePreview(
    state: CollageState,
    selectedSlot: Int?,
    onCanvasWidthPxChanged: (Float) -> Unit,
    onSlotTap: (Int) -> Unit,
    onSlotLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(state.cornerRadius.dp))
            .background(state.borderColor),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        LaunchedEffect(widthPx) { onCanvasWidthPxChanged(widthPx) }

        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val borderDp = state.borderWidth.dp
        val halfBorder = borderDp / 2
        val slotCornerShape = RoundedCornerShape((state.cornerRadius / 2).dp)

        for (slot in state.template.slots) {
            val slotImage = state.images.find { it.slotIndex == slot.index }
            val isSelected = selectedSlot == slot.index
            val left = totalWidth * slot.left + halfBorder
            val top = totalHeight * slot.top + halfBorder
            val width = (totalWidth * slot.width - borderDp).coerceAtLeast(0.dp)
            val height = (totalHeight * slot.height - borderDp).coerceAtLeast(0.dp)

            Box(
                modifier = Modifier
                    .offset(x = left, y = top)
                    .size(width = width, height = height)
                    .clip(slotCornerShape)
                    .background(EditorCanvasBackground)
                    .then(
                        if (isSelected) {
                            Modifier.border(width = 3.dp, color = EditorAccent, shape = slotCornerShape)
                        } else {
                            Modifier
                        },
                    )
                    .combinedClickable(
                        onClick = { onSlotTap(slot.index) },
                        onLongClick = { onSlotLongPress(slot.index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (slotImage != null) {
                    Image(
                        bitmap = slotImage.image,
                        contentDescription = "Collage photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(EditorAccent.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "Swap",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add photo",
                        tint = EditorLabelTint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollageTopBar(onClose: () -> Unit, onDone: () -> Unit, doneEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onClose)

        Text(
            text = "Collage",
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
private fun CollageToolTabs(selected: CollageTool, onToolSelected: (CollageTool) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CollageTool.entries.forEach { tool ->
            CollageToolTab(tool = tool, selected = tool == selected, onClick = { onToolSelected(tool) })
        }
    }
}

@Composable
private fun CollageToolTab(tool: CollageTool, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.label,
            tint = if (selected) EditorAccent else EditorLabelTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) EditorAccent else EditorLabelTint,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(2.dp)
                .width(if (selected) 28.dp else 0.dp)
                .background(EditorAccent),
        )
    }
}

@Composable
private fun FrameTab(currentTemplate: CollageTemplate, onTemplateSelected: (CollageTemplate) -> Unit) {
    var selectedCount by remember(currentTemplate.imageCount) { mutableStateOf(currentTemplate.imageCount) }

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(CollageImageCounts) { count ->
                PhotoCountPill(count = count, selected = count == selectedCount, onClick = { selectedCount = count })
            }
        }

        val templates = remember(selectedCount) { CollageTemplates.getTemplatesByImageCount(selectedCount) }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(templates) { template ->
                TemplateThumbnail(
                    template = template,
                    selected = template.id == currentTemplate.id,
                    onClick = { onTemplateSelected(template) },
                )
            }
        }
    }
}

@Composable
private fun PhotoCountPill(count: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EditorAccent else EditorControlBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "$count Photos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) EditorOnAccent else EditorLabelTint,
        )
    }
}

@Composable
private fun TemplateThumbnail(template: CollageTemplate, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(EditorControlBackground),
        ) {
            val width = maxWidth
            val height = maxHeight
            template.slots.forEachIndexed { index, slot ->
                Box(
                    modifier = Modifier
                        .offset(x = width * slot.left, y = height * slot.top)
                        .size(
                            width = (width * slot.width - 2.dp).coerceAtLeast(0.dp),
                            height = (height * slot.height - 2.dp).coerceAtLeast(0.dp),
                        )
                        .clip(RoundedCornerShape(3.dp))
                        .background(TemplateThumbnailPalette[index % TemplateThumbnailPalette.size]),
                )
            }
        }
    }
}

@Composable
private fun BorderTab(state: CollageState, onWidthChange: (Float) -> Unit, onColorChange: (Color) -> Unit) {
    Column {
        ValueRow(label = "Border", value = state.borderWidth)
        CenterFillSlider(
            value = state.borderWidth,
            onValueChange = onWidthChange,
            range = BorderWidthRange,
            referenceValue = BorderWidthRange.start,
        )

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(CollageBorderColors) { color ->
                BorderColorSwatch(color = color, selected = color == state.borderColor, onClick = { onColorChange(color) })
            }
        }
    }
}

@Composable
private fun BorderColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
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
private fun CornersTab(state: CollageState, onChange: (Float) -> Unit) {
    Column {
        ValueRow(label = "Corners", value = state.cornerRadius)
        CenterFillSlider(
            value = state.cornerRadius,
            onValueChange = onChange,
            range = CornerRadiusRange,
            referenceValue = CornerRadiusRange.start,
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
private fun CollageEditorScreenPreview() {
    ThemePreviews {
        CollageEditorContent(
            uiState = CollageEditorUiState.Loading,
            onClose = {},
            onDone = {},
            onTemplateSelected = {},
            onSwapImages = { _, _ -> },
            onRequestSlotImage = {},
            onBorderWidthChange = {},
            onBorderColorChange = {},
            onCornerRadiusChange = {},
        )
    }
}
