package org.example.project.ui.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.example.project.ui.common.CenterFillSlider
import org.example.project.ui.common.SelectableSwatch
import org.example.project.ui.common.SwatchInnerCorner
import org.example.project.ui.common.ToolTopBar
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
    val scheme = MaterialTheme.colorScheme
    var selectedColor by remember { mutableStateOf(FrameColors.first()) }
    var width by remember { mutableFloatStateOf(FrameWidthDefault) }
    var cornerRadius by remember { mutableFloatStateOf(FrameCornerRadiusDefault) }
    var displayedImageWidthPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .safeDrawingPadding(),
    ) {
        ToolTopBar(
            title = "Frame",
            onClose = onBack,
            onDone = { onApply(selectedColor.color, width, cornerRadius, displayedImageWidthPx) },
            doneEnabled = sourceImage != null,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(scheme.onSurface.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null) {
                // aspectRatio sizes the box to the photo itself, so the frame canvas's bounds are
                // the image bounds (drawn at Offset.Zero) and the rounded clip follows its edges.
                Box(
                    modifier = Modifier
                        .aspectRatio(sourceImage.width.toFloat() / sourceImage.height)
                        .clip(RoundedCornerShape(16.dp))
                        .onSizeChanged { displayedImageWidthPx = it.width.toFloat() },
                ) {
                    Image(
                        bitmap = sourceImage,
                        contentDescription = "Photo preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawFrame(
                            imageOffset = Offset.Zero,
                            imageSize = size,
                            color = selectedColor.color,
                            widthPx = width,
                            cornerRadiusPx = cornerRadius,
                        )
                    }
                }
            } else {
                Text(
                    text = "No image to frame",
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        FrameSlider(
            label = "Width",
            value = width,
            onValueChange = { width = it },
            range = FrameWidthRange,
            modifier = Modifier.padding(top = 12.dp),
        )
        FrameSlider(
            label = "Corner Radius",
            value = cornerRadius,
            onValueChange = { cornerRadius = it },
            range = FrameCornerRadiusRange,
        )

        FrameColorRow(selected = selectedColor, onSelected = { selectedColor = it })
    }
}

/** A "label ··· value" row over an accent slider, filling from the range's start. */
@Composable
private fun FrameSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${value.roundToInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary,
            )
        }
        CenterFillSlider(
            value = value,
            onValueChange = onValueChange,
            range = range,
            referenceValue = range.start,
            trackColor = scheme.onSurface.copy(alpha = 0.12f),
            fillColor = scheme.primary,
            thumbColor = scheme.primary,
            thumbWidth = 32.dp,
            thumbHeight = 18.dp,
            horizontalPadding = 12.dp,
            glassThumb = true,
            glassTint = if (scheme.surface.luminance() > 0.5f) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
        )
    }
}

@Composable
private fun FrameColorRow(selected: FrameColorOption, onSelected: (FrameColorOption) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(FrameColors) { option ->
            SelectableSwatch(selected = option == selected, size = 52.dp, onClick = { onSelected(option) }) {
                FrameColorSwatch(option)
            }
        }
    }
}

/**
 * A miniature framed photo: a soft neutral gradient "photo" inside a border of the option's
 * color. The "None" option gets a faint outline crossed by a diagonal slash instead.
 */
@Composable
private fun FrameColorSwatch(option: FrameColorOption) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SwatchInnerCorner)
    val isNone = option.color == FrameColorNone
    val outline = scheme.onSurface.copy(alpha = 0.15f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.onSurface.copy(alpha = 0.02f), scheme.onSurface.copy(alpha = 0.1f))))
            .border(width = if (isNone) 1.dp else 3.dp, color = if (isNone) outline else option.color, shape = shape)
            // A white border on a light surface would vanish, so every border gets a hairline edge.
            .border(width = 1.dp, color = outline, shape = shape),
    ) {
        if (isNone) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = scheme.onSurface.copy(alpha = 0.35f),
                    start = Offset(size.width * 0.2f, size.height * 0.8f),
                    end = Offset(size.width * 0.8f, size.height * 0.2f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun FrameScreenPreview() {
    ThemePreviews {
        FrameContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onApply = { _, _, _, _ -> })
    }
}
