package org.example.project.ui.shapereveal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.min
import org.example.project.ui.blur.BlurLevelDefault
import org.example.project.ui.blur.BlurLevelMax
import org.example.project.ui.blur.BlurLevelMin
import org.example.project.ui.common.AccentPillButton
import org.example.project.ui.common.EditorAccent
import org.example.project.ui.common.EditorBackground
import org.example.project.ui.common.EditorCanvasBackground
import org.example.project.ui.common.EditorCircleIconButton
import org.example.project.ui.common.EditorControlBackground
import org.example.project.ui.common.RevealShape
import org.example.project.ui.common.buildShapePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.reveal.RevealEditViewModel
import org.koin.compose.viewmodel.koinViewModel

/** s-Blur: blur the whole photo, then move/scale/rotate a shape to reveal the original sharpness. */
@Composable
fun SelectiveBlurScreen(onBack: () -> Unit, onApplied: () -> Unit, modifier: Modifier = Modifier, viewModel: RevealEditViewModel = koinViewModel()) {
    ShapeRevealContent(sourceImage = viewModel.sourceImage, title = "s-Blur", effect = RevealEffect.Blur, onBack = onBack, onDone = { viewModel.applyResult(it); onApplied() }, modifier = modifier)
}

/** s-Splash: grayscale the whole photo, then move/scale/rotate a shape to reveal the original colour. */
@Composable
fun SelectiveSplashScreen(onBack: () -> Unit, onApplied: () -> Unit, modifier: Modifier = Modifier, viewModel: RevealEditViewModel = koinViewModel()) {
    ShapeRevealContent(sourceImage = viewModel.sourceImage, title = "s-Splash", effect = RevealEffect.Grayscale, onBack = onBack, onDone = { viewModel.applyResult(it); onApplied() }, modifier = modifier)
}

@Composable
private fun ShapeRevealContent(
    sourceImage: ImageBitmap?,
    title: String,
    effect: RevealEffect,
    onBack: () -> Unit,
    onDone: (ImageBitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    var placement by remember { mutableStateOf(ShapePlacement(shape = RevealShape.Circle)) }
    var blurLevel by remember { mutableIntStateOf(BlurLevelDefault) }

    val baseImage = remember(sourceImage, effect, blurLevel) {
        sourceImage?.let { buildEffectBitmap(it, effect, blurLevel) }
    }
    val revealImage = remember(sourceImage) { sourceImage?.let { copyBitmap(it) } }

    Column(modifier = modifier.fillMaxSize().background(EditorBackground).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            AccentPillButton(
                text = "Done",
                enabled = baseImage != null && revealImage != null,
                onClick = { if (baseImage != null && revealImage != null) onDone(bakeShapeReveal(baseImage, revealImage, placement)) },
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EditorCanvasBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (sourceImage != null && baseImage != null && revealImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val fitScale = min(boxWidthPx / sourceImage.width, boxHeightPx / sourceImage.height)
                val imageWidthPx = sourceImage.width * fitScale
                val imageHeightPx = sourceImage.height * fitScale
                val imageOffsetPx = Offset((boxWidthPx - imageWidthPx) / 2f, (boxHeightPx - imageHeightPx) / 2f)
                val imageSizePx = Size(imageWidthPx, imageHeightPx)
                val strokePx = with(density) { 2.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageWidthPx, imageHeightPx) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                placement = placement.copy(
                                    centerXFraction = (placement.centerXFraction + pan.x / imageWidthPx).coerceIn(0f, 1f),
                                    centerYFraction = (placement.centerYFraction + pan.y / imageHeightPx).coerceIn(0f, 1f),
                                    sizeFraction = (placement.sizeFraction * zoom).coerceIn(0.12f, 2.5f),
                                    rotationDegrees = placement.rotationDegrees + rotation,
                                )
                            }
                        },
                ) {
                    drawShapeReveal(base = baseImage, reveal = revealImage, placement = placement, imageOffset = imageOffsetPx, imageSize = imageSizePx)
                    drawShapeOutline(placement = placement, imageOffset = imageOffsetPx, imageSize = imageSizePx, color = Color.White, strokeWidthPx = strokePx)
                }
            } else {
                Text("No image to edit", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text(
            text = "Drag to move · pinch to resize · twist to rotate",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )

        if (effect == RevealEffect.Blur) {
            BlurIntensityStepper(value = blurLevel, onValueChange = { blurLevel = it.coerceIn(BlurLevelMin, BlurLevelMax) })
        }

        ShapePickerRow(selected = placement.shape, onSelected = { placement = placement.copy(shape = it) })

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ShapePickerRow(selected: RevealShape, onSelected: (RevealShape) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(RevealShape.entries) { shape ->
            val isSelected = shape == selected
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EditorControlBackground)
                    .then(if (isSelected) Modifier.border(2.dp, EditorAccent, RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { onSelected(shape) }
                    .padding(12.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val s = min(size.width, size.height)
                    val path = buildShapePath(shape, s)
                    path.translate(Offset((size.width - s) / 2f, (size.height - s) / 2f))
                    drawPath(path = path, color = if (isSelected) EditorAccent else Color.White, style = Fill)
                }
            }
        }
    }
}

@Composable
private fun BlurIntensityStepper(value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(icon = Icons.Filled.Remove, contentDescription = "Less blur", enabled = value > BlurLevelMin, onClick = { onValueChange(value - 1) })
        Box(modifier = Modifier.width(56.dp), contentAlignment = Alignment.Center) {
            Text("$value", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        StepperButton(icon = Icons.Filled.Add, contentDescription = "More blur", enabled = value < BlurLevelMax, onClick = { onValueChange(value + 1) })
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(EditorControlBackground),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f))
    }
}

@Preview
@Composable
private fun ShapeRevealPreview() {
    ThemePreviews {
        ShapeRevealContent(sourceImage = ImageBitmap(360, 480), title = "s-Splash", effect = RevealEffect.Grayscale, onBack = {}, onDone = {})
    }
}
