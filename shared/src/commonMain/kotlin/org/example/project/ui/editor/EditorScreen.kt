package org.example.project.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.EditorIconTint
import org.example.project.ui.common.EditorLabelTint
import org.example.project.ui.common.EditorOnAccent
import org.example.project.ui.preview.ThemePreviews
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.core.parameter.parametersOf
import photocollagemaker.shared.generated.resources.Res
import photocollagemaker.shared.generated.resources.ic_adjust_editor
import photocollagemaker.shared.generated.resources.ic_blur_editor
import photocollagemaker.shared.generated.resources.ic_crop_editor
import photocollagemaker.shared.generated.resources.ic_draw_editor
import photocollagemaker.shared.generated.resources.ic_filter_editor
import photocollagemaker.shared.generated.resources.ic_frame_editor
import photocollagemaker.shared.generated.resources.ic_overlay_editor
import photocollagemaker.shared.generated.resources.ic_ratio_editor
import photocollagemaker.shared.generated.resources.ic_rotate_editor
import photocollagemaker.shared.generated.resources.ic_s_blur_editor
import photocollagemaker.shared.generated.resources.ic_s_splash_editor
import photocollagemaker.shared.generated.resources.ic_splash_editor
import photocollagemaker.shared.generated.resources.ic_stickers_editor
import photocollagemaker.shared.generated.resources.ic_text_editor

private enum class EditorTool(val label: String, val icon: DrawableResource) {
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

@Composable
fun EditorScreen(
    imagePath: String?,
    onBack: () -> Unit,
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

    EditorContent(
        uiState = uiState,
        onClose = onBack,
        onDone = onBack,
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
        modifier = modifier,
    )
}

@Composable
private fun EditorContent(
    uiState: EditorUiState,
    onClose: () -> Unit,
    onDone: () -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        EditorTopBar(onClose = onClose, onDone = onDone)

        EditorCanvas(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        )

        EditorToolbar(
            selectedTool = selectedTool,
            onToolSelected = { tool ->
                when (tool) {
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
private fun EditorTopBar(onClose: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.Filled.Close, contentDescription = "Close editor", onClick = onClose)

        Text(
            text = "Edit Photo",
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

        EditorCircleIconButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo",
            enabled = false,
            onClick = {},
        )
        Spacer(modifier = Modifier.width(16.dp))
        EditorCircleIconButton(
            icon = Icons.AutoMirrored.Filled.Redo,
            contentDescription = "Redo",
            enabled = false,
            onClick = {},
        )
        Box(modifier = Modifier.width(10.dp))
        AccentPillButton(text = "Done", onClick = onDone)
    }
}

@Composable
private fun EditorCanvas(uiState: EditorUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(EditorCanvasBackground),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            EditorUiState.Loading -> CircularProgressIndicator(color = EditorAccent)

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
    selectedTool: EditorTool?,
    onToolSelected: (EditorTool) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(EditorTool.entries) { tool ->
            EditorToolItem(
                tool = tool,
                selected = tool == selectedTool,
                onClick = { onToolSelected(tool) },
            )
        }
    }
}

@Composable
private fun EditorToolItem(tool: EditorTool, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) EditorAccent else EditorControlBackground),
            contentAlignment = Alignment.Center,
        ) {
            // The vectors are 46dp with the glyph inset to a 33dp area, so draw them larger than
            // the default 24dp icon size to keep the glyph at roughly Material icon scale.
            Icon(
                painter = painterResource(tool.icon),
                contentDescription = tool.label,
                modifier = Modifier.size(32.dp),
                tint = if (selected) EditorOnAccent else EditorIconTint,
            )
        }
        Box(modifier = Modifier.height(6.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            color = if (selected) EditorAccent else EditorLabelTint,
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
