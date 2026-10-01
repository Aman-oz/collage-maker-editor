package org.example.project.ui.editor

import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import org.example.project.ui.common.DiscardChangesPopup
import org.example.project.ui.common.rememberDiscardChangesState
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.common.GlassButtonStyle
import org.example.project.ui.common.GlassTopBarButton
import org.example.project.ui.common.UndoRedoButton
import org.example.project.ui.common.rememberSpringBounce
import org.example.project.ui.common.springBounce
import org.example.project.ui.common.topBar
import org.example.project.ui.preview.ThemePreviews
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_adjust_editor
import photocollagemaker.shared.generated.resources.ic_auto_editor
import photocollagemaker.shared.generated.resources.ic_blur_editor
import photocollagemaker.shared.generated.resources.ic_crop_editor
import photocollagemaker.shared.generated.resources.ic_draw_editor
import photocollagemaker.shared.generated.resources.ic_filter_editor
import photocollagemaker.shared.generated.resources.ic_frame_editor
import photocollagemaker.shared.generated.resources.ic_overlay_editor
import photocollagemaker.shared.generated.resources.ic_ratio_editor
import photocollagemaker.shared.generated.resources.ic_redo
import photocollagemaker.shared.generated.resources.ic_rotate_editor
import photocollagemaker.shared.generated.resources.ic_s_blur_editor
import photocollagemaker.shared.generated.resources.ic_s_splash_editor
import photocollagemaker.shared.generated.resources.ic_splash_editor
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor
import photocollagemaker.shared.generated.resources.ic_undo

private enum class EditorTool(val label: String, val icon: DrawableResource) {
    Auto("Auto", Res.drawable.ic_auto_editor),
    Crop("Crop", Res.drawable.ic_crop_editor),
    Filter("Filter", Res.drawable.ic_filter_editor),
    Adjust("Adjust", Res.drawable.ic_adjust_editor),
    Overlay("Overlay", Res.drawable.ic_overlay_editor),
    Ratio("Ratio", Res.drawable.ic_ratio_editor),
    Text("Text", Res.drawable.ic_text_editor),
    Sticker("Sticker", Res.drawable.ic_stickers_editor),
    Blur("Blur", Res.drawable.ic_blur_editor),
    SelectiveBlur("s-Blur", Res.drawable.ic_s_blur_editor),
    Rotate("Rotate", Res.drawable.ic_rotate_editor),
    Splash("Splash", Res.drawable.ic_splash_editor),
    SelectiveSplash("s-Splash", Res.drawable.ic_s_splash_editor),
    Draw("Draw", Res.drawable.ic_draw_editor),
    Frame("Frame", Res.drawable.ic_frame_editor),
}

/**
 * Chrome colors for the photo editor. It follows the app's light/dark [MaterialTheme] like the
 * collage and freestyle editors, instead of the always-dark `EditorPalette` the tool screens use.
 */
@Immutable
private data class EditorChrome(
    val background: Color,
    val canvas: Color,
    val control: Color,
    val icon: Color,
    val label: Color,
    val accent: Color,
    val onAccent: Color,
)

@Composable
private fun editorChrome(): EditorChrome {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return EditorChrome(
        background = scheme.surface,
        canvas = if (isLight) Color(0xFFE6E6EB) else Color(0xFF1C1C1E),
        control = if (isLight) Color(0xFFF1F1F4) else Color(0xFF2C2C2E),
        icon = scheme.onSurface,
        label = scheme.onSurface.copy(alpha = 0.7f),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
    )
}

