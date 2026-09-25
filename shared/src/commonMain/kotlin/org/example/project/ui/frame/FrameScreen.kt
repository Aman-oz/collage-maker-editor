package org.example.project.ui.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun FrameScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FrameViewModel = koinViewModel(),
) {
    FrameContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onApply = { color, width, cornerRadius, canvasWidthPx ->
            viewModel.sourceImage?.let { image ->
                viewModel.applyFrame(
                    bakeFrame(
                        source = image,
                        color = color,
                        width = width,
                        cornerRadius = cornerRadius,
                        previewCanvasWidthPx = canvasWidthPx,
                    ),
                )
            }
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun FrameContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onApply: (color: Color, width: Float, cornerRadius: Float, canvasWidthPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedColor by remember { mutableStateOf(FrameColors.first()) }
    var width by remember { mutableFloatStateOf(FrameWidthDefault) }
    var cornerRadius by remember { mutableFloatStateOf(FrameCornerRadiusDefault) }
    var displayedImageWidthPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorBackground)
            .safeDrawingPadding(),
    ) {
        FrameTopBar(
            onBack = onBack,
            onApply = { onApply(selectedColor.color, width, cornerRadius, displayedImageWidthPx) },
            applyEnabled = sourceImage != null,
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

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawFrame(
                        imageOffset = imageOffsetPx,
                        imageSize = Size(imageWidthPx, imageHeightPx),
                        color = selectedColor.color,
                        widthPx = width,
                        cornerRadiusPx = cornerRadius,
                    )
                }
            } else {
                Text(
                    text = "No image to frame",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        FrameValueRow(label = "Width", value = width)
        CenterFillSlider(
            value = width,
            onValueChange = { width = it },
            range = FrameWidthRange,
            referenceValue = FrameWidthRange.start,
        )

        FrameValueRow(label = "Corner Radius", value = cornerRadius)
        CenterFillSlider(
            value = cornerRadius,
            onValueChange = { cornerRadius = it },
            range = FrameCornerRadiusRange,
            referenceValue = FrameCornerRadiusRange.start,
        )

        FrameColorRow(selected = selectedColor, onSelected = { selectedColor = it })
    }
}

@Composable
private fun FrameTopBar(onBack: () -> Unit, onApply: () -> Unit, applyEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)

        Text(
            text = "Frame",
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

        AccentPillButton(text = "Apply", onClick = onApply, enabled = applyEnabled)
    }
}

@Composable
private fun FrameValueRow(label: String, value: Float) {
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

@Composable
private fun FrameColorRow(selected: FrameColorOption, onSelected: (FrameColorOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(FrameColors) { option ->
            FrameColorSwatch(option = option, selected = option == selected, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun FrameColorSwatch(option: FrameColorOption, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (selected) {
                    Modifier.border(width = 2.dp, color = EditorAccent, shape = RoundedCornerShape(16.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EditorControlBackground)
                .border(
                    width = 4.dp,
                    color = if (option.color == FrameColorNone) EditorLabelTint.copy(alpha = 0.4f) else option.color,
                    shape = RoundedCornerShape(12.dp),
                ),
        )
    }
}

@Preview
@Composable
private fun FrameScreenPreview() {
    ThemePreviews {
        FrameContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = { _, _, _, _ -> })
    }
}
