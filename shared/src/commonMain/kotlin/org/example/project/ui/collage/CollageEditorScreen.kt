package org.example.project.ui.collage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RoundedCorner
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.delay
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val SpaceRange = 0f..30f
private val CornerRange = 0f..60f

private val CollageBackgroundColors = listOf(
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
    val pickerState by viewModel.pickerState.collectAsStateWithLifecycle()
    var pendingSlotIndex by remember { mutableStateOf<Int?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { toastMessage = it } }

    val slotImagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        val slotIndex = pendingSlotIndex
        if (file != null && slotIndex != null) viewModel.setSlotImage(slotIndex, file.path)
        pendingSlotIndex = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        CollageEditorContent(
            uiState = uiState,
            pickerState = pickerState,
            onClose = onBack,
            onDone = { canvasWidthPx, spacePx, cornerPx ->
                viewModel.applyCollage(canvasWidthPx, spacePx, cornerPx)
                onBack()
            },
            onTemplateSelected = viewModel::applyTemplate,
            onSwapImages = viewModel::swapImages,
            onRequestSlotImage = { slotIndex ->
                pendingSlotIndex = slotIndex
                slotImagePicker.launch()
            },
            onSpaceChange = viewModel::updateSpace,
            onCornerChange = viewModel::updateCorner,
            onBackgroundColorChange = viewModel::updateBackgroundColor,
        )

        CollageToast(
            message = toastMessage,
            onDismissed = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun CollageEditorContent(
    uiState: CollageEditorUiState,
    pickerState: CollagePickerState,
    onClose: () -> Unit,
    onDone: (canvasWidthPx: Float, spacePx: Float, cornerPx: Float) -> Unit,
    onTemplateSelected: (TemplateItem) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onRequestSlotImage: (Int) -> Unit,
    onSpaceChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTool by remember { mutableStateOf(CollageTool.Frame) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeContentPadding(),
    ) {
        CollageTopBar(
            onClose = onClose,
            onDone = {
                val state = (uiState as? CollageEditorUiState.Ready)?.collage
                if (state != null) {
                    val spacePx = with(density) { state.space.dp.toPx() }
                    val cornerPx = with(density) { state.corner.dp.toPx() }
                    onDone(canvasWidthPx, spacePx, cornerPx)
                }
            },
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
                        val hasImage = uiState.collage.images.containsKey(index)
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
                    pickerState = pickerState,
                    onTemplateSelected = onTemplateSelected,
                )
                CollageTool.Border -> BorderTab(
                    state = uiState.collage,
                    onSpaceChange = onSpaceChange,
                    onColorChange = onBackgroundColorChange,
                )
                CollageTool.Corners -> CornersTab(state = uiState.collage, onChange = onCornerChange)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

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
            .aspectRatio(1f),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        LaunchedEffect(widthPx) { onCanvasWidthPxChanged(widthPx) }

        val spacePx = with(density) { state.space.dp.toPx() }
        val cornerPx = with(density) { state.corner.dp.toPx() }
        val geometries = remember(state.template, widthPx, heightPx, spacePx, cornerPx) {
            computeSlotGeometries(state.template, widthPx, heightPx, spacePx, cornerPx)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(geometries) {
                    detectTapGestures(
                        onTap = { pos -> hitTestSlot(geometries, pos)?.let { onSlotTap(it.item.index) } },
                        onLongPress = { pos -> hitTestSlot(geometries, pos)?.let { onSlotLongPress(it.item.index) } },
                    )
                },
        ) {
            drawCollage(
                geometries = geometries,
                images = state.images,
                background = state.backgroundColor,
                canvasW = widthPx,
                canvasH = heightPx,
                emptySlotColor = EditorCanvasBackground,
                selectedIndex = selectedSlot,
                selectionColor = EditorAccent,
            )
        }

        // "Add photo" affordance centered in each empty slot.
        for (g in geometries) {
            if (!state.images.containsKey(g.item.index) && g.touchPolygon.isNotEmpty()) {
                val cx = g.touchPolygon.sumOf { it.x.toDouble() }.toFloat() / g.touchPolygon.size
                val cy = g.touchPolygon.sumOf { it.y.toDouble() }.toFloat() / g.touchPolygon.size
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add photo",
                    tint = EditorLabelTint,
                    modifier = Modifier
                        .offset(
                            x = with(density) { cx.toDp() } - 14.dp,
                            y = with(density) { cy.toDp() } - 14.dp,
                        )
                        .size(28.dp),
                )
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
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        AccentPillButton(text = "Done", onClick = onDone, enabled = doneEnabled)
    }
}

@Composable
private fun CollageToolTabs(selected: CollageTool, onToolSelected: (CollageTool) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                .then(if (selected) Modifier.size(width = 28.dp, height = 2.dp) else Modifier.size(0.dp))
                .background(EditorAccent),
        )
    }
}

@Composable
private fun FrameTab(pickerState: CollagePickerState, onTemplateSelected: (TemplateItem) -> Unit) {
    when {
        pickerState.isLoading -> LoadingRow()
        pickerState.error != null && pickerState.templates.isEmpty() -> Box(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(pickerState.error, color = EditorLabelTint, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        else -> LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(pickerState.templates) { template ->
                LayoutThumbnail(
                    template = template,
                    selected = template.id == pickerState.selectedTemplateId,
                    onClick = { onTemplateSelected(template) },
                )
            }
        }
    }
}

@Composable
private fun LayoutThumbnail(template: TemplateItem, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (selected) Modifier.border(2.dp, EditorAccent, RoundedCornerShape(14.dp)) else Modifier)
            .background(EditorControlBackground)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = template.preview.orEmpty(),
            contentDescription = template.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
        )
        if (template.isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Premium", tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Box(modifier = Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EditorAccent, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun BorderTab(state: CollageState, onSpaceChange: (Float) -> Unit, onColorChange: (Color) -> Unit) {
    Column {
        ValueRow(label = "Border", value = state.space)
        CenterFillSlider(value = state.space, onValueChange = onSpaceChange, range = SpaceRange, referenceValue = SpaceRange.start)
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(CollageBackgroundColors) { color ->
                BackgroundColorSwatch(color = color, selected = color == state.backgroundColor, onClick = { onColorChange(color) })
            }
        }
    }
}

@Composable
private fun BackgroundColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (selected) Modifier.border(2.dp, EditorAccent, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 32.dp else 36.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, EditorLabelTint.copy(alpha = 0.3f), CircleShape),
        )
    }
}

@Composable
private fun CornersTab(state: CollageState, onChange: (Float) -> Unit) {
    Column {
        ValueRow(label = "Corners", value = state.corner)
        CenterFillSlider(value = state.corner, onValueChange = onChange, range = CornerRange, referenceValue = CornerRange.start)
    }
}

@Composable
private fun ValueRow(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
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

@Composable
private fun CollageToast(message: String?, onDismissed: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2200)
            onDismissed()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(message.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Preview
@Composable
private fun CollageEditorScreenPreview() {
    ThemePreviews {
        CollageEditorContent(
            uiState = CollageEditorUiState.Loading,
            pickerState = CollagePickerState(isLoading = true),
            onClose = {},
            onDone = { _, _, _ -> },
            onTemplateSelected = {},
            onSwapImages = { _, _ -> },
            onRequestSlotImage = {},
            onSpaceChange = {},
            onCornerChange = {},
            onBackgroundColorChange = {},
        )
    }
}
