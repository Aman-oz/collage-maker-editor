package org.example.project.ui.collage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import org.example.project.ui.collage.geom.TemplateItem
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.NetworkImage
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_premium_icon

private val SpaceRange = 0f..30f
private val CornerRange = 0f..60f

/** Fixed height of the tool area, so switching tabs never resizes the canvas above it. */
private val ToolPanelHeight = 132.dp

/**
 * Background swatches. The Background tab is a two-row horizontal grid that fills column by
 * column, so the list is ordered as (top, bottom) pairs.
 */
private val CollageBackgroundColors = listOf(
    Color.White, Color(0xFFE5E5EA),
    Color(0xFF1F2933), Color.Black,
    Color(0xFF1A8FD1), Color(0xFF136C94),
    Color(0xFF14204F), Color(0xFF050B4A),
    Color(0xFFF25565), Color(0xFFCB1F2E),
    Color(0xFF9747F5), Color(0xFFA35CF0),
    Color(0xFFF6AE45), Color(0xFFC3BB91),
    Color(0xFF4CAF50), Color(0xFF14B8A6),
    Color(0xFFEC4899), Color(0xFFFFB300),
)

private enum class CollageTool(val label: String) {
    Layouts("Layouts"),
    Border("Border"),
    Background("Background"),
    Ratio("Ratio"),
}

/**
 * Chrome colors for the collage editor. Unlike the other tools it follows the app's light/dark
 * [MaterialTheme] (like Home) rather than the always-dark `EditorPalette`, with a grey stage so the
 * collage's own (often white) background stays visible.
 */
@Immutable
private data class CollageChrome(
    val isLight: Boolean,
    val surface: Color,
    val stage: Color,
    val emptySlot: Color,
    val buttonBackground: Color,
    val content: Color,
    val muted: Color,
    val accent: Color,
    val track: Color,
)

@Composable
private fun collageChrome(): CollageChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return CollageChrome(
        isLight = isLight,
        surface = scheme.surface,
        stage = if (isLight) Color(0xFFE6E6EB) else Color(0xFF0E0E12),
        emptySlot = if (isLight) Color(0xFFF2F2F5) else Color(0xFF2C2C2E),
        buttonBackground = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
        content = scheme.onSurface,
        muted = scheme.onSurface.copy(alpha = 0.55f),
        accent = scheme.primary,
        track = if (isLight) Color(0xFFD9D6E3) else Color(0xFF3A3A40),
    )
}

@Composable
fun CollageEditorScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    onPremium: () -> Unit = {},
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
            onPremium = onPremium,
            onDone = { canvasWidthPx, spacePx, cornerPx ->
                viewModel.applyCollage(canvasWidthPx, spacePx, cornerPx)
                onOpenEditor()
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
            onRatioChange = viewModel::updateRatio,
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
    onPremium: () -> Unit,
    onDone: (canvasWidthPx: Float, spacePx: Float, cornerPx: Float) -> Unit,
    onTemplateSelected: (TemplateItem) -> Unit,
    onSwapImages: (Int, Int) -> Unit,
    onRequestSlotImage: (Int) -> Unit,
    onSpaceChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    onRatioChange: (CollageRatio) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chrome = collageChrome()
    var selectedTool by remember { mutableStateOf(CollageTool.Layouts) }
    var selectedSlot by remember { mutableStateOf<Int?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.surface)
            .safeDrawingPadding(),
    ) {
        CollageTopBar(
            chrome = chrome,
            onClose = onClose,
            onPremium = onPremium,
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
                .background(chrome.stage),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                CollageEditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

                is CollageEditorUiState.Ready -> CollagePreview(
                    state = uiState.collage,
                    chrome = chrome,
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

        CollageToolTabs(
            chrome = chrome,
            selected = selectedTool,
            onToolSelected = { selectedTool = it; selectedSlot = null },
        )

        Box(modifier = Modifier.fillMaxWidth().height(ToolPanelHeight)) {
            if (uiState is CollageEditorUiState.Ready) {
                val collage = uiState.collage
                when (selectedTool) {
                    CollageTool.Layouts -> LayoutsTab(
                        chrome = chrome,
                        pickerState = pickerState,
                        onTemplateSelected = onTemplateSelected,
                    )
                    CollageTool.Border -> BorderTab(
                        chrome = chrome,
                        state = collage,
                        onSpaceChange = onSpaceChange,
                        onCornerChange = onCornerChange,
                    )
                    CollageTool.Background -> BackgroundTab(
                        chrome = chrome,
                        selected = collage.backgroundColor,
                        onColorChange = onBackgroundColorChange,
                    )
                    CollageTool.Ratio -> RatioTab(
                        chrome = chrome,
                        selected = collage.ratio,
                        onRatioChange = onRatioChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollagePreview(
    state: CollageState,
    chrome: CollageChrome,
    selectedSlot: Int?,
    onCanvasWidthPxChanged: (Float) -> Unit,
    onSlotTap: (Int) -> Unit,
    onSlotLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val (widthPx, heightPx) = fitAspect(
            maxW = with(density) { maxWidth.toPx() },
            maxH = with(density) { maxHeight.toPx() },
            aspect = state.ratio.aspect,
        )
        LaunchedEffect(widthPx) { onCanvasWidthPxChanged(widthPx) }

        val spacePx = with(density) { state.space.dp.toPx() }
        val cornerPx = with(density) { state.corner.dp.toPx() }
        val geometries = remember(state.template, widthPx, heightPx, spacePx, cornerPx) {
            computeSlotGeometries(state.template, widthPx, heightPx, spacePx, cornerPx)
        }

        Box(modifier = Modifier.size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })) {
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
                    emptySlotColor = chrome.emptySlot,
                    selectedIndex = selectedSlot,
                    selectionColor = chrome.accent,
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
                        tint = chrome.muted,
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
}

@Composable
private fun CollageTopBar(
    chrome: CollageChrome,
    onClose: () -> Unit,
    onPremium: () -> Unit,
    onDone: () -> Unit,
    doneEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarCircleButton(
            chrome = chrome,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            onClick = onClose,
        )
        Text(
            text = "Collages",
            color = chrome.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        IconButton(onClick = onPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = "Premium",
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        TopBarCircleButton(
            chrome = chrome,
            icon = Icons.Filled.Check,
            contentDescription = "Done",
            onClick = onDone,
            enabled = doneEnabled,
        )
    }
}

@Composable
private fun TopBarCircleButton(
    chrome: CollageChrome,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(chrome.buttonBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) chrome.content else chrome.muted,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CollageToolTabs(chrome: CollageChrome, selected: CollageTool, onToolSelected: (CollageTool) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CollageTool.entries.forEach { tool ->
            CollageToolTab(chrome = chrome, tool = tool, selected = tool == selected, onClick = { onToolSelected(tool) })
        }
    }
}

@Composable
private fun CollageToolTab(chrome: CollageChrome, tool: CollageTool, selected: Boolean, onClick: () -> Unit) {
    // IntrinsicSize.Max lets the underline match the label's width.
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = tool.label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) chrome.content else chrome.muted,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) chrome.accent else Color.Transparent),
        )
    }
}

@Composable
private fun LayoutsTab(chrome: CollageChrome, pickerState: CollagePickerState, onTemplateSelected: (TemplateItem) -> Unit) {
    when {
        pickerState.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = chrome.accent, modifier = Modifier.size(28.dp))
        }
        pickerState.error != null && pickerState.templates.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(pickerState.error, color = chrome.muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        else -> LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(pickerState.templates) { template ->
                LayoutThumbnail(
                    chrome = chrome,
                    template = template,
                    selected = template.id == pickerState.selectedTemplateId,
                    onClick = { onTemplateSelected(template) },
                )
            }
        }
    }
}

