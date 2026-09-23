package org.example.project.ui.colorsplash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import org.example.project.ui.common.MaskBrushSizeDefault
import org.example.project.ui.common.MaskBrushSizeRange
import org.example.project.ui.common.MaskStroke
import org.example.project.ui.common.bakeMaskReveal
import org.example.project.ui.common.buildStrokePath
import org.example.project.ui.common.copyBitmap
import org.example.project.ui.common.drawMaskReveal
import org.example.project.ui.common.grayscaleBitmap
import org.example.project.ui.common.maskBrushRadiusFraction
import org.example.project.ui.common.strokeToPath
import org.example.project.ui.preview.ThemePreviews
import org.example.project.ui.reveal.RevealEditViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Color Splash (the LAS `SplashFragment` with `isSplashView=true`): the whole photo starts grayscale
 * and the user brushes to bring the original **colour** back. Same brush/reveal mechanic as the Blur
 * tool, only the base layer differs (grayscale instead of blur).
 */
@Composable
fun ColorSplashScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RevealEditViewModel = koinViewModel(),
) {
    ColorSplashContent(
        sourceImage = viewModel.sourceImage,
        onBack = onBack,
        onDone = { base, reveal, strokes ->
            viewModel.applyResult(bakeMaskReveal(base = base, reveal = reveal, strokes = strokes))
            onApplied()
        },
        modifier = modifier,
    )
}

@Composable
private fun ColorSplashContent(
    sourceImage: ImageBitmap?,
    onBack: () -> Unit,
    onDone: (base: ImageBitmap, reveal: ImageBitmap, strokes: List<MaskStroke>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(emptyList<MaskStroke>()) }
    var redoStack by remember { mutableStateOf(emptyList<MaskStroke>()) }
    var brushSize by remember { mutableFloatStateOf(MaskBrushSizeDefault) }
    var currentStroke by remember { mutableStateOf(emptyList<Offset>()) }
    var isDragging by remember { mutableStateOf(false) }
    var isAdjustingBrush by remember { mutableStateOf(false) }
    var cursorPositionPx by remember { mutableStateOf(Offset.Zero) }

    val grayImage = remember(sourceImage) { sourceImage?.let { grayscaleBitmap(it) } }
    val colorImage = remember(sourceImage) { sourceImage?.let { copyBitmap(it) } }

    Column(modifier = modifier.fillMaxSize().background(EditorBackground).safeContentPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorCircleIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", onClick = onBack)
            Text(
                text = "Splash",
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
                enabled = sourceImage != null,
                onClick = { if (grayImage != null && colorImage != null) onDone(grayImage, colorImage, strokes) },
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
            if (sourceImage != null && grayImage != null && colorImage != null) {
                val density = LocalDensity.current
                val boxWidthPx = with(density) { maxWidth.toPx() }
                val boxHeightPx = with(density) { maxHeight.toPx() }
                val fitScale = min(boxWidthPx / sourceImage.width, boxHeightPx / sourceImage.height)
                val imageWidthPx = sourceImage.width * fitScale
                val imageHeightPx = sourceImage.height * fitScale
                val imageOffsetPx = Offset((boxWidthPx - imageWidthPx) / 2f, (boxHeightPx - imageHeightPx) / 2f)
                val imageSizePx = Size(imageWidthPx, imageHeightPx)
                val brushRadiusPx = maskBrushRadiusFraction(brushSize) * imageWidthPx
                val imageCenterPx = imageOffsetPx + Offset(imageWidthPx / 2f, imageHeightPx / 2f)

                fun fractionFor(p: Offset) = Offset(
                    ((p.x - imageOffsetPx.x) / imageWidthPx).coerceIn(0f, 1f),
                    ((p.y - imageOffsetPx.y) / imageHeightPx).coerceIn(0f, 1f),
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageOffsetPx, imageWidthPx, imageHeightPx) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    isDragging = true
                                    cursorPositionPx = position
                                    currentStroke = listOf(fractionFor(position))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    if (currentStroke.isNotEmpty()) {
                                        strokes = strokes + MaskStroke(currentStroke, maskBrushRadiusFraction(brushSize))
                                        redoStack = emptyList()
                                    }
                                    currentStroke = emptyList()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    currentStroke = emptyList()
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    cursorPositionPx = change.position
                                    currentStroke = currentStroke + fractionFor(change.position)
                                },
                            )
                        },
                ) {
                    val committed = strokes.map { strokeToPath(it, imageOffsetPx, imageSizePx) }
                    val live = if (currentStroke.isNotEmpty()) {
                        listOf(buildStrokePath(currentStroke, imageOffsetPx, imageSizePx, brushRadiusPx))
                    } else {
                        emptyList()
                    }
                    drawMaskReveal(
                        base = grayImage,
                        reveal = colorImage,
                        imageOffset = imageOffsetPx,
                        imageSize = imageSizePx,
                        revealPaths = committed + live,
                    )
                    if (isDragging || isAdjustingBrush) {
                        drawCircle(
                            color = Color.White,
                            radius = brushRadiusPx,
                            center = if (isDragging) cursorPositionPx else imageCenterPx,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            } else {
                Text("No image to edit", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Text(
            text = "Brush over the photo to bring back its colour",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            EditorCircleIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                enabled = strokes.isNotEmpty(),
                onClick = {
                    val last = strokes.lastOrNull() ?: return@EditorCircleIconButton
                    redoStack = redoStack + last
                    strokes = strokes.dropLast(1)
                },
            )
            Spacer(modifier = Modifier.width(16.dp))
            EditorCircleIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                enabled = redoStack.isNotEmpty(),
                onClick = {
                    val next = redoStack.lastOrNull() ?: return@EditorCircleIconButton
                    strokes = strokes + next
                    redoStack = redoStack.dropLast(1)
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Brush Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            Text("${brushSize.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EditorAccent)
        }
        CenterFillSlider(
            value = brushSize,
            onValueChange = { brushSize = it },
            range = MaskBrushSizeRange,
            referenceValue = MaskBrushSizeRange.start,
            onDraggingChange = { isAdjustingBrush = it },
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
private fun ColorSplashScreenPreview() {
    ThemePreviews {
        ColorSplashContent(sourceImage = ImageBitmap(360, 480), onBack = {}, onDone = { _, _, _ -> })
    }
}
