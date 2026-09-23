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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.koin.core.parameter.parametersOf

private enum class EditorTool(val label: String, val icon: ImageVector) {
    Crop("Crop", Icons.Filled.Crop),
    Filter("Filter", Icons.Filled.FilterAlt),
    Adjust("Adjust", Icons.Filled.Tune),
    Overlay("Overlay", Icons.Filled.Layers),
    Ratio("Ratio", Icons.Filled.AspectRatio),
    Text("Text", Icons.Filled.Title),
    Sticker("Sticker", Icons.Filled.EmojiEmotions),
    Blur("Blur", Icons.Filled.BlurOn),
    SelectiveBlur("s-Blur", Icons.Filled.BlurCircular),
    Rotate("Rotate", Icons.AutoMirrored.Filled.RotateRight),
    Splash("Splash", Icons.Filled.ColorLens),
    SelectiveSplash("s-Splash", Icons.Filled.Colorize),
    Draw("Draw", Icons.Filled.Draw),
    Frame("Frame", Icons.Filled.FilterFrames),
}

@Composable
fun EditorScreen(
    imagePath: String,
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
            .safeContentPadding(),
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
            .clip(RoundedCornerShape(24.dp))
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
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
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