@Composable
private fun LayoutThumbnail(chrome: CollageChrome, template: TemplateItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .border(1.5.dp, if (selected) chrome.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        NetworkImage(
            url = (if (chrome.isLight) template.preview else template.previewDark).orEmpty(),
            contentDescription = template.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (template.isPremium) {
            Image(
                painter = painterResource(Res.drawable.ic_premium_icon),
                contentDescription = "Premium",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(14.dp),
            )
        }
    }
}

@Composable
private fun BorderTab(
    chrome: CollageChrome,
    state: CollageState,
    onSpaceChange: (Float) -> Unit,
    onCornerChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BorderSliderRow(
            chrome = chrome,
            icon = Icons.Outlined.SpaceDashboard,
            contentDescription = "Border width",
            value = state.space,
            range = SpaceRange,
            onValueChange = onSpaceChange,
        )
        BorderSliderRow(
            chrome = chrome,
            icon = Icons.Outlined.RoundedCorner,
            contentDescription = "Corner radius",
            value = state.corner,
            range = CornerRange,
            onValueChange = onCornerChange,
        )
    }
}

@Composable
private fun BorderSliderRow(
    chrome: CollageChrome,
    icon: ImageVector,
    contentDescription: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = chrome.content, modifier = Modifier.size(22.dp))
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            referenceValue = range.start,
            trackColor = chrome.track,
            fillColor = chrome.accent,
            thumbColor = chrome.accent,
            thumbWidth = 26.dp,
            thumbHeight = 14.dp,
            horizontalPadding = 24.dp,
            glassThumb = true,
            glassTint = if (chrome.isLight) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BackgroundTab(chrome: CollageChrome, selected: Color, onColorChange: (Color) -> Unit) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(CollageBackgroundColors) { color ->
            BackgroundColorSwatch(chrome = chrome, color = color, selected = color == selected, onClick = { onColorChange(color) })
        }
    }
}

@Composable
private fun BackgroundColorSwatch(chrome: CollageChrome, color: Color, selected: Boolean, onClick: () -> Unit) {
    val outer = RoundedCornerShape(12.dp)
    val inner = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(outer)
            .border(2.dp, if (selected) chrome.accent else Color.Transparent, outer)
            .clickable(onClick = onClick)
            .padding(4.dp)
            .clip(inner)
            .background(color)
            .border(1.dp, chrome.content.copy(alpha = 0.08f), inner),
    )
}

@Composable
private fun RatioTab(chrome: CollageChrome, selected: CollageRatio, onRatioChange: (CollageRatio) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(CollageRatio.entries) { ratio ->
            RatioOption(chrome = chrome, ratio = ratio, selected = ratio == selected, onClick = { onRatioChange(ratio) })
        }
    }
}

@Composable
private fun RatioOption(chrome: CollageChrome, ratio: CollageRatio, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) chrome.accent else chrome.content
    // Every option's shape is fitted into the same box so a tall and a wide ratio read at a glance.
    val (w, h) = fitAspect(48f, 48f, ratio.aspect)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(w.dp, h.dp)
                    .border(2.dp, tint, RoundedCornerShape(8.dp)),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = ratio.label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
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
            onPremium = {},
            onDone = { _, _, _ -> },
            onTemplateSelected = {},
            onSwapImages = { _, _ -> },
            onRequestSlotImage = {},
            onSpaceChange = {},
            onCornerChange = {},
            onBackgroundColorChange = {},
            onRatioChange = {},
        )
    }
}