@Composable
fun EditorScreen(
    imagePath: String?,
    openFilterOnLoad: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenAuto: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenRatio: () -> Unit,
    onOpenText: () -> Unit,
    onOpenEmoji: () -> Unit,
    onOpenBlur: () -> Unit,
    onOpenSplash: () -> Unit,
    onOpenSelectiveBlur: () -> Unit,
    onOpenSelectiveSplash: () -> Unit,
    onOpenFrame: () -> Unit,
    onOpenDraw: () -> Unit,
    onOpenRotate: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = koinViewModel { parametersOf(imagePath) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Saveable so returning from the filter (or a config change) doesn't open it a second time.
    var filterOpened by rememberSaveable { mutableStateOf(false) }
    val isReady = uiState is EditorUiState.Ready
    LaunchedEffect(isReady) {
        if (openFilterOnLoad && isReady && !filterOpened) {
            filterOpened = true
            onOpenFilter()
        }
    }

    val discard = rememberDiscardChangesState(
        hasChanges = (uiState as? EditorUiState.Ready)?.canUndo == true,
        onBack = onBack,
    )
    // The discard popup is Liquid Glass over the editor, so the editor is its liquefiable backdrop.
    val liquidState = rememberLiquidState()

    Box(modifier = modifier.fillMaxSize()) {
        EditorContent(
            uiState = uiState,
            onClose = discard::requestBack,
            onDone = onDone,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onOpenAuto = onOpenAuto,
            onOpenCrop = onOpenCrop,
            onOpenFilter = onOpenFilter,
            onOpenAdjust = onOpenAdjust,
            onOpenOverlay = onOpenOverlay,
            onOpenRatio = onOpenRatio,
            onOpenText = onOpenText,
            onOpenEmoji = onOpenEmoji,
            onOpenBlur = onOpenBlur,
            onOpenSplash = onOpenSplash,
            onOpenSelectiveBlur = onOpenSelectiveBlur,
            onOpenSelectiveSplash = onOpenSelectiveSplash,
            onOpenFrame = onOpenFrame,
            onOpenDraw = onOpenDraw,
            onOpenRotate = onOpenRotate,
            modifier = Modifier.liquefiable(liquidState),
        )
        DiscardChangesPopup(discard, liquidState)
    }
}

@Composable
private fun EditorContent(
    uiState: EditorUiState,
    onClose: () -> Unit,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenAuto: () -> Unit,
    onOpenCrop: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenAdjust: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenRatio: () -> Unit,
    onOpenText: () -> Unit,
    onOpenEmoji: () -> Unit,
    onOpenBlur: () -> Unit,
    onOpenSplash: () -> Unit,
    onOpenSelectiveBlur: () -> Unit,
    onOpenSelectiveSplash: () -> Unit,
    onOpenFrame: () -> Unit,
    onOpenDraw: () -> Unit,
    onOpenRotate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTool by rememberSaveable { mutableStateOf<EditorTool?>(null) }
    val chrome = editorChrome()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.background)
            .safeDrawingPadding(),
    ) {
        val ready = uiState as? EditorUiState.Ready
        EditorTopBar(
            chrome = chrome,
            canUndo = ready?.canUndo == true,
            canRedo = ready?.canRedo == true,
            onClose = onClose,
            onUndo = onUndo,
            onRedo = onRedo,
            onDone = onDone,
        )

        EditorCanvas(
            chrome = chrome,
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
        )

        EditorToolbar(
            chrome = chrome,
            selectedTool = selectedTool,
            onToolSelected = { tool ->
                when (tool) {
                    EditorTool.Auto -> onOpenAuto()
                    EditorTool.Crop -> onOpenCrop()
                    EditorTool.Filter -> onOpenFilter()
                    EditorTool.Adjust -> onOpenAdjust()
                    EditorTool.Overlay -> onOpenOverlay()
                    EditorTool.Ratio -> onOpenRatio()
                    EditorTool.Text -> onOpenText()
                    EditorTool.Sticker -> onOpenEmoji()
                    EditorTool.Blur -> onOpenBlur()
                    EditorTool.SelectiveBlur -> onOpenSelectiveBlur()
                    EditorTool.Splash -> onOpenSplash()
                    EditorTool.SelectiveSplash -> onOpenSelectiveSplash()
                    EditorTool.Frame -> onOpenFrame()
                    EditorTool.Draw -> onOpenDraw()
                    EditorTool.Rotate -> onOpenRotate()
                }
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    chrome: EditorChrome,
    canUndo: Boolean,
    canRedo: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDone: () -> Unit,
) {
    // Same layout as CollageTopBar: fixed-size circle buttons, and a title that takes the leftover
    // width and ellipsizes, so the bar never overflows on narrow screens or large font scales.
    Row(
        modifier = Modifier.topBar(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassTopBarButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Back",
            onClick = onClose,
            contentColor = chrome.icon,
        )
        Text(
            text = "Edit Photo",
            color = chrome.icon,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UndoRedoButton(
                icon = vectorResource(Res.drawable.ic_undo),
                contentDescription = "Undo",
                enabled = canUndo,
                onClick = onUndo,
                tint = chrome.icon,
            )
            UndoRedoButton(
                icon = vectorResource(Res.drawable.ic_redo),
                contentDescription = "Redo",
                enabled = canRedo,
                onClick = onRedo,
                tint = chrome.icon,
            )
            GlassTopBarButton(
                icon = Icons.Filled.Check,
                contentDescription = "Done",
                onClick = onDone,
                style = GlassButtonStyle.Primary,
            )
        }
    }
}

@Composable
private fun EditorCanvas(chrome: EditorChrome, uiState: EditorUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(chrome.canvas),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            EditorUiState.Loading -> CircularProgressIndicator(color = chrome.accent)

            is EditorUiState.Ready -> Image(
                bitmap = uiState.image,
                contentDescription = "Selected photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )

            is EditorUiState.Error -> Text(
                text = uiState.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    chrome: EditorChrome,
    selectedTool: EditorTool?,
    onToolSelected: (EditorTool) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(EditorTool.entries) { tool ->
            EditorToolItem(
                chrome = chrome,
                tool = tool,
                selected = tool == selectedTool,
                onClick = { onToolSelected(tool) },
            )
        }
    }
}

@Composable
private fun EditorToolItem(chrome: EditorChrome, tool: EditorTool, selected: Boolean, onClick: () -> Unit) {
    val bounce = rememberSpringBounce()
    Column(
        modifier = Modifier
            .springBounce(bounce)
            .width(60.dp)
            .clickable(
                interactionSource = bounce.interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
//                .clip(CircleShape)
//                .background(if (selected) chrome.accent else chrome.control)
            ,
            contentAlignment = Alignment.Center,
        ) {
            // The vectors are 46dp with the glyph inset to a 33dp area, so draw them larger than
            // the default 24dp icon size to keep the glyph at roughly Material icon scale.
            Icon(
                painter = painterResource(tool.icon),
                contentDescription = tool.label,
                modifier = Modifier.fillMaxSize(),
                tint = if (selected) chrome.onAccent else chrome.icon,
            )
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            color = if (selected) chrome.accent else chrome.label,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun EditorScreenPreview() {
    // Loading state avoids needing a real decoded image or Koin for the preview — the chrome
    // around the canvas (top bar, toolbar) is identical across states.
    ThemePreviews {
        EditorContent(
            uiState = EditorUiState.Loading,
            onClose = {},
            onDone = {},
            onUndo = {},
            onRedo = {},
            onOpenAuto = {},
            onOpenCrop = {},
            onOpenFilter = {},
            onOpenAdjust = {},
            onOpenOverlay = {},
            onOpenRatio = {},
            onOpenText = {},
            onOpenEmoji = {},
            onOpenBlur = {},
            onOpenSplash = {},
            onOpenSelectiveBlur = {},
            onOpenSelectiveSplash = {},
            onOpenFrame = {},
            onOpenDraw = {},
            onOpenRotate = {},
        )
    }
}
